package com.example.reddittube.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object RedditOAuthHelper {
    private const val PREFS_NAME = "reddittube_prefs"
    private const val KEY_CLIENT_ID = "reddit_client_id"
    private const val KEY_USER_AGENT = "reddit_user_agent"
    private const val KEY_REDIRECT_URI = "reddit_redirect_uri"
    private const val KEY_ACCESS_TOKEN = "reddit_access_token"
    private const val KEY_TOKEN_EXPIRES_AT = "reddit_token_expires_at"
    private const val KEY_DEVICE_ID = "reddit_device_id"

    // User OAuth Token Keys
    private const val KEY_USER_ACCESS_TOKEN = "reddit_user_access_token"
    private const val KEY_USER_REFRESH_TOKEN = "reddit_user_refresh_token"
    private const val KEY_USER_TOKEN_EXPIRES_AT = "reddit_user_token_expires_at"

    const val DEFAULT_CLIENT_ID = "yH0aTnJEt6qUgGn835B4vg"
    const val DEFAULT_USER_AGENT = "org.quantumbadger.redreader/1.25.1"
    const val DEFAULT_REDIRECT_URI = "redreader://rr_oauth_redir"

    fun getClientId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_CLIENT_ID, "") ?: ""
        return if (id.isEmpty()) DEFAULT_CLIENT_ID else id
    }

    fun getUserAgent(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ua = prefs.getString(KEY_USER_AGENT, "") ?: ""
        return if (ua.isEmpty()) DEFAULT_USER_AGENT else ua
    }

    fun getRedirectUri(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uri = prefs.getString(KEY_REDIRECT_URI, "") ?: ""
        return if (uri.isEmpty()) DEFAULT_REDIRECT_URI else uri
    }

    fun saveApiCredentials(context: Context, clientId: String, userAgent: String, redirectUri: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_CLIENT_ID, clientId.trim())
            .putString(KEY_USER_AGENT, userAgent.trim())
            .putString(KEY_REDIRECT_URI, redirectUri.trim())
            .apply()
        // Invalidate active token when client/agent/URI updates
        prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_TOKEN_EXPIRES_AT).apply()
    }

    fun isUserLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_USER_REFRESH_TOKEN, "").isNullOrEmpty()
    }

    fun getUsername(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("reddit_username", "") ?: ""
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_USER_ACCESS_TOKEN)
            .remove(KEY_USER_REFRESH_TOKEN)
            .remove(KEY_USER_TOKEN_EXPIRES_AT)
            .remove("reddit_username")
            .apply()
    }

    fun startLoginFlow(context: Context) {
        val clientId = getClientId(context)
        val redirectUri = getRedirectUri(context)
        val state = UUID.randomUUID().toString()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("oauth_state", state).apply()

        val urlStr = "https://www.reddit.com/api/v1/authorize" +
                "?client_id=$clientId" +
                "&response_type=code" +
                "&state=$state" +
                "&redirect_uri=$redirectUri" +
                "&duration=permanent" +
                "&scope=identity read mysubreddits"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlStr)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun exchangeCodeForToken(context: Context, code: String, state: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedState = prefs.getString("oauth_state", "") ?: ""
        if (state != savedState) {
            Log.e("RedditOAuth", "OAuth state mismatch! Expected $savedState, got $state")
            return false
        }

        val clientId = getClientId(context)
        val redirectUri = getRedirectUri(context)
        val userAgent = getUserAgent(context)

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

            val params = "grant_type=authorization_code&code=$code&redirect_uri=$redirectUri"
            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(params)
            writer.flush()
            writer.close()

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val token = json.optString("access_token")
                val refresh = json.optString("refresh_token")
                val expiresIn = json.optLong("expires_in", 3600L)

                if (token.isNotEmpty()) {
                    prefs.edit()
                        .putString(KEY_USER_ACCESS_TOKEN, token)
                        .putString(KEY_USER_REFRESH_TOKEN, refresh)
                        .putLong(KEY_USER_TOKEN_EXPIRES_AT, System.currentTimeMillis() + (expiresIn * 1000L))
                        .apply()
                    
                    fetchAndSaveUsername(context, token)
                    return true
                }
            } else {
                Log.e("RedditOAuth", "Exchange token failed code: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("RedditOAuth", "Error exchanging OAuth code: ${e.message}")
        }
        return false
    }

    private fun fetchAndSaveUsername(context: Context, token: String) {
        try {
            val url = URL("https://oauth.reddit.com/api/v1/me")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("User-Agent", getUserAgent(context))
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val name = json.optString("name")
                if (name.isNotEmpty()) {
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().putString("reddit_username", name).apply()
                }
            }
        } catch (e: Exception) {
            Log.e("RedditOAuth", "Failed to fetch username: ${e.message}")
        }
    }

    private fun refreshUserToken(context: Context, refreshToken: String): String? {
        val clientId = getClientId(context)
        val userAgent = getUserAgent(context)
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

            val params = "grant_type=refresh_token&refresh_token=$refreshToken"
            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(params)
            writer.flush()
            writer.close()

            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val token = json.optString("access_token")
                val expiresIn = json.optLong("expires_in", 3600L)
                if (token.isNotEmpty()) {
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putString(KEY_USER_ACCESS_TOKEN, token)
                        .putLong(KEY_USER_TOKEN_EXPIRES_AT, System.currentTimeMillis() + (expiresIn * 1000L))
                        .apply()
                    return token
                }
            }
        } catch (e: Exception) {
            Log.e("RedditOAuth", "Error refreshing user token: ${e.message}")
        }
        return null
    }

    @Synchronized
    fun getOrFetchAccessToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userRefreshToken = prefs.getString(KEY_USER_REFRESH_TOKEN, "") ?: ""
        
        // Tier 1: User OAuth Token (if logged in)
        if (userRefreshToken.isNotEmpty()) {
            val userToken = prefs.getString(KEY_USER_ACCESS_TOKEN, "") ?: ""
            val expiresAt = prefs.getLong(KEY_USER_TOKEN_EXPIRES_AT, 0L)
            
            if (userToken.isNotEmpty() && System.currentTimeMillis() < expiresAt - 120000L) {
                return userToken
            }
            
            val refreshed = refreshUserToken(context, userRefreshToken)
            if (refreshed != null) return refreshed
        }

        // Tier 2: Application-only token
        val clientId = getClientId(context)
        val userAgent = getUserAgent(context)

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
