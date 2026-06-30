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
}

sealed interface MainScreenUiState {
    object Loading : MainScreenUiState
    data class Error(val throwable: Throwable) : MainScreenUiState
    data class Success(val data: List<RedditPost>) : MainScreenUiState
}
