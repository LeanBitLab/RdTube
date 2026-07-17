package com.example.reddittube.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.reddittube.data.RedditPost
import kotlinx.coroutines.launch

// ponytail: Vertical pager for video feed with player recycling and pull-to-refresh
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoFeedContent(
    data: List<RedditPost>,
    modifier: Modifier = Modifier,
    subscribedSet: Set<String> = emptySet(),
    onSubscribeToggle: (String) -> Unit = {},
    onRemoveVideo: (RedditPost) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onRefresh: () -> Unit = {},
    isLoadingMore: Boolean = false,
    isRefreshing: Boolean = false,
    startIndex: Int = 0
) {
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { data.size })
    val coroutineScope = rememberCoroutineScope()
    var isMuted by remember { mutableStateOf(false) }

    val onNext: () -> Unit = {
        coroutineScope.launch {
            val next = pagerState.currentPage + 1
            if (next < data.size) {
                pagerState.animateScrollToPage(next)
            }
        }
    }

    // ponytail: trigger loadMore when reaching the last page, but not during initial load
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    LaunchedEffect(currentPage) {
        if (currentPage >= data.size - 2 && data.size > 1 && !isLoadingMore) {
            onLoadMore()
        }
    }

    // ponytail: player recycling — release players for pages far from current
    val visibleRange = (currentPage - 2).coerceAtLeast(0)..(currentPage + 2).coerceAtMost(data.size - 1)

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { index -> data[index].id }
            ) { pageIndex ->
                Box(modifier = Modifier.fillMaxSize()) {
                    // Only keep composable alive if within visible range, else placeholder
                    if (pageIndex in visibleRange) {
                        VideoPage(
                            post = data[pageIndex],
                            isActive = pagerState.currentPage == pageIndex,
                            isMuted = isMuted,
                            onMuteChange = { isMuted = it },
                            subscribedSet = subscribedSet,
                            onSubscribeToggle = onSubscribeToggle,
                            onRemoveVideo = onRemoveVideo,
                            onNext = onNext
                        )
                    } else {
                        // Placeholder to maintain pager structure without heavy player
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = data[pageIndex].title,
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (pageIndex == data.size - 1 && isLoadingMore) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.Red,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Finding more\u2026", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
