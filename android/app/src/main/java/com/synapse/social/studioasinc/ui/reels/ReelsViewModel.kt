package com.synapse.social.studioasinc.ui.reels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReelsUiState(
    val reels: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ReelsViewModel(application: Application) : AndroidViewModel(application) {
    private val postRepository: PostRepository = PostRepository(AppDatabase.getDatabase(application).postDao())

    private val _uiState = MutableStateFlow(ReelsUiState())
    val uiState: StateFlow<ReelsUiState> = _uiState.asStateFlow()

    init {
        loadReels()
    }

    fun loadReels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // We use getPosts() for now as it returns Flow<Result<List<Post>>>.
                // In a real scenario, we would filter for video type in the repository query.
                // Assuming client-side filter for now if repo doesn't support specific query yet.
                postRepository.getPosts().collect { result ->
                     result.onSuccess { posts ->
                         val videoPosts = posts.filter { it.postType == "VIDEO" || it.mediaItems?.any { m -> m.type == com.synapse.social.studioasinc.model.MediaType.VIDEO } == true }
                         _uiState.update { it.copy(reels = videoPosts, isLoading = false) }
                     }.onFailure { e ->
                         _uiState.update { it.copy(error = e.message, isLoading = false) }
                     }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadMoreReels() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
             // Load next page
        }
    }

    fun likeReel(reelId: String) {
        // Like logic
    }
}
