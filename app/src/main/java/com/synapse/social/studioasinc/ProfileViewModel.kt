package com.synapse.social.studioasinc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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

    private val _profileLikeCount = MutableLiveData<Long>()
    val profileLikeCount: LiveData<Long> = _profileLikeCount

    private val _isProfileLiked = MutableLiveData<Boolean>()
    val isProfileLiked: LiveData<Boolean> = _isProfileLiked

    /**
     * Fetches the posts of a specific user from Firebase.
     *
     * @param userId The ID of the user whose posts are to be fetched.
     * @return A [LiveData] object containing the list of posts.
     */
    fun getUserPosts(userId: String): LiveData<List<Post>> {
        val postsRef = FirebaseDatabase.getInstance().getReference("skyline/posts")
            .orderByChild("uid").equalTo(userId)

        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val posts = snapshot.children.mapNotNull { it.getValue(Post::class.java) }
                    _userPosts.postValue(posts)
                } else {
                    _userPosts.postValue(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
        return userPosts
    }

    /**
     * Toggles the follow status of a user.
     *
     * @param userId The ID of the user to follow/unfollow.
     * @param currentUid The ID of the current user.
     */
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

    /**
     * Toggles the like status of a user's profile.
     *
     * @param userId The ID of the user whose profile is to be liked/unliked.
     * @param currentUid The ID of the current user.
     */
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
}
