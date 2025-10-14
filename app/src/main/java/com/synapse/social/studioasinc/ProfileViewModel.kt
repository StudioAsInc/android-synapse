package com.synapse.social.studioasinc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.backend.SupabaseAuthService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService
import com.synapse.social.studioasinc.backend.interfaces.IDataListener
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError
import com.synapse.social.studioasinc.model.User
import com.synapse.social.studioasinc.repository.UserRepository
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

    private val authService: IAuthenticationService = SupabaseAuthService()
    private val dbService: IDatabaseService = SupabaseDatabaseService()

    fun getUserPosts(userId: String) {
        _userPosts.postValue(State.Loading)
        dbService.getData(dbService.getReference("posts").orderByChild("uid").equalTo(userId), object : IDataListener {
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                if (dataSnapshot.exists()) {
                    val posts = dataSnapshot.children.mapNotNull { it.getValue(Post::class.java) }
                    enrichPostsWithState(posts)
                } else {
                    _userPosts.postValue(State.Success(emptyList()))
                }
            }

            override fun onCancelled(databaseError: IDatabaseError) {
                _userPosts.postValue(State.Error)
            }
        })
    }

    private fun enrichPostsWithState(posts: List<Post>) {
        val currentUid = authService.getCurrentUser()?.getUid() ?: ""
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
        val deferred = CompletableDeferred<Pair<Long, Boolean>>()
        dbService.getData(dbService.getReference("posts-likes").orderByChild("post_id").equalTo(postKey), object : IDataListener {
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                val isLiked = dataSnapshot.children.any { it.jsonObject["user_id"]?.jsonPrimitive?.content == currentUid }
                deferred.complete(Pair(dataSnapshot.children.count().toLong(), isLiked))
            }
            override fun onCancelled(databaseError: IDatabaseError) {
                deferred.complete(Pair(0L, false))
            }
        })
        return deferred.await()
    }

    private suspend fun getCommentCount(postKey: String): Long {
        val deferred = CompletableDeferred<Long>()
        dbService.getData(dbService.getReference("posts-comments").orderByChild("post_id").equalTo(postKey), object : IDataListener {
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                deferred.complete(dataSnapshot.children.count().toLong())
            }
            override fun onCancelled(databaseError: IDatabaseError) {
                deferred.complete(0L)
            }
        })
        return deferred.await()
    }

    private suspend fun getFavoriteStatus(postKey: String, currentUid: String): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        dbService.getData(dbService.getReference("favorite-posts").orderByChild("post_id").equalTo(postKey).orderByChild("user_id").equalTo(currentUid), object : IDataListener {
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                deferred.complete(dataSnapshot.exists())
            }
            override fun onCancelled(databaseError: IDatabaseError) {
                deferred.complete(false)
            }
        })
        return deferred.await()
    }


    fun fetchInitialFollowState(userId: String, currentUid: String) {
        dbService.getData(dbService.getReference("followers").orderByChild("follower_uid").equalTo(currentUid).orderByChild("following_uid").equalTo(userId), object : IDataListener {
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                _isFollowing.postValue(dataSnapshot.exists())
            }

            override fun onCancelled(databaseError: IDatabaseError) {}
        })
    }

    fun fetchInitialProfileLikeState(userId: String, currentUid: String) {
        dbService.getData(dbService.getReference("profile-likes").orderByChild("user_id").equalTo(userId).orderByChild("liker_id").equalTo(currentUid), object : IDataListener {
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                _isProfileLiked.postValue(dataSnapshot.exists())
            }

            override fun onCancelled(databaseError: IDatabaseError) {}
        })
    }


    fun toggleFollow(userId: String, currentUid: String) {
        val followersRef = dbService.getReference("followers").orderByChild("follower_uid").equalTo(currentUid).orderByChild("following_uid").equalTo(userId)
        dbService.getData(followersRef, object : IDataListener {
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                if (dataSnapshot.exists()) {
                    dbService.delete(followersRef, (success, error) -> {})
                    _isFollowing.postValue(false)
                } else {
                    val followMap = mapOf("follower_uid" to currentUid, "following_uid" to userId)
                    dbService.setValue(dbService.getReference("followers"), followMap, (success, error) -> {})
                    _isFollowing.postValue(true)
                }
            }
            override fun onCancelled(databaseError: IDatabaseError) {}
        })
    }

    fun toggleProfileLike(userId: String, currentUid: String) {
        val profileLikesRef = dbService.getReference("profile-likes").orderByChild("user_id").equalTo(userId).orderByChild("liker_id").equalTo(currentUid)
        dbService.getData(profileLikesRef, object : IDataListener {
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                if (dataSnapshot.exists()) {
                    dbService.delete(profileLikesRef, (success, error) -> {})
                    _isProfileLiked.postValue(false)
                } else {
                    val likeMap = mapOf("user_id" to userId, "liker_id" to currentUid)
                    dbService.setValue(dbService.getReference("profile-likes"), likeMap, (success, error) -> {})
                    _isProfileLiked.postValue(true)
                }
            }
            override fun onCancelled(databaseError: IDatabaseError) {}
        })
    }

    fun togglePostLike(post: Post) {
        val currentUid = authService.getCurrentUser()?.getUid() ?: return
        val postLikesRef = dbService.getReference("posts-likes").orderByChild("post_id").equalTo(post.key).orderByChild("user_id").equalTo(currentUid)
        dbService.getData(postLikesRef, object : IDataListener {
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                if (dataSnapshot.exists()) {
                    dbService.delete(postLikesRef, (success, error) -> {})
                } else {
                    val likeMap = mapOf("post_id" to post.key, "user_id" to currentUid)
                    dbService.setValue(dbService.getReference("posts-likes"), likeMap, (success, error) -> {})
                }
            }
            override fun onCancelled(databaseError: IDatabaseError) {}
        })
    }

    fun toggleFavorite(post: Post) {
        val currentUid = authService.getCurrentUser()?.getUid() ?: return
        val favoriteRef = dbService.getReference("favorite-posts").orderByChild("post_id").equalTo(post.key).orderByChild("user_id").equalTo(currentUid)
        dbService.getData(favoriteRef, object : IDataListener {
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                if (dataSnapshot.exists()) {
                    dbService.delete(favoriteRef, (success, error) -> {})
                } else {
                    val favoriteMap = mapOf("post_id" to post.key, "user_id" to currentUid)
                    dbService.setValue(dbService.getReference("favorite-posts"), favoriteMap, (success, error) -> {})
                }
            }
            override fun onCancelled(databaseError: IDatabaseError) {}
        })
    }

    override fun onCleared() {
        super.onCleared()
        // No-op for now
    }
}
