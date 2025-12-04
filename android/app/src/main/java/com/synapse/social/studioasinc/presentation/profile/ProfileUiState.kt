package com.synapse.social.studioasinc.presentation.profile

import com.synapse.social.studioasinc.data.model.UserProfile
import com.synapse.social.studioasinc.model.Post

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val userProfile: UserProfile,
        val posts: List<Post> = emptyList(),
        val isFollowing: Boolean = false,
        val isCurrentUser: Boolean = false
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
    object Empty : ProfileUiState()
}
