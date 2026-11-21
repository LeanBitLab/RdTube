package com.lean.reddittube.ui.main

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lean.reddittube.data.DataRepository
import com.lean.reddittube.data.RedditComment
import com.lean.reddittube.data.RedditError
import com.lean.reddittube.data.RedditPost
import com.lean.reddittube.utils.toJson
import com.lean.reddittube.utils.toRedditPost
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

// ponytail: Simplified UI state container. Now manages separate states for Explore and Subscribed sections.
@Immutable
sealed interface MainScreenUiState {
    data object Loading : MainScreenUiState
    data class Error(val throwable: RedditError) : MainScreenUiState
    data class Success(val data: List<RedditPost>, val isLoadingMore: Boolean = false) : MainScreenUiState
}

// ponytail: sort options for Reddit feed
enum class SortOption(val value: String, val label: String, val description: String) {
    HOT("hot", "Hot", "Trending videos right now"),
    NEW("new", "New", "Latest uploaded video clips"),
    RISING("rising", "Rising", "Rapidly gaining popularity"),
    TOP_DAY("top?t=day", "Top (Today)", "Highest voted in the past 24 hours"),
    TOP_WEEK("top?t=week", "Top (This Week)", "Top rated videos this week"),
    TOP_MONTH("top?t=month", "Top (This Month)", "Top rated videos this month"),
    TOP_YEAR("top?t=year", "Top (This Year)", "Best videos of the entire year"),
    TOP_ALL("top?t=all", "Top (All Time)", "Greatest all-time highest voted videos")
}

class MainScreenViewModel(private val dataRepository: DataRepository) : ViewModel() {
    private val _exploreState = MutableStateFlow<MainScreenUiState>(MainScreenUiState.Loading)
    val exploreState: StateFlow<MainScreenUiState> = _exploreState.asStateFlow()

    private val _subscribedState = MutableStateFlow<MainScreenUiState>(MainScreenUiState.Loading)
    val subscribedState: StateFlow<MainScreenUiState> = _subscribedState.asStateFlow()

    companion object {
        const val DEFAULT_EXPLORE_FEED = "videos+tiktokcringe+unexpected+youtubehaiku+damnthatsinteresting+nextfuckinglevel+idiotsincars+publicfreakout+holdmybeer+maybemaybemaybe+gaming+contagiouslaughter+oddlysatisfying"
        private const val WATCHED_CAP = 1000
        private const val LIKED_CAP = 1000
        private const val CACHE_STALE_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    }

    var exploreQuery = DEFAULT_EXPLORE_FEED
        private set
    var subscribedQuery = ""
        private set

    var currentSort: SortOption by mutableStateOf(SortOption.HOT)
        private set

    private val _searchResults = MutableStateFlow<List<String>>(emptyList())
    val searchResults: StateFlow<List<String>> = _searchResults.asStateFlow()

    private val _videoSearchResults = MutableStateFlow<List<RedditPost>>(emptyList())
    val videoSearchResults: StateFlow<List<RedditPost>> = _videoSearchResults.asStateFlow()

    private val _isVideoSearching = MutableStateFlow(false)
    val isVideoSearching: StateFlow<Boolean> = _isVideoSearching.asStateFlow()

    private val _isSubredditSearchingMore = MutableStateFlow(false)
    val isSubredditSearchingMore: StateFlow<Boolean> = _isSubredditSearchingMore.asStateFlow()

    private val _isVideoSearchingMore = MutableStateFlow(false)
    val isVideoSearchingMore: StateFlow<Boolean> = _isVideoSearchingMore.asStateFlow()

    private val _subredditSearchResults = MutableStateFlow<List<RedditPost>>(emptyList())
    val subredditSearchResults: StateFlow<List<RedditPost>> = _subredditSearchResults.asStateFlow()

    private val _isSubredditSearching = MutableStateFlow(false)
    val isSubredditSearching: StateFlow<Boolean> = _isSubredditSearching.asStateFlow()

    private val _isSubredditSearchActive = MutableStateFlow(false)
    val isSubredditSearchActive: StateFlow<Boolean> = _isSubredditSearchActive.asStateFlow()

    private var inSubredditSearchJob: Job? = null
    private var inSubredditSearchAfter: String? = null
    private var currentInSubredditSearchQuery: String = ""
    private var currentInSubredditSearchTarget: String = ""
    private val inSubredditSearchCache = mutableMapOf<String, PagedVideoCache>()

    // ponytail: list + index opened from the browse/home grid, played by PlayerScreen
    private val _playerList = MutableStateFlow<List<RedditPost>>(emptyList())
    val playerList: StateFlow<List<RedditPost>> = _playerList.asStateFlow()
    var playerStartIndex = 0
        private set

    var playerFeed = "explore"
        private set

    fun openPlayer(list: List<RedditPost>, index: Int, feed: String = "explore") {
        _playerList.value = list
        playerStartIndex = index
        playerFeed = feed
    }

    private val lastExploreRefreshTime = ConcurrentHashMap<String, Long>()
    private val lastSubscribedRefreshTime = ConcurrentHashMap<String, Long>()
    private val prefs = dataRepository.getContext().getSharedPreferences("rdtube_prefs", android.content.Context.MODE_PRIVATE)
    private val _subscribedSubreddits = MutableStateFlow(
        prefs.getStringSet("subscriptions", setOf("funny", "videos"))!!
            .map { it.lowercase() }.toSet()
    )
    val subscribedSubreddits: StateFlow<Set<String>> = _subscribedSubreddits.asStateFlow()

    fun toggleSubscription(sub: String) {
        val next = sub.lowercase().trim().replace(" ", "")
        if (next.isEmpty()) return
        val updated = if (_subscribedSubreddits.value.contains(next)) _subscribedSubreddits.value - next else _subscribedSubreddits.value + next
        _subscribedSubreddits.value = updated
        prefs.edit().putStringSet("subscriptions", updated).apply()
        refreshSubscribed(updated.sorted().joinToString("+"))
    }

    private val watchedIds = prefs.getStringSet("watched_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
    private val watchedOrder: MutableList<String> = try {
        org.json.JSONArray(prefs.getString("watched_order", "[]") ?: "[]").let { arr ->
            (0 until arr.length()).map { arr.getString(it) }.toMutableList()
        }
    } catch (_: Exception) { mutableListOf() }
    private val watchedTitles: MutableMap<String, String> = try {
        val json = prefs.getString("watched_titles", null)
        if (json != null) org.json.JSONObject(json).let { obj ->
            obj.keys().asSequence().associateWith { obj.getString(it) }.toMutableMap()
        } else mutableMapOf()
    } catch (_: Exception) { mutableMapOf() }
    private val watchedPosts: MutableMap<String, RedditPost> = try {
        val json = prefs.getString("watched_posts", null)
        if (json != null) org.json.JSONArray(json).let { arr ->
            (0 until arr.length()).mapNotNull { i -> runCatching { arr.getJSONObject(i).toRedditPost() }.getOrNull() }
                .associateBy { it.id }.toMutableMap()
        } else mutableMapOf()
    } catch (_: Exception) { mutableMapOf() }
    private val _watchedIdsFlow = MutableStateFlow<Set<String>>(watchedIds.toSet())
    val watchedIdsFlow: StateFlow<Set<String>> = _watchedIdsFlow.asStateFlow()
    private val _watchedHistoryFlow = MutableStateFlow<List<RedditPost>>(getWatchedPosts())
    val watchedHistoryFlow: StateFlow<List<RedditPost>> = _watchedHistoryFlow.asStateFlow()

    init {
        // ponytail: trim to cap on load and prune any orphaned post/title entries
        if (watchedOrder.size > WATCHED_CAP) {
            val trim = watchedOrder.size - WATCHED_CAP
            val removeIds = watchedOrder.take(trim)
            repeat(trim) { watchedOrder.removeAt(0) }
            removeIds.forEach { watchedIds.remove(it); watchedTitles.remove(it); watchedPosts.remove(it) }
            _watchedIdsFlow.value = watchedIds.toSet()
            _watchedHistoryFlow.value = getWatchedPosts()
            saveWatched()
        }
        Log.i("WatchedVM", "loaded ${watchedIds.size} watched IDs")
    }

    // ponytail: liked posts — same persistence pattern as watched; drives swipe-right + Liked panel
    private val likedIdsSet = prefs.getStringSet("liked_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
    private val likedOrder: MutableList<String> = try {
        org.json.JSONArray(prefs.getString("liked_order", "[]") ?: "[]").let { arr ->
            (0 until arr.length()).map { arr.getString(it) }.toMutableList()
        }
    } catch (_: Exception) { mutableListOf() }
    private val likedPostsMap: MutableMap<String, RedditPost> = try {
        val json = prefs.getString("liked_posts", null)
        if (json != null) org.json.JSONArray(json).let { arr ->
            (0 until arr.length()).mapNotNull { i -> runCatching { arr.getJSONObject(i).toRedditPost() }.getOrNull() }
                .associateBy { it.id }.toMutableMap()
        } else mutableMapOf()
    } catch (_: Exception) { mutableMapOf() }
    private val _likedIdsFlow = MutableStateFlow<Set<String>>(likedIdsSet.toSet())
    val likedIdsFlow: StateFlow<Set<String>> = _likedIdsFlow.asStateFlow()

    init {
        if (likedOrder.size > LIKED_CAP) {
            val trim = likedOrder.size - LIKED_CAP
            val removeIds = likedOrder.take(trim)
            repeat(trim) { likedOrder.removeAt(0) }
            removeIds.forEach { likedIdsSet.remove(it); likedPostsMap.remove(it) }
            saveLiked()
        }
        Log.i("LikedVM", "loaded ${likedIdsSet.size} liked IDs")
    }

    private fun isHidden(id: String) = id in watchedIds || id in likedIdsSet

    fun toggleLike(post: RedditPost) {
        val id = post.id
        if (id in likedIdsSet) {
            likedIdsSet.remove(id)
            likedOrder.remove(id)
            likedPostsMap.remove(id)
        } else {
            likedIdsSet.add(id)
            likedOrder.add(id)
            likedPostsMap[id] = post
            while (likedOrder.size > LIKED_CAP) {
                val oldest = likedOrder.removeAt(0)
                likedIdsSet.remove(oldest)
                likedPostsMap.remove(oldest)
            }
        }
        _likedIdsFlow.value = likedIdsSet.toSet()
        saveLiked()
    }

    fun getLikedPosts(): List<RedditPost> = likedOrder.asReversed().mapNotNull { likedPostsMap[it] }

    private fun saveLiked() {
        prefs.edit()
            .putStringSet("liked_ids", likedIdsSet.toSet())
            .putString("liked_order", org.json.JSONArray(likedOrder).toString())
            .putString("liked_posts", org.json.JSONArray().apply { likedPostsMap.values.forEach { put(it.toJson()) } }.toString())
            .apply()
    }

    private fun showError(e: Throwable) {
        val msg = e.message ?: "Unknown error"
        // ponytail: show toast for rate limit so user knows to wait
        if (e is RedditError.RateLimited) {
            Toast.makeText(dataRepository.getContext(), "Rate limited by Reddit. Wait ${e.message?.substringAfter("after ")?.trim()?.removeSuffix("s") ?: ""}s", Toast.LENGTH_LONG).show()
        }
        Log.e("WatchedVM", "error: $msg")
    }

    private data class PagedSubredditCache(val subreddits: List<String>, val after: String?)
    private data class PagedVideoCache(val posts: List<RedditPost>, val after: String?)

    private var searchJob: Job? = null
    private val searchCache = ConcurrentHashMap<String, PagedSubredditCache>()
    private var subredditSearchAfter: String? = null
    private var currentSubredditSearchQuery: String = ""

    fun searchSubreddits(query: String, immediate: Boolean = false) {
        val trimmed = query.trim().lowercase().removePrefix("r/")
        if (trimmed.length < 2) { 
            _searchResults.value = emptyList()
            subredditSearchAfter = null
            return 
        }

        currentSubredditSearchQuery = trimmed
        val cached = searchCache[trimmed]
        if (cached != null) {
            _searchResults.value = cached.subreddits
            subredditSearchAfter = cached.after
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (!immediate) {
                delay(1000) // 1-second debounce when typing
            }
            try {
                dataRepository.searchSubredditsPaged(trimmed, null).collect { result ->
                    searchCache[trimmed] = PagedSubredditCache(result.subreddits, result.after)
                    subredditSearchAfter = result.after
                    _searchResults.value = result.subreddits
                }
            } catch (_: Exception) {
                _searchResults.value = emptyList()
            }
        }
    }

    fun loadMoreSubreddits() {
        val query = currentSubredditSearchQuery
        val currentList = _searchResults.value
        val after = subredditSearchAfter ?: currentList.lastOrNull()?.let { "t5_$it" }
        if (query.length < 2 || _isSubredditSearchingMore.value || currentList.isEmpty()) return

        viewModelScope.launch {
            _isSubredditSearchingMore.value = true
            try {
                dataRepository.searchSubredditsPaged(query, after).collect { result ->
                    if (result.after != null) {
                        subredditSearchAfter = result.after
                    }
                    val updated = (currentList + result.subreddits).distinct()
                    searchCache[query] = PagedSubredditCache(updated, subredditSearchAfter)
                    _searchResults.value = updated
                }
            } catch (_: Exception) {
                // Ignore failure
            } finally {
                _isSubredditSearchingMore.value = false
            }
        }
    }

    private var videoSearchJob: Job? = null
    private val videoSearchCache = ConcurrentHashMap<String, PagedVideoCache>()
    private var videoSearchAfter: String? = null
    private var currentVideoSearchQuery: String = ""
    private var currentVideoSearchSort: String = "relevance"
    private var currentVideoSearchSubreddit: String? = null

    fun searchVideos(query: String, sort: String = "relevance", immediate: Boolean = false, subreddit: String? = null) {
        val trimmed = query.trim()
        val cleanSub = subreddit?.trim()?.removePrefix("r/")?.takeIf { it.isNotEmpty() }
        if (trimmed.length < 2) {
            _videoSearchResults.value = emptyList()
            _isVideoSearching.value = false
            videoSearchAfter = null
            return
        }

        currentVideoSearchQuery = trimmed
        currentVideoSearchSort = sort
        currentVideoSearchSubreddit = cleanSub

        val cacheKey = if (cleanSub != null) "$cleanSub:$trimmed:$sort" else "$trimmed:$sort"
        val cached = videoSearchCache[cacheKey]
        if (cached != null) {
            _videoSearchResults.value = cached.posts
            videoSearchAfter = cached.after
            _isVideoSearching.value = false
            return
        }

        videoSearchJob?.cancel()
        videoSearchJob = viewModelScope.launch {
            _isVideoSearching.value = true
            if (!immediate) {
                delay(600) // 600ms debounce when typing
            }
            try {
                dataRepository.searchRedditVideosPaged(trimmed, sort, null, cleanSub).collect { result ->
                    val afterCursor = result.after ?: result.posts.lastOrNull()?.let { "t3_${it.id}" }
                    videoSearchCache[cacheKey] = PagedVideoCache(result.posts, afterCursor)
                    videoSearchAfter = afterCursor
                    _videoSearchResults.value = result.posts
                }
            } catch (_: Exception) {
                _videoSearchResults.value = emptyList()
            } finally {
                _isVideoSearching.value = false
            }
        }
    }

    fun loadMoreVideos() {
        val query = currentVideoSearchQuery
        val sort = currentVideoSearchSort
        val sub = currentVideoSearchSubreddit
        val currentList = _videoSearchResults.value
        val after = videoSearchAfter ?: currentList.lastOrNull()?.let { "t3_${it.id}" }
        if (query.length < 2 || _isVideoSearchingMore.value || _isVideoSearching.value || currentList.isEmpty()) return

        val cacheKey = if (sub != null) "$sub:$query:$sort" else "$query:$sort"

        viewModelScope.launch {
            _isVideoSearchingMore.value = true
            try {
                dataRepository.searchRedditVideosPaged(query, sort, after, sub).collect { result ->
                    val nextCursor = result.after ?: result.posts.lastOrNull()?.let { "t3_${it.id}" } ?: after
                    videoSearchAfter = nextCursor
                    val updated = (currentList + result.posts).distinctBy { it.id }
                    videoSearchCache[cacheKey] = PagedVideoCache(updated, nextCursor)
                    _videoSearchResults.value = updated
                }
            } catch (_: Exception) {
                // Ignore failure
            } finally {
                _isVideoSearchingMore.value = false
            }
        }
    }

    fun setSubredditSearchActive(active: Boolean) {
        _isSubredditSearchActive.value = active
        if (!active) {
            clearSubredditSearch()
        }
    }

    fun searchSubredditVideos(subreddit: String, query: String, sort: String = "relevance", immediate: Boolean = false) {
        val trimmed = query.trim()
        val cleanSub = subreddit.trim().removePrefix("r/")
        if (trimmed.length < 2 || cleanSub.isEmpty()) {
            _subredditSearchResults.value = emptyList()
            _isSubredditSearching.value = false
            inSubredditSearchAfter = null
            return
        }

        currentInSubredditSearchTarget = cleanSub
        currentInSubredditSearchQuery = trimmed

        val cacheKey = "$cleanSub:$trimmed:$sort"
        val cached = inSubredditSearchCache[cacheKey]
        if (cached != null) {
            _subredditSearchResults.value = cached.posts
            inSubredditSearchAfter = cached.after
            _isSubredditSearching.value = false
            return
        }

        inSubredditSearchJob?.cancel()
        inSubredditSearchJob = viewModelScope.launch {
            _isSubredditSearching.value = true
            if (!immediate) {
                delay(600)
            }
            try {
                dataRepository.searchRedditVideosPaged(trimmed, sort, null, cleanSub).collect { result ->
                    val afterCursor = result.after ?: result.posts.lastOrNull()?.let { "t3_${it.id}" }
                    inSubredditSearchCache[cacheKey] = PagedVideoCache(result.posts, afterCursor)
                    inSubredditSearchAfter = afterCursor
                    _subredditSearchResults.value = result.posts
                }
            } catch (_: Exception) {
                _subredditSearchResults.value = emptyList()
            } finally {
                _isSubredditSearching.value = false
            }
        }
    }

    fun loadMoreSubredditVideos() {
        val sub = currentInSubredditSearchTarget
        val query = currentInSubredditSearchQuery
        val currentList = _subredditSearchResults.value
        val after = inSubredditSearchAfter ?: currentList.lastOrNull()?.let { "t3_${it.id}" }
        if (query.length < 2 || sub.isEmpty() || _isSubredditSearchingMore.value || _isSubredditSearching.value || currentList.isEmpty()) return

        viewModelScope.launch {
            _isSubredditSearchingMore.value = true
            try {
                dataRepository.searchRedditVideosPaged(query, "relevance", after, sub).collect { result ->
                    val nextCursor = result.after ?: result.posts.lastOrNull()?.let { "t3_${it.id}" } ?: after
                    inSubredditSearchAfter = nextCursor
                    val updated = (currentList + result.posts).distinctBy { it.id }
                    inSubredditSearchCache["$sub:$query:relevance"] = PagedVideoCache(updated, nextCursor)
                    _subredditSearchResults.value = updated
                }
            } catch (_: Exception) {
                // Ignore failure
            } finally {
                _isSubredditSearchingMore.value = false
            }
        }
    }

    fun clearSubredditSearch() {
        inSubredditSearchJob?.cancel()
        _subredditSearchResults.value = emptyList()
        _isSubredditSearching.value = false
        _isSubredditSearchingMore.value = false
        inSubredditSearchAfter = null
        currentInSubredditSearchQuery = ""
        currentInSubredditSearchTarget = ""
    }

    fun setSort(sort: SortOption) {
        if (currentSort == sort) return
        currentSort = sort
        dataRepository.saveAfterMap(emptyMap(), "explore")
        dataRepository.saveAfterMap(emptyMap(), "subscribed")
        refreshExplore(forceRefresh = true)
        if (subscribedQuery.isNotEmpty()) {
            refreshSubscribed(subscribedQuery, forceRefresh = true)
        }
    }

    private val memoryFeedCache = com.lean.reddittube.util.AdaptiveCacheEngine<String, List<RedditPost>>(
        lowerBound = 16,
        upperBound = 100,
        memoryFraction = 0.10f,
        sizeEstimator = { list -> (list.size * 512L).coerceAtLeast(1024L) }
    )

    fun refreshExplore(query: String = exploreQuery, forceRefresh: Boolean = false) {
        if (query != exploreQuery) {
            setSubredditSearchActive(false)
        }
        exploreQuery = query
        viewModelScope.launch {
            val sortVal = currentSort.value
            val cacheKey = "explore_${query}_$sortVal"
            val cached = (memoryFeedCache[cacheKey] ?: loadExploreCache(query, sortVal).filter { !isHidden(it.id) }).distinctBy { it.id }
            val lastRefresh = lastExploreRefreshTime[cacheKey] ?: 0L
            val isFresh = (System.currentTimeMillis() - lastRefresh) < CACHE_STALE_DURATION_MS

            if (cached.isNotEmpty()) {
                memoryFeedCache[cacheKey] = cached
                _exploreState.value = MainScreenUiState.Success(cached)
                if (!forceRefresh && isFresh) {
                    return@launch
                }
            } else {
                _exploreState.value = MainScreenUiState.Loading
            }
            try {
                dataRepository.fetchRedditVideos(query, sortVal, "explore").collect { posts ->
                    val filtered = posts.filter { !isHidden(it.id) }.distinctBy { it.id }
                    if (filtered.isNotEmpty()) {
                        memoryFeedCache[cacheKey] = filtered
                        lastExploreRefreshTime[cacheKey] = System.currentTimeMillis()
                        _exploreState.value = MainScreenUiState.Success(filtered)
                        saveExploreCache(query, sortVal, filtered)
                    } else if (cached.isNotEmpty()) {
                        _exploreState.value = MainScreenUiState.Success(cached)
                    } else {
                        _exploreState.value = MainScreenUiState.Error(RedditError.NoVideosFound(query))
                    }
                }
            } catch (e: RedditError) {
                showError(e)
                if (_exploreState.value !is MainScreenUiState.Success) {
                    _exploreState.value = MainScreenUiState.Error(e)
                }
            } catch (e: Exception) {
                showError(e)
                if (_exploreState.value !is MainScreenUiState.Success) {
                    _exploreState.value = MainScreenUiState.Error(RedditError.Unknown(e.message ?: "Unknown error", e))
                }
            }
        }
    }

    fun refreshSubscribed(query: String, forceRefresh: Boolean = false) {
        subscribedQuery = query
        viewModelScope.launch {
            if (query.isEmpty()) {
                _subscribedState.value = MainScreenUiState.Error(RedditError.Unknown("No subscribed subreddits. Use the search icon to add subreddits."))
                return@launch
            }
            val sortVal = currentSort.value
            val cacheKey = "sub_${query}_$sortVal"
            val cached = (memoryFeedCache[cacheKey] ?: loadSubscribedCache(query, sortVal).filter { !isHidden(it.id) }).distinctBy { it.id }
            val lastRefresh = lastSubscribedRefreshTime[cacheKey] ?: 0L
            val isFresh = (System.currentTimeMillis() - lastRefresh) < CACHE_STALE_DURATION_MS

            if (cached.isNotEmpty()) {
                memoryFeedCache[cacheKey] = cached
                _subscribedState.value = MainScreenUiState.Success(cached)
                if (!forceRefresh && isFresh) {
                    return@launch
                }
            } else {
                _subscribedState.value = MainScreenUiState.Loading
            }
            try {
                dataRepository.fetchRedditVideos(query, sortVal, "subscribed").collect { posts ->
                    val filtered = posts.filter { !isHidden(it.id) }.distinctBy { it.id }
                    if (filtered.isNotEmpty()) {
                        memoryFeedCache[cacheKey] = filtered
                        lastSubscribedRefreshTime[cacheKey] = System.currentTimeMillis()
                        _subscribedState.value = MainScreenUiState.Success(filtered)
                        saveSubscribedCache(query, sortVal, filtered)
                    } else if (cached.isNotEmpty()) {
                        _subscribedState.value = MainScreenUiState.Success(cached)
                    } else {
                        _subscribedState.value = MainScreenUiState.Error(RedditError.NoVideosFound(query))
                    }
                }
            } catch (e: RedditError) {
                showError(e)
                if (_subscribedState.value !is MainScreenUiState.Success) {
                    _subscribedState.value = MainScreenUiState.Error(e)
                }
            } catch (e: Exception) {
                showError(e)
                if (_subscribedState.value !is MainScreenUiState.Success) {
                    _subscribedState.value = MainScreenUiState.Error(RedditError.Unknown(e.message ?: "Unknown error", e))
                }
            }
        }
    }

    fun loadMore(isExplore: Boolean) {
        val current = if (isExplore) _exploreState.value else _subscribedState.value
        if (current !is MainScreenUiState.Success) return
        if (current.isLoadingMore) return  // already loading
        val query = if (isExplore) exploreQuery else subscribedQuery
        val feed = if (isExplore) "explore" else "subscribed"
        val afterMap = dataRepository.getAfterMap(feed)
        if (afterMap.isNotEmpty() && afterMap.values.all { it == null }) return  // no more pages anywhere

        viewModelScope.launch {
            // set loading flag
            if (isExplore) {
                _exploreState.value = current.copy(isLoadingMore = true)
            } else {
                _subscribedState.value = current.copy(isLoadingMore = true)
            }
            try {
                dataRepository.fetchMoreVideos(query, afterMap, currentSort.value, feed).collect { result ->
                    dataRepository.saveAfterMap(result.afterMap, feed)
                    val existingIds = current.data.mapTo(HashSet()) { it.id }
                    val newPosts = result.posts
                        .filter { it.id !in existingIds && !isHidden(it.id) }
                        .distinctBy { it.id }
                    val updated = (current.data + newPosts).distinctBy { it.id }
                    if (isExplore) {
                        _exploreState.value = MainScreenUiState.Success(updated, isLoadingMore = false)
                    } else {
                        _subscribedState.value = MainScreenUiState.Success(updated, isLoadingMore = false)
                    }
                }
            } catch (e: Exception) {
                showError(e)
                if (isExplore) {
                    _exploreState.value = current.copy(isLoadingMore = false)
                } else {
                    _subscribedState.value = current.copy(isLoadingMore = false)
                }
            }
        }
    }

    fun markAsWatched(post: RedditPost) {
        val id = post.id
        watchedIds.add(id)
        watchedOrder.removeAll { it == id }
        watchedOrder.add(id)
        watchedTitles[id] = post.title
        watchedPosts[id] = post
        // trim oldest if over cap
        while (watchedOrder.size > WATCHED_CAP) {
            val oldest = watchedOrder.removeAt(0)
            watchedIds.remove(oldest)
            watchedTitles.remove(oldest)
            watchedPosts.remove(oldest)
        }
        _watchedIdsFlow.value = watchedIds.toSet()
        _watchedHistoryFlow.value = getWatchedPosts()
        saveWatched()
        Log.i("WatchedVM", "markAsWatched $id, total=${watchedIds.size}")
        // don't remove from current list; only filter on next refresh to avoid auto-advance
    }

    fun hidePost(post: RedditPost) {
        markAsWatched(post)
        val exploreCurrent = _exploreState.value
        if (exploreCurrent is MainScreenUiState.Success) {
            val updated = exploreCurrent.data.filterNot { it.id == post.id }
            _exploreState.value = exploreCurrent.copy(data = updated)
        }
        val subCurrent = _subscribedState.value
        if (subCurrent is MainScreenUiState.Success) {
            val updated = subCurrent.data.filterNot { it.id == post.id }
            _subscribedState.value = subCurrent.copy(data = updated)
        }
    }

    private fun saveWatched() {
        prefs.edit()
            .putStringSet("watched_ids", watchedIds.toSet())
            .putString("watched_order", org.json.JSONArray(watchedOrder).toString())
            .putString("watched_titles", org.json.JSONObject(watchedTitles).toString())
            .putString("watched_posts", org.json.JSONArray().apply { watchedPosts.values.forEach { put(it.toJson()) } }.toString())
            .apply()
    }

    fun getWatchedPosts(): List<RedditPost> {
        return watchedOrder.asReversed().mapNotNull { id ->
            watchedPosts[id] ?: watchedTitles[id]?.let { RedditPost(id, it, "", "", 0, "", "", "", "", "") }
        }
    }

    fun clearWatchedHistory() {
        watchedIds.clear()
        watchedOrder.clear()
        watchedTitles.clear()
        watchedPosts.clear()
        _watchedIdsFlow.value = emptySet()
        _watchedHistoryFlow.value = emptyList()
        saveWatched()
    }

    fun fetchComments(subreddit: String, postId: String): Flow<List<RedditComment>> {
        return dataRepository.fetchPostComments(subreddit, postId)
    }

    private fun saveExploreCache(query: String, sort: String, posts: List<RedditPost>) {
        val arr = org.json.JSONArray().apply { posts.take(60).forEach { put(it.toJson()) } }
        prefs.edit().putString("explore_cache_${query}_$sort", arr.toString()).apply()
    }

    private fun loadExploreCache(query: String, sort: String): List<RedditPost> {
        val str = prefs.getString("explore_cache_${query}_$sort", null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(str)
            (0 until arr.length()).mapNotNull { arr.getJSONObject(it).toRedditPost() }.distinctBy { it.id }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveSubscribedCache(query: String, sort: String, posts: List<RedditPost>) {
        val arr = org.json.JSONArray().apply { posts.take(60).forEach { put(it.toJson()) } }
        prefs.edit().putString("subscribed_cache_${query}_$sort", arr.toString()).apply()
    }

    private fun loadSubscribedCache(query: String, sort: String): List<RedditPost> {
        val str = prefs.getString("subscribed_cache_${query}_$sort", null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(str)
            (0 until arr.length()).mapNotNull { arr.getJSONObject(it).toRedditPost() }.distinctBy { it.id }
        } catch (_: Exception) {
            emptyList()
        }
    }

}
