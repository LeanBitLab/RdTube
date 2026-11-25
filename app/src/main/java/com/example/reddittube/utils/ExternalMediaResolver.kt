package com.lean.reddittube.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ExternalMediaResolver {
    private const val TAG = "ExternalMediaResolver"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    private const val REDGIFS_ORIGIN = "https://www.redgifs.com"

    private val tokenMutex = Mutex()
    private var cachedRedgifsToken: String? = null
    private var tokenExpiryTime: Long = 0L

    // Bounded LRU (up to 3 keys per video), synchronized because resolution runs on IO threads
    private val resolvedCache: MutableMap<String, String> = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 600
        }
    )

    fun isRedgifsUrl(url: String): Boolean {
        return url.contains("redgifs.com", ignoreCase = true) || url.contains("gfycat.com", ignoreCase = true)
    }

    fun isImgurUrl(url: String): Boolean {
        return url.contains("imgur.com", ignoreCase = true)
    }

    fun resolveImgurDirectUrl(url: String): String? {
        if (!isImgurUrl(url)) return null
        val clean = url.substringBefore('?').substringBefore('#').trim()
        if (clean.contains("/a/") || clean.contains("/gallery/")) return null
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
        val match = Regex("""(?:redgifs\.com|gfycat\.com)/(?:watch|ifr)/([a-zA-Z0-9_-]+)""", RegexOption.IGNORE_CASE).find(clean)
        if (match != null) return match.groupValues[1]
        val path = try {
            java.net.URI(clean).path?.trim('/') ?: ""
        } catch (_: Exception) {
            clean.substringAfter("redgifs.com/", "").substringAfter("gfycat.com/", "").trim('/')
        }
        if (path.isEmpty()) return null
        val lastSegment = path.substringAfterLast('/')
        val cleanId = lastSegment.substringBefore('.').removeSuffix("-mobile").removeSuffix("-silent").removeSuffix("-poster")
        val isInvalid = cleanId.length < 3 ||
            cleanId.equals("watch", ignoreCase = true) ||
            cleanId.equals("ifr", ignoreCase = true) ||
            cleanId.equals("www", ignoreCase = true) ||
            cleanId.contains("redgifs", ignoreCase = true) ||
            cleanId.contains("gfycat", ignoreCase = true)
        return if (!isInvalid) cleanId else null
    }

    fun getCachedRedgifsVideo(url: String): String? {
        resolvedCache[url]?.let { return it }
        val id = extractRedgifsId(url) ?: return null
        resolvedCache[id]?.let { return it }
        return resolvedCache[id.lowercase()]
    }

    fun cacheDirectRedgifsUrl(key: String, directUrl: String) {
        resolvedCache[key] = directUrl
        extractRedgifsId(key)?.let { id ->
            resolvedCache[id] = directUrl
            resolvedCache[id.lowercase()] = directUrl
        }
        extractRedgifsId(directUrl)?.let { id ->
            resolvedCache[id] = directUrl
            resolvedCache[id.lowercase()] = directUrl
        }
    }

    /**
     * Resolves direct Redgifs audio/video stream instantly from Reddit's oembed data.
     * Reddit embeds contain `oembed.thumbnail_url` pointing to the exact-cased CDN poster,
     * e.g., `https://media.redgifs.com/AzureWateryMarmoset-poster.jpg`.
     * Replacing `-poster.jpg` with `.mp4` yields the direct HD MP4 with audio without requiring an API round-trip.
     */
    fun resolveDirectRedgifsUrlFromPost(targetData: JSONObject, childData: JSONObject): String? {
        val oembed = targetData.optJSONObject("media")?.optJSONObject("oembed")
            ?: targetData.optJSONObject("secure_media")?.optJSONObject("oembed")
            ?: childData.optJSONObject("media")?.optJSONObject("oembed")
            ?: childData.optJSONObject("secure_media")?.optJSONObject("oembed")

        val thumb = oembed?.optString("thumbnail_url", "")?.takeIf { it.isNotEmpty() }
            ?: (targetData.optJSONObject("preview") ?: childData.optJSONObject("preview"))
                ?.optJSONArray("images")
                ?.optJSONObject(0)
                ?.optJSONObject("source")
                ?.optString("url", "")
                ?.replace("&amp;", "&")

        if (thumb != null && isRedgifsUrl(thumb)) {
            val direct = when {
                thumb.contains("-poster.jpg") -> thumb.replace("-poster.jpg", ".mp4")
                thumb.contains("-mobile.jpg") -> thumb.replace("-mobile.jpg", ".mp4")
                thumb.endsWith(".jpg") -> thumb.substringBeforeLast(".jpg") + ".mp4"
                thumb.endsWith(".png") -> thumb.substringBeforeLast(".png") + ".mp4"
                else -> null
            }
            if (direct != null) {
                val rawUrl = targetData.optString("url_overridden_by_dest", targetData.optString("url", "")).ifEmpty {
                    childData.optString("url_overridden_by_dest", childData.optString("url", ""))
                }
                if (rawUrl.isNotEmpty()) cacheDirectRedgifsUrl(rawUrl, direct)
                cacheDirectRedgifsUrl(direct, direct)
                return direct
            }
        }
        return null
    }

    suspend fun resolveRedgifsVideo(url: String): String? = withContext(Dispatchers.IO) {
        val cached = getCachedRedgifsVideo(url)
        if (cached != null) return@withContext cached

        val gifId = extractRedgifsId(url) ?: return@withContext null
        try {
            val token = getOrFetchRedgifsToken() ?: return@withContext null
            val apiUrl = "https://api.redgifs.com/v2/gifs/${gifId.lowercase()}"
            val conn = URL(apiUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Referer", "$REDGIFS_ORIGIN/")
            conn.setRequestProperty("Origin", REDGIFS_ORIGIN)
            conn.setRequestProperty("Accept", "application/json, text/plain, */*")
            conn.connectTimeout = 6000
            conn.readTimeout = 6000

            val code = conn.responseCode
            if (code == 200) {
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(raw)
                val urls = json.optJSONObject("gif")?.optJSONObject("urls")
                val hd = urls?.optString("hd")?.takeIf { it.isNotEmpty() }
                val sd = urls?.optString("sd")?.takeIf { it.isNotEmpty() }
                val resolved = (hd ?: sd)
                if (resolved != null) {
                    cacheDirectRedgifsUrl(url, resolved)
                    return@withContext resolved
                }
            } else if (code == 401 || code == 403) {
                Log.w(TAG, "Redgifs API auth failure $code for $gifId, clearing token")
                tokenMutex.withLock {
                    cachedRedgifsToken = null
                    tokenExpiryTime = 0L
                }
            } else {
                Log.w(TAG, "Redgifs API response $code for $gifId")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Redgifs audio resolution failed for $gifId: ${e.message}")
        }
        null
    }

    private suspend fun getOrFetchRedgifsToken(): String? = withContext(Dispatchers.IO) {
        tokenMutex.withLock {
            val now = System.currentTimeMillis()
            if (cachedRedgifsToken != null && now < tokenExpiryTime) {
                return@withLock cachedRedgifsToken
            }
            try {
                val conn = URL("https://api.redgifs.com/v2/auth/temporary").openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.setRequestProperty("Referer", "$REDGIFS_ORIGIN/")
                conn.setRequestProperty("Origin", REDGIFS_ORIGIN)
                conn.setRequestProperty("Accept", "application/json, text/plain, */*")
                conn.connectTimeout = 6000
                conn.readTimeout = 6000

                val code = conn.responseCode
                if (code == 200) {
                    val raw = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(raw)
                    val token = json.optString("token").takeIf { it.isNotEmpty() }
                    if (token != null) {
                        cachedRedgifsToken = token
                        tokenExpiryTime = now + (12 * 3600 * 1000L) // 12 hours
                        return@withLock token
                    }
                } else {
                    Log.w(TAG, "Failed to get temporary token, HTTP $code")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch Redgifs temporary token: ${e.message}")
            }
            null
        }
    }
}
