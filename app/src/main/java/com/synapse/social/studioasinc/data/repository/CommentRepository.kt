package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.*
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
    
    companion object {
        private const val TAG = "CommentRepository"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 100L
    }
    
    // ==================== COMMENT FETCHING ====================
    
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
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured"))
            }
            
            Log.d(TAG, "Fetching comments for post: $postId (limit=$limit, offset=$offset)")

            // Query comments with user join - only top-level comments (no parent)
            val response = client.from("comments")
                .select(
                    columns = Columns.raw("""
                        *,
                        users!comments_user_id_fkey(uid, username, display_name, email, bio, profile_image_url, followers_count, following_count, posts_count, status, account_type, verify, banned)
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
            
            val comments = response.mapNotNull { parseCommentFromJson(it) }
            
            Log.d(TAG, "Fetched ${comments.size} comments for post: $postId")
            Result.success(comments)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch comments: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
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
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured"))
            }
            
            Log.d(TAG, "Fetching replies for comment: $commentId")
            
            // Query replies with user join
            val response = client.from("comments")
                .select(
                    columns = Columns.raw("""
                        *,
                        users!comments_user_id_fkey(uid, username, display_name, email, bio, profile_image_url, followers_count, following_count, posts_count, status, account_type, verify, banned)
                    """.trimIndent())
                ) {
                    filter { eq("parent_comment_id", commentId) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<JsonObject>()
            
            val replies = response.mapNotNull { parseCommentFromJson(it) }
            
            Log.d(TAG, "Fetched ${replies.size} replies for comment: $commentId")
            Result.success(replies)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch replies: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
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
    fun observeComments(postId: String): Flow<CommentEvent> = flow {
        // Initial fetch would be handled separately
        // Real-time updates are handled via Supabase Realtime subscriptions in ViewModel
    }

    
    // ==================== COMMENT CRUD OPERATIONS ====================
    
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
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured"))
            }
            
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser == null) {
                return@withContext Result.failure(Exception("User must be authenticated to comment"))
            }
            
            val userId = currentUser.id
            Log.d(TAG, "Creating comment for post: $postId by user: $userId")
            
            var lastException: Exception? = null
            repeat(MAX_RETRIES) { attempt ->
                try {
                    // Insert the comment
                    val insertData = buildJsonObject {
                        put("post_id", postId)
                        put("user_id", userId)
                        put("content", content)
                        if (mediaUrl != null) put("media_url", mediaUrl)
                        if (parentCommentId != null) put("parent_comment_id", parentCommentId)
                    }
                    
                    val response = client.from("comments")
                        .insert(insertData) {
                            select(
                                columns = Columns.raw("""
                                    *,
                                    users!comments_user_id_fkey(uid, username, display_name, email, bio, profile_image_url, followers_count, following_count, posts_count, status, account_type, verify, banned)
                                """.trimIndent())
                            )
                        }
                        .decodeSingleOrNull<JsonObject>()
                    
                    if (response == null) {
                        return@withContext Result.failure(Exception("Failed to create comment"))
                    }
                    
                    val comment = parseCommentFromJson(response)
                        ?: return@withContext Result.failure(Exception("Failed to parse created comment"))
                    
                    // Update parent comment's replies_count if this is a reply
                    if (parentCommentId != null) {
                        updateRepliesCount(parentCommentId, 1)
                    }
                    
                    // Update post's comments_count
                    updatePostCommentsCount(postId, 1)
                    
                    Log.d(TAG, "Comment created successfully: ${comment.id}")
                    return@withContext Result.success(comment)
                } catch (e: Exception) {
                    lastException = e
                    val isRLSError = e.message?.contains("policy", true) == true
                    if (isRLSError || attempt == MAX_RETRIES - 1) throw e
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
            
            Result.failure(Exception(mapSupabaseError(lastException ?: Exception("Unknown error"))))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create comment: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
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
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured"))
            }
            
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser == null) {
                return@withContext Result.failure(Exception("User must be authenticated to delete comment"))
            }
            
            Log.d(TAG, "Deleting comment: $commentId")
            
            // Get the comment first to check ownership and get post_id
            val existingComment = client.from("comments")
                .select { filter { eq("id", commentId) } }
                .decodeSingleOrNull<JsonObject>()
            
            if (existingComment == null) {
                return@withContext Result.failure(Exception("Comment not found"))
            }
            
            val commentUserId = existingComment["user_id"]?.jsonPrimitive?.contentOrNull
            if (commentUserId != currentUser.id) {
                return@withContext Result.failure(Exception("Cannot delete another user's comment"))
            }
            
            val postId = existingComment["post_id"]?.jsonPrimitive?.contentOrNull
            val parentCommentId = existingComment["parent_comment_id"]?.jsonPrimitive?.contentOrNull
            
            // Soft delete - set is_deleted and clear content
            client.from("comments")
                .update({
                    set("is_deleted", true)
                    set("content", "[deleted]")
                    set("deleted_at", java.time.Instant.now().toString())
                }) {
                    filter { eq("id", commentId) }
                }
            
            // Update parent comment's replies_count if this was a reply
            if (parentCommentId != null) {
                updateRepliesCount(parentCommentId, -1)
            }
            
            // Update post's comments_count
            if (postId != null) {
                updatePostCommentsCount(postId, -1)
            }
            
            Log.d(TAG, "Comment deleted successfully: $commentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete comment: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
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
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured"))
            }
            
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser == null) {
                return@withContext Result.failure(Exception("User must be authenticated to edit comment"))
            }
            
            Log.d(TAG, "Editing comment: $commentId")
            
            // Get the comment first to check ownership
            val existingComment = client.from("comments")
                .select { filter { eq("id", commentId) } }
                .decodeSingleOrNull<JsonObject>()
            
            if (existingComment == null) {
                return@withContext Result.failure(Exception("Comment not found"))
            }
            
            val commentUserId = existingComment["user_id"]?.jsonPrimitive?.contentOrNull
            if (commentUserId != currentUser.id) {
                return@withContext Result.failure(Exception("Cannot edit another user's comment"))
            }
            
            val isDeleted = existingComment["is_deleted"]?.jsonPrimitive?.booleanOrNull ?: false
            if (isDeleted) {
                return@withContext Result.failure(Exception("Cannot edit a deleted comment"))
            }
            
            // Update the comment
            client.from("comments")
                .update({
                    set("content", content)
                    set("is_edited", true)
                    set("updated_at", java.time.Instant.now().toString())
                }) {
                    filter { eq("id", commentId) }
                }
            
            Log.d(TAG, "Comment edited successfully: $commentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to edit comment: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    /**
     * Parse CommentWithUser from JsonObject with embedded user data.
     */
    private fun parseCommentFromJson(data: JsonObject): CommentWithUser? {
        return try {
            val user = parseUserProfileFromJson(data["users"]?.jsonObject)
            
            CommentWithUser(
                id = data["id"]?.jsonPrimitive?.contentOrNull ?: return null,
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
                user = user
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse comment: ${e.message}")
            null
        }
    }
    
    /**
     * Parse UserProfile from joined user data.
     */
    private fun parseUserProfileFromJson(userData: JsonObject?): UserProfile? {
        if (userData == null) return null
        
        return try {
            UserProfile(
                uid = userData["uid"]?.jsonPrimitive?.contentOrNull ?: return null,
                username = userData["username"]?.jsonPrimitive?.contentOrNull ?: "",
                display_name = userData["display_name"]?.jsonPrimitive?.contentOrNull ?: "",
                email = userData["email"]?.jsonPrimitive?.contentOrNull ?: "",
                bio = userData["bio"]?.jsonPrimitive?.contentOrNull,
                profile_image_url = userData["profile_image_url"]?.jsonPrimitive?.contentOrNull,
                followers_count = userData["followers_count"]?.jsonPrimitive?.intOrNull ?: 0,
                following_count = userData["following_count"]?.jsonPrimitive?.intOrNull ?: 0,
                posts_count = userData["posts_count"]?.jsonPrimitive?.intOrNull ?: 0,
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
    
    /**
     * Update the replies_count for a parent comment.
     */
    private suspend fun updateRepliesCount(commentId: String, delta: Int) {
        try {
            val comment = client.from("comments")
                .select { filter { eq("id", commentId) } }
                .decodeSingleOrNull<JsonObject>()
            
            val currentCount = comment?.get("replies_count")?.jsonPrimitive?.intOrNull ?: 0
            val newCount = maxOf(0, currentCount + delta)
            
            client.from("comments")
                .update({ set("replies_count", newCount) }) {
                    filter { eq("id", commentId) }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update replies count: ${e.message}")
        }
    }
    
    /**
     * Update the comments_count for a post.
     */
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

    
    /**
     * Map Supabase errors to user-friendly messages.
     */
    private fun mapSupabaseError(exception: Exception): String {
        val message = exception.message ?: "Unknown error"
        
        Log.e(TAG, "Supabase error: $message", exception)
        
        return when {
            message.contains("PGRST200") -> "Database table not found"
            message.contains("PGRST100") -> "Database column does not exist"
            message.contains("PGRST116") -> "Comment not found"
            message.contains("relation", ignoreCase = true) -> "Database table does not exist"
            message.contains("column", ignoreCase = true) -> "Database column mismatch"
            message.contains("policy", ignoreCase = true) || message.contains("rls", ignoreCase = true) -> 
                "Permission denied"
            message.contains("connection", ignoreCase = true) || message.contains("network", ignoreCase = true) -> 
                "Connection failed. Please check your internet connection."
            message.contains("timeout", ignoreCase = true) -> "Request timed out. Please try again."
            message.contains("unauthorized", ignoreCase = true) -> "Permission denied."
            else -> "Failed to process comment: $message"
        }
    }
    
    // ==================== TESTABLE LOGIC METHODS ====================
    
    /**
     * Check if a comment has all required fields for display.
     * Used for Property 8: Comment loading completeness.
     * 
     * @param comment The comment to validate
     * @return True if all required fields are present
     * 
     * Requirements: 4.1, 4.2
     */
    fun isCommentComplete(comment: CommentWithUser): Boolean {
        // Required fields must be present
        if (comment.id.isEmpty()) return false
        if (comment.postId.isEmpty()) return false
        if (comment.userId.isEmpty()) return false
        if (comment.createdAt.isEmpty()) return false
        
        // User info should be present for display
        if (comment.user == null) return false
        if (comment.user.uid.isEmpty()) return false
        
        return true
    }
    
    /**
     * Check if a comment's reply count matches the actual number of replies.
     * Used for Property 12: Reply count accuracy.
     * 
     * @param comment The parent comment
     * @param actualReplies The list of actual replies
     * @return True if the count matches
     * 
     * Requirements: 5.1
     */
    fun isReplyCountAccurate(comment: CommentWithUser, actualReplies: List<CommentWithUser>): Boolean {
        return comment.repliesCount == actualReplies.size
    }
    
    /**
     * Check if a reply has the correct parent reference.
     * Used for Property 13: Reply parent reference.
     * 
     * @param reply The reply comment
     * @param expectedParentId The expected parent comment ID
     * @return True if the parent reference is correct
     * 
     * Requirements: 5.4
     */
    fun hasCorrectParentReference(reply: CommentWithUser, expectedParentId: String): Boolean {
        return reply.parentCommentId == expectedParentId
    }
    
    /**
     * Check if a comment's edit status is correctly reflected.
     * Used for Property 11: Comment edit and delete status.
     * 
     * @param comment The comment to check
     * @param wasEdited Whether the comment was edited
     * @param wasDeleted Whether the comment was deleted
     * @return True if the status flags are correct
     * 
     * Requirements: 4.5, 4.6
     */
    fun hasCorrectEditDeleteStatus(
        comment: CommentWithUser,
        wasEdited: Boolean,
        wasDeleted: Boolean
    ): Boolean {
        return comment.isEdited == wasEdited && comment.isDeleted == wasDeleted
    }
    
    /**
     * Check if a comment with media has the media URL included.
     * Used for Property 10: Comment media inclusion.
     * 
     * @param comment The comment to check
     * @param expectedMediaUrl The expected media URL (null if no media)
     * @return True if the media URL is correctly included
     * 
     * Requirements: 4.4
     */
    fun hasCorrectMediaInclusion(comment: CommentWithUser, expectedMediaUrl: String?): Boolean {
        return comment.mediaUrl == expectedMediaUrl
    }
    
    /**
     * Validate that comments are sorted by creation date.
     * 
     * @param comments List of comments to validate
     * @return True if comments are sorted ascending by created_at
     */
    fun areCommentsSortedByDate(comments: List<CommentWithUser>): Boolean {
        if (comments.size <= 1) return true
        
        return comments.zipWithNext().all { (a, b) ->
            a.createdAt <= b.createdAt
        }
    }
    
    /**
     * Filter comments to get only top-level comments (no parent).
     * 
     * @param comments List of all comments
     * @return List of top-level comments only
     */
    fun filterTopLevelComments(comments: List<CommentWithUser>): List<CommentWithUser> {
        return comments.filter { it.parentCommentId == null }
    }
    
    /**
     * Filter comments to get replies for a specific parent.
     * 
     * @param comments List of all comments
     * @param parentId The parent comment ID
     * @return List of replies to the specified parent
     */
    fun filterRepliesForParent(comments: List<CommentWithUser>, parentId: String): List<CommentWithUser> {
        return comments.filter { it.parentCommentId == parentId }
    }
    
    /**
     * Calculate the expected replies count for a comment based on actual data.
     * 
     * @param allComments List of all comments
     * @param commentId The comment ID to calculate replies for
     * @return The number of replies
     */
    fun calculateRepliesCount(allComments: List<CommentWithUser>, commentId: String): Int {
        return allComments.count { it.parentCommentId == commentId }
    }
}
