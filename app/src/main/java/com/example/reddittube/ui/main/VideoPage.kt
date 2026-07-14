package com.example.reddittube.ui.main

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.reddittube.data.RedditPost
import com.example.reddittube.ui.main.components.MinimalButton
import com.example.reddittube.ui.main.components.PlayerSlider
import com.example.reddittube.ui.main.components.QualityBottomSheet
import com.example.reddittube.utils.DownloadHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ponytail: Single video page with ExoPlayer, edge gestures, metadata, quick actions, player slider
@Composable
fun VideoPage(
    post: RedditPost,
    isActive: Boolean,
    subscribedSet: Set<String> = emptySet(),
    onSubscribeToggle: (String) -> Unit = {},
    onRemoveVideo: (RedditPost) -> Unit = {},
    onNext: () -> Unit = {},
    isMuted: Boolean = false,
    onMuteChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPreferences = remember { context.getSharedPreferences("reddittube_prefs", Context.MODE_PRIVATE) }

    // Player state
    var currentQuality by remember { mutableStateOf(sharedPreferences.getString("saved_quality", "Auto") ?: "Auto") }
    var currentSpeed by remember { mutableStateOf(1.0f) }
    var showQualitySheet by remember { mutableStateOf(false) }

    val exoPlayer = remember(post.id) {
        val url = post.dashUrl.ifEmpty { post.hlsUrl }.ifEmpty { post.videoUrl }
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("org.quantumbadger.redreader/1.25.1")
            .setConnectTimeoutMs(30000)
            .setReadTimeoutMs(30000)
            .setAllowCrossProtocolRedirects(true)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
        player.apply {
            repeatMode = if (sharedPreferences.getBoolean("auto_next", false)) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
            setAudioAttributes(androidx.media3.common.AudioAttributes.Builder()
                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                .setContentType(androidx.media3.common.C.CONTENT_TYPE_MOVIE)
                .build(), true)
            setMediaItem(MediaItem.Builder().setUri(Uri.parse(url)).build())
            playWhenReady = false
            prepare()
        }
        player
    }

    // Buffering & playback status
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var pendingAutoPlay by remember { mutableStateOf(false) }

    DisposableEffect(post.id) {
        val tag = "VideoPlayer"
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_ENDED && sharedPreferences.getBoolean("auto_next", false)) {
                    onNext()
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlayerError(error: PlaybackException) {
                Log.e(tag, "error=${error.localizedMessage}, cause=${error.cause?.localizedMessage}")
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Play/pause for active page
    LaunchedEffect(isActive) {
        if (isActive) {
            if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0)
            exoPlayer.playWhenReady = true
            exoPlayer.play()
            pendingAutoPlay = true
        } else {
            exoPlayer.pause()
        }
    }

    // Quality settings
    LaunchedEffect(currentQuality) {
        applyQualitySetting(exoPlayer, currentQuality)
    }

    // Playback speed
    LaunchedEffect(currentSpeed) {
        exoPlayer.setPlaybackSpeed(currentSpeed)
    }

    // Mute
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Auto-next repeat mode
    val autoNext by remember { mutableStateOf(sharedPreferences.getBoolean("auto_next", false)) }
    LaunchedEffect(autoNext) {
        exoPlayer.repeatMode = if (autoNext) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
    }

    // Gesture HUDs
    var brightnessPercentage by remember { mutableStateOf(-1f) }
    var volumePercentage by remember { mutableStateOf(-1f) }
    var showBrightnessHud by remember { mutableStateOf(false) }
    var showVolumeHud by remember { mutableStateOf(false) }
    var hudJob by remember { mutableStateOf<Job?>(null) }

    // Transient play/pause indicator
    var showPlayPauseTransient by remember { mutableStateOf<Boolean?>(null) }
    var transientJob by remember { mutableStateOf<Job?>(null) }

    // Downloader status
    var downloadProgress by remember { mutableStateOf<String?>(null) }

    // Rotation status
    var isRotationLocked by remember { mutableStateOf(true) }
    val activity = context as? Activity

    // Overlay visibility (auto-hide)
    var showOverlay by remember { mutableStateOf(true) }
    var overlayJob by remember { mutableStateOf<Job?>(null) }

    // Initialize orientation to locked portrait on active
    LaunchedEffect(isActive) {
        if (isActive && activity != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            isRotationLocked = true
        }
    }

    // Show overlay briefly on start, then auto-hide
    LaunchedEffect(isActive) {
            if (isActive) {
                showOverlay = true
                overlayJob?.cancel()
                overlayJob = coroutineScope.launch {
                    delay(4000)
                    if (exoPlayer.isPlaying) showOverlay = false
                }
            }
    }

Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ExoPlayer canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            // Toggle overlay
                            showOverlay = !showOverlay
                            if (showOverlay) {
                                overlayJob?.cancel()
                                overlayJob = coroutineScope.launch {
                                    delay(4000)
                                    showOverlay = false
                                }
                            }
                            // Consume pendingAutoPlay flag to suppress spurious tap after scroll
                            if (pendingAutoPlay) {
                                pendingAutoPlay = false
                                if (!exoPlayer.isPlaying) {
                                    exoPlayer.play()
                                    showPlayPauseTransient = true
                                    transientJob?.cancel()
                                    transientJob = coroutineScope.launch {
                                        delay(600)
                                        showPlayPauseTransient = null
                                    }
                                }
                            } else {
                                // Toggle play/pause
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                    showPlayPauseTransient = false
                                } else {
                                    exoPlayer.play()
                                    showPlayPauseTransient = true
                                }
                                transientJob?.cancel()
                                transientJob = coroutineScope.launch {
                                    delay(600)
                                    showPlayPauseTransient = null
                                }
                            }
                        }
                    )
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { pv -> pv.player = exoPlayer },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Auto-track watched: mark after 10% of video watched
        LaunchedEffect(isActive, post.id) {
            if (isActive) {
                while (exoPlayer.duration <= 0) delay(500)
                val duration = exoPlayer.duration
                val threshold = duration / 10
                while (isActive) {
                    delay(1000)
                    if (exoPlayer.currentPosition >= threshold && exoPlayer.isPlaying) {
                        onRemoveVideo(post)
                        break
                    }
                }
            }
        }

        // Bottom gradient overlay for readability
        if (showOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
        }

        // Left edge gesture zone (brightness)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.18f)
                .align(Alignment.CenterStart)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            showBrightnessHud = true
                            showVolumeHud = false
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            if (activity != null) {
                                val lp = activity.window.attributes
                                val currentBright = if (lp.screenBrightness < 0f) {
                                    Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
                                } else {
                                    lp.screenBrightness
                                }
                                val sensitivity = 0.003f
                                val nextBright = (currentBright - dragAmount * sensitivity).coerceIn(0.01f, 1f)
                                lp.screenBrightness = nextBright
                                activity.window.attributes = lp
                                brightnessPercentage = nextBright
                            }
                            hudJob?.cancel()
                            hudJob = coroutineScope.launch {
                                delay(1000)
                                showBrightnessHud = false
                            }
                        },
                        onDragEnd = {
                            hudJob?.cancel()
                            hudJob = coroutineScope.launch {
                                delay(800)
                                showBrightnessHud = false
                            }
                        }
                    )
                }
        )

        // Right edge gesture zone (volume)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.18f)
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            showVolumeHud = true
                            showBrightnessHud = false
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val sensitivity = 0.003f
                            val nextPercent = (exoPlayer.volume - dragAmount * sensitivity).coerceIn(0f, 1f)
                            onMuteChange(nextPercent == 0f)
                            exoPlayer.volume = nextPercent
                            volumePercentage = nextPercent
                            hudJob?.cancel()
                            hudJob = coroutineScope.launch {
                                delay(1000)
                                showVolumeHud = false
                            }
                        },
                        onDragEnd = {
                            hudJob?.cancel()
                            hudJob = coroutineScope.launch {
                                delay(800)
                                showVolumeHud = false
                            }
                        }
                    )
                }
        )

        // Buffering indicator
        if (isBuffering) {
            CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
            )
        }

        // Transient play/pause visual
        showPlayPauseTransient?.let { state ->
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (state) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Bottom metadata, quick actions, and player slider overlay
        if (showOverlay) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Subreddit/author with subscribe toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "r/${post.subreddit}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val isSubbed = subscribedSet.contains(post.subreddit.lowercase())
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = if (isSubbed) 0.2f else 0.15f))
                            .clickable { onSubscribeToggle(post.subreddit.lowercase()) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSubbed) "\u2713" else "+",
                            color = Color.Red,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "\u2022 u/${post.author}",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Post title
                Text(
                    text = post.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Horizontal row of quick action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
// Likes
MinimalButton(onClick = {}, label = formatScore(post.score)) {
    Icon(Icons.Default.ThumbUp, contentDescription = "Likes", tint = Color.White, modifier = Modifier.size(14.dp))
}
// Quality / Speed
MinimalButton(
    onClick = { showQualitySheet = true },
    label = currentQuality
) {
    Icon(Icons.Default.Settings, contentDescription = "Quality", tint = Color.White, modifier = Modifier.size(14.dp))
}
// Rotation lock
                    MinimalButton(
                        onClick = {
                            if (activity != null) {
                                if (isRotationLocked) {
                                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                                    isRotationLocked = false
                                } else {
                                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    isRotationLocked = true
                                }
                            }
                        },
                        label = ""
                    ) {
                        Icon(
                            if (isRotationLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Rotation",
                            tint = if (isRotationLocked) Color.Red else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Mute/unmute
                    MinimalButton(
                        onClick = { onMuteChange(!isMuted) },
                        label = ""
                    ) {
                        Icon(
                            if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute",
                            tint = if (isMuted) Color.Red else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Auto-next
                    var localAutoNext by remember { mutableStateOf(sharedPreferences.getBoolean("auto_next", false)) }
                    MinimalButton(
                        onClick = {
                            localAutoNext = !localAutoNext
                            sharedPreferences.edit().putBoolean("auto_next", localAutoNext).apply()
                        },
                        label = ""
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Auto-next",
                            tint = if (localAutoNext) Color.Red else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

// Download / Save
                    MinimalButton(
                        onClick = {
                            if (downloadProgress == null) {
                                coroutineScope.launch {
                                    DownloadHelper.downloadRedditVideo(
                                        context = context,
                                        fallbackUrl = post.fallbackUrl,
                                        dashUrl = post.dashUrl,
                                        title = post.title,
                                        onProgress = { text -> downloadProgress = text },
                                        onComplete = { success, result ->
                                            downloadProgress = null
                                            if (success) Toast.makeText(context, "Saved to Downloads: $result", Toast.LENGTH_LONG).show()
                                            else Toast.makeText(context, "Download failed: $result", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            }
                        },
        label = ""
    ) {
        Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(14.dp))
    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Player seek slider
                PlayerSlider(player = exoPlayer)
            }
        }

        // Brightness HUD slider
        if (showBrightnessHud) {
            val displayBright = if (brightnessPercentage < 0f && activity != null) {
                val lp = activity.window.attributes
                if (lp.screenBrightness < 0f) {
                    Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
                } else lp.screenBrightness
            } else brightnessPercentage
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(32.dp)
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp, top = 360.dp, bottom = 360.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        val trackW = 3.dp.toPx()
                        val fillH = size.height * displayBright.coerceIn(0f, 1f)
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.3f),
                            topLeft = Offset((size.width - trackW) / 2f, 0f),
                            size = Size(trackW, size.height),
                            cornerRadius = CornerRadius(trackW / 2f)
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset((size.width - trackW) / 2f, size.height - fillH),
                            size = Size(trackW, fillH),
                            cornerRadius = CornerRadius(trackW / 2f)
                        )
                    }
                    Icon(Icons.Default.BrightnessHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp).align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // Volume HUD slider
        if (showVolumeHud) {
            val displayVol = if (volumePercentage < 0f) {
                exoPlayer.volume
            } else volumePercentage
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(32.dp)
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp, top = 360.dp, bottom = 360.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        val trackW = 3.dp.toPx()
                        val fillH = size.height * displayVol.coerceIn(0f, 1f)
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.3f),
                            topLeft = Offset((size.width - trackW) / 2f, 0f),
                            size = Size(trackW, size.height),
                            cornerRadius = CornerRadius(trackW / 2f)
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset((size.width - trackW) / 2f, size.height - fillH),
                            size = Size(trackW, fillH),
                            cornerRadius = CornerRadius(trackW / 2f)
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp).align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // Download progress overlay
        downloadProgress?.let { text ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp, start = 16.dp, end = 16.dp)
                    .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(8.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = Color.Red,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = text, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }

    // Quality bottom sheet
    if (showQualitySheet) {
        QualityBottomSheet(
            currentQuality = currentQuality,
            currentSpeed = currentSpeed,
            onQualitySelected = { qual ->
                currentQuality = qual
                sharedPreferences.edit().putString("saved_quality", qual).apply()
            },
            onSpeedSelected = { speed -> currentSpeed = speed },
            onDismiss = { showQualitySheet = false }
        )
    }
}

private fun formatScore(score: Int): String {
    return when {
        score >= 1000000 -> String.format("%.1fM", score / 1000000f)
        score >= 1000 -> String.format("%.1fk", score / 1000f)
        else -> score.toString()
    }
}

private fun applyQualitySetting(player: ExoPlayer, quality: String) {
    val maxVideoSize = when (quality) {
        "240p" -> Pair(426, 240)
        "360p" -> Pair(640, 360)
        "480p" -> Pair(854, 480)
        "720p" -> Pair(1280, 720)
        "1080p" -> Pair(1920, 1080)
        else -> Pair(Int.MAX_VALUE, Int.MAX_VALUE)
    }
    val parameters = player.trackSelectionParameters.buildUpon()
        .setMaxVideoSize(maxVideoSize.first, maxVideoSize.second)
        .build()
    player.trackSelectionParameters = parameters
}
