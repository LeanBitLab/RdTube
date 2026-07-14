package com.example.reddittube.ui.main.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.reddittube.utils.RedditOAuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

// ponytail: process-lifetime LRU cache — avoids re-downloading thumbnails every time Home remounts
private val thumbCache = object : LinkedHashMap<String, Bitmap>(64, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean = size > 64
}

// ponytail: minimal native thumbnail loader (no image lib) — decode over HttpURLConnection, cached
@Composable
fun ThumbnailImage(url: String, contentDescription: String?, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(thumbCache[url]) }
    var failed by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        if (url.isBlank()) { failed = true; return@LaunchedEffect }
        if (thumbCache.containsKey(url)) { bitmap = thumbCache[url]; return@LaunchedEffect }
        runCatching {
            withContext(Dispatchers.IO) {
                val conn = URL(url).openConnection() as HttpURLConnection
                try {
                    conn.connectTimeout = 15000
                    conn.readTimeout = 15000
                    conn.setRequestProperty("User-Agent", RedditOAuthHelper.DEFAULT_USER_AGENT)
                    if (conn.responseCode == 200) {
                        // ponytail: full-quality decode (inSampleSize=1); cache keeps repeat opens instant
                        val opts = BitmapFactory.Options().apply { inSampleSize = 1 }
                        BitmapFactory.decodeStream(conn.inputStream, null, opts)
                    } else null
                } finally {
                    conn.disconnect()
                }
            }
        }.onSuccess { bmp ->
            if (bmp != null) { thumbCache[url] = bmp; bitmap = bmp } else failed = true
        }.onFailure { failed = true }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            if (!failed) {
                CircularProgressIndicator(color = Color.Red, modifier = Modifier.fillMaxSize(0.18f), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.fillMaxSize(0.4f))
            }
        }
    }
}
