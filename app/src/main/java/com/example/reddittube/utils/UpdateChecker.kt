package com.lean.reddittube.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRelease(
    val tagName: String,
    val name: String,
    val changelog: String,
    val apkDownloadUrl: String?,
    val publishedAt: String
)

object UpdateChecker {
    private const val GITHUB_REPO = "LeanBitLab/RdTube"
    private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    suspend fun checkForUpdate(currentVersion: String): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "RdTube-App")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "").trim()
                val name = json.optString("name", tagName)
                val body = json.optString("body", "Bug fixes and performance improvements.")
                val publishedAt = json.optString("published_at", "")

                var apkUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                val remoteVer = tagName.removePrefix("v").trim()
                val localVer = currentVersion.removePrefix("v").trim()

                if (isNewerVersion(remoteVer, localVer)) {
                    return@withContext GitHubRelease(
                        tagName = tagName,
                        name = name,
                        changelog = body,
                        apkDownloadUrl = apkUrl,
                        publishedAt = publishedAt
                    )
                }
            }
        } catch (e: Exception) {
            Log.d("UpdateChecker", "Update check skipped/offline: ${e.message}")
        }
        null
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        if (remote.isEmpty() || local.isEmpty()) return false
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val lParts = local.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val l = lParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean, String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val url = URL(apkUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
            }

            val totalBytes = connection.contentLength.coerceAtLeast(1)
            val apkFile = File(context.cacheDir, "update_rdtube.apk")
            if (apkFile.exists()) apkFile.delete()

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var downloaded = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        onProgress(downloaded.toFloat() / totalBytes)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
                onComplete(true, null)
            }
        } catch (e: Exception) {
            Log.e("UpdateChecker", "APK download failed: ${e.message}")
            withContext(Dispatchers.Main) {
                onComplete(false, e.message)
            }
        }
    }

    fun installApk(context: Context, file: File) {
        try {
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Error launching APK install intent: ${e.message}")
        }
    }
}
