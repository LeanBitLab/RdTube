package com.example.reddittube.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reddittube.data.DataRepository
import com.example.reddittube.data.RedditPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ponytail: Simplified UI state container. Dynamic subreddits query support added.
class MainScreenViewModel(private val dataRepository: DataRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<MainScreenUiState>(MainScreenUiState.Loading)
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()
    
    var currentSubreddits = "shorts+TikTokCringe+funny+videos"
        private set

    init {
        refresh(currentSubreddits)
    }

    fun refresh(query: String = currentSubreddits) {
        currentSubreddits = query
        viewModelScope.launch {
            _uiState.value = MainScreenUiState.Loading
            try {
                dataRepository.fetchRedditVideos(query).collect { posts ->
                    if (posts.isEmpty()) {
                        _uiState.value = MainScreenUiState.Error(Exception("No video posts found in r/$query"))
                    } else {
                        _uiState.value = MainScreenUiState.Success(posts)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = MainScreenUiState.Error(e)
            }
        }
    }
}

sealed interface MainScreenUiState {
    object Loading : MainScreenUiState
    data class Error(val throwable: Throwable) : MainScreenUiState
    data class Success(val data: List<RedditPost>) : MainScreenUiState
}
