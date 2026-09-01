package com.lean.reddittube.ui.main.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lean.reddittube.theme.*

private val POPULAR_SUBREDDITS = listOf(
    "videos", "tiktokcringe", "unexpected", "youtubehaiku",
    "damnthatsinteresting", "nextfuckinglevel", "idiotsincars",
    "publicfreakout", "holdmybeer", "maybemaybemaybe"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBottomSheet(
    currentSubscribed: Set<String>,
    onSubscribeToggle: (String) -> Unit,
    onSubredditSelect: (String) -> Unit,
    searchResults: List<String>,
    onSearchQuery: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("rdtube_prefs", Context.MODE_PRIVATE) }
    var searchQuery by remember { mutableStateOf("") }

    var recentSearches by remember {
        mutableStateOf(
            sharedPreferences.getStringSet("search_history", emptySet())?.toList() ?: emptyList()
        )
    }

    val saveSearchHistory: (String) -> Unit = { query ->
        if (query.isNotBlank()) {
            val updated = (listOf(query.lowercase()) + recentSearches.filterNot { it.equals(query, ignoreCase = true) }).take(8)
            recentSearches = updated
            sharedPreferences.edit().putStringSet("search_history", updated.toSet()).apply()
        }
    }

    val handleSelectSubreddit: (String) -> Unit = { sub ->
        saveSearchHistory(sub)
        onSubredditSelect(sub)
        onDismiss()
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceBase,
        scrimColor = Scrim,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp, max = (screenHeight.value * 0.85f).dp)
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Search Input Field
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceRaised,
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onSearchQuery(it)
                    },
                    placeholder = { Text("Search subreddits...", color = TextMuted, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = BrandRedLight, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; onSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = BrandRed,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(14.dp))

            // Recent Searches (if query is blank)
            if (searchQuery.isBlank() && recentSearches.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Searches", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Clear",
                        color = BrandRedLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            recentSearches = emptyList()
                            sharedPreferences.edit().remove("search_history").apply()
                        }
                    )
                }

                Spacer(Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recentSearches) { recent ->
                        Surface(
                            onClick = { handleSelectSubreddit(recent) },
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceRaised,
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("r/$recent", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
            }

            // Results or Popular Recommendations
            val displayList = if (searchQuery.isNotBlank()) searchResults else POPULAR_SUBREDDITS
            val headerText = if (searchQuery.isNotBlank()) "Search Results" else "Trending & Recommended"

            Text(headerText, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(displayList) { sub ->
                    val isSubbed = currentSubscribed.contains(sub.lowercase())
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { handleSelectSubreddit(sub) },
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceRaised,
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("r/$sub", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Tap to browse feed", color = TextMuted, fontSize = 11.sp)
                            }

                            IconButton(
                                onClick = { onSubscribeToggle(sub.lowercase()) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSubbed) BrandRed.copy(alpha = 0.2f) else SurfaceBar)
                            ) {
                                Icon(
                                    imageVector = if (isSubbed) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = if (isSubbed) "Subscribed" else "Subscribe",
                                    tint = if (isSubbed) BrandRedLight else TextSecondary,
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
