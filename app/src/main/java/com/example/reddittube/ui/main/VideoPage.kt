package com.lean.reddittube.ui.main
import com.lean.reddittube.theme.*

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
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
import com.lean.reddittube.data.RedditPost
import com.lean.reddittube.ui.main.components.MinimalButton
import com.lean.reddittube.ui.main.components.PlayerSlider
import com.lean.reddittube.ui.main.components.QualityBottomSheet
import com.lean.reddittube.utils.DownloadHelper
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
    onLike: (RedditPost) -> Unit = {},
    onSwipeAdvance: () -> Unit = {},
    onNext: () -> Unit = {},
    onSubredditClick: (String) -> Unit = {},
    isMuted: Boolean = false,
    onMuteChange: (Boolean) -> Unit = {},
    onBack: (() -> Unit)? = null,
    onRefresh: () -> Unit = {}
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPreferences = remember { context.getSharedPreferences("rdtube_prefs", Context.MODE_PRIVATE) }

    // Onboarding, Seek Feedback & Refresh state
    var showOnboarding by remember { mutableStateOf(!sharedPreferences.getBoolean("has_seen_onboarding", false)) }
    var seekFeedback by remember { mutableStateOf<String?>(null) }
    var seekFeedbackJob by remember { mutableStateOf<Job?>(null) }
    var showRefreshingIndicator by remember { mutableStateOf(false) }
    var refreshingJob by remember { mutableStateOf<Job?>(null) }

    // Player state
    var currentQuality by remember { mutableStateOf(sharedPreferences.getString("saved_quality", "Auto") ?: "Auto") }
    var currentSpeed by remember { mutableStateOf(1.0f) }
    var showQualitySheet by remember { mutableStateOf(false) }

    val perfController = remember { com.lean.reddittube.util.PerfTelemetryController.getInstance(context) }
    val perfParams by perfController.params.collectAsStateWithLifecycle()

    val exoPlayer = remember(post.id) {
        val url = post.dashUrl.ifEmpty { post.hlsUrl }.ifEmpty { post.videoUrl }
        val cacheDataSourceFactory = com.lean.reddittube.util.MediaCacheManager.getCacheDataSourceFactory(context)
        val loadControl = com.lean.reddittube.util.MediaCacheManager.getAdaptiveLoadControl(
            bufferPlayMs = perfParams.bufferPlayMs,
            bufferRebufferMs = perfParams.bufferRebufferMs,
            bufferMaxMs = perfParams.bufferMaxMs
        )
        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .setLoadControl(loadControl)
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
        val initTime = android.os.SystemClock.elapsedRealtime()
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                val startupMs = android.os.SystemClock.elapsedRealtime() - initTime
                perfController.recordStartupTime(startupMs)
            }
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

    // Pause player when app enters background or user moves away
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, isActive) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isActive) {
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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

    // Auto-next repeat mode (single source of truth, reacts to live toggles)
    var autoNextEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("auto_next", false)) }
    LaunchedEffect(autoNextEnabled) {
        exoPlayer.repeatMode = if (autoNextEnabled) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
    }

    // Swipe-to-like / swipe-to-history live indicator
    var swipeProgress by remember { mutableStateOf(0f) } // -1 left (history) .. +1 right (like)
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
    var originalBrightness by remember { mutableStateOf<Float?>(null) }
    val activity = context as? Activity

    // ponytail: restore system brightness when leaving the player (gesture writes it globally)
    DisposableEffect(Unit) {
        onDispose {
            originalBrightness?.let { orig ->
                activity?.window?.attributes?.let { lp ->
                    lp.screenBrightness = orig
                    activity.window.attributes = lp
                }
            }
        }
    }

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
        // ExoPlayer canvas — tap toggles overlay (reveal never pauses; pause only on a visible-surface tap)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val width = size.width
                            val isLeftThird = offset.x < width / 3f
                            val isRightThird = offset.x > (width * 2f / 3f)
                            if (isLeftThird) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                exoPlayer.seekTo(newPos)
                                seekFeedback = "« 10s"
                                seekFeedbackJob?.cancel()
                                seekFeedbackJob = coroutineScope.launch {
                                    delay(800)
                                    seekFeedback = null
                                }
                            } else if (isRightThird) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                val dur = exoPlayer.duration.coerceAtLeast(0)
                                val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(dur)
                                exoPlayer.seekTo(newPos)
                                seekFeedback = "10s »"
                                seekFeedbackJob?.cancel()
                                seekFeedbackJob = coroutineScope.launch {
                                    delay(800)
                                    seekFeedback = null
                                }
                            }
                        },
                        onTap = {
                            val wasShown = showOverlay
                            showOverlay = !showOverlay
                            if (showOverlay) {
                                overlayJob?.cancel()
                                overlayJob = coroutineScope.launch {
                                    delay(4000)
                                    if (exoPlayer.isPlaying) showOverlay = false
                                }
                            }
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
                            } else if (wasShown) {
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
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.size >= 2) {
                                var totalY = 0f
                                var active = true
                                while (active) {
                                    val dragEvent = awaitPointerEvent()
                                    if (dragEvent.changes.size < 2) {
                                        active = false
                                        break
                                    }
                                    val firstChange = dragEvent.changes.firstOrNull() ?: break
                                    if (firstChange.pressed) {
                                        val deltaY = firstChange.position.y - firstChange.previousPosition.y
                                        totalY += deltaY
                                        if (totalY > 160f) {
                                            dragEvent.changes.forEach { it.consume() }
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onRefresh()
                                            showRefreshingIndicator = true
                                            refreshingJob?.cancel()
                                            refreshingJob = coroutineScope.launch {
                                                delay(1200)
                                                showRefreshingIndicator = false
                                            }
                                            totalY = 0f
                                            active = false
                                            break
                                        }
                                    } else {
                                        active = false
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    // ponytail: swipe left -> history (mark watched), swipe right -> like. Horizontal only,
                    // so it never conflicts with vertical pager nav or edge volume/brightness.
                    var totalX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalX = 0f },
                        onDragCancel = { totalX = 0f; swipeProgress = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalX += dragAmount
                            val threshold = size.width * 0.25f
                            swipeProgress = (totalX / threshold).coerceIn(-1f, 1f)
                        },
                        onDragEnd = {
                            val threshold = size.width * 0.25f
                            if (totalX <= -threshold) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRemoveVideo(post)
                                onSwipeAdvance()
                            } else if (totalX >= threshold) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLike(post)
                                onSwipeAdvance()
                            }
                            totalX = 0f
                            swipeProgress = 0f
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

        // Left edge: brightness edge drag gesture
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.2f)
                .align(Alignment.CenterStart)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            showBrightnessHud = true
                            showVolumeHud = false
                            if (originalBrightness == null) {
                                originalBrightness = activity?.window?.attributes?.screenBrightness
                            }
                            if (brightnessPercentage < 0f && activity != null) {
                                val lp = activity.window.attributes
                                val currentBright = if (lp.screenBrightness < 0f) {
                                    Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
                                } else {
                                    lp.screenBrightness
                                }
                                brightnessPercentage = currentBright
                            }
                        },
                        onDragEnd = {
                            hudJob?.cancel()
                            hudJob = coroutineScope.launch {
                                delay(800)
                                showBrightnessHud = false
                            }
                        },
                        onDragCancel = {
                            hudJob?.cancel()
                            hudJob = coroutineScope.launch {
                                delay(800)
                                showBrightnessHud = false
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            if (activity != null) {
                                val sensitivity = 0.003f
                                val currentBright = if (brightnessPercentage < 0f) 0.5f else brightnessPercentage
                                val nextBright = (currentBright - dragAmount * sensitivity).coerceIn(0.01f, 1f)
                                val lp = activity.window.attributes
                                lp.screenBrightness = nextBright
                                activity.window.attributes = lp
                                brightnessPercentage = nextBright
                            }
                            hudJob?.cancel()
                            hudJob = coroutineScope.launch {
                                delay(1000)
                                showBrightnessHud = false
                            }
                        }
                    )
                }
        )

        // Right edge: volume. Swipe gesture opens volume bar showing current volume level, then drag up/down adjusts.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.2f)
                .align(Alignment.CenterEnd)
                .pointerInput(isMuted) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            showVolumeHud = true
                            showBrightnessHud = false
                            if (volumePercentage < 0f) {
                                volumePercentage = if (isMuted) 0f else (if (exoPlayer.volume < 1f && exoPlayer.volume > 0f) exoPlayer.volume else 0.5f)
                            }
                        },
                        onDragEnd = {
                            hudJob?.cancel()
                            hudJob = coroutineScope.launch {
                                delay(800)
                                showVolumeHud = false
                            }
                        },
                        onDragCancel = {
                            hudJob?.cancel()
                            hudJob = coroutineScope.launch {
                                delay(800)
                                showVolumeHud = false
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val sensitivity = 0.003f
                            val currentVol = if (volumePercentage < 0f) (if (isMuted) 0f else 0.5f) else volumePercentage
                            val nextPercent = (currentVol - dragAmount * sensitivity).coerceIn(0f, 1f)
                            onMuteChange(nextPercent == 0f)
                            exoPlayer.volume = nextPercent
                            volumePercentage = nextPercent
                            hudJob?.cancel()
                            hudJob = coroutineScope.launch {
                                delay(1000)
                                showVolumeHud = false
                            }
                        }
                    )
                }
        )

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
        AnimatedVisibility(
            visible = showOverlay,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
        }

        // ponytail: edge zones removed — unified gesture surface above classifies by start X.

        // Swipe-to-like / swipe-to-history minimal glassmorphic visualizer
        val likeAlpha = swipeProgress.coerceIn(0f, 1f)
        if (likeAlpha > 0.05f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 28.dp)
                    .alpha(likeAlpha)
                    .scale(0.85f + likeAlpha * 0.15f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceBase.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, BrandRed.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.ThumbUp, contentDescription = null, tint = BrandRed, modifier = Modifier.size(18.dp))
                        Text("Liked", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        val historyAlpha = (-swipeProgress).coerceIn(0f, 1f)
        if (historyAlpha > 0.05f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 28.dp)
                    .alpha(historyAlpha)
                    .scale(0.85f + historyAlpha * 0.15f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceBase.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                        Text("History", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Buffering indicator
        AnimatedVisibility(
            visible = isBuffering,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(48.dp)
            )
        }

        // Transient play/pause visual
        AnimatedVisibility(
            visible = showPlayPauseTransient != null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.8f),
            exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.8f)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (showPlayPauseTransient == true) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Transient seek feedback visual ("« 10s" / "10s »")
        AnimatedVisibility(
            visible = seekFeedback != null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.8f),
            exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.8f)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.75f),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Text(
                    text = seekFeedback ?: "",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }

        // Top back button overlay (auto-hides with controls overlay)
        AnimatedVisibility(
            visible = showOverlay && onBack != null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(start = HPad, top = 8.dp),
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { onBack?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        // First-run gesture onboarding overlay
        if (showOnboarding && isActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable {
                        showOnboarding = false
                        sharedPreferences.edit().putBoolean("has_seen_onboarding", true).apply()
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceRaised,
                    border = BorderStroke(1.dp, BrandRed)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Player Gestures",
                            color = BrandRed,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(14.dp))
                        Text("↔ Swipe Left / Right to dismiss or like video", color = TextPrimary, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("↕ Drag Left / Right edges for Brightness & Volume", color = TextPrimary, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("⏩ Double-Tap sides to Seek 10 seconds", color = TextPrimary, fontSize = 13.sp)
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                showOnboarding = false
                                sharedPreferences.edit().putBoolean("has_seen_onboarding", true).apply()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                        ) {
                            Text("Got it!", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Bottom metadata, quick actions, and player slider overlay
        AnimatedVisibility(
            visible = showOverlay,
            modifier = Modifier.align(Alignment.BottomStart),
            enter = fadeIn(tween(220)) + slideInVertically(tween(220), initialOffsetY = { it / 2 }),
            exit = fadeOut(tween(180)) + slideOutVertically(tween(180), targetOffsetY = { it / 2 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Subreddit/author with subscribe toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "r/${post.subreddit}",
                        color = BrandRed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onSubredditClick(post.subreddit) }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val isSubbed = subscribedSet.contains(post.subreddit.lowercase())
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(BrandRed.copy(alpha = if (isSubbed) 0.2f else 0.15f))
                            .clickable {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSubscribeToggle(post.subreddit.lowercase())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSubbed) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = if (isSubbed) "Unsubscribe" else "Subscribe",
                            tint = BrandRed,
                            modifier = Modifier.size(16.dp)
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
                            tint = if (isRotationLocked) BrandRed else Color.White,
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
                            tint = if (isMuted) BrandRed else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Auto-next
                    MinimalButton(
                        onClick = {
                            autoNextEnabled = !autoNextEnabled
                            sharedPreferences.edit().putBoolean("auto_next", autoNextEnabled).apply()
                        },
                        label = ""
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Auto-next",
                            tint = if (autoNextEnabled) BrandRed else Color.White,
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

                    // Share button
                    MinimalButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "https://reddit.com${post.permalink}")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share video link"))
                        },
                        label = ""
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share video", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Player seek slider
                PlayerSlider(player = exoPlayer)
            }
        }

        // Two-finger refresh transient indicator
        AnimatedVisibility(
            visible = showRefreshingIndicator,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(top = 16.dp),
            enter = fadeIn(tween(150)) + slideInVertically(tween(150), initialOffsetY = { -it }),
            exit = fadeOut(tween(150)) + slideOutVertically(tween(150), targetOffsetY = { -it })
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceBase.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, BrandRed)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = BrandRed, modifier = Modifier.size(16.dp))
                    Text("Refreshing feed…", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Brightness HUD (Top Center of video)
        AnimatedVisibility(
            visible = showBrightnessHud,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(top = 56.dp),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            val displayBright = if (brightnessPercentage < 0f && activity != null) {
                val lp = activity.window.attributes
                if (lp.screenBrightness < 0f) {
                    Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
                } else lp.screenBrightness
            } else brightnessPercentage
            TopHud(
                progress = displayBright.coerceIn(0f, 1f),
                icon = { Icon(Icons.Default.BrightnessHigh, contentDescription = "Brightness", tint = Color.White, modifier = Modifier.size(16.dp)) }
            )
        }

        // Volume HUD (Top Center of video)
        AnimatedVisibility(
            visible = showVolumeHud,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(top = 56.dp),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            val displayVol = if (volumePercentage < 0f) exoPlayer.volume else volumePercentage
            TopHud(
                progress = displayVol.coerceIn(0f, 1f),
                icon = {
                    Icon(
                        if (displayVol <= 0f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Volume",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
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
                        color = BrandRed,
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


@Composable
private fun TopHud(
    progress: Float,
    icon: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon()
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(BrandRed)
                )
            }
            Text(
                text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
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
