package com.example.reddittube.data

import android.content.Context
import android.util.Log
import com.example.reddittube.utils.RedditOAuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// ponytail: Simplified representation of Reddit posts containing video elements. Enforces OAuth.
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
    val hlsUrl: String
)

interface DataRepository {
    fun getContext(): Context
    fun fetchRedditVideos(subreddits: String): Flow<List<RedditPost>>
    fun searchSubreddits(query: String): Flow<List<String>>
    fun fetchMoreVideos(subreddits: String, afterMap: Map<String, String?>): Flow<FetchMoreResult>
    fun getAfterMap(): Map<String, String?>
    fun saveAfterMap(map: Map<String, String?>)
}

// ponytail: batch result plus per-subreddit cursors for infinite scroll
data class FetchMoreResult(val posts: List<RedditPost>, val afterMap: Map<String, String?>)

class DefaultDataRepository(private val context: Context) : DataRepository {
    override fun getContext(): Context = context

    override fun searchSubreddits(query: String): Flow<List<String>> = flow {
        val token = RedditOAuthHelper.getOrFetchAccessToken(context)
        if (token == null) { emit(emptyList()); return@flow }
        val results = mutableListOf<String>()
        try {
            val url = URL("https://oauth.reddit.com/subreddits/search.json?q=${java.net.URLEncoder.encode(query, "UTF-8")}&limit=15")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("User-Agent", RedditOAuthHelper.getUserAgent(context))
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val json = JSONObject(reader.readText())
                reader.close()
                val children = json.optJSONObject("data")?.optJSONArray("children")
                if (children != null) {
                    for (i in 0 until children.length()) {
                        val name = children.getJSONObject(i).optJSONObject("data")?.optString("display_name", "")?.ifEmpty { null }
                        if (name != null) results.add(name)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RedditRepository", "search error: ${e.message}")
        }
        emit(results)
    }.flowOn(Dispatchers.IO)

    override fun fetchRedditVideos(subreddits: String): Flow<List<RedditPost>> = flow {
        val clientId = RedditOAuthHelper.getClientId(context)
        if (clientId.isEmpty()) {
            throw Exception("Reddit API Client ID is not configured. Tap the gear icon in the top right to configure your API setup.")
        }

        Log.i("RedditRepository", "Connecting to Reddit API using Client ID...")
        val list = fetchOAuthJsonVideos(subreddits)
        
        if (list.isEmpty()) {
            throw Exception("Could not retrieve videos from r/$subreddits. Check your API Client ID and connection.")
        }
        
        emit(list)
    }.flowOn(Dispatchers.IO)

    override fun fetchMoreVideos(subreddits: String, afterMap: Map<String, String?>): Flow<FetchMoreResult> = flow {
        val token = RedditOAuthHelper.getOrFetchAccessToken(context) ?: run { emit(FetchMoreResult(emptyList(), afterMap)); return@flow }
        val subs = subreddits.replace(" ", "").trim().split("+").filter { it.isNotEmpty() }
        if (subs.isEmpty()) { emit(FetchMoreResult(emptyList(), afterMap)); return@flow }

        val nextAfterMap = afterMap.toMutableMap()
        val allPosts = mutableListOf<RedditPost>()
        for (sub in subs) {
            val cursor = afterMap[sub]
            if (cursor == null) continue  // no more pages for this sub
            var after: String? = cursor
            // fetch 1 additional page per sub
            try {
                val urlStr = "https://oauth.reddit.com/r/$sub/hot.json?limit=25&raw_json=1&include_over_18=on" +
                    (if (after.isNotEmpty()) "&after=$after" else "")
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("User-Agent", RedditOAuthHelper.getUserAgent(context))
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val json = JSONObject(reader.readText())
                    reader.close()
                    val data = json.optJSONObject("data") ?: continue
                    after = data.optString("after", null)
                    val children = data.optJSONArray("children") ?: continue
                    for (i in 0 until children.length()) {
                        val childData = children.getJSONObject(i).optJSONObject("data") ?: continue
                        var isVideo = childData.optBoolean("is_video", false)
                        var redditVideo = childData.optJSONObject("media")?.optJSONObject("reddit_video")
                        if (redditVideo == null) {
                            val preview = childData.optJSONObject("preview")
                            redditVideo = preview?.optJSONObject("reddit_video_preview")
                            if (redditVideo != null) isVideo = true
                        }
                        if (isVideo && redditVideo != null) {
                            val videoUrl = redditVideo.optString("fallback_url").replace("&amp;", "&")
                            if (videoUrl.isNotEmpty()) {
                                allPosts.add(RedditPost(
                                    id = childData.optString("id"),
                                    title = childData.optString("title"),
                                    subreddit = childData.optString("subreddit"),
                                    author = childData.optString("author"),
                                    score = childData.optInt("score"),
                                    permalink = childData.optString("permalink"),
                                    videoUrl = videoUrl,
                                    fallbackUrl = videoUrl,
                                    dashUrl = redditVideo.optString("dash_url").replace("&amp;", "&"),
                                    hlsUrl = redditVideo.optString("hls_url").replace("&amp;", "&")
                                ))
                            }
                        }
                    }
                    nextAfterMap[sub] = after  // null = no more pages
                } else {
                    nextAfterMap[sub] = null
                }
            } catch (e: Exception) {
                Log.e("RedditRepository", "fetchMore r/$sub error: ${e.message}")
                nextAfterMap[sub] = null
            }
        }
        emit(FetchMoreResult(allPosts, nextAfterMap))
    }.flowOn(Dispatchers.IO)

    private suspend fun fetchOAuthJsonVideos(subreddits: String): List<RedditPost> {
        val cleanSubs = subreddits.replace(" ", "").trim()
        val subs = cleanSubs.split("+").filter { it.isNotEmpty() }
        if (subs.isEmpty()) return emptyList()

        val token = RedditOAuthHelper.getOrFetchAccessToken(context) ?: return emptyList()
        
        val lists = coroutineScope {
            subs.map { sub ->
                async {
                    var result = performOAuthRequest(sub, token)
                    if (result.isEmpty()) {
                        val prefs = context.getSharedPreferences("reddittube_prefs", Context.MODE_PRIVATE)
                        prefs.edit().remove("reddit_access_token").remove("reddit_token_expires_at").apply()
                        val freshToken = RedditOAuthHelper.getOrFetchAccessToken(context)
                        if (freshToken != null) {
                            result = performOAuthRequest(sub, freshToken)
                        }
                    }
                    result
                }
            }.awaitAll()
        }

        val mergedList = mutableListOf<RedditPost>()
        var index = 0
        var addedAny = true
        while (addedAny) {
            addedAny = false
            for (list in lists) {
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

    private fun performOAuthRequest(subreddit: String, token: String): List<RedditPost> {
        val list = mutableListOf<RedditPost>()
        var after: String? = null
        for (page in 0 until 3) {
            if (list.size >= 15) break
            try {
                val urlStr = "https://oauth.reddit.com/r/$subreddit/hot.json?limit=25&raw_json=1&include_over_18=on" +
                    (if (after != null) "&after=$after" else "")
                val connection = URL(urlStr).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("User-Agent", RedditOAuthHelper.getUserAgent(context))
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                Log.i("RedditRepository", "r/$subreddit page $page returned code: ${connection.responseCode}")
                if (connection.responseCode != 200) break

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val jsonObject = JSONObject(response)
                val data = jsonObject.optJSONObject("data") ?: break
                after = data.optString("after", null)
                val children = data.optJSONArray("children")
                if (children == null || children.length() == 0) break

                for (i in 0 until children.length()) {
                    val childData = children.getJSONObject(i).optJSONObject("data") ?: continue
                    var isVideo = childData.optBoolean("is_video", false)
                    val media = childData.optJSONObject("media")
                    var redditVideo = media?.optJSONObject("reddit_video")

                    if (redditVideo == null) {
                        val preview = childData.optJSONObject("preview")
                        redditVideo = preview?.optJSONObject("reddit_video_preview")
                        if (redditVideo != null) isVideo = true
                    }

                    if (isVideo && redditVideo != null) {
                        val fallbackUrl = redditVideo.optString("fallback_url").replace("&amp;", "&")
                        val dashUrl = redditVideo.optString("dash_url").replace("&amp;", "&")
                        val hlsUrl = redditVideo.optString("hls_url").replace("&amp;", "&")
                        val id = childData.optString("id")
                        val title = childData.optString("title")
                        val subName = childData.optString("subreddit")
                        val author = childData.optString("author")
                        val score = childData.optInt("score")
                        val permalink = childData.optString("permalink")

                        val videoUrl = if (fallbackUrl.isNotEmpty()) fallbackUrl else if (hlsUrl.isNotEmpty()) hlsUrl else dashUrl
                        if (videoUrl.isNotEmpty()) {
                            list.add(RedditPost(id, title, subName, author, score, permalink, videoUrl, fallbackUrl, dashUrl, hlsUrl))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RedditRepository", "r/$subreddit page $page error: ${e.message}")
                break
            }
        }
        // store the final after cursor for this subreddit
        _afterMap[subreddit] = after
        Log.i("RedditRepository", "r/$subreddit parsed ${list.size} videos, after=$after")
        return list
    }
}
