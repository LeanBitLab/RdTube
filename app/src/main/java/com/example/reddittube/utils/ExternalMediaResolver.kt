package com.lean.reddittube.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ExternalMediaResolver {
    private const val TAG = "ExternalMediaResolver"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    private var cachedRedgifsToken: String? = null
    private var tokenExpiryTime: Long = 0L

    fun isRedgifsUrl(url: String): Boolean {
        return url.contains("redgifs.com", ignoreCase = true)
    }

    fun isImgurUrl(url: String): Boolean {
        return url.contains("imgur.com", ignoreCase = true)
    }

    fun resolveImgurDirectUrl(url: String): String? {
        if (!isImgurUrl(url)) return null
        val clean = url.substringBefore('?').substringBefore('#').trim()
        val filename = clean.substringAfterLast('/')
        val id = filename.substringBefore('.')
        return if (id.isNotEmpty() && id != "a" && id != "gallery") {
            "https://i.imgur.com/$id.mp4"
        } else {
            null
        }
    }

    fun extractRedgifsId(url: String): String? {
        val clean = url.substringBefore('?').substringBefore('#').trimEnd('/')
        val match = Regex("""redgifs\.com/(?:watch|ifr)/([a-zA-Z0-9]+)""", RegexOption.IGNORE_CASE).find(clean)
        if (match != null) return match.groupValues[1]
        val last = clean.substringAfterLast('/')
        return if (last.isNotEmpty() && !last.contains("redgifs", ignoreCase = true)) last else null
    }

    suspend fun resolveRedgifsVideo(url: String): String? = withContext(Dispatchers.IO) {
        val gifId = extractRedgifsId(url) ?: return@withContext null
        try {
            val token = getOrFetchRedgifsToken() ?: return@withContext null
            val apiUrl = "https://api.redgifs.com/v2/gifs/${gifId.lowercase()}"
            val conn = URL(apiUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == 200) {
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(raw)
                val urls = json.optJSONObject("gif")?.optJSONObject("urls")
                val hd = urls?.optString("hd")?.takeIf { it.isNotEmpty() }
                val sd = urls?.optString("sd")?.takeIf { it.isNotEmpty() }
                return@withContext (hd ?: sd)
            } else if (conn.responseCode == 401 || conn.responseCode == 403) {
                // Invalidate cached token on auth error
                cachedRedgifsToken = null
                tokenExpiryTime = 0L
            }
        } catch (e: Exception) {
            Log.w(TAG, "Redgifs audio resolution failed for $gifId: ${e.message}")
        }
        null
    }

    private suspend fun getOrFetchRedgifsToken(): String? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedRedgifsToken != null && now < tokenExpiryTime) {
            return@withContext cachedRedgifsToken
        }
        try {
            val conn = URL("https://api.redgifs.com/v2/auth/temporary").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == 200) {
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(raw)
                val token = json.optString("token").takeIf { it.isNotEmpty() }
                if (token != null) {
                    cachedRedgifsToken = token
                    tokenExpiryTime = now + (12 * 3600 * 1000L) // 12 hours
                    return@withContext token
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch Redgifs temporary token: ${e.message}")
        }
        null
    }
}
