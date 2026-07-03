package com.example.reddittube.ui.main

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.reddittube.RedditTubeApp
import com.example.reddittube.data.RedditError
import kotlinx.coroutines.launch

// ponytail: Main orchestrator — manages HorizontalPager (Explore / Search / Subscribed),
// persistent top header, bottom navigation tabs, and dialog overlays
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val app = context as RedditTubeApp
    val viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(app.container.repository) }
    val exploreState by viewModel.exploreState.collectAsStateWithLifecycle()
    val subscribedState by viewModel.subscribedState.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val horizontalPagerState = rememberPagerState(pageCount = { 3 })

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

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
    val activity = LocalContext.current as? Activity
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
            viewModel.refreshSubscribed(updated.sorted().joinToString("+"))
        }
    }

    fun refreshCurrentFeed() {
        if (horizontalPagerState.currentPage == 0) {
            viewModel.refreshExplore()
        } else {
            viewModel.refreshSubscribed(subscribedSet.sorted().joinToString("+"))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Core HorizontalPager: Explore (0), Search (1), Subscribed (2)
        HorizontalPager(
            state = horizontalPagerState,
            modifier = Modifier.fillMaxSize().padding(bottom = 48.dp)
        ) { pageIndex ->
            when (pageIndex) {
                0 -> FeedPage(
                    uiState = exploreState,
                    modifier = Modifier.fillMaxSize(),
                    subscribedSet = subscribedSet,
                    onSubscribeToggle = toggleSubscription,
                    onLoadMore = { viewModel.loadMore(true) },
                    onRefresh = { viewModel.refreshExplore() },
                    onRemoveVideo = { id, title -> viewModel.markAsWatched(id, title) },
                    isLoadingMore = (exploreState as? MainScreenUiState.Success)?.isLoadingMore ?: false
                )
                1 -> SearchPage(
                    currentSubscribed = subscribedSet,
                    onSubscribeToggle = toggleSubscription,
                    onSubredditSelect = {
                        viewModel.refreshExplore(it)
                        coroutineScope.launch { horizontalPagerState.animateScrollToPage(0) }
                    },
                    searchResults = viewModel.searchResults.collectAsStateWithLifecycle().value,
                    onSearchQuery = { viewModel.searchSubreddits(it) }
                )
                2 -> FeedPage(
                    uiState = subscribedState,
                    modifier = Modifier.fillMaxSize(),
                    subscribedSet = subscribedSet,
                    onSubscribeToggle = toggleSubscription,
                    onLoadMore = { viewModel.loadMore(false) },
                    onRefresh = { viewModel.refreshSubscribed(subscribedSet.sorted().joinToString("+")) },
                    onRemoveVideo = { id, title -> viewModel.markAsWatched(id, title) },
                    isLoadingMore = (subscribedState as? MainScreenUiState.Success)?.isLoadingMore ?: false
                )
            }
        }

        // Persistent top header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("R", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Tube", color = Color.Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            var showMenu by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(12.dp))
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
                            onClick = { showMenu = false; refreshCurrentFeed() },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("History", color = Color.White) },
                            onClick = { showMenu = false; showHistoryDialog = true },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
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
            val isSubscribed = horizontalPagerState.currentPage == 2

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    coroutineScope.launch { horizontalPagerState.animateScrollToPage(0) }
                }.padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = "Explore",
                    tint = if (isExplore) Color.Red else Color.LightGray, modifier = Modifier.size(22.dp))
                Text("Explore", color = if (isExplore) Color.Red else Color.LightGray,
                    fontSize = 11.sp, fontWeight = if (isExplore) FontWeight.Bold else FontWeight.Normal)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    coroutineScope.launch { horizontalPagerState.animateScrollToPage(1) }
                }.padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search",
                    tint = if (horizontalPagerState.currentPage == 1) Color.Red else Color.LightGray, modifier = Modifier.size(22.dp))
                Text("Search", color = if (horizontalPagerState.currentPage == 1) Color.Red else Color.LightGray,
                    fontSize = 11.sp, fontWeight = if (horizontalPagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    if (subscribedSet.isEmpty()) {
                        Toast.makeText(context, "No subreddits subscribed yet! Use search.", Toast.LENGTH_SHORT).show()
                    } else {
                        coroutineScope.launch { horizontalPagerState.animateScrollToPage(2) }
                    }
                }.padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = "Subscribed",
                    tint = if (isSubscribed) Color.Red else Color.LightGray, modifier = Modifier.size(22.dp))
                Text("Subscribed", color = if (isSubscribed) Color.Red else Color.LightGray,
                    fontSize = 11.sp, fontWeight = if (isSubscribed) FontWeight.Bold else FontWeight.Normal)
            }
        }

        // Overlays
        if (showSettingsDialog) SettingsDialog(onDismissRequest = { showSettingsDialog = false })
        if (showHistoryDialog) HistoryDialog(onDismissRequest = { showHistoryDialog = false })
    }
}

// ponytail: Reusable feed page that handles Loading / Success / Error states
@Composable
private fun FeedPage(
    uiState: MainScreenUiState,
    modifier: Modifier = Modifier,
    subscribedSet: Set<String> = emptySet(),
    onSubscribeToggle: (String) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onRemoveVideo: (String, String) -> Unit = { _, _ -> },
    isLoadingMore: Boolean = false
) {
    when (uiState) {
        MainScreenUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.Red)
            }
        }
        is MainScreenUiState.Success -> {
            VideoFeedContent(
                data = uiState.data,
                modifier = modifier,
                subscribedSet = subscribedSet,
                onSubscribeToggle = onSubscribeToggle,
                onLoadMore = onLoadMore,
                onRefresh = onRefresh,
                onRemoveVideo = onRemoveVideo,
                isLoadingMore = isLoadingMore
            )
        }
        is MainScreenUiState.Error -> {
            ErrorPage(
                error = uiState.throwable,
                subscribedSet = subscribedSet,
                onRefresh = onRefresh,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ErrorPage(
    error: RedditError,
    subscribedSet: Set<String>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = error.message ?: "Unknown error"
    val isNoSubscriptions = message.contains("No subscribed subreddits") || message.contains("No subscriptions")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(if (isNoSubscriptions) 32.dp else 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isNoSubscriptions) {
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
        } else {
            Text(
                text = if (error is RedditError.MissingClientId) "Configuration Required" else "Failed to load videos.",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = Color.LightGray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Try Again", color = Color.White)
            }
        }
    }
}
