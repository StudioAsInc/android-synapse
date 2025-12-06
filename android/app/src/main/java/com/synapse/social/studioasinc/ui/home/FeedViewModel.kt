package com.synapse.social.studioasinc.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.home.User
import com.synapse.social.studioasinc.ui.components.post.PostCardState
import com.synapse.social.studioasinc.util.ScrollPositionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class FeedUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class FeedViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository: AuthRepository = AuthRepository()
    private val postRepository: PostRepository = PostRepository(AppDatabase.getDatabase(application).postDao())

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    // Using PagingData for infinite scroll
    val posts: Flow<PagingData<Post>> = postRepository.getPostsPaged()
        .cachedIn(viewModelScope)

    private var savedScrollPosition: ScrollPositionState? = null

    init {
        // Initial load logic if needed (PagingData handles most)
    }

    fun likePost(post: Post) {
        viewModelScope.launch {
             try {
                 val currentUserId = authRepository.getCurrentUser()?.id
                 if (currentUserId != null) {
                    postRepository.toggleReaction(post.id, currentUserId, com.synapse.social.studioasinc.model.ReactionType.LIKE)
                 }
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }

    fun bookmarkPost(post: Post) {
        // Implement bookmark logic
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        // PagingAdapter refresh() triggers the refresh in UI usually
        // But if we have manual refresh logic here:
        // postRepository.refresh()
        // For now, we simulate refresh completion or let the UI pull trigger the paging refresh
         _uiState.value = _uiState.value.copy(isRefreshing = false)
    }

    /**
     * Helper to convert Post model to PostCardState for UI
     */
    fun mapPostToState(post: Post): PostCardState {
        // Fetch user data if not present in Post.
        // For now assuming Post has embedded user info or we can't map fully without extra calls.
        // In real app, Post usually has `author` field or we fetch users separately.
        // The `Post` model in `model/Post.kt` has `username` and `avatarUrl` as transient.
        // We might need to ensure these are populated by the Repository/PagingSource.

        val user = User(
            uid = post.authorUid,
            username = post.username ?: "Unknown",
            avatar = post.avatarUrl,
            verify = if(post.isVerified) "1" else "0"
        )

        val mediaUrls = post.mediaItems?.mapNotNull { it.url } ?: listOfNotNull(post.postImage)

        return PostCardState(
            post = post,
            user = user,
            isLiked = post.hasUserReacted(),
            likeCount = post.likesCount,
            commentCount = post.commentsCount,
            isBookmarked = false, // Add logic if available
            mediaUrls = mediaUrls,
            isVideo = post.postType == "VIDEO",
            pollQuestion = post.pollQuestion,
            pollOptions = post.pollOptions
        )
    }

    fun saveScrollPosition(position: Int, offset: Int) {
        savedScrollPosition = ScrollPositionState(position, offset)
    }

    fun restoreScrollPosition(): ScrollPositionState? {
        val position = savedScrollPosition
        return if (position != null && !position.isExpired()) {
            position
        } else {
            savedScrollPosition = null
            null
        }
    }
}
