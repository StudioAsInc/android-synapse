package com.synapse.social.studioasinc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.compatibility.FirebaseAuth
import com.synapse.social.studioasinc.compatibility.FirebaseDatabase
import com.synapse.social.studioasinc.compatibility.ValueEventListener
import com.synapse.social.studioasinc.compatibility.DataSnapshot
import com.synapse.social.studioasinc.compatibility.DatabaseError
import com.synapse.social.studioasinc.compatibility.Query
import com.synapse.social.studioasinc.model.Post
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

    private var postsListener: ValueEventListener? = null
    private var postsRef: Query? = null

    fun getUserPosts(userId: String) {
        _userPosts.postValue(State.Loading)
        postsRef = FirebaseDatabase.getInstance().getReference("skyline/posts")
            .orderByChild("uid").equalTo(userId)

        postsListener = postsRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val posts = snapshot.children.mapNotNull { it.getValue(Post::class.java) }
                    enrichPostsWithState(posts)
                } else {
                    _userPosts.postValue(State.Success(emptyList()))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _userPosts.postValue(State.Error)
            }
        })
    }

    private fun enrichPostsWithState(posts: List<Post>) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
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
        val ref = FirebaseDatabase.getInstance().getReference("skyline/posts-likes").child(postKey)
        val snapshot = ref.get().await()
        return Pair(snapshot.childrenCount, snapshot.hasChild(currentUid))
    }

    private suspend fun getCommentCount(postKey: String): Long {
        val ref = FirebaseDatabase.getInstance().getReference("skyline/posts-comments").child(postKey)
        val snapshot = ref.get().await()
        return snapshot.childrenCount
    }

    private suspend fun getFavoriteStatus(postKey: String, currentUid: String): Boolean {
        val ref = FirebaseDatabase.getInstance().getReference("skyline/favorite-posts").child(currentUid).child(postKey)
        val snapshot = ref.get().await()
        return snapshot.exists()
    }


    fun fetchInitialFollowState(userId: String, currentUid: String) {
        val followersRef = FirebaseDatabase.getInstance().getReference("skyline/followers").child(userId).child(currentUid)
        followersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _isFollowing.postValue(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun fetchInitialProfileLikeState(userId: String, currentUid: String) {
        val profileLikesRef = FirebaseDatabase.getInstance().getReference("skyline/profile-likes").child(userId).child(currentUid)
        profileLikesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _isProfileLiked.postValue(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    fun toggleFollow(userId: String, currentUid: String) {
        val followersRef = FirebaseDatabase.getInstance().getReference("skyline/followers").child(userId).child(currentUid)
        val followingRef = FirebaseDatabase.getInstance().getReference("skyline/following").child(currentUid).child(userId)

        followersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    followersRef.removeValue()
                    followingRef.removeValue()
                    _isFollowing.postValue(false)
                } else {
                    followersRef.setValue(true)
                    followingRef.setValue(true)
                    _isFollowing.postValue(true)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun toggleProfileLike(userId: String, currentUid: String) {
        val profileLikesRef = FirebaseDatabase.getInstance().getReference("skyline/profile-likes").child(userId).child(currentUid)

        profileLikesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    profileLikesRef.removeValue()
                    _isProfileLiked.postValue(false)
                } else {
                    profileLikesRef.setValue(true)
                    _isProfileLiked.postValue(true)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun togglePostLike(post: Post) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val postLikesRef = FirebaseDatabase.getInstance().getReference("skyline/posts-likes").child(post.key).child(currentUid)

        postLikesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    postLikesRef.removeValue()
                } else {
                    postLikesRef.setValue(true)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun toggleFavorite(post: Post) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val favoriteRef = FirebaseDatabase.getInstance().getReference("skyline/favorite-posts").child(currentUid).child(post.key)

        favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    favoriteRef.removeValue()
                } else {
                    favoriteRef.setValue(true)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onCleared() {
        super.onCleared()
        postsListener?.let { postsRef?.removeEventListener(it) }
    }
}
