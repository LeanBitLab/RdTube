package com.example.reddittube.data

import android.content.Context
import android.util.Log
import com.example.reddittube.utils.RedditOAuthHelper
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
    val numComments: Int = 0
)

// ponytail: Sealed error hierarchy for typed error handling
sealed class RedditError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkError(msg: String, cause: Throwable? = null) : RedditError(msg, cause)
    class RateLimited(retryAfter: Long) : RedditError("Rate limited by Reddit API. Retry after ${retryAfter}s")
    class NoVideosFound(subreddits: String) : RedditError("Could not retrieve videos from r/$subreddits. Check your API Client ID and connection.")
    class TokenExpired : RedditError("Access token expired. Retrying...")
    class Unknown(msg: String, cause: Throwable? = null) : RedditError(msg, cause)
}

interface DataRepository {
    fun getContext(): Context
    fun fetchRedditVideos(subreddits: String, sort: String = "hot"): Flow<List<RedditPost>>
    fun searchSubreddits(query: String): Flow<List<String>>
    fun fetchMoreVideos(subreddits: String, afterMap: Map<String, String?>, sort: String = "hot"): Flow<FetchMoreResult>
    fun getAfterMap(): Map<String, String?>
    fun saveAfterMap(map: Map<String, String?>)
}

// ponytail: batch result plus per-subreddit cursors for infinite scroll
data class FetchMoreResult(val posts: List<RedditPost>, val afterMap: Map<String, String?>)

class DefaultDataRepository(private val context: Context) : DataRepository {
    override fun getContext(): Context = context

    // ponytail: throttle to 55 req/min (limit is 60), global across all callers
    private val lastRequestTime = AtomicLong(0)
    private companion object {
        const val MIN_REQUEST_INTERVAL_MS = 1100L  // ~55 req/min
        const val MAX_RETRIES = 2
    }

    /**
     * Throttled HTTP request with 429 retry. Returns parsed JSONObject or null.
     * ponytail: single choke point for all Reddit API calls
     */
    private suspend fun performRequest(urlStr: String, token: String, timeout: Int = 15000): JSONObject? {
        for (attempt in 0 until MAX_RETRIES) {
            // throttle: wait minimum interval between requests
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime.get()
            if (elapsed < MIN_REQUEST_INTERVAL_MS) {
                delay(MIN_REQUEST_INTERVAL_MS - elapsed)
            }
            lastRequestTime.set(System.currentTimeMillis())

            val conn = URL(urlStr).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("User-Agent", RedditOAuthHelper.DEFAULT_USER_AGENT)
                conn.connectTimeout = timeout
                conn.readTimeout = timeout

                val code = conn.responseCode
                Log.i("RedditRepository", "request $urlStr → $code (attempt ${attempt + 1})")

                if (code == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val body = reader.readText()
                    reader.close()
                    return JSONObject(body)
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

    override fun searchSubreddits(query: String): Flow<List<String>> = flow {
        val token = RedditOAuthHelper.getOrFetchAccessToken(context)
        if (token == null) { emit(emptyList()); return@flow }
        val results = mutableListOf<String>()
        try {
            val url = "https://oauth.reddit.com/subreddits/search.json?q=${java.net.URLEncoder.encode(query, "UTF-8")}&limit=15"
            val json = performRequest(url, token, timeout = 10000)
            val children = json?.optJSONObject("data")?.optJSONArray("children")
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
        emit(results)
    }.flowOn(Dispatchers.IO)

    override fun fetchRedditVideos(subreddits: String, sort: String): Flow<List<RedditPost>> = flow {
        Log.i("RedditRepository", "Connecting to Reddit API using RedReader Client ID, sort=$sort")
        val list = fetchOAuthJsonVideos(subreddits, sort)
        
        if (list.isEmpty()) {
            throw RedditError.NoVideosFound(subreddits)
        }
        
        emit(list)
    }.flowOn(Dispatchers.IO)

    override fun fetchMoreVideos(subreddits: String, afterMap: Map<String, String?>, sort: String): Flow<FetchMoreResult> = flow {
        val token = RedditOAuthHelper.getOrFetchAccessToken(context) ?: run { emit(FetchMoreResult(emptyList(), afterMap)); return@flow }
        val subs = subreddits.replace(" ", "").trim().split("+").filter { it.isNotEmpty() }
        if (subs.isEmpty()) { emit(FetchMoreResult(emptyList(), afterMap)); return@flow }

        val nextAfterMap = afterMap.toMutableMap()
        val allPosts = mutableListOf<RedditPost>()
        for (sub in subs) {
            var after = afterMap[sub] ?: continue
            // ponytail: fetch up to 3 more pages per sub, stop early at 15
            for (page in 0 until 3) {
                if (allPosts.count { it.subreddit == sub } >= 15) break
                try {
                    val urlStr = "https://oauth.reddit.com/r/$sub/$sort.json?limit=25&raw_json=1&include_over_18=on" +
                        (if (after.isNotEmpty()) "&after=$after" else "")
                    val json = performRequest(urlStr, token) ?: break
                    val data = json.optJSONObject("data") ?: break
                    after = if (data.has("after") && !data.isNull("after")) data.optString("after") else break
                    val children = data.optJSONArray("children") ?: break
                    for (i in 0 until children.length()) {
                        val childData = children.getJSONObject(i).optJSONObject("data") ?: continue
                        parseRedditPost(childData)?.let { allPosts.add(it) }
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
        }
        emit(FetchMoreResult(allPosts, nextAfterMap))
    }.flowOn(Dispatchers.IO)

    // ponytail: serialize per-subreddit fetches to stay under rate limit
    private suspend fun fetchOAuthJsonVideos(subreddits: String, sort: String = "hot"): List<RedditPost> {
        val cleanSubs = subreddits.replace(" ", "").trim()
        val subs = cleanSubs.split("+").filter { it.isNotEmpty() }
        if (subs.isEmpty()) return emptyList()

        val token = RedditOAuthHelper.getOrFetchAccessToken(context) ?: return emptyList()
        
        val allLists = mutableListOf<List<RedditPost>>()
        for (sub in subs) {
            var result = performOAuthRequest(sub, token, sort)
            if (result.isEmpty()) {
                val prefs = context.getSharedPreferences("reddittube_prefs", Context.MODE_PRIVATE)
                prefs.edit().remove("reddit_access_token").remove("reddit_token_expires_at").apply()
                val freshToken = RedditOAuthHelper.getOrFetchAccessToken(context)
                if (freshToken != null) {
                    result = performOAuthRequest(sub, freshToken, sort)
                }
            }
            allLists.add(result)
        }

        val mergedList = mutableListOf<RedditPost>()
        var index = 0
        var addedAny = true
        while (addedAny) {
            addedAny = false
            for (list in allLists) {
                if (index < list.size) {
                    mergedList.add(list[index])
                    addedAny = true
                }
            }
            index++
        }
        Log.i("RedditRepository", "fetchOAuthJsonVideos returning ${mergedList.size} merged videos for $subreddits")
        return mergedList
    }

    /** per-subreddit after cursors, updated after each initial fetch */
    private val _afterMap = mutableMapOf<String, String?>()

    override fun getAfterMap(): Map<String, String?> = _afterMap.toMap()

    override fun saveAfterMap(map: Map<String, String?>) {
        _afterMap.putAll(map)
    }

    // ponytail: shared parser — extracts RedditPost (with thumbnail + comment count) from a child "data" object
    private fun parseRedditPost(childData: JSONObject): RedditPost? {
        var isVideo = childData.optBoolean("is_video", false)
        var redditVideo = childData.optJSONObject("media")?.optJSONObject("reddit_video")
        if (redditVideo == null) {
            val preview = childData.optJSONObject("preview")
            redditVideo = preview?.optJSONObject("reddit_video_preview")
            if (redditVideo != null) isVideo = true
        }
        if (!(isVideo && redditVideo != null)) return null

        val fallbackUrl = redditVideo.optString("fallback_url").replace("&amp;", "&")
        val dashUrl = redditVideo.optString("dash_url").replace("&amp;", "&")
        val hlsUrl = redditVideo.optString("hls_url").replace("&amp;", "&")
        val videoUrl = if (fallbackUrl.isNotEmpty()) fallbackUrl else if (hlsUrl.isNotEmpty()) hlsUrl else dashUrl
        if (videoUrl.isEmpty()) return null

        var thumb = childData.optString("thumbnail", "")
        if (!thumb.startsWith("http")) {
            thumb = childData.optJSONObject("preview")
                ?.optJSONArray("images")
                ?.optJSONObject(0)
                ?.optJSONObject("source")
                ?.optString("url", "")
                ?.replace("&amp;", "&") ?: ""
        }

        return RedditPost(
            id = childData.optString("id"),
            title = childData.optString("title"),
            subreddit = childData.optString("subreddit"),
            author = childData.optString("author"),
            score = childData.optInt("score"),
            permalink = childData.optString("permalink"),
            videoUrl = videoUrl,
            fallbackUrl = fallbackUrl,
            dashUrl = dashUrl,
            hlsUrl = hlsUrl,
            thumbnailUrl = thumb,
            numComments = childData.optInt("num_comments")
        )
    }

    private suspend fun performOAuthRequest(subreddit: String, token: String, sort: String = "hot"): List<RedditPost> {
        val list = mutableListOf<RedditPost>()
        var after: String? = null
        for (page in 0 until 3) {
            if (list.size >= 15) break
            try {
                val urlStr = "https://oauth.reddit.com/r/$subreddit/$sort.json?limit=25&raw_json=1&include_over_18=on" +
                    (if (after != null) "&after=$after" else "")
                // ponytail: throttled request
                val json = performRequest(urlStr, token) ?: break

                val data = json.optJSONObject("data") ?: break
                after = if (data.has("after") && !data.isNull("after")) data.optString("after") else null
                val children = data.optJSONArray("children")
                if (children == null || children.length() == 0) break

                for (i in 0 until children.length()) {
                    val childData = children.getJSONObject(i).optJSONObject("data") ?: continue
                    parseRedditPost(childData)?.let { list.add(it) }
                }
            } catch (e: RedditError.RateLimited) {
                Log.w("RedditRepository", "r/$subreddit rate limited on page $page")
                break
            } catch (e: Exception) {
                Log.e("RedditRepository", "r/$subreddit page $page error: ${e.message}")
                break
            }
        }
        _afterMap[subreddit] = after
        Log.i("RedditRepository", "r/$subreddit parsed ${list.size} videos, after=$after")
        return list
    }
}
