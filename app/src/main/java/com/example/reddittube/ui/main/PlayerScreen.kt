package com.example.reddittube.ui.main

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ponytail: full-screen player launched from the browse grid — plays the chosen video on the existing player
@Composable
fun PlayerScreen(
    viewModel: MainScreenViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val list by viewModel.playerList.collectAsStateWithLifecycle()
    val exploreState by viewModel.exploreState.collectAsStateWithLifecycle()
    val subscribedSubreddits by viewModel.subscribedSubreddits.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (list.isNotEmpty()) {
            VideoFeedContent(
                data = list,
                modifier = Modifier.fillMaxSize(),
                startIndex = viewModel.playerStartIndex,
                isLoadingMore = (exploreState as? MainScreenUiState.Success)?.isLoadingMore ?: false,
                subscribedSet = subscribedSubreddits,
                onSubscribeToggle = viewModel::toggleSubscription,
                onRemoveVideo = { viewModel.markAsWatched(it) },
                onLoadMore = { viewModel.loadMore(true) },
                onRefresh = { viewModel.refreshExplore() },
                isRefreshing = exploreState is MainScreenUiState.Loading
            )
        }

        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
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
