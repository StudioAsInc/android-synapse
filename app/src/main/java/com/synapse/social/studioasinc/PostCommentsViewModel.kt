package com.synapse.social.studioasinc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.model.Comment
import com.synapse.social.studioasinc.model.Reply
import com.synapse.social.studioasinc.model.User
import com.synapse.social.studioasinc.backend.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
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

    private val supabase = SupabaseClient.client
    private var commentsLimit = 20

    fun getComments(postKey: String, increaseLimit: Boolean = false) {
        if (increaseLimit) {
            commentsLimit += 20
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = supabase.postgrest
                    .from("posts-comments")
                    .select()
                //     .eq("postKey", postKey)
                //     .order("like", io.supabase.postgrest.query.Order.DESC)
                //     .limit(commentsLimit)
                //     .execute()
                //     .getBody()

                // if (response != null) {
                //     val commentsList = response.map { Comment.fromMap(it as Map<String, Any>) }
                //     val uids = commentsList.map { it.uid }
                //     _comments.postValue(commentsList)
                //     fetchUsersData(uids)
                // } else {
                //     _comments.postValue(emptyList())
                // }
            } catch (e: RestException) {
                _error.postValue(e.message)
            }
        }
    }

    fun getReplies(postKey: String, commentKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // val response = supabase.getDatabase()
                //     .from("posts-comments-replies")
                //     .select()
                //     .eq("postKey", postKey)
                //     .eq("commentKey", commentKey)
                //     .order("like", io.supabase.postgrest.query.Order.DESC)
                //     .execute()
                //     .getBody()

                // if (response != null) {
                //     val repliesList = response.map { Reply.fromMap(it as Map<String, Any>) }
                //     val uids = repliesList.map { it.uid }
                //     _replies.postValue(repliesList)
                //     fetchUsersData(uids)
                // } else {
                //     _replies.postValue(emptyList())
                // }
            } catch (e: RestException) {
                _error.postValue(e.message)
            }
        }
    }

    private fun fetchUsersData(uids: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // val response = supabase.getDatabase()
                //     .from("users")
                //     .select()
                //     .`in`("uid", uids.distinct())
                //     .execute()
                //     .getBody()

                // if (response != null) {
                //     val userMap = response.map { User.fromMap(it as Map<String, Any>) }
                //         .associateBy { it.uid }
                //     _userData.postValue(userMap)
                // }
            } catch (e: RestException) {
                _error.postValue(e.message)
            }
        }
    }

    fun getCommentCount(postKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // val response = supabase.getDatabase()
                //     .from("posts-comments")
                //     .select(count = io.supabase.postgrest.query.Count.EXACT)
                //     .eq("postKey", postKey)
                //     .execute()

                // _commentCount.postValue(response.count ?: 0L)
            } catch (e: RestException) {
                _error.postValue(e.message)
            }
        }
    }

    fun getUserData(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // val response = supabase.getDatabase()
                //     .from("users")
                //     .select("avatar")
                //     .eq("uid", uid)
                //     .limit(1)
                //     .execute()
                //     .getBody()

                // if (response != null && response.isNotEmpty()) {
                //     val user = User.fromMap(response[0] as Map<String, Any>)
                //     _userAvatar.postValue(user.avatar)
                // } else {
                //     _userAvatar.postValue(null)
                // }
            } catch (e: RestException) {
                _error.postValue(e.message)
            }
        }
    }

    fun postComment(postKey: String, commentText: String, isReply: Boolean, replyToCommentKey: String? = null) {
        val currentUser = supabase.auth.currentUserOrNull() ?: return
        val pushKey = java.util.UUID.randomUUID().toString()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (isReply && replyToCommentKey != null) {
                    val reply = Reply(currentUser.id, commentText, System.currentTimeMillis().toString(), pushKey, 0L, replyToCommentKey)
                    // supabase.getDatabase().from("posts-comments-replies").insert(reply).execute()
                } else {
                    val comment = Comment(currentUser.id, commentText, System.currentTimeMillis().toString(), pushKey, 0L)
                    // supabase.getDatabase().from("posts-comments").insert(comment).execute()
                }
            } catch (e: RestException) {
                _error.postValue(e.message)
            }
        }
    }

    fun likeComment(postKey: String, commentKey: String) {
        val currentUser = supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // val response = supabase.getDatabase().from("posts-comments-like")
                //     .select()
                //     .eq("postKey", postKey)
                //     .eq("commentKey", commentKey)
                //     .eq("uid", currentUser.id)
                //     .execute()
                //     .getBody()

                // if (response != null && response.isNotEmpty()) {
                //     supabase.getDatabase().from("posts-comments-like")
                //         .delete()
                //         .eq("postKey", postKey)
                //         .eq("commentKey", commentKey)
                //         .eq("uid", currentUser.id)
                //         .execute()
                // } else {
                //     supabase.getDatabase().from("posts-comments-like")
                //         .insert(mapOf("postKey" to postKey, "commentKey" to commentKey, "uid" to currentUser.id))
                //         .execute()
                // }
            } catch (e: RestException) {
                _error.postValue(e.message)
            }
        }
    }

    fun deleteComment(postKey: String, commentKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // supabase.getDatabase().from("posts-comments")
                //     .delete()
                //     .eq("postKey", postKey)
                //     .eq("key", commentKey)
                //     .execute()
                // supabase.getDatabase().from("posts-comments-like")
                //     .delete()
                //     .eq("postKey", postKey)
                //     .eq("commentKey", commentKey)
                //     .execute()
            } catch (e: RestException) {
                _error.postValue(e.message)
            }
        }
    }

    fun editComment(postKey: String, commentKey: String, newComment: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // supabase.getDatabase().from("posts-comments")
                //     .update(mapOf("comment" to newComment))
                //     .eq("postKey", postKey)
                //     .eq("key", commentKey)
                //     .execute()
            } catch (e: RestException) {
                _error.postValue(e.message)
            }
        }
    }

    fun likeReply(postKey: String, commentKey: String, replyKey: String) {
        val currentUser = supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // val response = supabase.getDatabase().from("posts-comments-replies-like")
                //     .select()
                //     .eq("postKey", postKey)
                //     .eq("replyKey", replyKey)
                //     .eq("uid", currentUser.id)
                //     .execute()
                //     .getBody()

                // if (response != null && response.isNotEmpty()) {
                //     supabase.getDatabase().from("posts-comments-replies-like")
                //         .delete()
                //         .eq("postKey", postKey)
                //         .eq("replyKey", replyKey)
                //         .eq("uid", currentUser.id)
                //         .execute()
                // } else {
                //     supabase.getDatabase().from("posts-comments-replies-like")
                //         .insert(mapOf("postKey" to postKey, "replyKey" to replyKey, "uid" to currentUser.id))
                //         .execute()
                // }
            } catch (e: RestException) {
                _error.postValue(e.message)
            }
        }
    }

    fun deleteReply(postKey: String, commentKey: String, replyKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // supabase.getDatabase().from("posts-comments-replies")
                //     .delete()
                //     .eq("postKey", postKey)
                //     .eq("commentKey", commentKey)
                //     .eq("key", replyKey)
                //     .execute()
                // supabase.getDatabase().from("posts-comments-replies-like")
                //     .delete()
                //     .eq("postKey", postKey)
                //     .eq("replyKey", replyKey)
                //     .execute()
            } catch (e: RestException) {
                _error.postValue(e.message)
            }
        }
    }

    fun editReply(postKey: String, commentKey: String, replyKey: String, newReply: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // supabase.getDatabase().from("posts-comments-replies")
                //     .update(mapOf("comment" to newReply))
                //     .eq("postKey", postKey)
                //     .eq("commentKey", commentKey)
                //     .eq("key", replyKey)
                //     .execute()
            } catch (e: RestException) {
                _error.postValue(e.message)
            }
        }
    }
}
