package com.example.reddittube.ui.main

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reddittube.data.DataRepository
import com.example.reddittube.data.RedditError
import com.example.reddittube.data.RedditPost
import com.example.reddittube.utils.toJson
import com.example.reddittube.utils.toRedditPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ponytail: Simplified UI state container. Now manages separate states for Explore and Subscribed sections.
@Immutable
sealed interface MainScreenUiState {
    data object Loading : MainScreenUiState
    data class Error(val throwable: RedditError) : MainScreenUiState
    data class Success(val data: List<RedditPost>, val isLoadingMore: Boolean = false) : MainScreenUiState
}

// ponytail: sort options for Reddit feed
enum class SortOption(val value: String, val label: String) {
    HOT("hot", "Hot"),
    NEW("new", "New"),
    TOP("top", "Top")
}

class MainScreenViewModel(private val dataRepository: DataRepository) : ViewModel() {
    private val _exploreState = MutableStateFlow<MainScreenUiState>(MainScreenUiState.Loading)
    val exploreState: StateFlow<MainScreenUiState> = _exploreState.asStateFlow()

    private val _subscribedState = MutableStateFlow<MainScreenUiState>(MainScreenUiState.Loading)
    val subscribedState: StateFlow<MainScreenUiState> = _subscribedState.asStateFlow()

    var exploreQuery = "shorts+TikTokCringe+funny+videos"
        private set
    var subscribedQuery = ""
        private set

    var currentSort: SortOption = SortOption.HOT
        private set

    private val _searchResults = MutableStateFlow<List<String>>(emptyList())
    val searchResults: StateFlow<List<String>> = _searchResults.asStateFlow()

    // ponytail: list + index opened from the browse/home grid, played by PlayerScreen
    private val _playerList = MutableStateFlow<List<RedditPost>>(emptyList())
    val playerList: StateFlow<List<RedditPost>> = _playerList.asStateFlow()
    var playerStartIndex = 0
        private set

    fun openPlayer(list: List<RedditPost>, index: Int) {
        _playerList.value = list
        playerStartIndex = index
    }

    // ponytail: persist watched IDs to SharedPreferences so they survive app restart
    // ponytail: cap at 1000 entries, trim oldest via watched_order list
    companion object { private const val WATCHED_CAP = 1000 }
    private val prefs = dataRepository.getContext().getSharedPreferences("reddittube_prefs", android.content.Context.MODE_PRIVATE)
    private val _subscribedSubreddits = MutableStateFlow(
        prefs.getStringSet("subscriptions", setOf("shorts", "TikTokCringe", "funny", "videos"))!!
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
    init {
        // ponytail: trim to cap on load
        if (watchedOrder.size > WATCHED_CAP) {
            val trim = watchedOrder.size - WATCHED_CAP
            val removeIds = watchedOrder.take(trim)
            repeat(trim) { watchedOrder.removeAt(0) }
            removeIds.forEach { watchedIds.remove(it); watchedTitles.remove(it); watchedPosts.remove(it) }
            saveWatched()
        }
        Log.i("WatchedVM", "loaded ${watchedIds.size} watched IDs")
    }

    private fun showError(e: Throwable) {
        val msg = e.message ?: "Unknown error"
        // ponytail: show toast for rate limit so user knows to wait
        if (e is RedditError.RateLimited) {
            Toast.makeText(dataRepository.getContext(), "Rate limited by Reddit. Wait ${e.message?.substringAfter("after ")?.trim()?.removeSuffix("s") ?: ""}s", Toast.LENGTH_LONG).show()
        }
        Log.e("WatchedVM", "error: $msg")
    }

    fun searchSubreddits(query: String) {
        if (query.length < 2) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            try {
                dataRepository.searchSubreddits(query).collect { results ->
                    _searchResults.value = results
                }
            } catch (_: Exception) {
                _searchResults.value = emptyList()
            }
        }
    }

    fun setSort(sort: SortOption) {
        currentSort = sort
        refreshExplore()
    }

    fun refreshExplore(query: String = exploreQuery) {
        exploreQuery = query
        viewModelScope.launch {
            _exploreState.value = MainScreenUiState.Loading
            try {
                dataRepository.fetchRedditVideos(query, currentSort.value).collect { posts ->
                    _exploreState.value = MainScreenUiState.Success(posts.filter { it.id !in watchedIds })
                }
            } catch (e: RedditError) {
                showError(e)
                _exploreState.value = MainScreenUiState.Error(e)
            } catch (e: Exception) {
                showError(e)
                _exploreState.value = MainScreenUiState.Error(RedditError.Unknown(e.message ?: "Unknown error", e))
            }
        }
    }

    fun refreshSubscribed(query: String) {
        subscribedQuery = query
        viewModelScope.launch {
            if (query.isEmpty()) {
                _subscribedState.value = MainScreenUiState.Error(RedditError.Unknown("No subscribed subreddits. Use the search icon to add subreddits."))
                return@launch
            }
            _subscribedState.value = MainScreenUiState.Loading
            try {
                dataRepository.fetchRedditVideos(query, currentSort.value, "subscribed").collect { posts ->
                    _subscribedState.value = MainScreenUiState.Success(posts.filter { it.id !in watchedIds })
                }
            } catch (e: RedditError) {
                showError(e)
                _subscribedState.value = MainScreenUiState.Error(e)
            } catch (e: Exception) {
                showError(e)
                _subscribedState.value = MainScreenUiState.Error(RedditError.Unknown(e.message ?: "Unknown error", e))
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
        if (afterMap.values.all { it == null }) return  // no more pages anywhere

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
                    val updated = current.data + result.posts.filter { it.id !in watchedIds }
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
        if (id in watchedIds) return  // ponytail: already tracked, skip
        watchedIds.add(id)
        watchedOrder.add(id)
        watchedTitles[id] = post.title
        watchedPosts[id] = post
        // ponytail: trim oldest if over cap
        while (watchedOrder.size > WATCHED_CAP) {
            val oldest = watchedOrder.removeAt(0)
            watchedIds.remove(oldest)
            watchedTitles.remove(oldest)
            watchedPosts.remove(oldest)
        }
        saveWatched()
        Log.i("WatchedVM", "markAsWatched $id, total=${watchedIds.size}")
        // ponytail: don't remove from current list — only filter on next refresh to avoid auto-advance
    }

    private fun saveWatched() {
        prefs.edit()
            .putStringSet("watched_ids", watchedIds.toSet())
            .putString("watched_order", org.json.JSONArray(watchedOrder).toString())
            .putString("watched_titles", org.json.JSONObject(watchedTitles).toString())
            .putString("watched_posts", org.json.JSONArray().apply { watchedPosts.values.forEach { put(it.toJson()) } }.toString())
            .commit()
    }

    fun getWatchedPosts(): List<RedditPost> {
        return watchedOrder.mapNotNull { id ->
            watchedPosts[id] ?: watchedTitles[id]?.let { RedditPost(id, it, "", "", 0, "", "", "", "", "") }
        }
    }

}
