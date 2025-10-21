package com.synapse.social.studioasinc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.model.Comment
import com.synapse.social.studioasinc.model.Reply
import com.synapse.social.studioasinc.model.User
import kotlinx.coroutines.launch

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

    private val dbService = SupabaseDatabaseService()
    private val authService = SupabaseAuthenticationService()
    private var commentsLimit = 20

    fun getComments(postKey: String, increaseLimit: Boolean = false) {
        if (increaseLimit) {
            commentsLimit += 20
        }

        viewModelScope.launch {
            try {
                // Fetch comments from Supabase
                val comments = dbService.selectWithFilter<Map<String, Any?>>(
                    table = "comments",
                    columns = "*"
                ) { /* Add filter for post_key and limit */ }
                
                val commentsList = mutableListOf<Comment>()
                val uids = mutableListOf<String>()
                
                comments.forEach { commentData ->
                    // Convert map to Comment object
                    val comment = Comment(
                        uid = commentData["uid"] as? String ?: "",
                        comment = commentData["text"] as? String ?: "",
                        push_time = commentData["timestamp"] as? String ?: "",
                        key = commentData["id"] as? String ?: "",
                        like = (commentData["like"] as? Number)?.toLong() ?: 0L
                    )
                    commentsList.add(comment)
                    comment.uid.let { uids.add(it) }
                }
                
                _comments.postValue(commentsList.sortedByDescending { it.like })
                fetchUsersData(uids)
            } catch (e: Exception) {
                _error.postValue("Failed to load comments: ${e.message}")
                _comments.postValue(emptyList())
            }
        }

    }

    fun getReplies(postKey: String, commentKey: String) {
        viewModelScope.launch {
            try {
                // Fetch replies from Supabase
                val replies = dbService.selectWithFilter<Map<String, Any?>>(
                    table = "replies",
                    columns = "*"
                ) { /* Add filter for post_key and comment_key */ }
                
                val repliesList = mutableListOf<Reply>()
                val uids = mutableListOf<String>()
                
                replies.forEach { replyData ->
                    // Convert map to Reply object
                    val reply = Reply(
                        uid = replyData["uid"] as? String ?: "",
                        comment = replyData["text"] as? String ?: "",
                        push_time = replyData["timestamp"] as? String ?: "",
                        key = replyData["id"] as? String ?: "",
                        like = (replyData["like"] as? Number)?.toLong() ?: 0L,
                        replyCommentkey = replyData["comment_key"] as? String ?: ""
                    )
                    repliesList.add(reply)
                    reply.uid.let { uids.add(it) }
                }
                
                _replies.postValue(repliesList.sortedByDescending { it.like })
                fetchUsersData(uids)
            } catch (e: Exception) {
                _error.postValue("Failed to load replies: ${e.message}")
                _replies.postValue(emptyList())
            }
        }

    }

    private fun fetchUsersData(uids: List<String>) {
        viewModelScope.launch {
            try {
                val userMap = mutableMapOf<String, User>()
                uids.distinct().forEach { uid ->
                    val users = dbService.selectWithFilter<Map<String, Any?>>(
                        table = "users",
                        columns = "*"
                    ) { /* Add filter for uid */ }
                    
                    users.firstOrNull()?.let { userData ->
                        val user = User(
                            id = userData["id"] as? String,
                            uid = userData["uid"] as? String ?: uid,
                            email = userData["email"] as? String,
                            username = userData["username"] as? String,
                            nickname = userData["nickname"] as? String,
                            avatar = userData["avatar"] as? String
                        )
                        userMap[uid] = user
                    }
                }
                _userData.postValue(userMap)
            } catch (e: Exception) {
                _error.postValue("Failed to fetch user data: ${e.message}")
            }
        }
    }

    fun getCommentCount(postKey: String) {
        viewModelScope.launch {
            try {
                val comments = dbService.selectWithFilter<Map<String, Any?>>(
                    table = "comments",
                    columns = "id"
                ) { /* Add filter for post_key */ }
                _commentCount.postValue(comments.size.toLong())
            } catch (e: Exception) {
                _error.postValue("Failed to get comment count: ${e.message}")
            }
        }
    }

    fun getUserData(uid: String) {
        viewModelScope.launch {
            try {
                val users = dbService.selectWithFilter<Map<String, Any?>>(
                    table = "users",
                    columns = "avatar"
                ) { /* Add filter for uid */ }
                
                val avatar = users.firstOrNull()?.get("avatar") as? String
                _userAvatar.postValue(avatar)
            } catch (e: Exception) {
                _error.postValue("Failed to get user data: ${e.message}")
                _userAvatar.postValue(null)
            }
        }
    }

    fun postComment(postKey: String, commentText: String, isReply: Boolean, replyToCommentKey: String? = null) {
        viewModelScope.launch {
            try {
                val currentUid = authService.getCurrentUserId() ?: return@launch
                
                if (isReply && replyToCommentKey != null) {
                    val replyData = mapOf(
                        "uid" to currentUid,
                        "text" to commentText,
                        "timestamp" to System.currentTimeMillis().toString(),
                        "comment_key" to replyToCommentKey,
                        "post_key" to postKey,
                        "like" to 0L
                    )
                    dbService.insert("replies", replyData)
                } else {
                    val commentData = mapOf(
                        "uid" to currentUid,
                        "text" to commentText,
                        "timestamp" to System.currentTimeMillis().toString(),
                        "post_key" to postKey,
                        "like" to 0L
                    )
                    dbService.insert("comments", commentData)
                }
            } catch (e: Exception) {
                _error.postValue("Failed to post comment: ${e.message}")
            }
        }
    }

    fun likeComment(postKey: String, commentKey: String) {
        viewModelScope.launch {
            try {
                val currentUid = authService.getCurrentUserId() ?: return@launch
                
                // Check if already liked
                val likes = dbService.select<Map<String, Any?>>(
                    table = "comment_likes",
                    columns = "*"
                ) { 
                    eq("comment_key", commentKey)
                    eq("user_id", currentUserId)
                }
                
                if (likes.isNotEmpty()) {
                    // Unlike - remove the like
                    dbService.delete("comment_likes")
                } else {
                    // Like - add the like
                    val likeData = mapOf(
                        "comment_key" to commentKey,
                        "user_id" to currentUid,
                        "post_key" to postKey
                    )
                    dbService.insert("comment_likes", likeData)
                }
            } catch (e: Exception) {
                _error.postValue("Failed to like comment: ${e.message}")
            }
        }
    }

    fun deleteComment(postKey: String, commentKey: String) {
        viewModelScope.launch {
            try {
                // Soft delete by setting deleted_at timestamp
                val updateData = mapOf("deleted_at" to System.currentTimeMillis().toString())
                dbService.update("comments", updateData)
            } catch (e: Exception) {
                _error.postValue("Failed to delete comment: ${e.message}")
            }
        }
    }

    fun editComment(postKey: String, commentKey: String, newComment: String) {
        viewModelScope.launch {
            try {
                val updateData = mapOf(
                    "text" to newComment,
                    "edited_at" to System.currentTimeMillis().toString()
                )
                dbService.update("comments", updateData)
            } catch (e: Exception) {
                _error.postValue("Failed to edit comment: ${e.message}")
            }
        }
    }

    fun likeReply(postKey: String, commentKey: String, replyKey: String) {
        viewModelScope.launch {
            try {
                val currentUid = authService.getCurrentUserId() ?: return@launch
                
                // Check if already liked
                val likes = dbService.select<Map<String, Any?>>(
                    table = "reply_likes",
                    columns = "*"
                ) { 
                    eq("reply_key", replyKey)
                    eq("user_id", currentUid)
                }
                
                if (likes.isNotEmpty()) {
                    // Unlike
                    dbService.delete("reply_likes")
                } else {
                    // Like
                    val likeData = mapOf(
                        "reply_key" to replyKey,
                        "user_id" to currentUid,
                        "comment_key" to commentKey,
                        "post_key" to postKey
                    )
                    dbService.insert("reply_likes", likeData)
                }
            } catch (e: Exception) {
                _error.postValue("Failed to like reply: ${e.message}")
            }
        }
    }

    fun deleteReply(postKey: String, commentKey: String, replyKey: String) {
        viewModelScope.launch {
            try {
                // Soft delete
                val updateData = mapOf("deleted_at" to System.currentTimeMillis().toString())
                dbService.update("replies", updateData)
            } catch (e: Exception) {
                _error.postValue("Failed to delete reply: ${e.message}")
            }
        }
    }

    fun editReply(postKey: String, commentKey: String, replyKey: String, newReply: String) {
        viewModelScope.launch {
            try {
                val updateData = mapOf(
                    "text" to newReply,
                    "edited_at" to System.currentTimeMillis().toString()
                )
                dbService.update("replies", updateData)
            } catch (e: Exception) {
                _error.postValue("Failed to edit reply: ${e.message}")
            }
        }
    }
}
