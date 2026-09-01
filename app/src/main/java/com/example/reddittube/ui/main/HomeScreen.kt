package com.lean.reddittube.ui.main
import com.lean.reddittube.theme.*

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
// ponytail: single-column browse
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lean.reddittube.data.RedditError
import com.lean.reddittube.data.RedditPost
import com.lean.reddittube.ui.main.SortOption
import com.lean.reddittube.utils.formatScore
import com.lean.reddittube.utils.toJson
import com.lean.reddittube.utils.toRedditPost
import com.lean.reddittube.ui.main.components.ThumbnailImage
import com.lean.reddittube.ui.main.components.SectionLoadingIndicator
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import org.json.JSONArray
import org.json.JSONObject

private enum class PanelView { Menu, History, Liked }

// ponytail: YouTube-style browse landing — thumbnail grid for Explore/Subscribed, Search tab, then click → Player
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

    var showPanel by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    var panelView by remember { mutableStateOf(PanelView.Menu) }
    var bottomBarVisible by remember { mutableStateOf(true) }

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

    val canGoBack = showPanel ||
            panelView != PanelView.Menu ||
            (horizontalPagerState.currentPage == 0 && isCustomExplore) ||
            (horizontalPagerState.currentPage == 2 && isCustomSubscribed) ||
            horizontalPagerState.currentPage != 0

    BackHandler(enabled = canGoBack) {
        when {
            showPanel -> showPanel = false
            panelView != PanelView.Menu -> panelView = PanelView.Menu
            horizontalPagerState.currentPage == 0 && isCustomExplore -> {
                viewModel.refreshExplore(defaultExploreQuery)
            }
            horizontalPagerState.currentPage == 2 && isCustomSubscribed -> {
                viewModel.refreshSubscribed(defaultSubscribedQuery)
            }
            horizontalPagerState.currentPage != 0 -> {
                coroutineScope.launch { horizontalPagerState.animateScrollToPage(0) }
            }
        }
    }

    val likedIds by viewModel.likedIdsFlow.collectAsStateWithLifecycle()
    val likedPosts = viewModel.getLikedPosts()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                var totalX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalX = 0f },
                    onDragCancel = { totalX = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalX += dragAmount
                    },
                    onDragEnd = {
                        val threshold = size.width * 0.25f
                        if (totalX <= -threshold) {
                            panelView = PanelView.Menu
                            showPanel = true
                        }
                        totalX = 0f
                    }
                )
            }
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
                    onLoadMore = { viewModel.loadMore(false) },
                    onRefresh = { viewModel.refreshSubscribed(subscribedSubreddits.sorted().joinToString("+")) },
                    onRemoveVideo = viewModel::hidePost,
                    onScrollDirection = { bottomBarVisible = !it },
                    isLoadingMore = (subscribedState as? MainScreenUiState.Success)?.isLoadingMore ?: false
                )
                2 -> SearchPage(
                    currentSubscribed = subscribedSubreddits,
                    onSubscribeToggle = viewModel::toggleSubscription,
                    onSubredditSelect = {
                        viewModel.refreshExplore(it)
                        coroutineScope.launch { horizontalPagerState.scrollToPage(0) }
                    },
                    searchResults = viewModel.searchResults.collectAsStateWithLifecycle().value,
                    onSearchQuery = { viewModel.searchSubreddits(it) }
                )
                3 -> AboutPage()
            }
        }

        // Top header with subtle scrim + brand mark
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(TopBarHeight)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = HPad),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(BrandRed, shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("Rd", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    Text("Tube", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black, shape = CircleShape)
                        .border(BorderStroke(1.dp, GlassBorder), shape = CircleShape)
                        .clickable { showPanel = true; panelView = PanelView.Menu },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Solid full-width bottom navigation bar — hides on scroll down (except search page), black background
        AnimatedVisibility(
            visible = bottomBarVisible || horizontalPagerState.currentPage == 2,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = Color.Black,
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 10.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(
                        icon = Icons.Default.Home,
                        label = "Explore",
                        selected = horizontalPagerState.currentPage == 0,
                        onClick = {
                            if (viewModel.exploreQuery != defaultExploreQuery) {
                                viewModel.refreshExplore(defaultExploreQuery)
                            }
                            coroutineScope.launch { horizontalPagerState.animateScrollToPage(0) }
                        }
                    )
                    BottomNavItem(
                        icon = Icons.Default.Star,
                        label = "Subscribed",
                        selected = horizontalPagerState.currentPage == 1,
                        onClick = {
                            if (subscribedSubreddits.isEmpty()) {
                                Toast.makeText(context, "No subreddits subscribed yet! Use search.", Toast.LENGTH_SHORT).show()
                            } else {
                                if (viewModel.subscribedQuery != defaultSubscribedQuery) {
                                    viewModel.refreshSubscribed(defaultSubscribedQuery)
                                }
                                coroutineScope.launch { horizontalPagerState.animateScrollToPage(1) }
                            }
                        }
                    )
                    BottomNavItem(
                        icon = Icons.Default.Search,
                        label = "Search",
                        selected = horizontalPagerState.currentPage == 2,
                        onClick = { coroutineScope.launch { horizontalPagerState.animateScrollToPage(2) } }
                    )
                    BottomNavItem(
                        icon = Icons.Default.Info,
                        label = "About",
                        selected = horizontalPagerState.currentPage == 3,
                        onClick = { coroutineScope.launch { horizontalPagerState.animateScrollToPage(3) } }
                    )
                }
            }
        }

        // Right-side panel (does not overlap status bar, starts below status bar)
        AnimatedVisibility(
            visible = showPanel,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Scrim)
                    .clickable { showPanel = false }
                    .pointerInput(Unit) {
                        var totalX = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalX = 0f },
                            onDragCancel = { totalX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalX += dragAmount
                            },
                            onDragEnd = {
                                val threshold = size.width * 0.25f
                                if (totalX >= threshold) showPanel = false
                                totalX = 0f
                            }
                        )
                    }
            ) {
                Surface(
                    color = Color.Black,
                    border = BorderStroke(1.dp, GlassBorder),
                    shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                    shadowElevation = 24.dp,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .widthIn(max = 400.dp)
                        .fillMaxWidth(0.85f)
                        .clickable { }
                        .pointerInput(Unit) {
                            var totalX = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalX = 0f },
                                onDragCancel = { totalX = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalX += dragAmount
                                },
                                onDragEnd = {
                                    val threshold = size.width * 0.25f
                                    if (totalX >= threshold) showPanel = false
                                    totalX = 0f
                                }
                            )
                        }
                ) {
                    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        // Panel header: back (if subview) + title + close
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (panelView != PanelView.Menu) {
                                    Box(
                                        modifier = Modifier.size(32.dp).clickable { panelView = PanelView.Menu },
                                        contentAlignment = Alignment.Center
                                    ) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(22.dp)) }
                                    Spacer(Modifier.width(8.dp))
                                }
                                val titleText = when (panelView) { PanelView.Menu -> "RdTube"; PanelView.History -> "History"; PanelView.Liked -> "Liked" }
                                Text(titleText, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier.size(32.dp).clickable { showPanel = false },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(22.dp)) }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        when (panelView) {
                            PanelView.Menu -> PanelMenu(
                                sortExpanded = sortExpanded,
                                onToggleSort = { sortExpanded = !sortExpanded },
                                currentSort = viewModel.currentSort,
                                onSort = { showPanel = false; viewModel.setSort(it) },
                                onRefresh = { showPanel = false; refreshCurrentFeed() },
                                onHistory = { panelView = PanelView.History },
                                onLiked = { panelView = PanelView.Liked }
                            )
                            PanelView.History -> PostGridContent(
                                posts = viewModel.getWatchedPosts(),
                                likedIds = likedIds,
                                onLike = viewModel::toggleLike,
                                onItemClick = { list, index -> showPanel = false; viewModel.openPlayer(list, index, "other"); onItemClick() }
                            )
                            PanelView.Liked -> PostGridContent(
                                posts = likedPosts,
                                likedIds = likedIds,
                                onLike = viewModel::toggleLike,
                                onItemClick = { list, index -> showPanel = false; viewModel.openPlayer(list, index, "other"); onItemClick() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (selected) BrandRed.copy(alpha = 0.18f) else Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) BrandRed else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = if (selected) BrandRed else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun PanelMenu(
    sortExpanded: Boolean,
    onToggleSort: () -> Unit,
    currentSort: SortOption,
    onSort: (SortOption) -> Unit,
    onRefresh: () -> Unit,
    onHistory: () -> Unit,
    onLiked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Menu & Options",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        // Loop Video Toggle Card
        val context = LocalContext.current
        val hapticFeedback = LocalHapticFeedback.current
        val sharedPreferences = remember { context.getSharedPreferences("rdtube_prefs", Context.MODE_PRIVATE) }
        var isLoopEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("loop_video", true)) }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        isLoopEnabled = !isLoopEnabled
                        sharedPreferences.edit().putBoolean("loop_video", isLoopEnabled).apply()
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Loop Video",
                        tint = if (isLoopEnabled) BrandRed else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Loop Video",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isLoopEnabled) "Repeats video endlessly" else "Stops when video ends",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Switch(
                    checked = isLoopEnabled,
                    onCheckedChange = { checked ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        isLoopEnabled = checked
                        sharedPreferences.edit().putBoolean("loop_video", checked).apply()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BrandRed,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceBase
                    )
                )
            }
        }

        // Refresh card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onRefresh() },
            shape = RoundedCornerShape(14.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White, modifier = Modifier.size(18.dp))
                Text("Refresh Feed", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Minimal Expandable Sort Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, if (sortExpanded) BrandRed.copy(alpha = 0.5f) else GlassBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleSort() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            tint = BrandRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text("Sort Posts", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Current: ${currentSort.label}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Icon(
                        imageVector = if (sortExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Expandable Sort Options
                AnimatedVisibility(
                    visible = sortExpanded,
                    enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(180))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(bottom = 6.dp))
                        SortOption.entries.forEach { sort ->
                            val isSelected = currentSort == sort
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSort(sort) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) BrandRed.copy(alpha = 0.15f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sort.label,
                                        color = if (isSelected) BrandRed else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = BrandRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // History card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onHistory() },
            shape = RoundedCornerShape(14.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = "History", tint = Color.White, modifier = Modifier.size(18.dp))
                Text("Watch History", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Liked card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onLiked() },
            shape = RoundedCornerShape(14.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Favorite, contentDescription = "Liked", tint = BrandRed, modifier = Modifier.size(18.dp))
                Text("Liked Videos", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                            initialValue = 0.15f,
                            targetValue = 0.40f,
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
                                colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Refresh", color = Color.White, fontWeight = FontWeight.Bold)
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
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    color = SurfaceBase.copy(alpha = 0.88f),
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
                color = SurfaceBase.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, BrandRed)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = BrandRed,
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
private fun VideoCard(
    post: RedditPost,
    isLiked: Boolean,
    onLike: (RedditPost) -> Unit,
    onClick: () -> Unit,
    onSubredditClick: (String) -> Unit = {},
    onRemoveVideo: (RedditPost) -> Unit = {}
) {
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isHiding by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(post.id) {
                    detectTapGestures(
                        onLongPress = {
                            if (!isHiding) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                isHiding = true
                                coroutineScope.launch {
                                    delay(350)
                                    onRemoveVideo(post)
                                }
                            }
                        },
                        onTap = { if (!isHiding) onClick() }
                    )
                },
            shape = RoundedCornerShape(14.dp),
            color = Color.Black,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .background(Color.Black)
                ) {
                    ThumbnailImage(url = post.thumbnailUrl, contentDescription = post.title, modifier = Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play video",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        post.title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceGlass)
                                .clickable { onSubredditClick(post.subreddit) }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "r/${post.subreddit}",
                                color = BrandRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onLike(post) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                if (isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                                contentDescription = "Like",
                                tint = if (isLiked) BrandRed else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                formatScore(if (isLiked) post.score + 1 else post.score),
                                color = if (isLiked) BrandRed else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Hiding overlay indicator
        AnimatedVisibility(
            visible = isHiding,
            enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.88f),
            exit = fadeOut(tween(150)),
            modifier = Modifier.matchParentSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceBase.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, BrandRed)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Hidden",
                            tint = BrandRed,
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

// ponytail: grid of posts shown inside the side panel (shared by History + Liked)
@Composable
private fun PostGridContent(
    posts: List<RedditPost>,
    likedIds: Set<String>,
    onLike: (RedditPost) -> Unit,
    onItemClick: (List<RedditPost>, Int) -> Unit
) {
    if (posts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nothing here yet", color = Color.LightGray, fontSize = 14.sp)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(posts) { index, post ->
                VideoCard(
                    post = post,
                    isLiked = likedIds.contains(post.id),
                    onLike = onLike,
                    onClick = { onItemClick(posts, index) }
                )
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
            Icon(Icons.Default.Add, contentDescription = "Add", tint = BrandRed, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("No Subscriptions", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tap the search icon in the top right to discover and subscribe to subreddits.",
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
            Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = BrandRed)) {
                Text("Try Again", color = Color.White)
            }
        }
    }
}


