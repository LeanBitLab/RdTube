package com.lean.reddittube.ui.main
import com.lean.reddittube.theme.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.lean.reddittube.ui.main.components.SectionLoadingIndicator
import androidx.media3.exoplayer.ExoPlayer
import com.lean.reddittube.data.RedditPost
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
    onLike: (RedditPost) -> Unit = {},
    onSubredditClick: (String) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onRefresh: () -> Unit = {},
    isLoadingMore: Boolean = false,
    isRefreshing: Boolean = false,
    startIndex: Int = 0,
    onBack: (() -> Unit)? = null
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

    // ponytail: trigger loadMore when user approaches end of feed (within last 2 items, minimum 10 items)
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    LaunchedEffect(currentPage, data.size, isLoadingMore) {
        if (currentPage >= (data.size - 5).coerceAtLeast(0) && !isLoadingMore && data.isNotEmpty()) {
            onLoadMore()
        }
    }

    // ponytail: player recycling — release players for pages far from current
    val visibleRange = (currentPage - 2).coerceAtLeast(0)..(currentPage + 2).coerceAtMost(data.size - 1)

    val pullToRefreshState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                containerColor = SurfaceBase,
                color = BrandRed,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
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
                            onLike = onLike,
                            onSwipeAdvance = onNext,
                            onNext = onNext,
                            onSubredditClick = onSubredditClick,
                            onBack = onBack
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
                }
            }

            // Sleek glassmorphic minimal loading badge overlay when fetching next video batch
            AnimatedVisibility(
                visible = isLoadingMore && currentPage >= (data.size - 5).coerceAtLeast(0),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp),
                enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.9f),
                exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.9f)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceBase.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, GlassBorder),
                    shadowElevation = 8.dp
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
