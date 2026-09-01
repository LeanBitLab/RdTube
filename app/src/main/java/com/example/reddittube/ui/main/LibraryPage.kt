package com.lean.reddittube.ui.main

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lean.reddittube.data.RedditPost
import com.lean.reddittube.theme.*

enum class LibrarySegment(val title: String, val icon: ImageVector) {
    SUBSCRIPTIONS("Subreddits", Icons.Outlined.Subscriptions),
    HISTORY("History", Icons.Outlined.History),
    LIKED("Liked", Icons.Outlined.ThumbUp)
}

@Composable
fun LibraryPage(
    viewModel: MainScreenViewModel,
    onItemClick: (List<RedditPost>, Int) -> Unit,
    onSubredditClick: (String) -> Unit = {},
    onCommentClick: (RedditPost) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSegment by remember { mutableStateOf(LibrarySegment.SUBSCRIPTIONS) }
    val likedIds by viewModel.likedIdsFlow.collectAsStateWithLifecycle()
    val subscribedSubreddits by viewModel.subscribedSubreddits.collectAsStateWithLifecycle()

    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val likedPosts = remember(likedIds) { viewModel.getLikedPosts() }
    val watchedPosts = remember(likedIds) { viewModel.getWatchedPosts() }

    val currentVideoList = if (selectedSegment == LibrarySegment.LIKED) likedPosts else watchedPosts

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(top = TopBarHeight + 2.dp)
    ) {
        // Segmented Control (Enclosed Glass Pill Matching SearchPage)
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
                LibrarySegment.entries.forEach { segment ->
                    val isSelected = segment == selectedSegment
                    Surface(
                        onClick = { selectedSegment = segment },
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
                                segment.icon,
                                contentDescription = segment.title,
                                tint = if (isSelected) Color.Black else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = segment.title,
                                color = if (isSelected) Color.Black else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Action Chip (Clear History only when on History tab and has history)
        if (selectedSegment == LibrarySegment.HISTORY && watchedPosts.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                GlassActionChip(
                    icon = Icons.Outlined.History,
                    label = "Clear History",
                    onClick = { showClearHistoryDialog = true }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Content Area
        when (selectedSegment) {
            LibrarySegment.SUBSCRIPTIONS -> {
                val subList = remember(subscribedSubreddits) { subscribedSubreddits.sorted() }
                if (subList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Subscriptions,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "No Subscribed Subreddits",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Use search to find and subscribe to your favorite video subreddits.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "${subList.size} Subscribed Subreddits",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(subList, key = { it }) { sub ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSubredditClick(sub) },
                                shape = RoundedCornerShape(14.dp),
                                color = SurfaceRaised,
                                border = BorderStroke(1.dp, GlassBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "r/",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "r/$sub",
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Tap to open feed",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.toggleSubscription(sub)
                                            Toast.makeText(context, "Unsubscribed from r/$sub", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceBar)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Unsubscribe",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            LibrarySegment.LIKED, LibrarySegment.HISTORY -> {
                if (currentVideoList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (selectedSegment == LibrarySegment.LIKED) Icons.Outlined.ThumbUp else Icons.Outlined.History,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (selectedSegment == LibrarySegment.LIKED) "No liked videos yet." else "No watch history recorded yet.",
                                color = TextSecondary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Videos you interact with will appear here.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(currentVideoList, key = { index, post -> "${post.id}_$index" }) { index, post ->
                            VideoCard(
                                post = post,
                                isLiked = likedIds.contains(post.id),
                                onLike = viewModel::toggleLike,
                                onClick = { onItemClick(currentVideoList, index) },
                                onSubredditClick = onSubredditClick,
                                onCommentClick = onCommentClick,
                                onRemoveVideo = viewModel::hidePost
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Watch History?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will erase all previously watched video records.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearWatchedHistory()
                        showClearHistoryDialog = false
                        Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceRaised,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun GlassActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = SurfaceRaised,
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}
