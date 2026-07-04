package com.example.reddittube.ui.main

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reddittube.data.DataRepository
import com.example.reddittube.data.RedditError
import com.example.reddittube.data.RedditPost
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

    // ponytail: persist watched IDs to SharedPreferences so they survive app restart
    private val prefs = dataRepository.getContext().getSharedPreferences("reddittube_prefs", android.content.Context.MODE_PRIVATE)
    private val watchedIds = prefs.getStringSet("watched_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
    private val watchedTitles: MutableMap<String, String> = try {
        val json = prefs.getString("watched_titles", null)
        if (json != null) org.json.JSONObject(json).let { obj ->
            obj.keys().asSequence().associateWith { obj.getString(it) }.toMutableMap()
        } else mutableMapOf()
    } catch (_: Exception) { mutableMapOf() }
    init { Log.i("WatchedVM", "loaded ${watchedIds.size} watched IDs: $watchedIds") }

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
                _exploreState.value = MainScreenUiState.Error(e)
            } catch (e: Exception) {
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
                dataRepository.fetchRedditVideos(query).collect { posts ->
                    _subscribedState.value = MainScreenUiState.Success(posts.filter { it.id !in watchedIds })
                }
            } catch (e: RedditError) {
                _subscribedState.value = MainScreenUiState.Error(e)
            } catch (e: Exception) {
                _subscribedState.value = MainScreenUiState.Error(RedditError.Unknown(e.message ?: "Unknown error", e))
            }
        }
    }

    fun loadMore(isExplore: Boolean) {
        val current = if (isExplore) _exploreState.value else _subscribedState.value
        if (current !is MainScreenUiState.Success) return
        if (current.isLoadingMore) return  // already loading
        val query = if (isExplore) exploreQuery else subscribedQuery
        val afterMap = dataRepository.getAfterMap()
        if (afterMap.values.all { it == null }) return  // no more pages anywhere

        viewModelScope.launch {
            // set loading flag
            if (isExplore) {
                _exploreState.value = current.copy(isLoadingMore = true)
            } else {
                _subscribedState.value = current.copy(isLoadingMore = true)
            }
            try {
                dataRepository.fetchMoreVideos(query, afterMap, currentSort.value).collect { result ->
                    dataRepository.saveAfterMap(result.afterMap)
                    val updated = current.data + result.posts.filter { it.id !in watchedIds }
                    if (isExplore) {
                        _exploreState.value = MainScreenUiState.Success(updated, isLoadingMore = false)
                    } else {
                        _subscribedState.value = MainScreenUiState.Success(updated, isLoadingMore = false)
                    }
                }
            } catch (_: Exception) {
                if (isExplore) {
                    _exploreState.value = current.copy(isLoadingMore = false)
                } else {
                    _subscribedState.value = current.copy(isLoadingMore = false)
                }
            }
        }
    }

    fun markAsWatched(id: String, title: String = "") {
        watchedIds.add(id)
        if (title.isNotEmpty()) watchedTitles[id] = title
        prefs.edit()
            .putStringSet("watched_ids", watchedIds.toSet())
            .putString("watched_titles", org.json.JSONObject(watchedTitles).toString())
            .commit()
        Log.i("WatchedVM", "markAsWatched $id, total=${watchedIds.size}")
        // ponytail: don't remove from current list — only filter on next refresh to avoid auto-advance
    }

    fun getWatchedTitles(): Map<String, String> = watchedTitles.toMap()
}
