package com.lean.reddittube.utils

import android.content.Context
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object RedditOAuthHelper {
    private const val PREFS_NAME = "rdtube_prefs"

    private const val KEY_ACCESS_TOKEN = "reddit_access_token"
    private const val KEY_TOKEN_EXPIRES_AT = "reddit_token_expires_at"
    private const val KEY_DEVICE_ID = "reddit_device_id"

    const val DEFAULT_CLIENT_ID = "yH0aTnJEt6qUgGn835B4vg"
    const val DEFAULT_USER_AGENT = "org.quantumbadger.redreader/1.25.1"

    @Synchronized
    fun getOrFetchAccessToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Application-only token — ponytail: always use RedReader default credentials
        val clientId = DEFAULT_CLIENT_ID
        val userAgent = DEFAULT_USER_AGENT

        val currentToken = prefs.getString(KEY_ACCESS_TOKEN, "")
        val expiresAt = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0L)

        // Return cached token if valid (with 2 min buffer)
        if (!currentToken.isNullOrEmpty() && System.currentTimeMillis() < expiresAt - 120000L) {
            return currentToken
        }

        var deviceId = prefs.getString(KEY_DEVICE_ID, "")
        if (deviceId.isNullOrEmpty()) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }

        try {
            val url = URL("https://www.reddit.com/api/v1/access_token")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            
            val authString = "$clientId:"
            val authBase64 = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)
            conn.setRequestProperty("Authorization", "Basic $authBase64")
            conn.setRequestProperty("User-Agent", userAgent)
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            val params = "grant_type=https://oauth.reddit.com/grants/installed_client&device_id=$deviceId"
            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(params)
            writer.flush()
            writer.close()

            if (conn.responseCode == 200) {
                val reader = conn.inputStream.bufferedReader()
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val token = json.optString("access_token")
                val expiresIn = json.optLong("expires_in", 3600L)
                if (token.isNotEmpty()) {
                    prefs.edit()
                        .putString(KEY_ACCESS_TOKEN, token)
                        .putLong(KEY_TOKEN_EXPIRES_AT, System.currentTimeMillis() + (expiresIn * 1000L))
                        .apply()
                    return token
                }
            } else {
                Log.e("RedditOAuth", "OAuth token POST failed with code: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("RedditOAuth", "Exception during OAuth fetch: ${e.message}")
        }
        return null
    }
}
