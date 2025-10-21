package com.synapse.social.studioasinc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.model.User
import com.synapse.social.studioasinc.model.Post
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.coroutines.launch

/**
 * ViewModel for managing profile data and operations
 */
class ProfileViewModel : ViewModel() {

    private val dbService = SupabaseDatabaseService()
    private val authService = SupabaseAuthenticationService()

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
                val result = dbService.selectById("users", uid, "uid")
                if (result != null) {
                    val user = User(
                        uid = result["uid"] as? String ?: "",
                        username = result["username"] as? String ?: "",
                        email = result["email"] as? String ?: "",
                        displayName = result["display_name"] as? String ?: "",
                        profileImageUrl = result["profile_image_url"] as? String,
                        bio = result["bio"] as? String,
                        followersCount = (result["followers_count"] as? Number)?.toInt() ?: 0,
                        followingCount = (result["following_count"] as? Number)?.toInt() ?: 0,
                        postsCount = (result["posts_count"] as? Number)?.toInt() ?: 0
                    )
                    _userProfile.value = State.Success(user)
                } else {
                    _userProfile.value = State.Error("User not found")
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
                val results = dbService.select<Map<String, Any?>>("posts") {
                    eq("author_uid", uid)
                }
                val posts = results.map { result ->
                    Post(
                        id = result["id"] as? String ?: "",
                        authorUid = result["author_uid"] as? String ?: "",
                        content = result["content"] as? String ?: "",
                        timestamp = (result["created_at"] as? String)?.toLongOrNull() ?: 0L,
                        likesCount = (result["likes_count"] as? Number)?.toInt() ?: 0,
                        commentsCount = (result["comments_count"] as? Number)?.toInt() ?: 0
                    )
                }
                _userPosts.value = State.Success(posts)
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
                
                if (isCurrentlyFollowing) {
                    // Unfollow
                    dbService.delete("follows") {
                        eq("follower_uid", currentUid)
                        eq("following_uid", targetUid)
                    }
                } else {
                    // Follow
                    val followData = mapOf(
                        "follower_uid" to currentUid,
                        "following_uid" to targetUid,
                        "created_at" to System.currentTimeMillis().toString()
                    )
                    dbService.insert("follows", followData)
                }
                
                _isFollowing.value = !isCurrentlyFollowing
            } catch (e: Exception) {
                // Handle error
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
                    // Unlike profile
                    dbService.delete("profile_likes") {
                        eq("liker_uid", currentUid)
                        eq("profile_uid", targetUid)
                    }
                } else {
                    // Like profile
                    val likeData = mapOf(
                        "liker_uid" to currentUid,
                        "profile_uid" to targetUid,
                        "created_at" to System.currentTimeMillis().toString()
                    )
                    dbService.insert("profile_likes", likeData)
                }
                
                _isProfileLiked.value = !isCurrentlyLiked
            } catch (e: Exception) {
                // Handle error
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
                val existingLike = dbService.select<Map<String, Any?>>("post_likes") {
                    eq("user_uid", currentUid)
                    eq("post_id", postId)
                }
                
                if (existingLike.isNotEmpty()) {
                    // Unlike
                    dbService.delete("post_likes") {
                        eq("user_uid", currentUid)
                        eq("post_id", postId)
                    }
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
                val existingFavorite = dbService.select<Map<String, Any?>>("favorites") {
                    eq("user_uid", currentUid)
                    eq("post_id", postId)
                }
                
                if (existingFavorite.isNotEmpty()) {
                    // Remove favorite
                    dbService.delete("favorites") {
                        eq("user_uid", currentUid)
                        eq("post_id", postId)
                    }
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
                val follows = dbService.select<Map<String, Any?>>("follows") {
                    eq("follower_uid", currentUid)
                    eq("following_uid", targetUid)
                }
                _isFollowing.value = follows.isNotEmpty()
            } catch (e: Exception) {
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
                val likes = dbService.select<Map<String, Any?>>("profile_likes") {
                    eq("liker_uid", currentUid)
                    eq("profile_uid", targetUid)
                }
                _isProfileLiked.value = likes.isNotEmpty()
            } catch (e: Exception) {
                _isProfileLiked.value = false
            }
        }
    }
}