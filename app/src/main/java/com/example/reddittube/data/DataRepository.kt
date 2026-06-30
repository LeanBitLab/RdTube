package com.example.reddittube.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// ponytail: Simplified representation of Reddit posts containing video elements. No database, memory-only.
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
    fun fetchRedditVideos(): Flow<List<RedditPost>>
}

class DefaultDataRepository : DataRepository {
    override fun fetchRedditVideos(): Flow<List<RedditPost>> = flow {
        val list = mutableListOf<RedditPost>()
        try {
            // Fetch hot posts from popular video subreddits
            val url = URL("https://www.reddit.com/r/shorts+TikTokCringe+funny+videos/hot.json?limit=50&raw_json=1")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "RedditTube/1.0 (by /u/reddittube_app)")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonObject = JSONObject(response.toString())
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
                            val subreddit = childData.optString("subreddit")
                            val author = childData.optString("author")
                            val score = childData.optInt("score")
                            val permalink = childData.optString("permalink")

                            // Select HLS as primary because ExoPlayer plays it best on Android
                            val videoUrl = if (hlsUrl.isNotEmpty()) hlsUrl else if (dashUrl.isNotEmpty()) dashUrl else fallbackUrl
                            if (videoUrl.isNotEmpty()) {
                                list.add(
                                    RedditPost(
                                        id = id,
                                        title = title,
                                        subreddit = subreddit,
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
                Log.e("RedditRepository", "HTTP error code: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("RedditRepository", "Fetch exception: ${e.message}", e)
        }
        emit(list)
    }.flowOn(Dispatchers.IO)
}
