package com.example.reddittube.ui.main

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
// ponytail: single-column browse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reddittube.data.RedditError
import com.example.reddittube.data.RedditPost
import com.example.reddittube.ui.main.SortOption
import com.example.reddittube.ui.main.components.ThumbnailImage
import kotlinx.coroutines.launch
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

    val coroutineScope = rememberCoroutineScope()
    val horizontalPagerState = rememberPagerState(pageCount = { 3 })

    var showPanel by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    var panelView by remember { mutableStateOf(PanelView.Menu) }

    var subscribedSet by remember {
        val saved = context.getSharedPreferences("reddittube_prefs", Context.MODE_PRIVATE)
            .getStringSet("subscriptions", setOf("shorts", "TikTokCringe", "funny", "videos")) ?: emptySet()
        mutableStateOf(saved.map { it.lowercase() }.toSet())
    }

    LaunchedEffect(Unit) {
        // ponytail: only fetch on first open; ViewModel state persists across navigation so returning to Home stays instant
        if (exploreState is MainScreenUiState.Loading) viewModel.refreshExplore()
        if (subscribedSet.isNotEmpty() && subscribedState is MainScreenUiState.Loading) {
            viewModel.refreshSubscribed(subscribedSet.sorted().joinToString("+"))
        }
    }

    val toggleSubscription = { sub: String ->
        val next = sub.lowercase().trim().replace(" ", "")
        if (next.isNotEmpty()) {
            val updated = if (subscribedSet.contains(next)) subscribedSet - next else subscribedSet + next
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

    // Liked state persisted
    val prefs = remember { context.getSharedPreferences("reddittube_prefs", Context.MODE_PRIVATE) }
    val savedLiked = prefs.getStringSet("liked_ids", emptySet()) ?: emptySet<String>()
    val initialLiked: MutableSet<String> = mutableSetOf<String>().apply { addAll(savedLiked) }
    var likedIds by remember { mutableStateOf(initialLiked) }
    var likedPosts by remember { mutableStateOf(loadLikedPosts(prefs)) }

    fun toggleLike(post: RedditPost) {
        val id = post.id
        val ids = if (likedIds.contains(id)) (likedIds - id) else (likedIds + id)
        likedIds = ids.toMutableSet()
        prefs.edit().putStringSet("liked_ids", ids).apply()
        val posts = if (likedIds.contains(id)) (likedPosts + post).distinctBy { it.id }
        else likedPosts.filter { it.id != id }
        likedPosts = posts
        prefs.edit().putString("liked_posts", likedPostsToJson(posts)).apply()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = horizontalPagerState,
            modifier = Modifier.fillMaxSize().padding(bottom = 88.dp),
            userScrollEnabled = false
        ) { pageIndex ->
            when (pageIndex) {
                0 -> BrowseGrid(
                    uiState = exploreState,
                    likedIds = likedIds,
                    onLike = { toggleLike(it) },
                    onItemClick = { list: List<RedditPost>, index: Int -> viewModel.openPlayer(list, index); onItemClick() },
                    onLoadMore = { viewModel.loadMore(true) },
                    onRefresh = { viewModel.refreshExplore() },
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
                2 -> BrowseGrid(
                    uiState = subscribedState,
                    likedIds = likedIds,
                    onLike = { toggleLike(it) },
                    onItemClick = { list: List<RedditPost>, index: Int -> viewModel.openPlayer(list, index); onItemClick() },
                    onLoadMore = { viewModel.loadMore(false) },
                    onRefresh = { viewModel.refreshSubscribed(subscribedSet.sorted().joinToString("+")) },
                    isLoadingMore = (subscribedState as? MainScreenUiState.Success)?.isLoadingMore ?: false
                )
            }
        }

        // Top header
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
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                    .clickable { showPanel = true; panelView = PanelView.Menu },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        // Floating bottom navigation (icons only)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF242424),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isExplore = horizontalPagerState.currentPage == 0
                    val isSubscribed = horizontalPagerState.currentPage == 2
                    Icon(Icons.Default.Home, contentDescription = "Explore",
                        tint = if (isExplore) Color.Red else Color.LightGray,
                        modifier = Modifier.clip(CircleShape).clickable { coroutineScope.launch { horizontalPagerState.animateScrollToPage(0) } }.padding(horizontal = 24.dp, vertical = 12.dp).size(24.dp))
                    Icon(Icons.Default.Search, contentDescription = "Search",
                        tint = if (horizontalPagerState.currentPage == 1) Color.Red else Color.LightGray,
                        modifier = Modifier.clip(CircleShape).clickable { coroutineScope.launch { horizontalPagerState.animateScrollToPage(1) } }.padding(horizontal = 24.dp, vertical = 12.dp).size(24.dp))
                    Icon(Icons.Default.Star, contentDescription = "Subscribed",
                        tint = if (isSubscribed) Color.Red else Color.LightGray,
                        modifier = Modifier.clip(CircleShape).clickable {
                            if (subscribedSet.isEmpty()) {
                                Toast.makeText(context, "No subreddits subscribed yet! Use search.", Toast.LENGTH_SHORT).show()
                            } else {
                                coroutineScope.launch { horizontalPagerState.animateScrollToPage(2) }
                            }
                        }.padding(horizontal = 24.dp, vertical = 12.dp).size(24.dp))
                }
            }
        }

        // Right-side panel (menu / History grid / Liked grid)
        if (showPanel) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showPanel = false }
            ) {
                Surface(
                    color = Color(0xFF1A1A1A),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(300.dp)
                        .clickable { }  // ponytail: consume clicks so scrim close doesn't fire
                ) {
                    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        // Panel header: back (if subview) + title + close
                        Row(
                            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
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
                                val titleText = when (panelView) { PanelView.Menu -> "R-Tube"; PanelView.History -> "History"; PanelView.Liked -> "Liked" }
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
                                onLike = { toggleLike(it) },
                                onItemClick = { list, index -> showPanel = false; viewModel.openPlayer(list, index); onItemClick() }
                            )
                            PanelView.Liked -> PostGridContent(
                                posts = likedPosts,
                                likedIds = likedIds,
                                onLike = { toggleLike(it) },
                                onItemClick = { list, index -> showPanel = false; viewModel.openPlayer(list, index); onItemClick() }
                            )
                        }
                    }
                }
            }
        }
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
    val drawerColors = NavigationDrawerItemDefaults.colors(
        unselectedContainerColor = Color.Transparent,
        selectedContainerColor = Color.White.copy(alpha = 0.1f),
        unselectedTextColor = Color.White,
        selectedTextColor = Color.Red
    )
    Column(Modifier.fillMaxSize().padding(8.dp).navigationBarsPadding()) {
        NavigationDrawerItem(
            label = { Text("Refresh") },
            selected = false,
            onClick = onRefresh,
            icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White) },
            colors = drawerColors
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        NavigationDrawerItem(
            label = { Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sort"); Spacer(Modifier.width(4.dp))
                Icon(if (sortExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            } },
            selected = false,
            onClick = onToggleSort,
            icon = { Icon(Icons.Default.Sort, contentDescription = null, tint = Color.White) },
            colors = drawerColors
        )
        if (sortExpanded) {
            SortOption.entries.forEach { sort ->
                NavigationDrawerItem(
                    label = { Text(sort.label, modifier = Modifier.padding(start = 16.dp), color = if (currentSort == sort) Color.Red else Color.White) },
                    selected = currentSort == sort,
                    onClick = { onSort(sort) },
                    icon = { Icon(if (currentSort == sort) Icons.Default.Check else Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = if (currentSort == sort) Color.Red else Color.LightGray, modifier = Modifier.size(18.dp)) },
                    colors = drawerColors
                )
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        NavigationDrawerItem(
            label = { Text("History") },
            selected = false,
            onClick = onHistory,
            icon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White) },
            colors = drawerColors
        )
        NavigationDrawerItem(
            label = { Text("Liked") },
            selected = false,
            onClick = onLiked,
            icon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Red) },
            colors = drawerColors
        )
    }
}

// ponytail: grid browse for a feed, with infinite scroll trigger
@Composable
private fun BrowseGrid(
    uiState: MainScreenUiState,
    likedIds: Set<String>,
    onLike: (RedditPost) -> Unit,
    onItemClick: (List<RedditPost>, Int) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    isLoadingMore: Boolean
) {
    when (uiState) {
        MainScreenUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.Red)
            }
        }
        is MainScreenUiState.Success -> {
            val data = uiState.data
            val gridState = rememberLazyGridState()
            LaunchedEffect(gridState.firstVisibleItemIndex) {
                if (data.isNotEmpty() && !isLoadingMore &&
                    gridState.firstVisibleItemIndex + 8 >= data.size
                ) {
                    onLoadMore()
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 56.dp, bottom = 8.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(data) { index, post ->
                    VideoCard(
                        post = post,
                        isLiked = likedIds.contains(post.id),
                        onLike = onLike,
                        onClick = { onItemClick(data, index) }
                    )
                }
                if (isLoadingMore) {
                    item(span = { GridItemSpan(1) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Red, strokeWidth = 2.dp)
                        }
                    }
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
    }
}

@Composable
private fun VideoCard(
    post: RedditPost,
    isLiked: Boolean,
    onLike: (RedditPost) -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color(0xFF0F0F0F), shape = RoundedCornerShape(10.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            ThumbnailImage(url = post.thumbnailUrl, contentDescription = post.title, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(40.dp))
            }
        }
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                post.title,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "r/${post.subreddit}",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLike(post) }
                ) {
                    Icon(
                        if (isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUp,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(formatScore(if (isLiked) post.score + 1 else post.score), color = if (isLiked) Color.Red else Color.LightGray, fontSize = 12.sp)
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

private fun postToJson(p: RedditPost) = JSONObject().apply {
    put("id", p.id)
    put("title", p.title)
    put("subreddit", p.subreddit)
    put("author", p.author)
    put("score", p.score)
    put("permalink", p.permalink)
    put("videoUrl", p.videoUrl)
    put("fallbackUrl", p.fallbackUrl)
    put("dashUrl", p.dashUrl)
    put("hlsUrl", p.hlsUrl)
    put("thumbnailUrl", p.thumbnailUrl)
    put("numComments", p.numComments)
}

private fun jsonToPost(o: JSONObject) = RedditPost(
    id = o.optString("id"),
    title = o.optString("title"),
    subreddit = o.optString("subreddit"),
    author = o.optString("author"),
    score = o.optInt("score"),
    permalink = o.optString("permalink"),
    videoUrl = o.optString("videoUrl"),
    fallbackUrl = o.optString("fallbackUrl"),
    dashUrl = o.optString("dashUrl"),
    hlsUrl = o.optString("hlsUrl"),
    thumbnailUrl = o.optString("thumbnailUrl"),
    numComments = o.optInt("numComments")
)

private fun likedPostsToJson(list: List<RedditPost>) =
    JSONArray().apply { list.forEach { put(postToJson(it)) } }.toString()

private fun loadLikedPosts(prefs: SharedPreferences): List<RedditPost> {
    val str = prefs.getString("liked_posts", null) ?: return emptyList()
    return try {
        val arr = JSONArray(str)
        (0 until arr.length()).map { jsonToPost(arr.getJSONObject(it)) }
    } catch (_: Exception) {
        emptyList()
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
            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Red, modifier = Modifier.size(64.dp))
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
            Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("Try Again", color = Color.White)
            }
        }
    }
}

private fun formatScore(score: Int): String {
    return when {
        score >= 1000000 -> String.format("%.1fM", score / 1000000f)
        score >= 1000 -> String.format("%.1fk", score / 1000f)
        else -> score.toString()
    }
}
