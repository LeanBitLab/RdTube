package com.lean.reddittube.ui.main

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lean.reddittube.data.RedditPost
import com.lean.reddittube.theme.*

private val POPULAR_SUBREDDITS = listOf(
    "videos", "tiktokcringe", "unexpected", "youtubehaiku",
    "damnthatsinteresting", "nextfuckinglevel", "idiotsincars",
    "publicfreakout", "holdmybeer", "maybemaybemaybe",
    "gaming", "contagiouslaughter", "instant_regret", "oddlysatisfying"
)

private val POPULAR_VIDEO_TAGS = listOf(
    "Funny", "Gaming", "Satisfying", "Unexpected", "Pets & Animals",
    "Next Level", "Fails", "Music", "Science", "Memes", "Sports", "Nature"
)

private val SearchSurfaceBg = Color(0xFF141414)
private val SearchCardBg = Color(0xFF161616)
private val SearchCardHighlight = Color(0xFF1E1E1E)
private val SearchPillBg = Color(0xFF222222)
private val SearchBarBg = Color(0xFF181818)
private val SearchButtonBg = Color(0xFF1E1E1E)

enum class SearchTab(val label: String, val icon: ImageVector) {
    SUBREDDITS("Subreddits", Icons.Outlined.Subscriptions),
    VIDEOS("Videos", Icons.Outlined.PlayCircleOutline)
}

enum class SearchVideoFilter(val sortParam: String, val label: String) {
    RELEVANCE("relevance", "Relevance"),
    HOT("hot", "Hot"),
    TOP_DAY("top&t=day", "Top (Today)"),
    TOP_WEEK("top&t=week", "Top (This Week)"),
    TOP_ALL("top&t=all", "Top (All Time)"),
    NEW("new", "New"),
    COMMENTS("comments", "Most Discussed")
}

@Composable
fun SearchPage(
    currentSubscribed: Set<String>,
    onSubscribeToggle: (String) -> Unit,
    onSubredditSelect: (String) -> Unit,
    searchResults: List<String>,
    onSearchQuery: (String, Boolean) -> Unit,
    onLoadMoreSubreddits: () -> Unit = {},
    isSubredditSearchingMore: Boolean = false,
    videoSearchResults: List<RedditPost>,
    isVideoSearching: Boolean,
    onLoadMoreVideos: () -> Unit = {},
    isVideoSearchingMore: Boolean = false,
    onVideoSearchQuery: (String, String, Boolean) -> Unit,
    onVideoClick: (List<RedditPost>, Int) -> Unit,
    likedIds: Set<String> = emptySet(),
    onLike: (RedditPost) -> Unit = {},
    onCommentClick: (RedditPost) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val hapticFeedback = LocalHapticFeedback.current
    val sharedPreferences = remember { context.getSharedPreferences("rdtube_prefs", Context.MODE_PRIVATE) }
    
    var selectedTab by rememberSaveable { mutableStateOf(SearchTab.SUBREDDITS) }
    var selectedVideoFilter by rememberSaveable { mutableStateOf(SearchVideoFilter.RELEVANCE) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    var recentSearches by remember {
        mutableStateOf(
            sharedPreferences.getStringSet("search_history", emptySet())?.toList() ?: emptyList()
        )
    }

    BackHandler(enabled = searchQuery.isNotBlank()) {
        searchQuery = ""
        if (selectedTab == SearchTab.SUBREDDITS) {
            onSearchQuery("", true)
        } else {
            onVideoSearchQuery("", selectedVideoFilter.sortParam, true)
        }
    }

    LaunchedEffect(searchQuery, selectedTab, selectedVideoFilter) {
        if (searchQuery.isNotBlank()) {
            if (selectedTab == SearchTab.SUBREDDITS) {
                onSearchQuery(searchQuery, false)
            } else {
                onVideoSearchQuery(searchQuery, selectedVideoFilter.sortParam, false)
            }
        }
    }

    val saveSearchHistory: (String) -> Unit = { query ->
        val clean = query.trim().lowercase().removePrefix("r/")
        if (clean.isNotBlank()) {
            val updated = (listOf(clean) + recentSearches.filterNot { it.equals(clean, ignoreCase = true) }).take(10)
            recentSearches = updated
            sharedPreferences.edit().putStringSet("search_history", updated.toSet()).apply()
        }
    }

    val removeSearchHistory: (String) -> Unit = { query ->
        val clean = query.trim().lowercase().removePrefix("r/")
        val updated = recentSearches.filterNot { it.equals(clean, ignoreCase = true) }
        recentSearches = updated
        sharedPreferences.edit().putStringSet("search_history", updated.toSet()).apply()
    }

    val handleSelectSubreddit: (String) -> Unit = { sub ->
        val clean = sub.trim().lowercase().removePrefix("r/")
        saveSearchHistory(clean)
        keyboardController?.hide()
        focusManager.clearFocus()
        onSubredditSelect(clean)
    }

    val cleanQuery = searchQuery.trim().lowercase().removePrefix("r/")
    val displaySubList = if (cleanQuery.isNotBlank()) {
        (listOf(cleanQuery) + searchResults).filter { it.isNotBlank() }.distinct()
    } else {
        POPULAR_SUBREDDITS.filter { !currentSubscribed.contains(it.lowercase()) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(top = TopBarHeight + 2.dp)
    ) {
        // Top Distinctive Search Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            color = SearchBarBg,
            shadowElevation = 4.dp
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (selectedTab == SearchTab.SUBREDDITS) "Search subreddits (e.g. videos, gaming)..." else "Search videos across Reddit...",
                        color = TextMuted,
                        fontSize = 12.5.sp
                    )
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 13.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                ),
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(SearchPillBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(SearchPillBg)
                                .clickable {
                                    searchQuery = ""
                                    if (selectedTab == SearchTab.SUBREDDITS) {
                                        onSearchQuery("", true)
                                    } else {
                                        onVideoSearchQuery("", selectedVideoFilter.sortParam, true)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        if (cleanQuery.isNotBlank()) {
                            saveSearchHistory(cleanQuery)
                            if (selectedTab == SearchTab.SUBREDDITS) {
                                onSearchQuery(cleanQuery, true)
                            } else {
                                onVideoSearchQuery(cleanQuery, selectedVideoFilter.sortParam, true)
                            }
                        }
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.White,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Segmented Search Tab Bar [Subreddits | Videos]
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SearchSurfaceBg,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SearchTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        onClick = { selectedTab = tab },
                        shape = RoundedCornerShape(11.dp),
                        color = if (isSelected) Color.White else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.Black else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                tab.label,
                                color = if (isSelected) Color.Black else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

            // Tab Content
            when (selectedTab) {
                SearchTab.SUBREDDITS -> {
                    val subListState = rememberLazyListState()
                    val isNearSubEnd by remember {
                        derivedStateOf {
                            val total = subListState.layoutInfo.totalItemsCount
                            val lastVisible = subListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            total > 0 && lastVisible >= total - 3
                        }
                    }
                    LaunchedEffect(isNearSubEnd) {
                        if (isNearSubEnd && cleanQuery.isNotBlank() && !isSubredditSearchingMore) {
                            onLoadMoreSubreddits()
                        }
                    }

                    LazyColumn(
                        state = subListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Recent Searches Chips
                        if (cleanQuery.isBlank() && recentSearches.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, bottom = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Recent Searches", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Clear All",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.clickable {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            recentSearches = emptyList()
                                            sharedPreferences.edit().remove("search_history").apply()
                                        }
                                    )
                                }
                            }

                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(recentSearches, key = { it }) { recent ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = SearchCardBg,
                                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.clickable {
                                                        searchQuery = recent
                                                        onSearchQuery(recent, true)
                                                        saveSearchHistory(recent)
                                                    },
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.History, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(recent, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                }
                                                Spacer(Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(SearchPillBg)
                                                        .clickable {
                                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            removeSearchHistory(recent)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remove search term", tint = TextMuted, modifier = Modifier.size(10.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }

                        item {
                            Text(
                                text = if (cleanQuery.isNotBlank()) "Matching Subreddits" else "Trending & Recommended Subreddits",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(displaySubList, key = { sub -> sub }) { sub ->
                            val isSubbed = currentSubscribed.contains(sub.lowercase())
                            val isExactTyped = cleanQuery.isNotBlank() && sub.equals(cleanQuery, ignoreCase = true)

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { handleSelectSubreddit(sub) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isExactTyped) SearchCardHighlight else SearchCardBg
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (isExactTyped) Color.White else SearchPillBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = sub.take(1).uppercase(),
                                                color = if (isExactTyped) Color.Black else Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("r/$sub", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                                if (isExactTyped) {
                                                    Spacer(Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = SearchPillBg
                                                    ) {
                                                        Text(
                                                            text = "EXACT",
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = if (isSubbed) "Subscribed • Tap to open feed" else if (isExactTyped) "Exact match • Tap to open feed" else "Tap to open feed",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSubscribeToggle(sub.lowercase())
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (isSubbed) Color.White else SearchPillBg)
                                    ) {
                                        Icon(
                                            imageVector = if (isSubbed) Icons.Default.Check else Icons.Default.Add,
                                            contentDescription = if (isSubbed) "Subscribed" else "Subscribe",
                                            tint = if (isSubbed) Color.Black else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (cleanQuery.isNotBlank() && (displaySubList.size >= 8 || isSubredditSearchingMore)) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (isSubredditSearchingMore) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Loading more subreddits...", color = TextSecondary, fontSize = 12.sp)
                                        }
                                    } else {
                                        Surface(
                                            onClick = { onLoadMoreSubreddits() },
                                            shape = RoundedCornerShape(14.dp),
                                            color = SearchButtonBg,
                                            modifier = Modifier.fillMaxWidth(0.7f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 11.dp, horizontal = 14.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.ExpandMore, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Load More Subreddits", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SearchTab.VIDEOS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Filter Bar when search is active or has results
                        if (searchQuery.isNotBlank() || videoSearchResults.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(SearchVideoFilter.entries.toTypedArray()) { filter ->
                                    val isSelected = selectedVideoFilter == filter
                                    Surface(
                                        onClick = {
                                            selectedVideoFilter = filter
                                            if (searchQuery.isNotBlank()) {
                                                onVideoSearchQuery(searchQuery, filter.sortParam, true)
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) Color.White else SearchCardBg
                                    ) {
                                        Text(
                                            text = filter.label,
                                            color = if (isSelected) Color.Black else TextSecondary,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (searchQuery.isBlank() && videoSearchResults.isEmpty() && !isVideoSearching) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Recent Searches Chips
                                if (recentSearches.isNotEmpty()) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp, bottom = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Recent Searches", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Clear All",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.clickable {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    recentSearches = emptyList()
                                                    sharedPreferences.edit().remove("search_history").apply()
                                                }
                                            )
                                        }
                                    }

                                    item {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(recentSearches, key = { it }) { recent ->
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = SearchCardBg,
                                                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.clickable {
                                                                searchQuery = recent
                                                                onVideoSearchQuery(recent, selectedVideoFilter.sortParam, true)
                                                                saveSearchHistory(recent)
                                                            },
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(Icons.Default.History, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                                                            Spacer(Modifier.width(6.dp))
                                                            Text(recent, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                        }
                                                        Spacer(Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .size(18.dp)
                                                                .clip(CircleShape)
                                                                .background(SearchPillBg)
                                                                .clickable {
                                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                    removeSearchHistory(recent)
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(Icons.Default.Close, contentDescription = "Remove search term", tint = TextMuted, modifier = Modifier.size(10.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }

                                item {
                                    Text(
                                        text = "Popular Video Topics",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        POPULAR_VIDEO_TAGS.chunked(3).forEach { rowTags ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                rowTags.forEach { tag ->
                                                    Surface(
                                                        onClick = {
                                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            searchQuery = tag
                                                            onVideoSearchQuery(tag, selectedVideoFilter.sortParam, true)
                                                            saveSearchHistory(tag)
                                                        },
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = SearchCardBg,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.Center
                                                        ) {
                                                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                                                            Spacer(Modifier.width(6.dp))
                                                            Text(tag, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = SearchCardBg,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(SearchPillBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Outlined.PlayCircleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                            }
                                            Spacer(Modifier.height(10.dp))
                                            Text("Search Across All Reddit Videos", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "Type any keyword below to find playable video clips Reddit-wide.",
                                                color = TextMuted,
                                                fontSize = 12.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (isVideoSearching) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.height(14.dp))
                                    Text("Searching videos for \"$searchQuery\"...", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        } else if (videoSearchResults.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = SearchCardBg,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(SearchPillBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.SearchOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        Text("No Videos Found", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "No playable video posts matched \"$searchQuery\". Try different keywords or switch filters.",
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            lineHeight = 16.sp
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                searchQuery = ""
                                                onVideoSearchQuery("", selectedVideoFilter.sortParam, true)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SearchButtonBg, contentColor = Color.White),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Clear Search", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            val videoGridState = rememberLazyGridState()
                            val isNearVideoEnd by remember {
                                derivedStateOf {
                                    val total = videoGridState.layoutInfo.totalItemsCount
                                    val lastVisible = videoGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                    total > 0 && lastVisible >= total - 4
                                }
                            }
                            LaunchedEffect(isNearVideoEnd) {
                                if (isNearVideoEnd && searchQuery.isNotBlank() && !isVideoSearchingMore && !isVideoSearching) {
                                    onLoadMoreVideos()
                                }
                            }

                            LazyVerticalGrid(
                                state = videoGridState,
                                columns = GridCells.Fixed(1),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Text(
                                        text = "${videoSearchResults.size} Videos Found",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }

                                itemsIndexed(videoSearchResults, key = { index, post -> "${post.id}_$index" }) { index, post ->
                                    VideoCard(
                                        post = post,
                                        isLiked = likedIds.contains(post.id),
                                        onLike = onLike,
                                        onClick = { onVideoClick(videoSearchResults, index) },
                                        onSubredditClick = { sub -> onSubredditSelect(sub) },
                                        onCommentClick = onCommentClick
                                    )
                                }

                                if (cleanQuery.isNotBlank() && (videoSearchResults.size >= 8 || isVideoSearchingMore)) {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp, bottom = 20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            if (isVideoSearchingMore) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.padding(8.dp)
                                                ) {
                                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(10.dp))
                                                    Text("Loading more videos...", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                }
                                            } else {
                                                Surface(
                                                    onClick = { onLoadMoreVideos() },
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = SearchButtonBg,
                                                    modifier = Modifier.fillMaxWidth(0.8f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ExpandMore,
                                                            contentDescription = "Load More Videos",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            text = "Load More Videos (+20)",
                                                            color = Color.White,
                                                            fontSize = 12.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
