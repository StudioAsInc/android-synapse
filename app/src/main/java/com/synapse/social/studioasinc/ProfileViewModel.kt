package com.synapse.social.studioasinc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.synapse.social.studioasinc.model.Post

/**
 * ViewModel for the [ProfileActivity].
 *
 * This ViewModel is responsible for fetching and managing the data
 * related to a user's profile, such as their posts, follow status,
 * and profile likes.
 */
class ProfileViewModel : ViewModel() {

    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts

    private val _isFollowing = MutableLiveData<Boolean>()
    val isFollowing: LiveData<Boolean> = _isFollowing

    private val _isProfileLiked = MutableLiveData<Boolean>()
    val isProfileLiked: LiveData<Boolean> = _isProfileLiked

    private var postsListener: ValueEventListener? = null
    private var postsRef: DatabaseReference? = null

    fun getUserPosts(userId: String) {
        postsRef = FirebaseDatabase.getInstance().getReference("skyline/posts")
            .orderByChild("uid").equalTo(userId)

        postsListener = postsRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val posts = snapshot.children.mapNotNull { it.getValue(Post::class.java) }
                    _userPosts.postValue(posts)
                } else {
                    _userPosts.postValue(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _userPosts.postValue(emptyList())
            }
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
