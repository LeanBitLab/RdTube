package com.lean.reddittube.ui.main

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
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
    LIKED("Liked Videos", Icons.Outlined.ThumbUp),
    HISTORY("Watch History", Icons.Outlined.History)
}

@Composable
fun LibraryPage(
    viewModel: MainScreenViewModel,
    onItemClick: (List<RedditPost>, Int) -> Unit,
    onSubredditClick: (String) -> Unit = {},
    onShowAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSegment by remember { mutableStateOf(LibrarySegment.LIKED) }
    val likedIds by viewModel.likedIdsFlow.collectAsStateWithLifecycle()

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    val likedPosts = remember(likedIds) { viewModel.getLikedPosts() }
    val watchedPosts = remember(likedIds) { viewModel.getWatchedPosts() }

    val currentList = if (selectedSegment == LibrarySegment.LIKED) likedPosts else watchedPosts

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RichObsidian)
            .statusBarsPadding()
            .padding(top = 8.dp)
    ) {
        // Header
        Text(
            text = "Your Vault",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        // Segmented Control Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LibrarySegment.entries.forEach { segment ->
                val isSelected = segment == selectedSegment
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .clickable { selectedSegment = segment },
                    shape = RoundedCornerShape(21.dp),
                    color = if (isSelected) BrandRed else SurfaceRaised,
                    border = BorderStroke(1.dp, if (isSelected) BrandRedLight else GlassBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            segment.icon,
                            contentDescription = segment.title,
                            tint = if (isSelected) Color.White else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = segment.title,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Quick Action Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassActionChip(
                icon = Icons.Outlined.History,
                label = "Clear History",
                onClick = { showClearHistoryDialog = true }
            )
            GlassActionChip(
                icon = Icons.Outlined.CleaningServices,
                label = "Clear Cache",
                onClick = { showClearCacheDialog = true }
            )
            GlassActionChip(
                icon = Icons.Outlined.Info,
                label = "Guide & Info",
                onClick = onShowAbout
            )
        }

        Spacer(Modifier.height(8.dp))

        // Content Area
        if (currentList.isEmpty()) {
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(currentList, key = { index, post -> "${post.id}_$index" }) { index, post ->
                    VideoCard(
                        post = post,
                        isLiked = likedIds.contains(post.id),
                        onLike = viewModel::toggleLike,
                        onClick = { onItemClick(currentList, index) },
                        onSubredditClick = onSubredditClick,
                        onRemoveVideo = viewModel::hidePost
                    )
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
                    Text("Clear", color = BrandRed, fontWeight = FontWeight.Bold)
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

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Frees up in-memory thumbnail and feed caches.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear", color = BrandRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
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
