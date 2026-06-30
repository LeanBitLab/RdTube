package com.example.reddittube.ui.main

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.reddittube.data.DefaultDataRepository
import com.example.reddittube.data.RedditPost
import com.example.reddittube.utils.DownloadHelper
import com.example.reddittube.utils.RedditOAuthHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository(context)) }
    val exploreState by viewModel.exploreState.collectAsStateWithLifecycle()
    val subscribedState by viewModel.subscribedState.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val horizontalPagerState = rememberPagerState(pageCount = { 2 })

    var showSearchDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var subscribedSet by remember {
        val saved = context.getSharedPreferences("reddittube_prefs", Context.MODE_PRIVATE)
            .getStringSet("subscriptions", setOf("shorts", "TikTokCringe", "funny", "videos")) ?: emptySet()
        mutableStateOf(saved.map { it.lowercase() }.toSet())
    }

    // Load initial feeds
    LaunchedEffect(Unit) {
        viewModel.refreshExplore()
        if (subscribedSet.isNotEmpty()) {
            viewModel.refreshSubscribed(subscribedSet.sorted().joinToString("+"))
        }
    }

    val toggleSubscription = { sub: String ->
        val next = sub.lowercase().trim().replace(" ", "")
        if (next.isNotEmpty()) {
            val updated = if (subscribedSet.contains(next)) {
                subscribedSet - next
            } else {
                subscribedSet + next
            }
            subscribedSet = updated
            context.getSharedPreferences("reddittube_prefs", Context.MODE_PRIVATE)
                .edit().putStringSet("subscriptions", updated).apply()
            
            // Auto refresh subscribed feed with updated list
            viewModel.refreshSubscribed(updated.sorted().joinToString("+"))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Core Horizontal Pager switching between Explore (Page 0) and Subscribed (Page 1)
        HorizontalPager(
            state = horizontalPagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            if (pageIndex == 0) {
                // Explore Section
                when (val uiState = exploreState) {
                    MainScreenUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.Red)
                        }
                    }
                    is MainScreenUiState.Success -> {
                        MainScreenContent(
                            data = uiState.data,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is MainScreenUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Failed to load Explore videos.",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.throwable.message ?: "Unknown error",
                                color = Color.LightGray,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.refreshExplore() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Try Again", color = Color.White)
                            }
                        }
                    }
                }
            } else {
                // Subscribed Section
                when (val uiState = subscribedState) {
                    MainScreenUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.Red)
                        }
                    }
                    is MainScreenUiState.Success -> {
                        MainScreenContent(
                            data = uiState.data,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is MainScreenUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Failed to load Subscribed videos.",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.throwable.message ?: "Unknown error",
                                color = Color.LightGray,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { 
                                    if (subscribedSet.isNotEmpty()) {
                                        viewModel.refreshSubscribed(subscribedSet.sorted().joinToString("+"))
                                    } else {
                                        viewModel.refreshSubscribed("")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Try Again", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // 2. Persistent Top App Header (Always visible!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Title Left
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Reddit",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tube",
                    color = Color.Red,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Middle Tab Selectors (Synced with HorizontalPagerState)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isExploreActive = horizontalPagerState.currentPage == 0
                val isSubscribedActive = horizontalPagerState.currentPage == 1
                
                // Explore Tab
                Text(
                    text = "Explore",
                    color = if (isExploreActive) Color.Red else Color.LightGray,
                    fontSize = 16.sp,
                    fontWeight = if (isExploreActive) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            horizontalPagerState.animateScrollToPage(0)
                        }
                    }
                )

                // Subscribed Tab
                Text(
                    text = "Subscribed",
                    color = if (isSubscribedActive) Color.Red else Color.LightGray,
                    fontSize = 16.sp,
                    fontWeight = if (isSubscribedActive) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.clickable {
                        if (subscribedSet.isEmpty()) {
                            Toast.makeText(context, "No subreddits subscribed yet! Use search.", Toast.LENGTH_SHORT).show()
                        } else {
                            coroutineScope.launch {
                                horizontalPagerState.animateScrollToPage(1)
                            }
                        }
                    }
                )
            }
            
            // Search, Settings, & Refresh Right
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showSearchDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        if (horizontalPagerState.currentPage == 0) {
                            viewModel.refreshExplore()
                        } else {
                            viewModel.refreshSubscribed(subscribedSet.sorted().joinToString("+"))
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Search Results Active Indicator Overlay (Always visible!)
        val defaultExploreQuery = "shorts+TikTokCringe+funny+videos"
        val isSpecialFeed = viewModel.exploreQuery != defaultExploreQuery && horizontalPagerState.currentPage == 0
        if (isSpecialFeed) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Viewing: r/${viewModel.exploreQuery}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val isSubbed = subscribedSet.contains(viewModel.exploreQuery.lowercase())
                    Text(
                        text = if (isSubbed) "Subscribed" else "Subscribe",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            toggleSubscription(viewModel.exploreQuery)
                        }
                    )
                }
            }
        }

        // Search and Subscribe Dialog overlay
        if (showSearchDialog) {
            SearchAndSubscribeDialog(
                onDismissRequest = { showSearchDialog = false },
                currentSubscribed = subscribedSet,
                onSubscribeToggle = { toggleSubscription(it) },
                onSubredditSelect = {
                    viewModel.refreshExplore(it)
                    coroutineScope.launch {
                        horizontalPagerState.animateScrollToPage(0)
                    }
                }
            )
        }

        // Settings Dialog overlay
        if (showSettingsDialog) {
            SettingsDialog(
                onDismissRequest = { showSettingsDialog = false }
            )
        }
    }
}

@Composable
internal fun MainScreenContent(
    data: List<RedditPost>,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { data.size })
    Box(modifier = modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index -> data[index].id }
        ) { pageIndex ->
            VideoPage(
                post = data[pageIndex],
                isActive = pagerState.currentPage == pageIndex
            )
        }
    }
}

@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var clientIdInput by remember { mutableStateOf(RedditOAuthHelper.getClientId(context)) }
    var userAgentInput by remember { mutableStateOf(RedditOAuthHelper.getUserAgent(context)) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.DarkGray,
        title = {
            Text("API Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = Color.Red)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    RedditOAuthHelper.saveApiCredentials(context, clientIdInput, userAgentInput)
                    Toast.makeText(context, "Settings saved. Please refresh.", Toast.LENGTH_SHORT).show()
                    onDismissRequest()
                }
            ) {
                Text("Save", color = Color.Green, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Reddit API Client ID",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = clientIdInput,
                    onValueChange = { clientIdInput = it },
                    placeholder = { Text("Paste client ID here...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.Red
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "User Agent",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = userAgentInput,
                    onValueChange = { userAgentInput = it },
                    placeholder = { Text("Enter User-Agent...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.Red
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Note: If left empty, defaults to the official RedReader Client ID and User Agent so the app works out of the box.",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    )
}

@Composable
fun SearchAndSubscribeDialog(
    onDismissRequest: () -> Unit,
    currentSubscribed: Set<String>,
    onSubscribeToggle: (String) -> Unit,
    onSubredditSelect: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val recommended = listOf("nextfuckinglevel", "interestingasfuck", "animalsdoingstuff", "oddlysatisfying", "aww", "gaming")

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.DarkGray,
        title = {
            Text("Search & Subscriptions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close", color = Color.Red)
            }
        },
        confirmButton = {},
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
            ) {
                // Search Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Subreddit name...", color = Color.LightGray) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.Red
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val query = searchQuery.trim().replace(" ", "")
                            if (query.isNotEmpty()) {
                                onSubredditSelect(query)
                                onDismissRequest()
                            } else {
                                Toast.makeText(context, "Please enter a subreddit name", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.background(Color.Red, shape = RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom search subscribe state if query is typed
                val trimmedQuery = searchQuery.trim().lowercase()
                if (trimmedQuery.isNotEmpty()) {
                    val isSubbed = currentSubscribed.contains(trimmedQuery)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("r/$trimmedQuery", color = Color.White, fontWeight = FontWeight.Medium)
                        TextButton(
                            onClick = {
                                onSubscribeToggle(trimmedQuery)
                            }
                        ) {
                            Text(if (isSubbed) "Unsubscribe" else "Subscribe", color = Color.Red)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Subscriptions Title
                Text("My Subscriptions", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // Subscriptions List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (currentSubscribed.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No subscriptions yet.", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            currentSubscribed.sorted().forEach { sub ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSubredditSelect(sub)
                                            onDismissRequest()
                                        }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("r/$sub", color = Color.White, fontSize = 16.sp)
                                    IconButton(
                                        onClick = { onSubscribeToggle(sub) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recommended Subreddits
                Text("Recommended", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recommended.forEach { rec ->
                        val isSubbed = currentSubscribed.contains(rec.lowercase())
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .clickable {
                                    onSubredditSelect(rec)
                                    onDismissRequest()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("r/$rec", color = Color.White, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { onSubscribeToggle(rec.lowercase()) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSubbed) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = if (isSubbed) Color.Green else Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun VideoPage(
    post: RedditPost,
    isActive: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPreferences = remember { context.getSharedPreferences("reddittube_prefs", Context.MODE_PRIVATE) }
    
    // Player State
    var currentQuality by remember { mutableStateOf(sharedPreferences.getString("saved_quality", "Auto") ?: "Auto") }
    val exoPlayer = remember(post.id) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            val mediaItem = MediaItem.fromUri(Uri.parse(post.videoUrl))
            setMediaItem(mediaItem)
            prepare()
        }
    }

    // Buffering & Playback status
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    
    DisposableEffect(post.id) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Play/Pause active page
    LaunchedEffect(isActive) {
        if (isActive) {
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Quality setting application
    LaunchedEffect(currentQuality) {
        applyQualitySetting(exoPlayer, currentQuality)
    }

    // Gesture State HUDs
    var brightnessPercentage by remember { mutableStateOf(-1f) }
    var volumePercentage by remember { mutableStateOf(-1f) }
    var showBrightnessHud by remember { mutableStateOf(false) }
    var showVolumeHud by remember { mutableStateOf(false) }
    var hudJob by remember { mutableStateOf<Job?>(null) }

    // Transient Play/Pause indicator
    var showPlayPauseTransient by remember { mutableStateOf<Boolean?>(null) }
    var transientJob by remember { mutableStateOf<Job?>(null) }

    // Downloader status
    var downloadProgress by remember { mutableStateOf<String?>(null) }

    // Rotation status
    var isRotationLocked by remember { mutableStateOf(true) }
    val activity = context as? Activity
    
    // Initialize orientation to locked portrait on active
    LaunchedEffect(isActive) {
        if (isActive && activity != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            isRotationLocked = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ExoPlayer Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
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

        // Bottom Transparent Gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
        )

        // Left Edge Gesture Zone (Brightness)
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

        // Right Edge Gesture Zone (Volume)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.18f)
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    detectVerticalDragGestures(
                        onDragStart = {
                            showVolumeHud = true
                            showBrightnessHud = false
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val currentPercent = currentVol.toFloat() / maxVol
                            val sensitivity = 0.003f
                            val nextPercent = (currentPercent - dragAmount * sensitivity).coerceIn(0f, 1f)
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                (nextPercent * maxVol).toInt(),
                                0
                            )
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

        // ponytail: vertical sliders removed in favor of unified bottom horizontal sliders

        // Buffering circular progress indicator
        if (isBuffering) {
            CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
            )
        }

        // Transient play/pause visual feed
        showPlayPauseTransient?.let { state ->
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                if (state) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    PauseIcon(modifier = Modifier.size(36.dp))
                }
            }
        }

        // Bottom Metadata, Quick Actions and Sliders Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            // Subreddit/Author details
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "r/${post.subreddit}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• u/${post.author}",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Post Title
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
                // Simulated Likes Button
                MinimalButton(
                    onClick = {},
                    label = formatScore(post.score)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Likes",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Rotation Lock Toggle
                MinimalButton(
                    onClick = {
                        if (activity != null) {
                            if (isRotationLocked) {
                                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                                isRotationLocked = false
                                Toast.makeText(context, "Auto Rotate Enabled", Toast.LENGTH_SHORT).show()
                            } else {
                                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                isRotationLocked = true
                                Toast.makeText(context, "Locked Portrait", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    label = if (isRotationLocked) "Locked" else "Auto"
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rotation",
                        tint = if (isRotationLocked) Color.Red else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Quality Selection Button
                var showQualityMenu by remember { mutableStateOf(false) }
                MinimalButton(
                    onClick = { showQualityMenu = true },
                    label = currentQuality
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Quality",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (showQualityMenu) {
                    AlertDialog(
                        onDismissRequest = { showQualityMenu = false },
                        title = { Text("Playback Quality", color = Color.White) },
                        containerColor = Color.DarkGray,
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showQualityMenu = false }) {
                                Text("Cancel", color = Color.Red)
                            }
                        },
                        text = {
                            Column {
                                val qualities = listOf("Auto", "1080p", "720p", "480p", "360p", "240p")
                                qualities.forEach { qual ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                currentQuality = qual
                                                sharedPreferences.edit().putString("saved_quality", qual).apply()
                                                showQualityMenu = false
                                                Toast.makeText(context, "Quality updated to $qual", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(qual, color = Color.White, fontSize = 16.sp)
                                        if (currentQuality == qual) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.Red,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                // Download/Save Button
                MinimalButton(
                    onClick = {
                        if (downloadProgress == null) {
                            coroutineScope.launch {
                                DownloadHelper.downloadRedditVideo(
                                    context = context,
                                    fallbackUrl = post.fallbackUrl,
                                    dashUrl = post.dashUrl,
                                    title = post.title,
                                    onProgress = { text ->
                                        downloadProgress = text
                                    },
                                    onComplete = { success, result ->
                                        downloadProgress = null
                                        if (success) {
                                            Toast.makeText(context, "Saved to Downloads: $result", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Download failed: $result", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                )
                            }
                        }
                    },
                    label = "Save"
                ) {
                    DownloadIcon(modifier = Modifier.size(14.dp))
                }
            }

            // Brightness and Volume Sliders Row (visible if either gesture HUD is active)
            if (showBrightnessHud || showVolumeHud) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Brightness Slider
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayBright = if (brightnessPercentage < 0f && activity != null) {
                            val lp = activity.window.attributes
                            if (lp.screenBrightness < 0f) {
                                Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
                            } else lp.screenBrightness
                        } else brightnessPercentage
                        BrightnessIcon(modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = { displayBright.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }

                    // Volume Slider
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val displayVol = if (volumePercentage < 0f) {
                            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        } else volumePercentage
                        VolumeIcon(modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = { displayVol.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        // Global download status message overlay
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
}

@Composable
fun SidebarButton(
    label: String,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            iconContent()
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ponytail: Canvas-based icons to avoid importing heavy material-icons-extended dependencies
@Composable
fun PauseIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = Color.White, topLeft = Offset(w * 0.28f, h * 0.2f), size = Size(w * 0.12f, h * 0.6f))
        drawRect(color = Color.White, topLeft = Offset(w * 0.6f, h * 0.2f), size = Size(w * 0.12f, h * 0.6f))
    }
}

@Composable
fun DownloadIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.15f), end = Offset(w * 0.5f, h * 0.65f), strokeWidth = 2.dp.toPx())
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.45f)
            lineTo(w * 0.5f, h * 0.7f)
            lineTo(w * 0.75f, h * 0.45f)
        }
        drawPath(path, color = color, style = Stroke(width = 2.dp.toPx()))
        drawLine(color = color, start = Offset(w * 0.2f, h * 0.85f), end = Offset(w * 0.8f, h * 0.85f), strokeWidth = 2.dp.toPx())
    }
}

@Composable
fun MinimalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun BrightnessIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 4
        drawCircle(color = Color.White, radius = radius)
        
        val rayStart = size.minDimension * 0.35f
        val rayEnd = size.minDimension * 0.48f
        for (i in 0 until 8) {
            val angle = i * Math.PI / 4
            val start = Offset(
                (center.x + rayStart * Math.cos(angle)).toFloat(),
                (center.y + rayStart * Math.sin(angle)).toFloat()
            )
            val end = Offset(
                (center.x + rayEnd * Math.cos(angle)).toFloat(),
                (center.y + rayEnd * Math.sin(angle)).toFloat()
            )
            drawLine(color = Color.White, start = start, end = end, strokeWidth = 2.dp.toPx())
        }
    }
}

@Composable
fun VolumeIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.35f)
            lineTo(w * 0.45f, h * 0.35f)
            lineTo(w * 0.7f, h * 0.15f)
            lineTo(w * 0.7f, h * 0.85f)
            lineTo(w * 0.45f, h * 0.65f)
            lineTo(w * 0.2f, h * 0.65f)
            close()
        }
        drawPath(path, color = Color.White)
        drawArc(
            color = Color.White,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.15f, h * 0.15f),
            size = Size(w * 0.7f, h * 0.7f),
            style = Stroke(width = 2.dp.toPx())
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
