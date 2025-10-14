package com.synapse.social.studioasinc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.User
import com.synapse.social.studioasinc.repository.UserRepository
import com.synapse.social.studioasinc.util.SupabaseClient
import io.supabase.postgrest.http.PostgrestHttpException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel for the [ProfileActivity].
 *
 * This ViewModel is responsible for fetching and managing the data
 * related to a user's profile, such as their posts, follow status,
 * and profile likes.
 */
class ProfileViewModel : ViewModel() {

    data class PostUiState(
        val post: Post,
        val user: User?,
        val likeCount: Long,
        val commentCount: Long,
        val isLiked: Boolean,
        val isFavorited: Boolean
    )

    sealed class State {
        object Loading : State()
        data class Success(val posts: List<PostUiState>) : State()
        object Error : State()
    }

    private val _userPosts = MutableLiveData<State>()
    val userPosts: LiveData<State> = _userPosts

    private val _isFollowing = MutableLiveData<Boolean>()
    val isFollowing: LiveData<Boolean> = _isFollowing

    private val _isProfileLiked = MutableLiveData<Boolean>()
    val isProfileLiked: LiveData<Boolean> = _isProfileLiked

    private val supabase = SupabaseClient.getClient()

    fun getUserPosts(userId: String) {
        _userPosts.postValue(State.Loading)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = supabase.getDatabase()
                    .from("posts")
                    .select()
                    .eq("uid", userId)
                    .execute()
                    .getBody()

                if (response != null) {
                    // Assuming the response can be mapped to a List<Post>
                    // This will require a custom serializer or manual mapping
                    val posts = response.map { Post.fromMap(it as Map<String, Any>) }
                    enrichPostsWithState(posts)
                } else {
                    _userPosts.postValue(State.Success(emptyList()))
                }
            } catch (e: PostgrestHttpException) {
                _userPosts.postValue(State.Error)
            }
        }
    }

    private fun enrichPostsWithState(posts: List<Post>) {
        val currentUid = supabase.getAuth().getUser()?.id ?: ""
        if (posts.isEmpty()) {
            _userPosts.postValue(State.Success(emptyList()))
            return
        }

        viewModelScope.launch {
            val enrichedPosts = posts.map { post ->
                async {
                    val userDeferred = async { UserRepository.getUser(post.uid) }
                    val likesDeferred = async { getLikes(post.key, currentUid) }
                    val commentsDeferred = async { getCommentCount(post.key) }
                    val favoritesDeferred = async { getFavoriteStatus(post.key, currentUid) }

                    val user = userDeferred.await()
                    val (likeCount, isLiked) = likesDeferred.await()
                    val commentCount = commentsDeferred.await()
                    val isFavorited = favoritesDeferred.await()

                    PostUiState(post, user, likeCount, commentCount, isLiked, isFavorited)
                }
            }.awaitAll()
            _userPosts.postValue(State.Success(enrichedPosts))
        }
    }

    private suspend fun getLikes(postKey: String, currentUid: String): Pair<Long, Boolean> {
        val countDeferred = viewModelScope.async(Dispatchers.IO) {
            try {
                supabase.getDatabase().from("posts_likes")
                    .select(count = io.supabase.postgrest.query.Count.EXACT)
                    .eq("post_key", postKey)
                    .execute()
                    .count ?: 0L
            } catch (e: PostgrestHttpException) { 0L }
        }

        val isLikedDeferred = viewModelScope.async(Dispatchers.IO) {
            try {
                supabase.getDatabase().from("posts_likes")
                    .select()
                    .eq("post_key", postKey)
                    .eq("user_id", currentUid)
                    .limit(1)
                    .execute()
                    .getBody()?.isNotEmpty() == true
            } catch (e: PostgrestHttpException) { false }
        }

        return Pair(countDeferred.await(), isLikedDeferred.await())
    }

    private suspend fun getCommentCount(postKey: String): Long {
        return viewModelScope.async(Dispatchers.IO) {
            try {
                supabase.getDatabase().from("posts_comments")
                    .select(count = io.supabase.postgrest.query.Count.EXACT)
                    .eq("post_key", postKey)
                    .execute()
                    .count ?: 0L
            } catch (e: PostgrestHttpException) { 0L }
        }.await()
    }

    private suspend fun getFavoriteStatus(postKey: String, currentUid: String): Boolean {
        return viewModelScope.async(Dispatchers.IO) {
             try {
                 supabase.getDatabase().from("favorite_posts")
                    .select()
                    .eq("user_id", currentUid)
                    .eq("post_key", postKey)
                    .limit(1)
                    .execute()
                    .getBody()?.isNotEmpty() == true
             } catch (e: PostgrestHttpException) { false }
        }.await()
    }


    fun fetchInitialFollowState(userId: String, currentUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = supabase.getDatabase().from("followers")
                    .select()
                    .eq("user_id", userId)
                    .eq("follower_id", currentUid)
                    .limit(1)
                    .execute()
                _isFollowing.postValue(response.getBody()?.isNotEmpty() == true)
            } catch (e: PostgrestHttpException) {
                _isFollowing.postValue(false)
            }
        }
    }

    fun fetchInitialProfileLikeState(userId: String, currentUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = supabase.getDatabase().from("profile_likes")
                    .select()
                    .eq("profile_id", userId)
                    .eq("user_id", currentUid)
                    .limit(1)
                    .execute()
                _isProfileLiked.postValue(response.getBody()?.isNotEmpty() == true)
            } catch (e: PostgrestHttpException) {
                _isProfileLiked.postValue(false)
            }
        }
    }


    fun toggleFollow(userId: String, currentUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isFollowingResponse = supabase.getDatabase().from("followers")
                    .select()
                    .eq("user_id", userId)
                    .eq("follower_id", currentUid)
                    .limit(1)
                    .execute()

                if (isFollowingResponse.getBody()?.isNotEmpty() == true) {
                    supabase.getDatabase().from("followers")
                        .delete()
                        .eq("user_id", userId)
                        .eq("follower_id", currentUid)
                        .execute()
                    supabase.getDatabase().from("following")
                        .delete()
                        .eq("user_id", currentUid)
                        .eq("following_id", userId)
                        .execute()
                    _isFollowing.postValue(false)
                } else {
                    supabase.getDatabase().from("followers")
                        .insert(mapOf("user_id" to userId, "follower_id" to currentUid))
                        .execute()
                    supabase.getDatabase().from("following")
                        .insert(mapOf("user_id" to currentUid, "following_id" to userId))
                        .execute()
                    _isFollowing.postValue(true)
                }
            } catch (e: PostgrestHttpException) {
            }
        }
    }

    fun toggleProfileLike(userId: String, currentUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                 val isLikedResponse = supabase.getDatabase().from("profile_likes")
                    .select()
                    .eq("profile_id", userId)
                    .eq("user_id", currentUid)
                    .limit(1)
                    .execute()

                if (isLikedResponse.getBody()?.isNotEmpty() == true) {
                    supabase.getDatabase().from("profile_likes")
                        .delete()
                        .eq("profile_id", userId)
                        .eq("user_id", currentUid)
                        .execute()
                    _isProfileLiked.postValue(false)
                } else {
                    supabase.getDatabase().from("profile_likes")
                        .insert(mapOf("profile_id" to userId, "user_id" to currentUid))
                        .execute()
                    _isProfileLiked.postValue(true)
                }
            } catch (e: PostgrestHttpException) {
            }
        }
    }

    fun togglePostLike(post: Post) {
        val currentUid = supabase.getAuth().getUser()?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isLikedResponse = supabase.getDatabase().from("posts_likes")
                    .select()
                    .eq("post_key", post.key)
                    .eq("user_id", currentUid)
                    .limit(1)
                    .execute()

                if (isLikedResponse.getBody()?.isNotEmpty() == true) {
                    supabase.getDatabase().from("posts_likes")
                        .delete()
                        .eq("post_key", post.key)
                        .eq("user_id", currentUid)
                        .execute()
                } else {
                    supabase.getDatabase().from("posts_likes")
                        .insert(mapOf("post_key" to post.key, "user_id" to currentUid))
                        .execute()
                }
            } catch (e: PostgrestHttpException) {
            }
        }
    }

    fun toggleFavorite(post: Post) {
        val currentUid = supabase.getAuth().getUser()?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isFavoriteResponse = supabase.getDatabase().from("favorite_posts")
                    .select()
                    .eq("user_id", currentUid)
                    .eq("post_key", post.key)
                    .limit(1)
                    .execute()

                if (isFavoriteResponse.getBody()?.isNotEmpty() == true) {
                    supabase.getDatabase().from("favorite_posts")
                        .delete()
                        .eq("user_id", currentUid)
                        .eq("post_key", post.key)
                        .execute()
                } else {
                    supabase.getDatabase().from("favorite_posts")
                        .insert(mapOf("user_id" to currentUid, "post_key" to post.key))
                        .execute()
                }
            } catch (e: PostgrestHttpException) {
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
