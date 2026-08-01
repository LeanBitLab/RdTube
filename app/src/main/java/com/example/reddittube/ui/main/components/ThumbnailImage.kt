package com.lean.reddittube.ui.main.components
import com.lean.reddittube.theme.*

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.lean.reddittube.utils.RedditOAuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

// Adaptive Cache Engine with heap-aware dynamic sizing and time-decayed eviction score S(i)
private val thumbCache = com.lean.reddittube.util.AdaptiveCacheEngine<String, Bitmap>(
    lowerBound = 32,
    upperBound = 300,
    memoryFraction = 0.15f,
    sizeEstimator = { bmp -> bmp.allocationByteCount.toLong().coerceAtLeast(1024L) }
)

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
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.setRequestProperty("User-Agent", RedditOAuthHelper.DEFAULT_USER_AGENT)
                    conn.setRequestProperty("Connection", "keep-alive")
                    if (conn.responseCode == 200) {
                        val stream = if ("gzip".equals(conn.contentEncoding, ignoreCase = true)) {
                            java.util.zip.GZIPInputStream(conn.inputStream)
                        } else {
                            conn.inputStream
                        }
                        val bytes = stream.readBytes()
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                        opts.inJustDecodeBounds = false
                        opts.inSampleSize = (opts.outWidth / 480f).coerceAtLeast(opts.outHeight / 480f)
                            .toInt().coerceAtLeast(1)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                    } else null
                } finally {
                    conn.disconnect()
                }
            }
        }.onSuccess { bmp ->
            if (bmp != null) { thumbCache.put(url, bmp); bitmap = bmp } else failed = true
        }.onFailure { failed = true }
    }

    Box(modifier = modifier) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Subtle dark bottom vignette for high-contrast badge text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.45f)
                            ),
                            startY = 100f
                        )
                    )
            )
        } else {
            if (!failed) {
                val transition = rememberInfiniteTransition(label = "thumb-pulse")
                val pulseAlpha by transition.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 0.65f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceRaised.copy(alpha = pulseAlpha)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 1.5.dp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceRaised),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
