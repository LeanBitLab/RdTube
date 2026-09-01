package com.lean.reddittube.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    // User authentication keys (required for 18+/NSFW content on Reddit API post-2023)
    private const val KEY_USER_ACCESS_TOKEN = "reddit_user_access_token"
    private const val KEY_USER_REFRESH_TOKEN = "reddit_user_refresh_token"
    private const val KEY_USER_TOKEN_EXPIRES_AT = "reddit_user_token_expires_at"
    private const val KEY_USERNAME = "reddit_username"

    const val DEFAULT_CLIENT_ID = "yH0aTnJEt6qUgGn835B4vg"
    const val DEFAULT_USER_AGENT = "org.quantumbadger.redreader/1.25.1"
    const val REDIRECT_URI = "redreader://rr_oauth_redir"

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_USER_REFRESH_TOKEN, "").isNullOrEmpty()
    }

    fun getUsername(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USERNAME, null)
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_USER_ACCESS_TOKEN)
            .remove(KEY_USER_REFRESH_TOKEN)
            .remove(KEY_USER_TOKEN_EXPIRES_AT)
            .remove(KEY_USERNAME)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_TOKEN_EXPIRES_AT)
            .apply()
    }

    fun launchLogin(context: Context) {
        val authUrl = "https://www.reddit.com/api/v1/authorize.compact?" +
            "client_id=$DEFAULT_CLIENT_ID" +
            "&response_type=code" +
            "&state=rdtube_auth_${System.currentTimeMillis()}" +
            "&redirect_uri=${Uri.encode(REDIRECT_URI)}" +
            "&duration=permanent" +
            "&scope=identity,read,mysubreddits,history"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    suspend fun handleOAuthCallback(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val code = uri.getQueryParameter("code") ?: return@withContext false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        try {
            val url = URL("https://www.reddit.com/api/v1/access_token")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true

            val authString = "$DEFAULT_CLIENT_ID:"
            val authBase64 = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)
            conn.setRequestProperty("Authorization", "Basic $authBase64")
            conn.setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            val params = "grant_type=authorization_code&code=$code&redirect_uri=${Uri.encode(REDIRECT_URI)}"
            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(params)
            writer.flush()
            writer.close()

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val accessToken = json.optString("access_token")
                val refreshToken = json.optString("refresh_token")
                val expiresIn = json.optLong("expires_in", 3600L)

                if (accessToken.isNotEmpty()) {
                    prefs.edit()
                        .putString(KEY_USER_ACCESS_TOKEN, accessToken)
                        .putString(KEY_USER_REFRESH_TOKEN, refreshToken)
                        .putLong(KEY_USER_TOKEN_EXPIRES_AT, System.currentTimeMillis() + (expiresIn * 1000L))
                        .apply()

                    // Fetch username
                    fetchAndSaveUsername(accessToken, prefs)
                    return@withContext true
                }
            } else {
                Log.e("RedditOAuth", "Token exchange failed with code: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("RedditOAuth", "Exception during token exchange: ${e.message}")
        }
        false
    }

    private fun fetchAndSaveUsername(accessToken: String, prefs: android.content.SharedPreferences) {
        try {
            val url = URL("https://oauth.reddit.com/api/v1/me")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val name = json.optString("name")
                if (name.isNotEmpty()) {
                    prefs.edit().putString(KEY_USERNAME, name).apply()
                }
            }
        } catch (e: Exception) {
            Log.w("RedditOAuth", "Failed to fetch username: ${e.message}")
        }
    }

    @Synchronized
    fun getOrFetchAccessToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. If user is logged in, use and refresh User Access Token (unlocks 18+/NSFW content!)
        val userRefreshToken = prefs.getString(KEY_USER_REFRESH_TOKEN, "")
        if (!userRefreshToken.isNullOrEmpty()) {
            val userAccessToken = prefs.getString(KEY_USER_ACCESS_TOKEN, "")
            val userExpiresAt = prefs.getLong(KEY_USER_TOKEN_EXPIRES_AT, 0L)

            if (!userAccessToken.isNullOrEmpty() && System.currentTimeMillis() < userExpiresAt - 120000L) {
                return userAccessToken
            }

            // Refresh user token
            val refreshedToken = refreshUserToken(prefs, userRefreshToken)
            if (refreshedToken != null) {
                return refreshedToken
            }
        }

        // 2. Fallback to application-only anonymous token
        val currentToken = prefs.getString(KEY_ACCESS_TOKEN, "")
        val expiresAt = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0L)

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

            val authString = "$DEFAULT_CLIENT_ID:"
            val authBase64 = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)
            conn.setRequestProperty("Authorization", "Basic $authBase64")
            conn.setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            val params = "grant_type=https://oauth.reddit.com/grants/installed_client&device_id=$deviceId"
            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(params)
            writer.flush()
            writer.close()

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
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

    private fun refreshUserToken(prefs: android.content.SharedPreferences, refreshToken: String): String? {
        try {
            val url = URL("https://www.reddit.com/api/v1/access_token")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true

            val authString = "$DEFAULT_CLIENT_ID:"
            val authBase64 = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)
            conn.setRequestProperty("Authorization", "Basic $authBase64")
            conn.setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            val params = "grant_type=refresh_token&refresh_token=$refreshToken"
            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(params)
            writer.flush()
            writer.close()

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val token = json.optString("access_token")
                val expiresIn = json.optLong("expires_in", 3600L)
                if (token.isNotEmpty()) {
                    prefs.edit()
                        .putString(KEY_USER_ACCESS_TOKEN, token)
                        .putLong(KEY_USER_TOKEN_EXPIRES_AT, System.currentTimeMillis() + (expiresIn * 1000L))
                        .apply()
                    return token
                }
            }
        } catch (e: Exception) {
            Log.w("RedditOAuth", "Failed to refresh user token: ${e.message}")
        }
        return null
    }
}
