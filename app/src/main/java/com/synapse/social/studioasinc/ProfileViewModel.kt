package com.synapse.social.studioasinc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseFollowService
import com.synapse.social.studioasinc.model.User
import com.synapse.social.studioasinc.model.Post
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

/**
 * ViewModel for managing profile data and operations
 */
class ProfileViewModel : ViewModel() {

    private val dbService = SupabaseDatabaseService()
    private val authService = SupabaseAuthenticationService()
    private val followService = SupabaseFollowService()

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

    /**
     * Loads user profile data
     */
    fun loadUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                _userProfile.value = State.Loading
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository()
                // Fetch user data directly to get all fields including join_date
                val result = dbService.selectWhere("users", "*", "uid", uid)
                result.onSuccess { users ->
                    val userProfile = users.firstOrNull()
                    if (userProfile != null) {
                        val user = User(
                            uid = userProfile["uid"]?.toString() ?: uid,
                            username = userProfile["username"]?.toString(),
                            email = userProfile["email"]?.toString(),
                            displayName = userProfile["display_name"]?.toString() ?: userProfile["nickname"]?.toString(),
                            profileImageUrl = userProfile["avatar"]?.toString() ?: userProfile["profile_image_url"]?.toString(),
                            bio = userProfile["bio"]?.toString() ?: userProfile["biography"]?.toString(),
                            joinDate = userProfile["join_date"]?.toString(),
                            createdAt = userProfile["created_at"]?.toString(),
                            followersCount = userProfile["followers_count"]?.toString()?.toIntOrNull() ?: 0,
                            followingCount = userProfile["following_count"]?.toString()?.toIntOrNull() ?: 0,
                            postsCount = userProfile["posts_count"]?.toString()?.toIntOrNull() ?: 0,
                            status = userProfile["status"]?.toString() ?: "offline"
                        )
                        _userProfile.value = State.Success(user)
                    } else {
                        _userProfile.value = State.Error("User not found")
                    }
                }
                    .onFailure { exception ->
                        _userProfile.value = State.Error(exception.message ?: "Failed to load user profile")
                    }
            } catch (e: Exception) {
                _userProfile.value = State.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Gets user posts
     */
    fun getUserPosts(uid: String) {
        viewModelScope.launch {
            try {
                _userPosts.value = State.Loading
                val postRepository = com.synapse.social.studioasinc.data.repository.PostRepository()
                postRepository.getUserPosts(uid)
                    .onSuccess { posts ->
                        _userPosts.value = State.Success(posts)
                    }
                    .onFailure { exception ->
                        _userPosts.value = State.Error(exception.message ?: "Failed to load posts")
                    }
            } catch (e: Exception) {
                _userPosts.value = State.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Toggles follow status for a user
     */
    fun toggleFollow(targetUid: String) {
        viewModelScope.launch {
            try {
                val currentUid = authService.getCurrentUserId() ?: return@launch
                val isCurrentlyFollowing = _isFollowing.value ?: false
                
                val result = if (isCurrentlyFollowing) {
                    followService.unfollowUser(currentUid, targetUid)
                } else {
                    followService.followUser(currentUid, targetUid)
                }
                
                result.fold(
                    onSuccess = {
                        _isFollowing.value = !isCurrentlyFollowing
                        // Refresh user profile to update follower counts
                        loadUserProfile(targetUid)
                    },
                    onFailure = { error ->
                        android.util.Log.e("ProfileViewModel", "Failed to toggle follow", error)
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error toggling follow", e)
            }
        }
    }

    /**
     * Toggles profile like status
     */
    fun toggleProfileLike(targetUid: String) {
        viewModelScope.launch {
            try {
                val currentUid = authService.getCurrentUserId() ?: return@launch
                val isCurrentlyLiked = _isProfileLiked.value ?: false
                
                if (isCurrentlyLiked) {
                    // Unlike profile - delete the specific like
                    val result = SupabaseClient.client.from("profile_likes").delete {
                        filter {
                            eq("liker_uid", currentUid)
                            eq("profile_uid", targetUid)
                        }
                    }
                    _isProfileLiked.value = false
                    android.util.Log.d("ProfileViewModel", "Profile unliked successfully")
                } else {
                    // Like profile
                    val likeData = mapOf(
                        "liker_uid" to currentUid,
                        "profile_uid" to targetUid
                    )
                    dbService.insert("profile_likes", likeData).fold(
                        onSuccess = {
                            _isProfileLiked.value = true
                            android.util.Log.d("ProfileViewModel", "Profile liked successfully")
                        },
                        onFailure = { error ->
                            android.util.Log.e("ProfileViewModel", "Failed to like profile", error)
                        }
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error toggling profile like", e)
            }
        }
    }

    /**
     * Toggles post like status
     */
    fun togglePostLike(postId: String) {
        viewModelScope.launch {
            try {
                val currentUid = authService.getCurrentUserId() ?: return@launch
                
                // Check if already liked
                val existingLike = dbService.selectWithFilter("post_likes", "*", "user_uid", currentUid).getOrNull() ?: emptyList()
                
                if (existingLike.isNotEmpty()) {
                    // Unlike
                    dbService.delete("post_likes", "user_uid", currentUid)
                } else {
                    // Like
                    val likeData = mapOf(
                        "user_uid" to currentUid,
                        "post_id" to postId,
                        "created_at" to System.currentTimeMillis().toString()
                    )
                    dbService.insert("post_likes", likeData)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * Toggles favorite status for a post
     */
    fun toggleFavorite(postId: String) {
        viewModelScope.launch {
            try {
                val currentUid = authService.getCurrentUserId() ?: return@launch
                
                // Check if already favorited
                val existingFavorite = dbService.selectWithFilter("favorites", "*", "user_uid", currentUid).getOrNull() ?: emptyList()
                
                if (existingFavorite.isNotEmpty()) {
                    // Remove favorite
                    dbService.delete("favorites", "user_uid", currentUid)
                } else {
                    // Add favorite
                    val favoriteData = mapOf(
                        "user_uid" to currentUid,
                        "post_id" to postId,
                        "created_at" to System.currentTimeMillis().toString()
                    )
                    dbService.insert("favorites", favoriteData)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * Fetches initial follow state
     */
    fun fetchInitialFollowState(targetUid: String) {
        viewModelScope.launch {
            try {
                val currentUid = authService.getCurrentUserId() ?: return@launch
                val result = followService.isFollowing(currentUid, targetUid)
                result.fold(
                    onSuccess = { isFollowing ->
                        _isFollowing.value = isFollowing
                    },
                    onFailure = { error ->
                        android.util.Log.e("ProfileViewModel", "Failed to check follow state", error)
                        _isFollowing.value = false
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error fetching follow state", e)
                _isFollowing.value = false
            }
        }
    }

    /**
     * Fetches initial profile like state
     */
    fun fetchInitialProfileLikeState(targetUid: String) {
        viewModelScope.launch {
            try {
                val currentUid = authService.getCurrentUserId() ?: return@launch
                
                // Check if current user has liked this profile
                val result = SupabaseClient.client.from("profile_likes")
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("id")) {
                        filter {
                            eq("liker_uid", currentUid)
                            eq("profile_uid", targetUid)
                        }
                    }
                    .decodeList<kotlinx.serialization.json.JsonObject>()
                
                _isProfileLiked.value = result.isNotEmpty()
                android.util.Log.d("ProfileViewModel", "Profile like state: ${result.isNotEmpty()}")
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error fetching profile like state", e)
                _isProfileLiked.value = false
            }
        }
    }
}