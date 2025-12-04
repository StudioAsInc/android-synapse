package com.synapse.social.studioasinc.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.data.repository.ProfileRepositoryImpl
import com.synapse.social.studioasinc.domain.usecase.profile.*
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val getProfileContentUseCase: GetProfileContentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _filter = MutableStateFlow(ProfileContentFilter.POSTS)
    val filter: StateFlow<ProfileContentFilter> = _filter.asStateFlow()

    fun loadProfile(uid: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading

            // Determine if it's the current user
            val currentUid = SupabaseClient.client.auth.currentUserOrNull()?.id
            val isCurrentUser = currentUid == uid

            // Run tasks in parallel if possible, but for simplicity sequential here
            val profileResult = getProfileUseCase(uid)

            profileResult.fold(
                onSuccess = { profile ->
                    // Fetch content
                    val contentResult = getProfileContentUseCase(uid)
                    val posts = contentResult.getOrElse { emptyList() }

                    // Fetch follow status if not current user
                    var isFollowing = false
                    if (!isCurrentUser && currentUid != null) {
                         // TODO: Use IsFollowingUseCase
                         val repo = ProfileRepositoryImpl() // Temporary direct instantiation
                         isFollowing = repo.isFollowing(currentUid, uid).getOrElse { false }
                    }

                    _uiState.value = ProfileUiState.Success(
                        userProfile = profile,
                        posts = posts,
                        isFollowing = isFollowing,
                        isCurrentUser = isCurrentUser
                    )
                },
                onFailure = { error ->
                    _uiState.value = ProfileUiState.Error(error.message ?: "Failed to load profile")
                }
            )
        }
    }

    fun handleAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.Refresh -> {
                // Reload current profile
                val currentState = _uiState.value
                if (currentState is ProfileUiState.Success) {
                    loadProfile(currentState.userProfile.uid)
                }
            }
            is ProfileAction.ChangeFilter -> {
                _filter.value = action.filter
                // TODO: reload content based on filter
            }
            // Implement other actions
            else -> {}
        }
    }

    // Factory for creating ViewModel without DI for now
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                val repository = ProfileRepositoryImpl()
                return ProfileViewModel(
                    GetProfileUseCase(repository),
                    FollowUserUseCase(repository),
                    UnfollowUserUseCase(repository),
                    GetProfileContentUseCase(repository)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
