package com.lean.reddittube.data

import android.content.Context
import android.util.Log
import com.lean.reddittube.utils.RedditOAuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

// ponytail: Simplified representation of Reddit posts containing video elements. Enforces OAuth.
@androidx.compose.runtime.Immutable
data class RedditPost(
    val id: String,
    val title: String,
    val subreddit: String,
    val author: String,
    val score: Int,
    val permalink: String,
    val videoUrl: String,
    val fallbackUrl: String,
    val dashUrl: String,
    val hlsUrl: String,
    val thumbnailUrl: String = "",
    val numComments: Int = 0,
    val duration: Int = 0,
    val over18: Boolean = false
)

@androidx.compose.runtime.Immutable
data class RedditComment(
    val id: String,
    val author: String,
    val body: String,
    val score: Int,
    val createdUtc: Long
)

// ponytail: Sealed error hierarchy for typed error handling
sealed class RedditError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkError(msg: String, cause: Throwable? = null) : RedditError(msg, cause)
    class RateLimited(retryAfter: Long) : RedditError("Rate limited by Reddit API. Retry after ${retryAfter}s")
    class NoVideosFound(subreddits: String) : RedditError("Could not retrieve videos from r/$subreddits. Check your API Client ID and connection.")
    class TokenExpired : RedditError("Access token expired. Retrying...")
    class Unknown(msg: String, cause: Throwable? = null) : RedditError(msg, cause)
}

data class SearchVideosResult(val posts: List<RedditPost>, val after: String?)
data class SearchSubredditsResult(val subreddits: List<String>, val after: String?)

interface DataRepository {
    fun getContext(): Context
    fun fetchRedditVideos(subreddits: String, sort: String = "hot", feed: String = "explore"): Flow<List<RedditPost>>
    fun searchSubreddits(query: String): Flow<List<String>>
    fun searchSubredditsPaged(query: String, after: String? = null): Flow<SearchSubredditsResult>
    fun searchRedditVideos(query: String, sort: String = "relevance"): Flow<List<RedditPost>>
    fun searchRedditVideosPaged(query: String, sort: String = "relevance", after: String? = null): Flow<SearchVideosResult>
    fun fetchMoreVideos(subreddits: String, afterMap: Map<String, String?>, sort: String = "hot", feed: String = "explore"): Flow<FetchMoreResult>
    fun fetchPostComments(subreddit: String, postId: String): Flow<List<RedditComment>>
    fun getAfterMap(feed: String = "explore"): Map<String, String?>
    fun saveAfterMap(map: Map<String, String?>, feed: String = "explore")
}

// ponytail: batch result plus per-subreddit cursors for infinite scroll
data class FetchMoreResult(val posts: List<RedditPost>, val afterMap: Map<String, String?>)

class DefaultDataRepository(private val context: Context) : DataRepository {
    override fun getContext(): Context = context

    // ponytail: throttle to 55 req/min (limit is 60), global across all callers
    private val lastRequestTime = AtomicLong(0)
    private val requestMutex = Mutex()
    private companion object {
        const val MIN_REQUEST_INTERVAL_MS = 1100L  // ~55 req/min
        const val MAX_RETRIES = 2
    }

    /**
     * Throttled HTTP request with 429 retry. Returns raw response body string or null.
     */
    private suspend fun performRawRequest(urlStr: String, token: String, timeout: Int = 15000): String? {
        for (attempt in 0 until MAX_RETRIES) {
            requestMutex.withLock {
                val now = System.currentTimeMillis()
                val elapsed = now - lastRequestTime.get()
                if (elapsed < MIN_REQUEST_INTERVAL_MS) {
                    delay(MIN_REQUEST_INTERVAL_MS - elapsed)
                }
                lastRequestTime.set(System.currentTimeMillis())
            }

            val conn = URL(urlStr).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("User-Agent", RedditOAuthHelper.getUserAgent(context))
                conn.setRequestProperty("Connection", "keep-alive")
                conn.connectTimeout = timeout
                conn.readTimeout = timeout

                val code = conn.responseCode
                Log.i("RedditRepository", "request $urlStr → $code (attempt ${attempt + 1})")

                if (code == 200) {
                    val stream = if ("gzip".equals(conn.contentEncoding, ignoreCase = true)) {
                        java.util.zip.GZIPInputStream(conn.inputStream)
                    } else {
                        conn.inputStream
                    }
                    return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }

                if (code == 429) {
                    val retryAfter = conn.getHeaderField("Retry-After")?.toLongOrNull() ?: 60L
                    Log.w("RedditRepository", "429 rate limited, Retry-After=${retryAfter}s")
                    if (attempt < MAX_RETRIES - 1) {
                        delay(retryAfter * 1000)
                        continue
                    }
                    throw RedditError.RateLimited(retryAfter)
                }

                // ponytail: non-200 non-429 → don't retry, caller handles
                Log.w("RedditRepository", "HTTP $code for $urlStr")
                return null
            } catch (e: RedditError.RateLimited) {
                throw e
            } catch (e: Exception) {
                Log.e("RedditRepository", "request error: ${e.message}")
                return null
            } finally {
                conn.disconnect()
            }
        }
        return null
    }

    /**
     * Throttled HTTP request with 429 retry. Returns parsed JSONObject or null.
     * ponytail: single choke point for all Reddit API calls
     */
    private suspend fun performRequest(urlStr: String, token: String, timeout: Int = 15000): JSONObject? {
        val raw = performRawRequest(urlStr, token, timeout) ?: return null
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            null
        }
    }

    override fun fetchPostComments(subreddit: String, postId: String): Flow<List<RedditComment>> = flow {
        val token = RedditOAuthHelper.getOrFetchAccessToken(context)
        if (token == null) { emit(emptyList()); return@flow }
        val comments = mutableListOf<RedditComment>()
        try {
            val url = "https://oauth.reddit.com/r/$subreddit/comments/$postId.json?limit=100&sort=top&raw_json=1"
            val raw = performRawRequest(url, token, timeout = 12000)
            if (raw != null) {
                val jsonArray = org.json.JSONArray(raw)
                if (jsonArray.length() > 1) {
                    val commentListing = jsonArray.getJSONObject(1)
                    val dataObj = commentListing.optJSONObject("data")
                    val children = dataObj?.optJSONArray("children")
                    if (children != null) {
                        for (i in 0 until children.length()) {
                            val child = children.getJSONObject(i)
                            val kind = child.optString("kind", "")
                            if (kind == "t1") {
                                val cData = child.optJSONObject("data") ?: continue
                                val body = cData.optString("body", "").trim()
                                if (body.isNotEmpty() && body != "[deleted]" && body != "[removed]") {
                                    comments.add(
                                        RedditComment(
                                            id = cData.optString("id"),
                                            author = cData.optString("author", "[deleted]"),
                                            body = body,
                                            score = cData.optInt("score", 0),
                                            createdUtc = cData.optLong("created_utc", 0L)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            comments.sortByDescending { it.score }
        } catch (e: Exception) {
            Log.e("RedditRepository", "fetchPostComments error: ${e.message}")
        }
        emit(comments)
    }.flowOn(Dispatchers.IO)

    override fun searchSubreddits(query: String): Flow<List<String>> = flow {
        searchSubredditsPaged(query, null).collect { emit(it.subreddits) }
    }

    override fun searchSubredditsPaged(query: String, after: String?): Flow<SearchSubredditsResult> = flow {
        val token = RedditOAuthHelper.getOrFetchAccessToken(context)
        if (token == null) { emit(SearchSubredditsResult(emptyList(), null)); return@flow }
        val results = mutableListOf<String>()
        var nextAfter: String? = null
        try {
            val url = "https://oauth.reddit.com/subreddits/search.json?q=${java.net.URLEncoder.encode(query, "UTF-8")}&limit=25&include_over_18=on" +
                (if (after != null) "&after=$after" else "")
            val json = performRequest(url, token, timeout = 10000)
            val data = json?.optJSONObject("data")
            nextAfter = if (data != null && data.has("after") && !data.isNull("after")) data.optString("after").ifEmpty { null } else null
            val children = data?.optJSONArray("children")
            if (children != null) {
                for (i in 0 until children.length()) {
                    val name = children.getJSONObject(i).optJSONObject("data")?.optString("display_name", "")?.ifEmpty { null }
                    if (name != null) results.add(name)
                }
            }
        } catch (e: RedditError.RateLimited) {
            Log.w("RedditRepository", "search rate limited: ${e.message}")
        } catch (e: Exception) {
            Log.e("RedditRepository", "search error: ${e.message}")
        }
        emit(SearchSubredditsResult(results, nextAfter))
    }.flowOn(Dispatchers.IO)

    override fun searchRedditVideos(query: String, sort: String): Flow<List<RedditPost>> = flow {
        searchRedditVideosPaged(query, sort, null).collect { emit(it.posts) }
    }

    override fun searchRedditVideosPaged(query: String, sort: String, after: String?): Flow<SearchVideosResult> = flow {
        val token = RedditOAuthHelper.getOrFetchAccessToken(context)
        if (token == null) { emit(SearchVideosResult(emptyList(), null)); return@flow }
        val results = mutableListOf<RedditPost>()
        var currentAfter = after
        var finalAfter: String? = null
        try {
            for (page in 0 until 2) {
                val url = "https://oauth.reddit.com/search.json?q=${java.net.URLEncoder.encode(query, "UTF-8")}&type=link&sort=$sort&limit=100&raw_json=1&include_over_18=on" +
                    (if (currentAfter != null) "&after=$currentAfter" else "")
                val json = performRequest(url, token, timeout = 10000) ?: break
                val data = json.optJSONObject("data") ?: break
                val newAfter = if (data.has("after") && !data.isNull("after")) data.optString("after").ifEmpty { null } else null
                currentAfter = newAfter
                if (newAfter != null) {
                    finalAfter = newAfter
                }
                val children = data.optJSONArray("children") ?: break
                for (i in 0 until children.length()) {
                    val childData = children.getJSONObject(i).optJSONObject("data") ?: continue
                    parseRedditPost(childData)?.let { post ->
                        if (results.none { it.id == post.id }) {
                            results.add(post)
                        }
                    }
                }
                if (results.size >= 20 || currentAfter == null) break
            }
        } catch (e: RedditError.RateLimited) {
            Log.w("RedditRepository", "video search rate limited: ${e.message}")
        } catch (e: Exception) {
            Log.e("RedditRepository", "video search error: ${e.message}")
        }
        emit(SearchVideosResult(results, finalAfter))
    }.flowOn(Dispatchers.IO)

    override fun fetchRedditVideos(subreddits: String, sort: String, feed: String): Flow<List<RedditPost>> = flow {
        Log.i("RedditRepository", "Connecting to Reddit API using RedReader Client ID, sort=$sort, feed=$feed")
        val list = fetchOAuthJsonVideos(subreddits, sort, feed)
        
        if (list.isEmpty()) {
            throw RedditError.NoVideosFound(subreddits)
        }
        
        emit(list)
    }.flowOn(Dispatchers.IO)

    override fun fetchMoreVideos(subreddits: String, afterMap: Map<String, String?>, sort: String, feed: String): Flow<FetchMoreResult> = flow {
        val token = RedditOAuthHelper.getOrFetchAccessToken(context) ?: run { emit(FetchMoreResult(emptyList(), afterMap)); return@flow }
        val subs = subreddits.replace(" ", "").trim().split("+").filter { it.isNotEmpty() }
        if (subs.isEmpty()) { emit(FetchMoreResult(emptyList(), afterMap)); return@flow }

        val nextAfterMap = java.util.concurrent.ConcurrentHashMap<String, String?>(afterMap)
        val allPosts = coroutineScope {
            subs.map { sub ->
                async(Dispatchers.IO) {
                    val subPosts = mutableListOf<RedditPost>()
                    var after = afterMap[sub] ?: ""
                    for (page in 0 until 5) {
                        if (subPosts.size >= 15) break
                        try {
                            val sortPath = if (sort.contains("?")) sort.substringBefore("?") else sort
                            val sortExtra = if (sort.contains("?")) "&" + sort.substringAfter("?") else ""
                            val urlStr = "https://oauth.reddit.com/r/$sub/$sortPath.json?limit=25&raw_json=1&include_over_18=on$sortExtra" +
                                (if (after.isNotEmpty()) "&after=$after" else "")
                            val json = performRequest(urlStr, token) ?: break
                            val data = json.optJSONObject("data") ?: break
                            after = if (data.has("after") && !data.isNull("after")) data.optString("after") else break
                            val children = data.optJSONArray("children") ?: break
                            for (i in 0 until children.length()) {
                                val childData = children.getJSONObject(i).optJSONObject("data") ?: continue
                                parseRedditPost(childData)?.let { post ->
                                    if (subPosts.none { it.id == post.id }) {
                                        subPosts.add(post)
                                    }
                                }
                            }
                        } catch (e: RedditError.RateLimited) {
                            Log.w("RedditRepository", "fetchMore r/$sub rate limited, stopping")
                            break
                        } catch (e: Exception) {
                            Log.e("RedditRepository", "fetchMore r/$sub page $page error: ${e.message}")
                            break
                        }
                    }
                    nextAfterMap[sub] = after
                    subPosts
                }
            }.awaitAll().flatten().distinctBy { it.id }
        }
        emit(FetchMoreResult(allPosts, nextAfterMap))
    }.flowOn(Dispatchers.IO)

    // ponytail: parallel per-subreddit fetches using coroutines async for 3x-5x speedup
    private suspend fun fetchOAuthJsonVideos(subreddits: String, sort: String = "hot", feed: String = "explore"): List<RedditPost> = coroutineScope {
        val cleanSubs = subreddits.replace(" ", "").trim()
        val subs = cleanSubs.split("+").filter { it.isNotEmpty() }
        if (subs.isEmpty()) return@coroutineScope emptyList()

        val token = RedditOAuthHelper.getOrFetchAccessToken(context)
            ?: throw RedditError.Unknown("Failed to authenticate with Reddit. Check your connection.")
        
        val deferredLists = subs.map { sub ->
            async(Dispatchers.IO) {
                var result = performOAuthRequest(sub, token, sort, feed)
                if (result.isEmpty()) {
                    val prefs = context.getSharedPreferences("rdtube_prefs", Context.MODE_PRIVATE)
                    prefs.edit().remove("reddit_access_token").remove("reddit_token_expires_at").apply()
                    val freshToken = RedditOAuthHelper.getOrFetchAccessToken(context)
                    if (freshToken != null) {
                        result = performOAuthRequest(sub, freshToken, sort, feed)
                    }
                }
                result
            }
        }
        val allLists = deferredLists.awaitAll()

        val mergedList = mutableListOf<RedditPost>()
        val seenIds = mutableSetOf<String>()
        var index = 0
        var addedAny = true
        while (addedAny) {
            addedAny = false
            for (list in allLists) {
                if (index < list.size) {
                    val post = list[index]
                    if (seenIds.add(post.id)) {
                        mergedList.add(post)
                    }
                    addedAny = true
                }
            }
            index++
        }
        Log.i("RedditRepository", "fetchOAuthJsonVideos returning ${mergedList.size} merged videos for $subreddits")
        mergedList
    }

    // ponytail: per-feed after cursors (explore + subscribed kept separate so a sub in both feeds doesn't clobber)
    private val afterMaps = mutableMapOf<String, MutableMap<String, String?>>().apply {
        put("explore", mutableMapOf())
        put("subscribed", mutableMapOf())
    }

    private fun feedMap(feed: String): MutableMap<String, String?> = afterMaps.getOrPut(feed) { mutableMapOf() }

    override fun getAfterMap(feed: String): Map<String, String?> = feedMap(feed).toMap()

    override fun saveAfterMap(map: Map<String, String?>, feed: String) {
        feedMap(feed).putAll(map)
    }

    private fun parseRedditPost(childData: JSONObject): RedditPost? {
        val targetData = childData.optJSONArray("crosspost_parent_list")?.optJSONObject(0) ?: childData

        val isVideo = childData.optBoolean("is_video", false) || targetData.optBoolean("is_video", false)
        val domain = childData.optString("domain", "")
        val url = childData.optString("url", "")
        val postHint = childData.optString("post_hint", "")

        Log.i("RedditRepository", "Post debug: title='${childData.optString("title").take(20)}', domain='$domain', postHint='$postHint', url='$url', isVideo=$isVideo, hasMedia=${childData.has("media")}, hasSecureMedia=${childData.has("secure_media")}, hasPreview=${childData.has("preview")}")

        val isVideoCandidate = isVideo ||
            postHint == "hosted:video" || postHint == "rich:video" ||
            domain.contains("v.redd.it") || domain.contains("gfycat") || domain.contains("imgur") || domain.contains("redgifs") ||
            url.endsWith(".mp4") || childData.has("media") || childData.has("secure_media") || childData.has("preview") ||
            targetData.has("media") || targetData.has("secure_media") || targetData.has("preview")

        if (!isVideoCandidate) return null

        var redditVideo = targetData.optJSONObject("secure_media")?.optJSONObject("reddit_video")
        if (redditVideo == null) redditVideo = targetData.optJSONObject("media")?.optJSONObject("reddit_video")
        if (redditVideo == null) redditVideo = targetData.optJSONObject("preview")?.optJSONObject("reddit_video_preview")
        if (redditVideo == null) redditVideo = childData.optJSONObject("secure_media")?.optJSONObject("reddit_video")
        if (redditVideo == null) redditVideo = childData.optJSONObject("media")?.optJSONObject("reddit_video")
        if (redditVideo == null) redditVideo = childData.optJSONObject("preview")?.optJSONObject("reddit_video_preview")

        var fallbackUrl = ""
        var dashUrl = ""
        var hlsUrl = ""
        var duration = 0

        if (redditVideo != null) {
            fallbackUrl = redditVideo.optString("fallback_url").replace("&amp;", "&")
            dashUrl = redditVideo.optString("dash_url").replace("&amp;", "&")
            hlsUrl = redditVideo.optString("hls_url").replace("&amp;", "&")
            duration = redditVideo.optInt("duration", 0)
        } else {
            val mp4Variant = (targetData.optJSONObject("preview") ?: childData.optJSONObject("preview"))
                ?.optJSONArray("images")
                ?.optJSONObject(0)
                ?.optJSONObject("variants")
                ?.optJSONObject("mp4")
                ?.optJSONObject("source")
                ?.optString("url", "")
                ?.replace("&amp;", "&") ?: ""

            if (mp4Variant.isNotEmpty()) {
                fallbackUrl = mp4Variant
            } else {
                val directUrl = childData.optString("url_overridden_by_dest", childData.optString("url", ""))
                if (directUrl.endsWith(".mp4", ignoreCase = true)) {
                    fallbackUrl = directUrl
                }
            }
        }

        val videoUrl = if (fallbackUrl.isNotEmpty()) fallbackUrl else if (hlsUrl.isNotEmpty()) hlsUrl else dashUrl
        if (videoUrl.isEmpty()) return null

        var thumb = childData.optString("thumbnail", "")
        if (thumb == "nsfw" || thumb == "default" || thumb == "spoiler" || !thumb.startsWith("http")) {
            thumb = (targetData.optJSONObject("preview") ?: childData.optJSONObject("preview"))
                ?.optJSONArray("images")
                ?.optJSONObject(0)
                ?.optJSONObject("source")
                ?.optString("url", "")
                ?.replace("&amp;", "&") ?: ""
        }

        val isOver18 = childData.optBoolean("over_18", false) || targetData.optBoolean("over_18", false)
        val nsfwAllowed = context.getSharedPreferences("rdtube_prefs", Context.MODE_PRIVATE).getBoolean("pref_unrestricted_nsfw", true)
        if (isOver18 && !nsfwAllowed) return null

        return RedditPost(
            id = childData.optString("id"),
            title = childData.optString("title"),
            subreddit = childData.optString("subreddit"),
            author = "",
            score = childData.optInt("score"),
            permalink = childData.optString("permalink"),
            videoUrl = videoUrl,
            fallbackUrl = fallbackUrl,
            dashUrl = dashUrl,
            hlsUrl = hlsUrl,
            thumbnailUrl = thumb,
            numComments = childData.optInt("num_comments"),
            duration = duration,
            over18 = isOver18
        )
    }

    private suspend fun performOAuthRequest(subreddit: String, token: String, sort: String = "hot", feed: String = "explore"): List<RedditPost> {
        val list = mutableListOf<RedditPost>()
        var after: String? = null
        val sortPath = if (sort.contains("?")) sort.substringBefore("?") else sort
        val sortExtra = if (sort.contains("?")) "&" + sort.substringAfter("?") else ""
        for (page in 0 until 5) {
            if (list.size >= 25) break
            try {
                val urlStr = "https://oauth.reddit.com/r/$subreddit/$sortPath.json?limit=50&raw_json=1&include_over_18=on$sortExtra" +
                    (if (after != null) "&after=$after" else "")
                // ponytail: throttled request
                val json = performRequest(urlStr, token) ?: break

                val data = json.optJSONObject("data") ?: break
                after = if (data.has("after") && !data.isNull("after")) data.optString("after") else null
                val children = data.optJSONArray("children")
                if (children == null || children.length() == 0) break

                for (i in 0 until children.length()) {
                    val childData = children.getJSONObject(i).optJSONObject("data") ?: continue
                    parseRedditPost(childData)?.let { post ->
                        if (list.none { it.id == post.id }) {
                            list.add(post)
                        }
                    }
                }
            } catch (e: RedditError.RateLimited) {
                Log.w("RedditRepository", "r/$subreddit rate limited on page $page")
                break
            } catch (e: Exception) {
                Log.e("RedditRepository", "r/$subreddit page $page error: ${e.message}")
                break
            }
        }

        // Fallback: If anonymous OAuth returned 0 videos, try public JSON endpoint
        if (list.isEmpty()) {
            try {
                val publicUrl = "https://www.reddit.com/r/$subreddit/$sortPath.json?limit=50&raw_json=1&include_over_18=on$sortExtra"
                val rawJson = performRawPublicRequest(publicUrl)
                if (rawJson != null) {
                    val json = JSONObject(rawJson)
                    val children = json.optJSONObject("data")?.optJSONArray("children")
                    if (children != null) {
                        for (i in 0 until children.length()) {
                            val childData = children.getJSONObject(i).optJSONObject("data") ?: continue
                            parseRedditPost(childData)?.let { post ->
                                if (list.none { it.id == post.id }) {
                                    list.add(post)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("RedditRepository", "r/$subreddit public fallback error: ${e.message}")
            }
        }

        // ponytail: server-side video filtering for text-heavy subreddits
        if (list.size < 8) {
            try {
                val searchUrl = "https://oauth.reddit.com/r/$subreddit/search.json?q=site:v.redd.it&restrict_sr=1&sort=$sortPath&limit=50&raw_json=1&include_over_18=on$sortExtra"
                val json = performRequest(searchUrl, token)
                val data = json?.optJSONObject("data")
                val children = data?.optJSONArray("children")
                if (children != null) {
                    for (i in 0 until children.length()) {
                        val childData = children.getJSONObject(i).optJSONObject("data") ?: continue
                        val post = parseRedditPost(childData)
                        if (post != null && list.none { it.id == post.id }) {
                            list.add(post)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("RedditRepository", "r/$subreddit site:v.redd.it search fallback failed: ${e.message}")
            }
        }

        feedMap(feed)[subreddit] = after
        Log.i("RedditRepository", "r/$subreddit parsed ${list.size} videos, after=$after")
        return list
    }

    private suspend fun performRawPublicRequest(urlStr: String, timeout: Int = 12000): String? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", RedditOAuthHelper.getUserAgent(context))
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = timeout
            conn.readTimeout = timeout

            if (conn.responseCode == 200) {
                val stream = if ("gzip".equals(conn.contentEncoding, ignoreCase = true)) {
                    java.util.zip.GZIPInputStream(conn.inputStream)
                } else {
                    conn.inputStream
                }
                return@withContext stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        } catch (e: Exception) {
            Log.w("RedditRepository", "performRawPublicRequest failed for $urlStr: ${e.message}")
        }
        null
    }
}
