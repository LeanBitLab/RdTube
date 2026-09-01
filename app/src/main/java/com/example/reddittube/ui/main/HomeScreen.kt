package com.lean.reddittube.ui.main

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lean.reddittube.data.RedditError
import com.lean.reddittube.data.RedditPost
import com.lean.reddittube.theme.*
import com.lean.reddittube.ui.main.components.CommentsBottomSheet
import com.lean.reddittube.ui.main.components.SearchBottomSheet
import com.lean.reddittube.ui.main.components.SectionLoadingIndicator
import com.lean.reddittube.ui.main.components.SortBottomSheet
import com.lean.reddittube.ui.main.components.ThumbnailImage
import com.lean.reddittube.utils.formatDuration
import com.lean.reddittube.utils.formatScore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class RdTab(
    val title: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
) {
    EXPLORE("Explore", Icons.Outlined.Explore, Icons.Filled.Explore),
    SUBSCRIBED("Subs", Icons.Outlined.Subscriptions, Icons.Filled.Subscriptions),
    LIBRARY("Library", Icons.Outlined.VideoLibrary, Icons.Filled.VideoLibrary),
    ABOUT("About", Icons.Outlined.Info, Icons.Filled.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainScreenViewModel,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val exploreState by viewModel.exploreState.collectAsStateWithLifecycle()
    val subscribedState by viewModel.subscribedState.collectAsStateWithLifecycle()
    val subscribedSubreddits by viewModel.subscribedSubreddits.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val horizontalPagerState = rememberPagerState(pageCount = { 4 })

    var bottomBarVisible by remember { mutableStateOf(true) }
    var activeCommentPost by remember { mutableStateOf<RedditPost?>(null) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (exploreState is MainScreenUiState.Loading) viewModel.refreshExplore()
        if (subscribedSubreddits.isNotEmpty() && subscribedState is MainScreenUiState.Loading) {
            viewModel.refreshSubscribed(subscribedSubreddits.sorted().joinToString("+"))
        }
    }
    LaunchedEffect(horizontalPagerState.currentPage) {
        bottomBarVisible = true
    }

    fun refreshCurrentFeed() {
        if (horizontalPagerState.currentPage == 0) {
            viewModel.refreshExplore()
        } else if (horizontalPagerState.currentPage == 1) {
            viewModel.refreshSubscribed(subscribedSubreddits.sorted().joinToString("+"))
        }
    }

    val defaultExploreQuery = "popular"
    val defaultSubscribedQuery = remember(subscribedSubreddits) { subscribedSubreddits.sorted().joinToString("+") }
    val isCustomExplore = viewModel.exploreQuery != defaultExploreQuery
    val isCustomSubscribed = viewModel.subscribedQuery.isNotEmpty() && viewModel.subscribedQuery != defaultSubscribedQuery

    val canGoBack = (horizontalPagerState.currentPage == 0 && isCustomExplore) ||
            (horizontalPagerState.currentPage == 1 && isCustomSubscribed) ||
            horizontalPagerState.currentPage != 0

    BackHandler(enabled = canGoBack) {
        when {
            horizontalPagerState.currentPage == 0 && isCustomExplore -> {
                viewModel.refreshExplore(defaultExploreQuery)
            }
            horizontalPagerState.currentPage == 1 && isCustomSubscribed -> {
                viewModel.refreshSubscribed(defaultSubscribedQuery)
            }
            horizontalPagerState.currentPage != 0 -> {
                coroutineScope.launch { horizontalPagerState.animateScrollToPage(0) }
            }
        }
    }

    val likedIds by viewModel.likedIdsFlow.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = horizontalPagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false,
            beyondViewportPageCount = 1
        ) { pageIndex ->
            when (pageIndex) {
                0 -> BrowseGrid(
                    uiState = exploreState,
                    likedIds = likedIds,
                    onLike = viewModel::toggleLike,
                    onItemClick = { list: List<RedditPost>, index: Int -> viewModel.openPlayer(list, index, "explore"); onItemClick() },
                    onSubredditClick = { sub ->
                        viewModel.refreshExplore(sub)
                        coroutineScope.launch { horizontalPagerState.scrollToPage(0) }
                    },
                    onCommentClick = { post -> activeCommentPost = post },
                    onLoadMore = { viewModel.loadMore(true) },
                    onRefresh = { viewModel.refreshExplore() },
                    onRemoveVideo = viewModel::hidePost,
                    onScrollDirection = { bottomBarVisible = !it },
                    isLoadingMore = (exploreState as? MainScreenUiState.Success)?.isLoadingMore ?: false
                )
                1 -> BrowseGrid(
                    uiState = subscribedState,
                    likedIds = likedIds,
                    onLike = viewModel::toggleLike,
                    onItemClick = { list: List<RedditPost>, index: Int -> viewModel.openPlayer(list, index, "subscribed"); onItemClick() },
                    onSubredditClick = { sub ->
                        viewModel.refreshSubscribed(sub)
                    },
                    onCommentClick = { post -> activeCommentPost = post },
                    onLoadMore = { viewModel.loadMore(false) },
                    onRefresh = { viewModel.refreshSubscribed(subscribedSubreddits.sorted().joinToString("+")) },
                    onRemoveVideo = viewModel::hidePost,
                    onScrollDirection = { bottomBarVisible = !it },
                    isLoadingMore = (subscribedState as? MainScreenUiState.Success)?.isLoadingMore ?: false
                )
                2 -> LibraryPage(
                    viewModel = viewModel,
                    onItemClick = { list, index ->
                        viewModel.openPlayer(list, index, "other")
                        onItemClick()
                    },
                    onSubredditClick = { sub ->
                        viewModel.refreshExplore(sub)
                        coroutineScope.launch { horizontalPagerState.scrollToPage(0) }
                    },
                    onCommentClick = { post -> activeCommentPost = post },
                    onShowAbout = {
                        coroutineScope.launch { horizontalPagerState.animateScrollToPage(3) }
                    }
                )
                3 -> AboutPage(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Minimal Clean Top Header: Monochrome Brand mark only
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(TopBarHeight)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = HPad),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (horizontalPagerState.currentPage != 0) {
                                coroutineScope.launch { horizontalPagerState.animateScrollToPage(0) }
                            } else {
                                viewModel.refreshExplore(defaultExploreQuery)
                            }
                        }
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color.White, shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Rd", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Tube", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }

        // Bottom Controls & Floating Pill Dock Container
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // One-Handed Quick Actions Ribbon (Explore / Subscribed feeds)
            AnimatedVisibility(
                visible = bottomBarVisible && (horizontalPagerState.currentPage == 0 || horizontalPagerState.currentPage == 1),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(160)),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(160))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Search Bar Pill
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .clickable { showSearchSheet = true },
                        shape = RoundedCornerShape(19.dp),
                        color = SurfaceRaised.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Search subreddits...", color = TextMuted, fontSize = 12.sp)
                        }
                    }

                    // Sort Bottom Sheet Trigger Pill
                    Surface(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .clickable { showSortSheet = true },
                        shape = RoundedCornerShape(19.dp),
                        color = SurfaceRaised.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(viewModel.currentSort.label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Ultra-Smooth, Minimalist Monochrome Floating Pill Navigation Dock
            AnimatedVisibility(
                visible = bottomBarVisible || horizontalPagerState.currentPage == 2 || horizontalPagerState.currentPage == 3,
                enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(tween(180)),
                exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(tween(180))
            ) {
                FloatingPillNavBar(
                    selectedTab = RdTab.entries[horizontalPagerState.currentPage],
                    onTabSelected = { tab ->
                        if (tab.ordinal == 1 && subscribedSubreddits.isEmpty()) {
                            Toast.makeText(context, "No subreddits subscribed yet! Use search.", Toast.LENGTH_SHORT).show()
                        } else {
                            if (tab.ordinal == 0 && viewModel.exploreQuery != defaultExploreQuery) {
                                viewModel.refreshExplore(defaultExploreQuery)
                            }
                            if (tab.ordinal == 1 && viewModel.subscribedQuery != defaultSubscribedQuery) {
                                viewModel.refreshSubscribed(defaultSubscribedQuery)
                            }
                            coroutineScope.launch {
                                horizontalPagerState.animateScrollToPage(
                                    page = tab.ordinal,
                                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                                )
                            }
                        }
                    }
                )
            }
        }

        // Bottom Search Sheet
        if (showSearchSheet) {
            SearchBottomSheet(
                currentSubscribed = subscribedSubreddits,
                onSubscribeToggle = viewModel::toggleSubscription,
                onSubredditSelect = { sub ->
                    viewModel.refreshExplore(sub)
                    coroutineScope.launch { horizontalPagerState.scrollToPage(0) }
                },
                searchResults = viewModel.searchResults.collectAsStateWithLifecycle().value,
                onSearchQuery = { viewModel.searchSubreddits(it) },
                onDismiss = { showSearchSheet = false }
            )
        }

        // Bottom Sort Sheet
        if (showSortSheet) {
            SortBottomSheet(
                currentSort = viewModel.currentSort,
                onSortSelected = { viewModel.setSort(it) },
                onDismiss = { showSortSheet = false }
            )
        }

        // Comments Bottom Sheet
        if (activeCommentPost != null) {
            CommentsBottomSheet(
                post = activeCommentPost!!,
                viewModel = viewModel,
                onDismiss = { activeCommentPost = null }
            )
        }
    }
}

@Composable
private fun FloatingPillNavBar(
    selectedTab: RdTab,
    onTabSelected: (RdTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = RdTab.entries
    val haptic = LocalHapticFeedback.current

    val springSpec = spring<androidx.compose.ui.unit.Dp>(
        stiffness = 850f,
        dampingRatio = 0.85f
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        color = SurfaceBar.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, GlassBorder),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabWidth = maxWidth / tabs.size

            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedTab.ordinal,
                animationSpec = springSpec,
                label = "PillIndicator"
            )

            // Animated sliding high-contrast white highlight pill
            Box(
                modifier = Modifier
                    .padding(vertical = 5.dp, horizontal = 5.dp)
                    .width(tabWidth - 10.dp)
                    .fillMaxHeight()
                    .offset(x = indicatorOffset)
                    .clip(RoundedCornerShape(23.dp))
                    .background(Color.White)
            )

            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
                    val isSelected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTabSelected(tab)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                tint = if (isSelected) Color.Black else TextSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = tab.title,
                                color = if (isSelected) Color.Black else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// ponytail: grid browse for a feed, with infinite scroll trigger
@Composable
private fun BrowseGrid(
    uiState: MainScreenUiState,
    likedIds: Set<String>,
    onLike: (RedditPost) -> Unit,
    onItemClick: (List<RedditPost>, Int) -> Unit,
    onSubredditClick: (String) -> Unit = {},
    onCommentClick: (RedditPost) -> Unit = {},
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onRemoveVideo: (RedditPost) -> Unit = {},
    isLoadingMore: Boolean,
    onScrollDirection: (Boolean) -> Unit = {}
) {
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var showRefreshingBadge by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                                    if (totalY > 150f) {
                                        dragEvent.changes.forEach { it.consume() }
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onRefresh()
                                        showRefreshingBadge = true
                                        coroutineScope.launch {
                                            delay(1400)
                                            showRefreshingBadge = false
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
    ) {
        when (uiState) {
            MainScreenUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = TopBarHeight + 8.dp, start = 12.dp, end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(4) {
                        val transition = rememberInfiniteTransition(label = "shimmer")
                        val pulseAlpha by transition.animateFloat(
                            initialValue = 0.12f,
                            targetValue = 0.30f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseAlpha"
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceRaised.copy(alpha = pulseAlpha),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {}
                    }
                }
            }
            is MainScreenUiState.Error -> {
                ErrorPage(
                    error = uiState.throwable,
                    subscribedSet = emptySet(),
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is MainScreenUiState.Success -> {
                val data = uiState.data
                if (data.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(top = TopBarHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No videos available",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Try searching subreddits or refresh",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Refresh", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    val distinctData = remember(data) { data.distinctBy { it.id } }
                    val gridState = rememberLazyGridState()
                    LaunchedEffect(gridState.firstVisibleItemIndex, distinctData.size, isLoadingMore) {
                        val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: gridState.firstVisibleItemIndex
                        if (distinctData.isNotEmpty() && !isLoadingMore && lastVisible >= (distinctData.size - 5).coerceAtLeast(0)) {
                            onLoadMore()
                        }
                    }
                    var prevIndex by remember { mutableIntStateOf(0) }
                    var prevOffset by remember { mutableIntStateOf(0) }
                    LaunchedEffect(Unit) {
                        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
                            .collect { (index, offset) ->
                                val atTop = index == 0 && offset == 0
                                val scrollingDown = index > prevIndex || (index == prevIndex && offset > prevOffset)
                                prevIndex = index
                                prevOffset = offset
                                onScrollDirection(!atTop && scrollingDown)
                            }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(top = TopBarHeight, bottom = 8.dp),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = distinctData,
                            key = { post -> post.id }
                        ) { post ->
                            val index = distinctData.indexOf(post).coerceAtLeast(0)
                            VideoCard(
                                post = post,
                                isLiked = likedIds.contains(post.id),
                                onLike = onLike,
                                onClick = { onItemClick(distinctData, index) },
                                onSubredditClick = onSubredditClick,
                                onCommentClick = onCommentClick,
                                onRemoveVideo = onRemoveVideo
                            )
                        }
                        if (isLoadingMore) {
                            item(span = { GridItemSpan(1) }, key = "loading_more_indicator") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.Black.copy(alpha = 0.88f),
                                        border = BorderStroke(1.dp, GlassBorder)
                                    ) {
                                        SectionLoadingIndicator(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            label = "Loading more videos…"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Transient top-center "Refreshing feed…" badge
        AnimatedVisibility(
            visible = showRefreshingBadge,
            enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { h -> -h },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { h -> -h },
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = TopBarHeight + 8.dp)
                .align(Alignment.TopCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Refreshing feed…",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun VideoCard(
    post: RedditPost,
    isLiked: Boolean,
    onLike: (RedditPost) -> Unit,
    onClick: () -> Unit,
    onSubredditClick: (String) -> Unit = {},
    onCommentClick: (RedditPost) -> Unit = {},
    onRemoveVideo: (RedditPost) -> Unit = {}
) {
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isHiding by remember { mutableStateOf(false) }

    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(
            stiffness = 300f,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "VideoCardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(post.id) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onLongPress = {
                            if (!isHiding) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                isHiding = true
                                coroutineScope.launch {
                                    delay(300)
                                    onRemoveVideo(post)
                                }
                            }
                        },
                        onTap = { if (!isHiding) onClick() }
                    )
                },
            shape = RoundedCornerShape(16.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, GlassBorder),
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(Color.Black)
                ) {
                    ThumbnailImage(url = post.thumbnailUrl, contentDescription = post.title, modifier = Modifier.fillMaxSize())

                    // Glassmorphic Video Duration Badge (Bottom-Right)
                    val durationStr = formatDuration(post.duration)
                    if (durationStr.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .padding(10.dp)
                                .align(Alignment.BottomEnd),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.80f),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Text(
                                text = durationStr,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Center Play Overlay Icon
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.60f))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play video",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Metadata Details
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        post.title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Upvote / Like Button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onLike(post) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    if (isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                                    contentDescription = "Like",
                                    tint = if (isLiked) Color.White else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    formatScore(if (isLiked) post.score + 1 else post.score),
                                    color = if (isLiked) Color.White else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Comments Counter & Opener
                            if (post.numComments > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onCommentClick(post) }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ChatBubbleOutline,
                                        contentDescription = "Comments",
                                        tint = TextMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        formatScore(post.numComments),
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Subreddit tag
                        if (post.subreddit.isNotEmpty()) {
                            Text(
                                text = "r/${post.subreddit}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onSubredditClick(post.subreddit) },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Hiding overlay indicator
        AnimatedVisibility(
            visible = isHiding,
            enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.92f),
            exit = fadeOut(tween(150)),
            modifier = Modifier.matchParentSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Hidden",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Card Hidden",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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
            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("No Subscriptions", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tap the search bar in the bottom controls to discover and subscribe to subreddits.",
                color = Color.LightGray,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )
        } else {
            Text("Failed to load videos.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = Color.LightGray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                Text("Try Again", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
