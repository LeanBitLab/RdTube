package com.example.reddittube.data

import android.content.Context
import android.util.Log
import com.example.reddittube.utils.RedditOAuthHelper
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
    fun fetchRedditVideos(subreddits: String): Flow<List<RedditPost>>
}

class DefaultDataRepository(private val context: Context) : DataRepository {
    
    companion object {
        // Rotatable list of fast, public Redlib instances to bypass Reddit client blocks
        private val REDLIB_INSTANCES = listOf(
            "https://redlib.perennialte.ch",
            "https://redlib.privacyredirect.com",
            "https://redlib.r4fo.com",
            "https://redlib.cow.rip"
        )
    }

    override fun fetchRedditVideos(subreddits: String): Flow<List<RedditPost>> = flow {
        var list = emptyList<RedditPost>()
        val clientId = RedditOAuthHelper.getClientId(context)

        // Tier 1: If Client ID is set, try Official Reddit OAuth JSON API
        if (clientId.isNotEmpty()) {
            Log.d("RedditRepository", "Client ID found, trying OAuth JSON API...")
            list = fetchOAuthJsonVideos(subreddits)
        }

        // Tier 2: Try Redlib instances (fastest, no client-side rate limits or 403s)
        if (list.isEmpty()) {
            for (instance in REDLIB_INSTANCES) {
                Log.d("RedditRepository", "Trying RSS fetch from proxy instance: $instance")
                list = fetchRssVideos(subreddits, instance)
                if (list.isNotEmpty()) {
                    Log.d("RedditRepository", "Successfully loaded feed from: $instance")
                    break
                }
            }
        }
        
        // Tier 3: Try direct Reddit JSON
        if (list.isEmpty()) {
            Log.d("RedditRepository", "All proxy instances empty, trying direct Reddit JSON...")
            list = fetchJsonVideos(subreddits)
        }
        
        // Tier 4: Try direct Reddit RSS
        if (list.isEmpty()) {
            Log.d("RedditRepository", "Direct Reddit JSON empty, trying direct Reddit RSS...")
            list = fetchRssVideos(subreddits, "https://www.reddit.com")
        }
        
        emit(list)
    }.flowOn(Dispatchers.IO)

    private fun fetchOAuthJsonVideos(subreddits: String): List<RedditPost> {
        val cleanSubs = subreddits.replace(" ", "").trim()
        val token = RedditOAuthHelper.getOrFetchAccessToken(context) ?: return emptyList()
        
        var list = performOAuthRequest(cleanSubs, token)
        if (list.isEmpty()) {
            // Force fetch a fresh token and try once more
            val prefs = context.getSharedPreferences("reddittube_prefs", Context.MODE_PRIVATE)
            prefs.edit().remove("reddit_access_token").remove("reddit_token_expires_at").apply()
            val freshToken = RedditOAuthHelper.getOrFetchAccessToken(context)
            if (freshToken != null) {
                list = performOAuthRequest(cleanSubs, freshToken)
            }
        }
        return list
    }

    private fun performOAuthRequest(subreddits: String, token: String): List<RedditPost> {
        val list = mutableListOf<RedditPost>()
        try {
            val url = URL("https://oauth.reddit.com/r/$subreddits/hot.json?limit=50&raw_json=1")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("User-Agent", "android:com.example.reddittube:v1.0.0 (by /u/arjun_reddittube_dev)")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

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
                Log.e("RedditRepository", "OAuth request failed with code: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("RedditRepository", "OAuth request exception: ${e.message}")
        }
        return list
    }

    private fun fetchJsonVideos(subreddits: String): List<RedditPost> {
        val list = mutableListOf<RedditPost>()
        try {
            val cleanSubs = subreddits.replace(" ", "").trim()
            val url = URL("https://www.reddit.com/r/$cleanSubs/hot.json?limit=50&raw_json=1")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

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

    private fun fetchRssVideos(subreddits: String, baseUrl: String): List<RedditPost> {
        val list = mutableListOf<RedditPost>()
        try {
            val cleanSubs = subreddits.replace(" ", "").trim()
            val url = URL("$baseUrl/r/$cleanSubs.rss")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val xmlText = response
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
                Log.e("RedditRepository", "RSS HTTP error: ${connection.responseCode} on $baseUrl")
            }
        } catch (e: Exception) {
            Log.e("RedditRepository", "RSS fetch error on $baseUrl: ${e.message}")
        }
        return list
    }
}
