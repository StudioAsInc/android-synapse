package com.synapse.social.studioasinc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.synapse.social.studioasinc.model.Comment
import com.synapse.social.studioasinc.model.Reply
import com.synapse.social.studioasinc.model.User

class PostCommentsViewModel : ViewModel() {

    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments

    private val _replies = MutableLiveData<List<Reply>>()
    val replies: LiveData<List<Reply>> = _replies

    private val _commentCount = MutableLiveData<Long>()
    val commentCount: LiveData<Long> = _commentCount

    private val _userAvatar = MutableLiveData<String?>()
    val userAvatar: LiveData<String?> = _userAvatar

    private val _userData = MutableLiveData<Map<String, User>>()
    val userData: LiveData<Map<String, User>> = _userData

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val database = FirebaseDatabase.getInstance().getReference("skyline")
    private var commentsLimit = 20

    fun getComments(postKey: String, increaseLimit: Boolean = false) {
        if (increaseLimit) {
            commentsLimit += 20
        }

        val commentsQuery: Query = database.child("posts-comments").child(postKey).orderByChild("like").limitToLast(commentsLimit)
        commentsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val commentsList = mutableListOf<Comment>()
                    val uids = mutableListOf<String>()
                    for (data in snapshot.children) {
                        data.getValue(Comment::class.java)?.let {
                            commentsList.add(it)
                            uids.add(it.uid)
                        }
                    }
                    _comments.postValue(commentsList.sortedByDescending { it.like })
                    fetchUsersData(uids)
                } else {
                    _comments.postValue(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _error.postValue(error.message)
            }
        })
    }

    fun getReplies(postKey: String, commentKey: String) {
        val repliesQuery: Query = database.child("posts-comments-replies").child(postKey).child(commentKey).orderByChild("like")
        repliesQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val repliesList = mutableListOf<Reply>()
                    val uids = mutableListOf<String>()
                    for (data in snapshot.children) {
                        data.getValue(Reply::class.java)?.let {
                            repliesList.add(it)
                            uids.add(it.uid)
                        }
                    }
                    _replies.postValue(repliesList.sortedByDescending { it.like })
                    fetchUsersData(uids)
                } else {
                    _replies.postValue(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _error.postValue(error.message)
            }
        })
    }

    private fun fetchUsersData(uids: List<String>) {
        val userMap = mutableMapOf<String, User>()
        val usersRef = database.child("users")
        uids.distinct().forEach { uid ->
            usersRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.getValue(User::class.java)?.let {
                        userMap[uid] = it
                    }
                    if (userMap.size == uids.distinct().size) {
                        _userData.postValue(userMap)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _error.postValue(error.message)
                }
            })
        }
    }

    fun getCommentCount(postKey: String) {
        val getCommentsCount: DatabaseReference = database.child("posts-comments").child(postKey)
        getCommentsCount.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                _commentCount.postValue(dataSnapshot.childrenCount)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                _error.postValue(databaseError.message)
            }
        })
    }

    fun getUserData(uid: String) {
        val getUserDetails: DatabaseReference = database.child("users").child(uid)
        getUserDetails.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    _userAvatar.postValue(dataSnapshot.child("avatar").getValue(String::class.java))
                } else {
                    _userAvatar.postValue(null)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                _error.postValue(databaseError.message)
            }
        })
    }

    fun postComment(postKey: String, commentText: String, isReply: Boolean, replyToCommentKey: String? = null) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val pushKey = database.push().key ?: ""
        if (isReply && replyToCommentKey != null) {
            val reply = Reply(currentUser.uid, commentText, System.currentTimeMillis().toString(), pushKey, 0L, replyToCommentKey)
            database.child("posts-comments-replies").child(postKey).child(replyToCommentKey).child(pushKey).setValue(reply)
        } else {
            val comment = Comment(currentUser.uid, commentText, System.currentTimeMillis().toString(), pushKey, 0L)
            database.child("posts-comments").child(postKey).child(pushKey).setValue(comment)
        }
    }

    fun likeComment(postKey: String, commentKey: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val likeRef = database.child("posts-comments-like").child(postKey).child(commentKey).child(currentUser.uid)
        likeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    likeRef.removeValue()
                } else {
                    likeRef.setValue(true)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _error.postValue(error.message)
            }
        })
    }

    fun deleteComment(postKey: String, commentKey: String) {
        database.child("posts-comments").child(postKey).child(commentKey).removeValue()
        database.child("posts-comments-like").child(postKey).child(commentKey).removeValue()
    }

    fun editComment(postKey: String, commentKey: String, newComment: String) {
        database.child("posts-comments").child(postKey).child(commentKey).child("comment").setValue(newComment)
    }

    fun likeReply(postKey: String, commentKey: String, replyKey: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val likeRef = database.child("posts-comments-replies-like").child(postKey).child(replyKey).child(currentUser.uid)
        likeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    likeRef.removeValue()
                } else {
                    likeRef.setValue(true)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _error.postValue(error.message)
            }
        })
    }

    fun deleteReply(postKey: String, commentKey: String, replyKey: String) {
        database.child("posts-comments-replies").child(postKey).child(commentKey).child(replyKey).removeValue()
        database.child("posts-comments-replies-like").child(postKey).child(replyKey).removeValue()
    }

    fun editReply(postKey: String, commentKey: String, replyKey: String, newReply: String) {
        database.child("posts-comments-replies").child(postKey).child(commentKey).child(replyKey).child("comment").setValue(newReply)
    }
}
