package com.lean.reddittube.ui.main

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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

enum class SearchTab(val label: String, val icon: ImageVector) {
    SUBREDDITS("Subreddits", Icons.Outlined.Subscriptions),
    VIDEOS("Videos", Icons.Outlined.PlayCircleOutline)
}

@Composable
fun SearchPage(
    currentSubscribed: Set<String>,
    onSubscribeToggle: (String) -> Unit,
    onSubredditSelect: (String) -> Unit,
    searchResults: List<String>,
    onSearchQuery: (String) -> Unit,
    videoSearchResults: List<RedditPost>,
    isVideoSearching: Boolean,
    onVideoSearchQuery: (String) -> Unit,
    onVideoClick: (List<RedditPost>, Int) -> Unit,
    likedIds: Set<String> = emptySet(),
    onLike: (RedditPost) -> Unit = {},
    onCommentClick: (RedditPost) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val sharedPreferences = remember { context.getSharedPreferences("rdtube_prefs", Context.MODE_PRIVATE) }
    
    var selectedTab by rememberSaveable { mutableStateOf(SearchTab.SUBREDDITS) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    var recentSearches by remember {
        mutableStateOf(
            sharedPreferences.getStringSet("search_history", emptySet())?.toList() ?: emptyList()
        )
    }

    LaunchedEffect(searchQuery, selectedTab) {
        if (searchQuery.isNotBlank()) {
            if (selectedTab == SearchTab.SUBREDDITS) {
                onSearchQuery(searchQuery)
            } else {
                onVideoSearchQuery(searchQuery)
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
        POPULAR_SUBREDDITS
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(top = TopBarHeight + 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Segmented Search Tab Bar [Subreddits | Videos]
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceBar,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SearchTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        Surface(
                            onClick = { selectedTab = tab },
                            shape = RoundedCornerShape(17.dp),
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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 145.dp),
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
                                    items(recentSearches) { recent ->
                                        Surface(
                                            onClick = { handleSelectSubreddit(recent) },
                                            shape = RoundedCornerShape(14.dp),
                                            color = SurfaceRaised,
                                            border = BorderStroke(1.dp, GlassBorder)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.History, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("r/$recent", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                                color = if (isExactTyped) SurfaceRaised.copy(alpha = 0.90f) else SurfaceRaised,
                                border = BorderStroke(1.dp, if (isExactTyped) Color.White.copy(alpha = 0.35f) else GlassBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(if (isExactTyped) Color.White else Color.White.copy(alpha = 0.08f)),
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
                                            Text("r/$sub", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = if (isExactTyped) "Browse r/$sub feed" else "Tap to view video feed",
                                                color = if (isExactTyped) Color.White.copy(alpha = 0.8f) else TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onSubscribeToggle(sub.lowercase()) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isSubbed) Color.White.copy(alpha = 0.15f) else SurfaceBar)
                                    ) {
                                        Icon(
                                            imageVector = if (isSubbed) Icons.Default.Check else Icons.Default.Add,
                                            contentDescription = if (isSubbed) "Subscribed" else "Subscribe",
                                            tint = if (isSubbed) Color.White else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SearchTab.VIDEOS -> {
                    if (searchQuery.isBlank() && videoSearchResults.isEmpty() && !isVideoSearching) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 145.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
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
                                                        searchQuery = tag
                                                        onVideoSearchQuery(tag)
                                                        saveSearchHistory(tag)
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = SurfaceRaised,
                                                    border = BorderStroke(1.dp, GlassBorder),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
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
                                Spacer(Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Outlined.PlayCircleOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(42.dp))
                                        Spacer(Modifier.height(8.dp))
                                        Text("Search Across All Reddit Videos", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                                Spacer(Modifier.height(12.dp))
                                Text("Searching videos for \"$searchQuery\"...", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    } else if (videoSearchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("No videos found", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "No playable video posts matched \"$searchQuery\". Try different keywords.",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(1),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 145.dp),
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
                        }
                    }
                }
            }
        }

        // Bottom-Aligned Search Bar (Positioned above floating dock)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = SurfaceRaised.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, GlassBorder),
                shadowElevation = 8.dp
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (selectedTab == SearchTab.SUBREDDITS) {
                            onSearchQuery(it)
                        } else {
                            onVideoSearchQuery(it)
                        }
                    },
                    placeholder = {
                        Text(
                            text = if (selectedTab == SearchTab.SUBREDDITS) "Search subreddits..." else "Search videos across Reddit...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                if (selectedTab == SearchTab.SUBREDDITS) onSearchQuery("") else onVideoSearchQuery("")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
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
                                    onSearchQuery(cleanQuery)
                                } else {
                                    onVideoSearchQuery(cleanQuery)
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
        }
    }
}
