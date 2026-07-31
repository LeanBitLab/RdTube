package com.lean.reddittube.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.lean.reddittube.theme.HPad
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lean.reddittube.data.RedditPost

// ponytail: full-screen player launched from the browse grid — plays the chosen video on the existing player
@Composable
fun PlayerScreen(
    viewModel: MainScreenViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val list by viewModel.playerList.collectAsStateWithLifecycle()
    val exploreState by viewModel.exploreState.collectAsStateWithLifecycle()
    val subscribedState by viewModel.subscribedState.collectAsStateWithLifecycle()
    val subscribedSubreddits by viewModel.subscribedSubreddits.collectAsStateWithLifecycle()
    val feed = viewModel.playerFeed

    // ponytail: pager must follow the live feed (explore/subscribed) so load-more actually grows it;
    // for history/liked ("other") it stays the opened snapshot
    val liveData: List<RedditPost> = when (feed) {
        "explore" -> (exploreState as? MainScreenUiState.Success)?.data ?: list
        "subscribed" -> (subscribedState as? MainScreenUiState.Success)?.data ?: list
        else -> list
    }
    val liveLoadingMore = when (feed) {
        "explore" -> (exploreState as? MainScreenUiState.Success)?.isLoadingMore ?: false
        "subscribed" -> (subscribedState as? MainScreenUiState.Success)?.isLoadingMore ?: false
        else -> false
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (liveData.isNotEmpty()) {
            VideoFeedContent(
                data = liveData,
                modifier = Modifier.fillMaxSize(),
                startIndex = viewModel.playerStartIndex,
                isLoadingMore = liveLoadingMore,
                subscribedSet = subscribedSubreddits,
                onSubscribeToggle = viewModel::toggleSubscription,
                onRemoveVideo = { viewModel.markAsWatched(it) },
                onLike = viewModel::toggleLike,
                onSubredditClick = { sub -> viewModel.refreshExplore(sub); onBack() },
                onLoadMore = { if (feed != "other") viewModel.loadMore(feed != "subscribed") },
                onRefresh = { viewModel.refreshExplore() },
                isRefreshing = exploreState is MainScreenUiState.Loading
            )
        }

        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = HPad, top = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}
