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
import java.util.regex.Pattern

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
        var list = fetchJsonVideos()
        if (list.isEmpty()) {
            Log.d("RedditRepository", "JSON fetch failed or empty, trying RSS fallback...")
            list = fetchRssVideos()
        }
        emit(list)
    }.flowOn(Dispatchers.IO)

    private fun fetchJsonVideos(): List<RedditPost> {
        val list = mutableListOf<RedditPost>()
        try {
            val url = URL("https://www.reddit.com/r/shorts+TikTokCringe+funny+videos/hot.json?limit=50&raw_json=1")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
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
                Log.e("RedditRepository", "JSON HTTP error: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("RedditRepository", "JSON fetch error: ${e.message}")
        }
        return list
    }

    private fun fetchRssVideos(): List<RedditPost> {
        val list = mutableListOf<RedditPost>()
        try {
            val url = URL("https://www.reddit.com/r/shorts+TikTokCringe+funny+videos.rss")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
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

                val xmlText = response.toString()
                val entryPattern = Pattern.compile("<entry>(.*?)</entry>", Pattern.DOTALL)
                val matcher = entryPattern.matcher(xmlText)
                while (matcher.find()) {
                    val entry = matcher.group(1) ?: continue

                    // Title
                    val titleMatch = Regex("<title>([^<]+)</title>").find(entry)
                    val title = titleMatch?.groupValues?.get(1) ?: "Reddit Video"

                    // Subreddit
                    val subMatch = Regex("<category[^>]*term=\"([^\"]+)\"").find(entry)
                    val subreddit = subMatch?.groupValues?.get(1) ?: "shorts"

                    // Author
                    val authorMatch = Regex("<author><name>/u/([^<]+)</name>").find(entry)
                    val author = authorMatch?.groupValues?.get(1) ?: "anonymous"

                    // Permalink
                    val linkMatch = Regex("<link href=\"([^\"]+)\"").find(entry)
                    val permalink = linkMatch?.groupValues?.get(1) ?: ""

                    // v.redd.it ID
                    val videoIdMatch = Regex("https://v.redd.it/([a-zA-Z0-9]+)").find(entry)
                    val videoId = videoIdMatch?.groupValues?.get(1)

                    if (videoId != null) {
                        val base = "https://v.redd.it/$videoId"
                        val hlsUrl = "$base/HLSPlaylist.m3u8"
                        val dashUrl = "$base/DASHPlaylist.mpd"
                        val fallbackUrl = "$base/DASH_480.mp4"
                        
                        list.add(
                            RedditPost(
                                id = videoId,
                                title = title,
                                subreddit = subreddit,
                                author = author,
                                score = 0,
                                permalink = permalink,
                                videoUrl = hlsUrl,
                                fallbackUrl = fallbackUrl,
                                dashUrl = dashUrl,
                                hlsUrl = hlsUrl
                            )
                        )
                    }
                }
            } else {
                Log.e("RedditRepository", "RSS HTTP error: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("RedditRepository", "RSS fetch error: ${e.message}")
        }
        return list
    }
}
