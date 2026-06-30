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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.activity.compose.BackHandler
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
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
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

    // BackHandler: navigate HorizontalPager back, double-tap on first page to exit
    var backPressTime by remember { mutableStateOf(0L) }
    val activity = androidx.compose.ui.platform.LocalContext.current as? Activity
    BackHandler {
        val current = horizontalPagerState.currentPage
        if (current > 0) {
            coroutineScope.launch { horizontalPagerState.animateScrollToPage(current - 1) }
        } else {
            val now = System.currentTimeMillis()
            if (now - backPressTime < 2000) {
                activity?.finish()
            } else {
                backPressTime = now
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
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
            modifier = Modifier.fillMaxSize().padding(bottom = 48.dp)
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
                            modifier = Modifier.fillMaxSize(),
                            subscribedSet = subscribedSet,
                            onSubscribeToggle = toggleSubscription,
                            onLoadMore = { viewModel.loadMore(true) }
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
                            modifier = Modifier.fillMaxSize(),
                            subscribedSet = subscribedSet,
                            onSubscribeToggle = toggleSubscription,
                            onLoadMore = { viewModel.loadMore(false) }
                        )
                    }
                    is MainScreenUiState.Error -> {
                        val errorMsg = uiState.throwable.message ?: ""
                        if (errorMsg.contains("No subscribed subreddits") || errorMsg.contains("No subscriptions")) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = Color.Red,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Subscriptions",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap the search icon in the top right to discover and subscribe to subreddits.",
                                    color = Color.LightGray,
                                    fontSize = 14.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        } else {
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
                    text = "R",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tube",
                    color = Color.Red,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Search icon + Settings/Refresh menu
            var showMenu by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { showSearchDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Search", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Settings", color = Color.White) },
                            onClick = { showMenu = false; showSettingsDialog = true },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Refresh", color = Color.White) },
                            onClick = {
                                showMenu = false
                                if (horizontalPagerState.currentPage == 0) {
                                    viewModel.refreshExplore()
                                } else {
                                    viewModel.refreshSubscribed(subscribedSet.sorted().joinToString("+"))
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        }

        // Bottom navigation tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .background(Color.Black.copy(alpha = 0.95f))
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isExplore = horizontalPagerState.currentPage == 0
            val isSubscribed = horizontalPagerState.currentPage == 1

            // Explore Tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    coroutineScope.launch { horizontalPagerState.animateScrollToPage(0) }
                }.padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Explore",
                    tint = if (isExplore) Color.Red else Color.LightGray,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Explore",
                    color = if (isExplore) Color.Red else Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = if (isExplore) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Subscribed Tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    if (subscribedSet.isEmpty()) {
                        Toast.makeText(context, "No subreddits subscribed yet! Use search.", Toast.LENGTH_SHORT).show()
                    } else {
                        coroutineScope.launch { horizontalPagerState.animateScrollToPage(1) }
                    }
                }.padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Subscribed",
                    tint = if (isSubscribed) Color.Red else Color.LightGray,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Subscribed",
                    color = if (isSubscribed) Color.Red else Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = if (isSubscribed) FontWeight.Bold else FontWeight.Normal
                )
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
                },
                searchResults = viewModel.searchResults.collectAsStateWithLifecycle().value,
                onSearchQuery = { viewModel.searchSubreddits(it) }
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
    modifier: Modifier = Modifier,
    subscribedSet: Set<String> = emptySet(),
    onSubscribeToggle: (String) -> Unit = {},
    onLoadMore: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { data.size })
    // ponytail: trigger loadMore when reaching the last page
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    LaunchedEffect(currentPage) {
        if (currentPage >= data.size - 1 && data.isNotEmpty()) onLoadMore()
    }
    Box(modifier = modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index -> data[index].id }
        ) { pageIndex ->
            VideoPage(
                post = data[pageIndex],
                isActive = pagerState.currentPage == pageIndex,
                subscribedSet = subscribedSet,
                onSubscribeToggle = onSubscribeToggle
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
    var redirectUriInput by remember { mutableStateOf(RedditOAuthHelper.getRedirectUri(context)) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.Black,
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
                    RedditOAuthHelper.saveApiCredentials(context, clientIdInput, userAgentInput, redirectUriInput)
                    Toast.makeText(context, "Settings saved. Please refresh.", Toast.LENGTH_SHORT).show()
                    onDismissRequest()
                }
            ) {
                Text("Save", color = Color.Red, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // API Section
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
                    text = "Redirect URI",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = redirectUriInput,
                    onValueChange = { redirectUriInput = it },
                    placeholder = { Text("Enter Redirect URI...", color = Color.Gray) },
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
                    text = "Note: If left empty, defaults to the official RedReader Client ID, User Agent, and Redirect URI so the app works out of the box.",
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
    onSubredditSelect: (String) -> Unit,
    searchResults: List<String>,
    onSearchQuery: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val recommended = listOf("nextfuckinglevel", "interestingasfuck", "animalsdoingstuff", "oddlysatisfying", "aww", "gaming")

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header with close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Discover",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "r/edditTube",
                        color = Color.Red.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Premium Search Bar
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search subreddits...",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 15.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    val query = searchQuery.trim().replace(" ", "")
                                    if (query.isNotEmpty()) {
                                        onSubredditSelect(query)
                                        onDismissRequest()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = "Go",
                                    tint = Color.Red,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.Red,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Search results from Reddit API
                val trimmedQuery = searchQuery.trim().lowercase()
                if (trimmedQuery.isNotEmpty()) {
                    LaunchedEffect(trimmedQuery) { onSearchQuery(trimmedQuery) }
                    if (searchResults.isNotEmpty()) {
                        Column(modifier = Modifier.wrapContentHeight()) {
                            searchResults.forEach { sub ->
                                val isSubbed = currentSubscribed.contains(sub.lowercase())
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .clickable { onSubredditSelect(sub); onDismissRequest() }
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.Red.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) { Text(sub.take(1).uppercase(), color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("r/$sub", color = Color.White, fontSize = 14.sp)
                                    }
                                    TextButton(onClick = { onSubscribeToggle(sub) }) {
                                        Text(if (isSubbed) "Unsub" else "Sub", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Subscriptions Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "My Subscriptions",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        "${currentSubscribed.size}",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Subscriptions List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (currentSubscribed.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "No subscriptions yet",
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            currentSubscribed.sorted().forEachIndexed { i, sub ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (i % 2 == 0) Color.White.copy(alpha = 0.03f) else Color.Transparent)
                                        .clickable {
                                            onSubredditSelect(sub)
                                            onDismissRequest()
                                        }
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color.Red.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                sub.take(1).uppercase(),
                                                color = Color.Red,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("r/$sub", color = Color.White, fontSize = 15.sp)
                                    }
                                    IconButton(onClick = { onSubscribeToggle(sub) }, modifier = Modifier.size(28.dp)) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = Color.White.copy(alpha = 0.3f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Recommended Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Trending",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.Red.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recommended.forEach { rec ->
                        val isSubbed = currentSubscribed.contains(rec.lowercase())
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSubbed) Color.Red.copy(alpha = 0.15f)
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .clickable {
                                    onSubredditSelect(rec)
                                    onDismissRequest()
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "r/$rec",
                                    color = if (isSubbed) Color.Red else Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (isSubbed) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (isSubbed) Color.Red else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Close button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable { onDismissRequest() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Close", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun VideoPage(
    post: RedditPost,
    isActive: Boolean,
    subscribedSet: Set<String> = emptySet(),
    onSubscribeToggle: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPreferences = remember { context.getSharedPreferences("reddittube_prefs", Context.MODE_PRIVATE) }
    
    // Player State
    var currentQuality by remember { mutableStateOf(sharedPreferences.getString("saved_quality", "Auto") ?: "Auto") }
    val exoPlayer = remember(post.id) {
        val url = post.videoUrl
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
            repeatMode = Player.REPEAT_MODE_ONE
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(url))
                .setMimeType("video/mp4")
                .build()
            setMediaItem(mediaItem)
            playWhenReady = true
            prepare()
        }
        player
    }

    // Buffering & Playback status
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    
    DisposableEffect(post.id) {
        val tag = "VideoPlayer"
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                val s = when (state) { Player.STATE_IDLE -> "IDLE"; Player.STATE_BUFFERING -> "BUFFERING"; Player.STATE_READY -> "READY"; Player.STATE_ENDED -> "ENDED"; else -> "$state" }
                Log.i(tag, "state=$s")
                isBuffering = state == Player.STATE_BUFFERING
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                Log.i(tag, "playing=$playing")
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
            // Subreddit/Author details with subscribe toggle
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
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = if (isSubbed) 0.2f else 0.15f))
                        .clickable { onSubscribeToggle(post.subreddit.lowercase()) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSubbed) "✓" else "+",
                        color = Color.Red,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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

                // Rotation Lock Toggle — icon only, no label
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
                    label = ""
                ) {
                    if (isRotationLocked) {
                        LockIcon(modifier = Modifier.size(14.dp), tint = Color.Red)
                    } else {
                        LockOpenIcon(modifier = Modifier.size(14.dp), tint = Color.White)
                    }
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
                        containerColor = Color.Black,
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

            // Brightness and Volume vertical sliders on each side
        }

        // Left vertical brightness slider
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
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackW / 2f)
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset((size.width - trackW) / 2f, size.height - fillH),
                            size = Size(trackW, fillH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackW / 2f)
                        )
                    }
                    BrightnessIcon(modifier = Modifier.size(16.dp).align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // Right vertical volume slider
        if (showVolumeHud) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val displayVol = if (volumePercentage < 0f) {
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
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
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackW / 2f)
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset((size.width - trackW) / 2f, size.height - fillH),
                            size = Size(trackW, fillH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackW / 2f)
                        )
                    }
                    VolumeIcon(modifier = Modifier.size(16.dp).align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(20.dp))
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

// ponytail: Canvas lock icons — cleaner geometry for small sizes
@Composable
fun LockIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier.padding(1.dp)) {
        val s = size.minDimension
        val stroke = s * 0.12f
        // shackle (rounded arc)
        drawArc(tint, 160f, 220f, false,
            topLeft = Offset(s * 0.3f, s * 0.08f),
            size = Size(s * 0.4f, s * 0.38f),
            style = Stroke(width = stroke)
        )
        // body (filled rectangle)
        drawRoundRect(tint, Offset(s * 0.2f, s * 0.38f), Size(s * 0.6f, s * 0.55f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f)
        )
        // keyhole
        drawCircle(tint, s * 0.06f, Offset(s * 0.5f, s * 0.58f))
        drawLine(tint, Offset(s * 0.5f, s * 0.64f), Offset(s * 0.5f, s * 0.78f), stroke * 0.8f)
    }
}

@Composable
fun LockOpenIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier.padding(1.dp)) {
        val s = size.minDimension
        val stroke = s * 0.12f
        // open shackle (gap at bottom-right)
        drawArc(tint, 150f, 230f, false,
            topLeft = Offset(s * 0.3f, s * 0.08f),
            size = Size(s * 0.4f, s * 0.38f),
            style = Stroke(width = stroke)
        )
        // body (filled)
        drawRoundRect(tint, Offset(s * 0.2f, s * 0.38f), Size(s * 0.6f, s * 0.55f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f)
        )
        // keyhole
        drawCircle(tint, s * 0.06f, Offset(s * 0.5f, s * 0.58f))
        drawLine(tint, Offset(s * 0.5f, s * 0.64f), Offset(s * 0.5f, s * 0.78f), stroke * 0.8f)
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
