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
    fun fetchRedditVideos(subreddits: String): Flow<List<RedditPost>>
}

class DefaultDataRepository(private val context: Context) : DataRepository {

    override fun fetchRedditVideos(subreddits: String): Flow<List<RedditPost>> = flow {
        val clientId = RedditOAuthHelper.getClientId(context)
        if (clientId.isEmpty()) {
            throw Exception("Reddit API Client ID is not configured. Tap the gear icon in the top right to configure your API setup.")
        }

        Log.d("RedditRepository", "Connecting to Reddit API using Client ID...")
        val list = fetchOAuthJsonVideos(subreddits)
        
        if (list.isEmpty()) {
            throw Exception("Could not retrieve videos from r/$subreddits. Check your API Client ID and connection.")
        }
        
        emit(list)
    }.flowOn(Dispatchers.IO)

    private suspend fun fetchOAuthJsonVideos(subreddits: String): List<RedditPost> {
        val cleanSubs = subreddits.replace(" ", "").trim()
        val subs = cleanSubs.split("+").filter { it.isNotEmpty() }
        if (subs.isEmpty()) return emptyList()

        val token = RedditOAuthHelper.getOrFetchAccessToken(context) ?: return emptyList()
        
        // Parallel fetch for multiple subreddits to bypass 404 constraints on multireddit paths
        val lists = coroutineScope {
            subs.map { sub ->
                async {
                    var subList = performOAuthRequest(sub, token)
                    if (subList.isEmpty()) {
                        // Force retry token refresh once on failure
                        val prefs = context.getSharedPreferences("reddittube_prefs", Context.MODE_PRIVATE)
                        prefs.edit().remove("reddit_access_token").remove("reddit_token_expires_at").apply()
                        val freshToken = RedditOAuthHelper.getOrFetchAccessToken(context)
                        if (freshToken != null) {
                            subList = performOAuthRequest(sub, freshToken)
                        }
                    }
                    subList
                }
            }.awaitAll()
        }

        // Interleave (Round-robin) the lists to provide a balanced combined feed
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
        Log.d("RedditRepository", "fetchOAuthJsonVideos returning ${mergedList.size} merged videos for $subreddits")
        return mergedList
    }

    private fun performOAuthRequest(subreddit: String, token: String): List<RedditPost> {
        val list = mutableListOf<RedditPost>()
        try {
            val url = URL("https://oauth.reddit.com/r/$subreddit/hot.json?limit=25&raw_json=1")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("User-Agent", RedditOAuthHelper.getUserAgent(context))
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            Log.d("RedditRepository", "r/$subreddit API returned code: ${connection.responseCode}")
            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("RedditRepository", "r/$subreddit JSON length: ${response.length}")
                val jsonObject = JSONObject(response)
                val data = jsonObject.optJSONObject("data")
                val children = data?.optJSONArray("children")
                if (children != null) {
                    for (i in 0 until children.length()) {
                        val childData = children.getJSONObject(i).optJSONObject("data") ?: continue
                        val isVideo = childData.optBoolean("is_video", false)
                        val media = childData.optJSONObject("media")
                        val redditVideo = media?.optJSONObject("reddit_video")
                        
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

                            val videoUrl = if (hlsUrl.isNotEmpty()) hlsUrl else if (dashUrl.isNotEmpty()) dashUrl else fallbackUrl
                            if (videoUrl.isNotEmpty()) {
                                list.add(
                                    RedditPost(
                                        id = id,
                                        title = title,
                                        subreddit = subName,
                                        author = author,
                                        score = score,
                                        permalink = permalink,
                                        videoUrl = videoUrl,
                                        fallbackUrl = fallbackUrl,
                                        dashUrl = dashUrl,
                                        hlsUrl = hlsUrl
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                Log.e("RedditRepository", "OAuth request failed for r/$subreddit with code: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("RedditRepository", "OAuth request exception for r/$subreddit: ${e.message}")
        }
        Log.d("RedditRepository", "r/$subreddit parsed ${list.size} videos")
        return list
    }
}
