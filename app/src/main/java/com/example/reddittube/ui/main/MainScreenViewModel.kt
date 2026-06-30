package com.example.reddittube.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reddittube.data.DataRepository
import com.example.reddittube.data.RedditPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ponytail: Simplified UI state container. Now manages separate states for Explore and Subscribed sections.
class MainScreenViewModel(private val dataRepository: DataRepository) : ViewModel() {
    private val _exploreState = MutableStateFlow<MainScreenUiState>(MainScreenUiState.Loading)
    val exploreState: StateFlow<MainScreenUiState> = _exploreState.asStateFlow()

    private val _subscribedState = MutableStateFlow<MainScreenUiState>(MainScreenUiState.Loading)
    val subscribedState: StateFlow<MainScreenUiState> = _subscribedState.asStateFlow()

    var exploreQuery = "shorts+TikTokCringe+funny+videos"
        private set
    var subscribedQuery = ""
        private set

    private val _searchResults = MutableStateFlow<List<String>>(emptyList())
    val searchResults: StateFlow<List<String>> = _searchResults.asStateFlow()

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

    fun refreshExplore(query: String = exploreQuery) {
        exploreQuery = query
        viewModelScope.launch {
            _exploreState.value = MainScreenUiState.Loading
            try {
                dataRepository.fetchRedditVideos(query).collect { posts ->
                    _exploreState.value = MainScreenUiState.Success(posts)
                }
            } catch (e: Exception) {
                _exploreState.value = MainScreenUiState.Error(e)
            }
        }
    }

    fun refreshSubscribed(query: String) {
        subscribedQuery = query
        viewModelScope.launch {
            if (query.isEmpty()) {
                _subscribedState.value = MainScreenUiState.Error(Exception("No subscribed subreddits. Use the search icon to add subreddits."))
                return@launch
            }
            _subscribedState.value = MainScreenUiState.Loading
            try {
                dataRepository.fetchRedditVideos(query).collect { posts ->
                    _subscribedState.value = MainScreenUiState.Success(posts)
                }
            } catch (e: Exception) {
                _subscribedState.value = MainScreenUiState.Error(e)
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
                dataRepository.fetchMoreVideos(query, afterMap).collect { result ->
                    dataRepository.saveAfterMap(result.afterMap)
                    val updated = current.data + result.posts
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
}

sealed interface MainScreenUiState {
    object Loading : MainScreenUiState
    data class Error(val throwable: Throwable) : MainScreenUiState
    data class Success(val data: List<RedditPost>, val isLoadingMore: Boolean = false) : MainScreenUiState
}
