package com.synapse.social.studioasinc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.synapse.social.studioasinc.model.User
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.data.Result
import com.synapse.social.studioasinc.data.repository.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ProfileViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val postRepository = PostRepository()
    private val followRepository = FollowRepository()
    private val likeRepository = LikeRepository()
    private val favoriteRepository = FavoriteRepository()
    private val profileLikeRepository = ProfileLikeRepository()

    private val _userProfile = MutableLiveData<State<User>>()
    val userProfile: LiveData<State<User>> = _userProfile

    private val _userPosts = MutableLiveData<State<List<Post>>>()
    val userPosts: LiveData<State<List<Post>>> = _userPosts

    private val _isFollowing = MutableLiveData<Boolean>()
    val isFollowing: LiveData<Boolean> = _isFollowing

    private val _isProfileLiked = MutableLiveData<Boolean>()
    val isProfileLiked: LiveData<Boolean> = _isProfileLiked

    sealed class State<out T> {
        object Loading : State<Nothing>()
        data class Success<T>(val data: T) : State<T>()
        data class Error(val message: String) : State<Nothing>()
    }
    
    fun loadUserProfile(uid: String) {
        viewModelScope.launch {
            _userProfile.value = State.Loading
            when (val result = userRepository.getUserById(uid)) {
                is Result.Success -> {
                    val user = result.data
                    if (user != null) {
                        _userProfile.value = State.Success(user)
                    } else {
                        _userProfile.value = State.Error("User not found")
                    }
                }
                is Result.Error -> _userProfile.value = State.Error(result.message)
                else -> {}
            }
        }
    }

    fun getUserPosts(uid: String) {
        viewModelScope.launch {
            _userPosts.value = State.Loading
            when (val result = postRepository.getUserPosts(uid)) {
                is Result.Success -> _userPosts.value = State.Success(result.data)
                is Result.Error -> _userPosts.value = State.Error(result.message)
                else -> {}
            }
        }
    }

    fun toggleFollow(targetUid: String) {
        viewModelScope.launch {
            val currentUid = AuthRepository().getCurrentUserId() ?: return@launch
            val result = followRepository.toggleFollow(currentUid, targetUid)
            if (result is Result.Success) {
                _isFollowing.value = result.data
                loadUserProfile(targetUid)
            }
        }
    }

    fun toggleProfileLike(targetUid: String) {
        viewModelScope.launch {
            val currentUid = AuthRepository().getCurrentUserId() ?: return@launch
            val result = profileLikeRepository.toggleProfileLike(currentUid, targetUid)
            if (result is Result.Success) {
                _isProfileLiked.value = result.data
            }
        }
    }

    fun fetchInitialFollowState(targetUid: String) {
        viewModelScope.launch {
            val currentUid = AuthRepository().getCurrentUserId() ?: return@launch
            val result = followRepository.isFollowing(currentUid, targetUid)
            if (result is Result.Success) {
                _isFollowing.value = result.data
            }
        }
    }

    fun fetchInitialProfileLikeState(targetUid: String) {
        viewModelScope.launch {
            val currentUid = AuthRepository().getCurrentUserId() ?: return@launch
            val result = profileLikeRepository.isProfileLiked(currentUid, targetUid)
            if (result is Result.Success) {
                _isProfileLiked.value = result.data
            }
        }
    }

    fun togglePostLike(postId: String) {
        viewModelScope.launch {
            val currentUid = AuthRepository().getCurrentUserId() ?: return@launch
            postRepository.toggleReaction(postId, currentUid, com.synapse.social.studioasinc.model.ReactionType.LIKE)
        }
    }

    fun toggleFavorite(postId: String) {
        viewModelScope.launch {
            val currentUid = AuthRepository().getCurrentUserId() ?: return@launch
            favoriteRepository.toggleFavorite(currentUid, postId)
        }
    }
}
