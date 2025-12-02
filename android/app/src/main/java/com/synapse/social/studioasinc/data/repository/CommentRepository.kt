package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.SynapseApplication
import com.synapse.social.studioasinc.data.Result
import com.synapse.social.studioasinc.model.*
import com.synapse.social.studioasinc.util.ErrorHandler
import com.synapse.social.studioasinc.util.RetryPolicy
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/**
 * Repository for managing comments and replies.
 * Handles comment fetching, creation, editing, and deletion with user joins.
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 5.1, 5.4
 */
class CommentRepository {
    
    private val client = SupabaseClient.client
    private val retryPolicy = RetryPolicy()
    
    companion object {
        private const val TAG = "CommentRepository"
    }
    
    /**
     * Fetch comments for a post with user information.
     * Returns top-level comments (no parent_comment_id) sorted by creation date.
     * 
     * @param postId The ID of the post
     * @param limit Maximum number of comments to fetch
     * @param offset Number of comments to skip for pagination
     * @return Result containing list of CommentWithUser or error
     * 
     * Requirements: 4.1, 4.2
     */
    suspend fun getComments(
        postId: String,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<CommentWithUser>> = withContext(Dispatchers.IO) {
        try {
            val response = retryPolicy.executeWithRetry {
                client.from("comments")
                    .select(
                        columns = Columns.raw("""
                            *,
                            users!comments_user_id_fkey(uid, username, display_name, email, bio, avatar, followers_count, following_count, posts_count, status, account_type, verify, banned)
                        """.trimIndent())
                    ) {
                        filter {
                            eq("post_id", postId)
                            exact("parent_comment_id", null)
                        }
                        order("created_at", Order.ASCENDING)
                        limit(limit.toLong())
                        range(offset.toLong(), (offset + limit - 1).toLong())
                    }
                    .decodeList<JsonObject>()
            }
            
            val comments = mutableListOf<CommentWithUser>()
            for (json in response) {
                parseCommentFromJson(json)?.let { comments.add(it) }
            }
            
            Result.Success(comments)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    /**
     * Fetch replies for a specific comment.
     * Returns comments where parent_comment_id matches the given commentId.
     * 
     * @param commentId The ID of the parent comment
     * @return Result containing list of CommentWithUser replies or error
     * 
     * Requirements: 5.1
     */
    suspend fun getReplies(commentId: String): Result<List<CommentWithUser>> = withContext(Dispatchers.IO) {
        try {
            val response = retryPolicy.executeWithRetry {
                client.from("comments")
                    .select(
                        columns = Columns.raw("""
                            *,
                            users!comments_user_id_fkey(uid, username, display_name, email, bio, avatar, followers_count, following_count, posts_count, status, account_type, verify, banned)
                        """.trimIndent())
                    ) {
                        filter { eq("parent_comment_id", commentId) }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<JsonObject>()
            }
            
            val replies = mutableListOf<CommentWithUser>()
            for (json in response) {
                parseCommentFromJson(json)?.let { replies.add(it) }
            }
            
            Result.Success(replies)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    /**
     * Observe comments for a post in real-time.
     * 
     * @param postId The ID of the post to observe
     * @return Flow emitting CommentEvent updates
     * 
     * Requirements: 11.1, 11.3
     */
    fun observeComments(postId: String): Flow<Result<List<CommentWithUser>>> = flow {
        emit(Result.Loading)
        try {
            client.channel("comments:$postId").postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "comments"
                filter = "post_id=eq.$postId"
            }.map {
                when (val result = getComments(postId)) {
                    is Result.Success -> Result.Success(result.data)
                    is Result.Error -> Result.Error(result.exception, result.message)
                    else -> Result.Loading
                }
            }.catch { e ->
                emit(Result.Error(e as Exception, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext())))
            }.collect { emit(it) }
        } catch (e: Exception) {
            emit(Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext())))
        }
    }

    /**
     * Create a new comment on a post.
     * 
     * @param postId The ID of the post to comment on
     * @param content The comment text content
     * @param mediaUrl Optional media attachment URL
     * @param parentCommentId Optional parent comment ID for replies
     * @return Result containing the created CommentWithUser or error
     * 
     * Requirements: 4.3, 5.4
     */
    suspend fun createComment(
        postId: String,
        content: String,
        mediaUrl: String? = null,
        parentCommentId: String? = null
    ): Result<CommentWithUser> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.Error(Exception("User not authenticated"), "User not authenticated")
            
            val userId = currentUser.id
            
            val response = retryPolicy.executeWithRetry {
                client.from("comments")
                    .insert(buildJsonObject {
                        put("post_id", postId)
                        put("user_id", userId)
                        put("content", content)
                        if (mediaUrl != null) put("media_url", mediaUrl)
                        if (parentCommentId != null) put("parent_comment_id", parentCommentId)
                    }) {
                        select(
                            columns = Columns.raw("""
                                *,
                                users!comments_user_id_fkey(uid, username, display_name, email, bio, avatar, followers_count, following_count, posts_count, status, account_type, verify, banned)
                            """.trimIndent())
                        )
                    }
                    .decodeSingleOrNull<JsonObject>()
            }
            
            val comment = parseCommentFromJson(response ?: JsonObject(emptyMap()))
                ?: return@withContext Result.Error(Exception("Failed to parse created comment"), "Failed to parse created comment")

            if (parentCommentId != null) {
                updateRepliesCount(parentCommentId, 1)
            }

            updatePostCommentsCount(postId, 1)

            Result.Success(comment)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    /**
     * Delete a comment (soft delete).
     * Sets is_deleted to true and clears content.
     * 
     * @param commentId The ID of the comment to delete
     * @return Result indicating success or failure
     * 
     * Requirements: 4.6
     */
    suspend fun deleteComment(commentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.Error(Exception("User not authenticated"), "User not authenticated")
            
            val existingComment = retryPolicy.executeWithRetry {
                client.from("comments")
                    .select { filter { eq("id", commentId) } }
                    .decodeSingleOrNull<JsonObject>()
            } ?: return@withContext Result.Error(Exception("Comment not found"), "Comment not found")
            
            val commentUserId = existingComment["user_id"]?.jsonPrimitive?.contentOrNull
            if (commentUserId != currentUser.id) {
                return@withContext Result.Error(Exception("Cannot delete another user's comment"), "Cannot delete another user's comment")
            }
            
            val postId = existingComment["post_id"]?.jsonPrimitive?.contentOrNull
            val parentCommentId = existingComment["parent_comment_id"]?.jsonPrimitive?.contentOrNull
            
            retryPolicy.executeWithRetry {
                client.from("comments")
                    .update({
                        set("is_deleted", true)
                        set("content", "[deleted]")
                        set("deleted_at", java.time.Instant.now().toString())
                    }) {
                        filter { eq("id", commentId) }
                    }
            }
            
            if (parentCommentId != null) {
                updateRepliesCount(parentCommentId, -1)
            }
            
            if (postId != null) {
                updatePostCommentsCount(postId, -1)
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    /**
     * Edit a comment's content.
     * Sets is_edited to true and updates the content.
     * 
     * @param commentId The ID of the comment to edit
     * @param content The new comment content
     * @return Result indicating success or failure
     * 
     * Requirements: 4.5
     */
    suspend fun editComment(commentId: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.Error(Exception("User not authenticated"), "User not authenticated")
            
            val existingComment = retryPolicy.executeWithRetry {
                client.from("comments")
                    .select { filter { eq("id", commentId) } }
                    .decodeSingleOrNull<JsonObject>()
            } ?: return@withContext Result.Error(Exception("Comment not found"), "Comment not found")
            
            val commentUserId = existingComment["user_id"]?.jsonPrimitive?.contentOrNull
            if (commentUserId != currentUser.id) {
                return@withContext Result.Error(Exception("Cannot edit another user's comment"), "Cannot edit another user's comment")
            }
            
            val isDeleted = existingComment["is_deleted"]?.jsonPrimitive?.booleanOrNull ?: false
            if (isDeleted) {
                return@withContext Result.Error(Exception("Cannot edit a deleted comment"), "Cannot edit a deleted comment")
            }
            
            retryPolicy.executeWithRetry {
                client.from("comments")
                    .update({
                        set("content", content)
                        set("is_edited", true)
                        set("updated_at", java.time.Instant.now().toString())
                    }) {
                        filter { eq("id", commentId) }
                    }
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    private suspend fun parseCommentFromJson(data: JsonObject): CommentWithUser? {
        return try {
            val user = parseUserProfileFromJson(data["users"]?.jsonObject)
            val commentId = data["id"]?.jsonPrimitive?.contentOrNull ?: return null
            
            val reactionSummary = getCommentReactionSummarySync(commentId)
            val userReaction = getUserCommentReactionSync(commentId)
            
            CommentWithUser(
                id = commentId,
                postId = data["post_id"]?.jsonPrimitive?.contentOrNull ?: return null,
                userId = data["user_id"]?.jsonPrimitive?.contentOrNull ?: return null,
                parentCommentId = data["parent_comment_id"]?.jsonPrimitive?.contentOrNull,
                content = data["content"]?.jsonPrimitive?.contentOrNull ?: "",
                mediaUrl = data["media_url"]?.jsonPrimitive?.contentOrNull,
                createdAt = data["created_at"]?.jsonPrimitive?.contentOrNull ?: "",
                updatedAt = data["updated_at"]?.jsonPrimitive?.contentOrNull,
                likesCount = data["likes_count"]?.jsonPrimitive?.intOrNull ?: 0,
                repliesCount = data["replies_count"]?.jsonPrimitive?.intOrNull ?: 0,
                isDeleted = data["is_deleted"]?.jsonPrimitive?.booleanOrNull ?: false,
                isEdited = data["is_edited"]?.jsonPrimitive?.booleanOrNull ?: false,
                isPinned = data["is_pinned"]?.jsonPrimitive?.booleanOrNull ?: false,
                user = user,
                reactionSummary = reactionSummary,
                userReaction = userReaction
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse comment: ${e.message}")
            null
        }
    }
    
    private suspend fun getCommentReactionSummarySync(commentId: String): Map<ReactionType, Int> {
        return try {
            val reactions = client.from("comment_reactions")
                .select { filter { eq("comment_id", commentId) } }
                .decodeList<JsonObject>()
            
            reactions
                .groupBy { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "like") }
                .mapValues { it.value.size }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch comment reaction summary: ${e.message}")
            emptyMap()
        }
    }
    
    private suspend fun getUserCommentReactionSync(commentId: String): ReactionType? {
        return try {
            val currentUser = client.auth.currentUserOrNull() ?: return null
            
            val reaction = client.from("comment_reactions")
                .select { filter { eq("comment_id", commentId); eq("user_id", currentUser.id) } }
                .decodeSingleOrNull<JsonObject>()
            
            reaction?.get("reaction_type")?.jsonPrimitive?.contentOrNull?.let {
                ReactionType.fromString(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user comment reaction: ${e.message}")
            null
        }
    }
    
    private fun parseUserProfileFromJson(userData: JsonObject?): UserProfile? {
        if (userData == null) return null
        
        return try {
            UserProfile(
                uid = userData["uid"]?.jsonPrimitive?.contentOrNull ?: return null,
                username = userData["username"]?.jsonPrimitive?.contentOrNull ?: "",
                displayName = userData["display_name"]?.jsonPrimitive?.contentOrNull ?: "",
                email = userData["email"]?.jsonPrimitive?.contentOrNull ?: "",
                bio = userData["bio"]?.jsonPrimitive?.contentOrNull,
                profileImageUrl = userData["avatar"]?.jsonPrimitive?.contentOrNull,
                followersCount = userData["followers_count"]?.jsonPrimitive?.intOrNull ?: 0,
                followingCount = userData["following_count"]?.jsonPrimitive?.intOrNull ?: 0,
                postsCount = userData["posts_count"]?.jsonPrimitive?.intOrNull ?: 0,
                status = userData["status"]?.jsonPrimitive?.contentOrNull ?: "offline",
                account_type = userData["account_type"]?.jsonPrimitive?.contentOrNull ?: "user",
                verify = userData["verify"]?.jsonPrimitive?.booleanOrNull ?: false,
                banned = userData["banned"]?.jsonPrimitive?.booleanOrNull ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse user profile: ${e.message}")
            null
        }
    }
    
    private suspend fun updateRepliesCount(commentId: String, delta: Int) {
        try {
            val replies = client.from("comments")
                .select { 
                    filter { 
                        eq("parent_comment_id", commentId)
                        eq("is_deleted", false)
                    } 
                }
                .decodeList<JsonObject>()
            
            val actualCount = replies.size
            
            client.from("comments")
                .update({ set("replies_count", actualCount) }) {
                    filter { eq("id", commentId) }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update replies count: ${e.message}")
        }
    }
    
    private suspend fun updatePostCommentsCount(postId: String, delta: Int) {
        try {
            val post = client.from("posts")
                .select { filter { eq("id", postId) } }
                .decodeSingleOrNull<JsonObject>()
            
            val currentCount = post?.get("comments_count")?.jsonPrimitive?.intOrNull ?: 0
            val newCount = maxOf(0, currentCount + delta)
            
            client.from("posts")
                .update({ set("comments_count", newCount) }) {
                    filter { eq("id", postId) }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update post comments count: ${e.message}")
        }
    }

    suspend fun pinComment(commentId: String, postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.Error(Exception("User not authenticated"), "User not authenticated")

            val post = retryPolicy.executeWithRetry {
                client.from("posts")
                    .select { filter { eq("id", postId) } }
                    .decodeSingleOrNull<JsonObject>()
            }

            val postAuthorId = post?.get("author_uid")?.jsonPrimitive?.contentOrNull
            if (postAuthorId != currentUser.id) {
                return@withContext Result.Error(Exception("Only post author can pin comments"), "Only post author can pin comments")
            }

            retryPolicy.executeWithRetry {
                client.from("comments")
                    .update({
                        set("is_pinned", false)
                        set("pinned_at", null as String?)
                        set("pinned_by", null as String?)
                    }) {
                        filter {
                            eq("post_id", postId)
                            eq("is_pinned", true)
                        }
                    }

                client.from("comments")
                    .update({
                        set("is_pinned", true)
                        set("pinned_at", java.time.Instant.now().toString())
                        set("pinned_by", currentUser.id)
                    }) {
                        filter { eq("id", commentId) }
                    }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    suspend fun hideComment(commentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.Error(Exception("User not authenticated"), "User not authenticated")

            retryPolicy.executeWithRetry {
                client.from("hidden_comments").insert(buildJsonObject {
                    put("comment_id", commentId)
                    put("user_id", currentUser.id)
                })
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    suspend fun reportComment(
        commentId: String,
        reason: String,
        description: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.Error(Exception("User not authenticated"), "User not authenticated")

            retryPolicy.executeWithRetry {
                client.from("comment_reports").insert(buildJsonObject {
                    put("comment_id", commentId)
                    put("reporter_id", currentUser.id)
                    put("reason", reason)
                    if (description != null) put("description", description)
                })
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    fun isCommentComplete(comment: CommentWithUser): Boolean {
        if (comment.id.isEmpty()) return false
        if (comment.postId.isEmpty()) return false
        if (comment.userId.isEmpty()) return false
        if (comment.createdAt.isEmpty()) return false
        
        if (comment.user == null) return false
        if (comment.user.uid.isEmpty()) return false
        
        return true
    }
    
    fun isReplyCountAccurate(comment: CommentWithUser, actualReplies: List<CommentWithUser>): Boolean {
        return comment.repliesCount == actualReplies.size
    }
    
    fun hasCorrectParentReference(reply: CommentWithUser, expectedParentId: String): Boolean {
        return reply.parentCommentId == expectedParentId
    }
    
    fun hasCorrectEditDeleteStatus(
        comment: CommentWithUser,
        wasEdited: Boolean,
        wasDeleted: Boolean
    ): Boolean {
        return comment.isEdited == wasEdited && comment.isDeleted == wasDeleted
    }
    
    fun hasCorrectMediaInclusion(comment: CommentWithUser, expectedMediaUrl: String?): Boolean {
        return comment.mediaUrl == expectedMediaUrl
    }
    
    fun areCommentsSortedByDate(comments: List<CommentWithUser>): Boolean {
        if (comments.size <= 1) return true
        
        return comments.zipWithNext().all { (a, b) ->
            a.createdAt <= b.createdAt
        }
    }
    
    fun filterTopLevelComments(comments: List<CommentWithUser>): List<CommentWithUser> {
        return comments.filter { it.parentCommentId == null }
    }
    
    fun filterRepliesForParent(comments: List<CommentWithUser>, parentId: String): List<CommentWithUser> {
        return comments.filter { it.parentCommentId == parentId }
    }
    
    fun calculateRepliesCount(allComments: List<CommentWithUser>, commentId: String): Int {
        return allComments.count { it.parentCommentId == commentId }
    }
}
