package com.synapse.social.studioasinc.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.model.UserProfile
import com.synapse.social.studioasinc.domain.usecase.profile.FollowUserUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.GetProfileContentUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.GetProfileUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.UnfollowUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileScreenState(
    val profileState: ProfileUiState = ProfileUiState.Loading,
    val contentFilter: ProfileContentFilter = ProfileContentFilter.POSTS,
    val posts: List<Any> = emptyList(),
    val photos: List<Any> = emptyList(),
    val reels: List<Any> = emptyList(),
    val isFollowing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val postsOffset: Int = 0,
    val photosOffset: Int = 0,
    val reelsOffset: Int = 0,
    val currentUserId: String = "",
    val isOwnProfile: Boolean = false,
    val showMoreMenu: Boolean = false
)

class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase,
    private val getProfileContentUseCase: GetProfileContentUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileScreenState())
    val state: StateFlow<ProfileScreenState> = _state.asStateFlow()

    fun loadProfile(userId: String, currentUserId: String) {
        _state.update { it.copy(profileState = ProfileUiState.Loading, currentUserId = currentUserId) }
        viewModelScope.launch {
            getProfileUseCase(userId).collect { result ->
                result.onSuccess { profile ->
                    _state.update {
                        it.copy(
                            profileState = ProfileUiState.Success(profile),
                            isOwnProfile = userId == currentUserId
                        )
                    }
                    loadContent(userId, ProfileContentFilter.POSTS)
                }.onFailure { exception ->
                    _state.update {
                        it.copy(
                            profileState = ProfileUiState.Error(
                                exception.message ?: "Failed to load profile",
                                exception
                            )
                        )
                    }
                }
            }
        }
    }

    fun refreshProfile(userId: String) {
        loadProfile(userId, _state.value.currentUserId)
    }

    fun switchContentFilter(filter: ProfileContentFilter) {
        _state.update { it.copy(contentFilter = filter) }
        val profile = (_state.value.profileState as? ProfileUiState.Success)?.profile ?: return
        loadContent(profile.id, filter)
    }

    fun loadMoreContent(filter: ProfileContentFilter) {
        val profile = (_state.value.profileState as? ProfileUiState.Success)?.profile ?: return
        _state.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            when (filter) {
                ProfileContentFilter.POSTS -> {
                    val offset = _state.value.postsOffset
                    getProfileContentUseCase.getPosts(profile.id, offset = offset).onSuccess { posts ->
                        _state.update {
                            it.copy(
                                posts = it.posts + posts,
                                postsOffset = offset + posts.size,
                                isLoadingMore = false
                            )
                        }
                    }.onFailure {
                        _state.update { it.copy(isLoadingMore = false) }
                    }
                }
                ProfileContentFilter.PHOTOS -> {
                    val offset = _state.value.photosOffset
                    getProfileContentUseCase.getPhotos(profile.id, offset = offset).onSuccess { photos ->
                        _state.update {
                            it.copy(
                                photos = it.photos + photos,
                                photosOffset = offset + photos.size,
                                isLoadingMore = false
                            )
                        }
                    }.onFailure {
                        _state.update { it.copy(isLoadingMore = false) }
                    }
                }
                ProfileContentFilter.REELS -> {
                    val offset = _state.value.reelsOffset
                    getProfileContentUseCase.getReels(profile.id, offset = offset).onSuccess { reels ->
                        _state.update {
                            it.copy(
                                reels = it.reels + reels,
                                reelsOffset = offset + reels.size,
                                isLoadingMore = false
                            )
                        }
                    }.onFailure {
                        _state.update { it.copy(isLoadingMore = false) }
                    }
                }
            }
        }
    }

    fun followUser(targetUserId: String) {
        viewModelScope.launch {
            followUserUseCase(_state.value.currentUserId, targetUserId).onSuccess {
                _state.update { it.copy(isFollowing = true) }
            }
        }
    }

    fun unfollowUser(targetUserId: String) {
        viewModelScope.launch {
            unfollowUserUseCase(_state.value.currentUserId, targetUserId).onSuccess {
                _state.update { it.copy(isFollowing = false) }
            }
        }
    }

    fun toggleMoreMenu() {
        _state.update { it.copy(showMoreMenu = !it.showMoreMenu) }
    }

    private fun loadContent(userId: String, filter: ProfileContentFilter) {
        viewModelScope.launch {
            when (filter) {
                ProfileContentFilter.POSTS -> {
                    getProfileContentUseCase.getPosts(userId).onSuccess { posts ->
                        _state.update { it.copy(posts = posts, postsOffset = posts.size) }
                    }
                }
                ProfileContentFilter.PHOTOS -> {
                    getProfileContentUseCase.getPhotos(userId).onSuccess { photos ->
                        _state.update { it.copy(photos = photos, photosOffset = photos.size) }
                    }
                }
                ProfileContentFilter.REELS -> {
                    getProfileContentUseCase.getReels(userId).onSuccess { reels ->
                        _state.update { it.copy(reels = reels, reelsOffset = reels.size) }
                    }
                }
            }
        }
    }
}
