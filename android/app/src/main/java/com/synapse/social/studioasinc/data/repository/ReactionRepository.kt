package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.SynapseApplication
import com.synapse.social.studioasinc.data.Result
import com.synapse.social.studioasinc.model.ReactionType
import com.synapse.social.studioasinc.util.ErrorHandler
import com.synapse.social.studioasinc.util.RetryPolicy
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/**
 * Repository for handling post and comment reactions.
 * Uses the `reactions` table for post reactions and `comment_reactions` table for comment reactions.
 * 
 * Requirements: 3.2, 3.3, 3.4, 3.5, 6.2, 6.3, 6.4
 */
class ReactionRepository {
    
    private val client = SupabaseClient.client
    private val retryPolicy = RetryPolicy()
    
    companion object {
        private const val TAG = "ReactionRepository"
    }
    
    /**
     * Toggle a reaction on a post.
     * - If no reaction exists, adds the reaction
     * - If same reaction type exists, removes it
     * - If different reaction type exists, updates to new type
     * 
     * @param postId The ID of the post
     * @param reactionType The type of reaction to toggle
     * @return Result indicating success or failure
     * 
     * Requirements: 3.2, 3.3, 3.4
     */
    suspend fun togglePostReaction(
        postId: String,
        reactionType: ReactionType
    ): Result<ReactionToggleResult> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.Error(Exception("User not authenticated"), "User not authenticated")
            val userId = currentUser.id
            
            val result = retryPolicy.executeWithRetry {
                val existingReaction = client.from("reactions")
                    .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                    .decodeSingleOrNull<JsonObject>()

                if (existingReaction != null) {
                    val existingType = existingReaction["reaction_type"]?.jsonPrimitive?.contentOrNull
                    if (existingType == reactionType.name.lowercase()) {
                        client.from("reactions")
                            .delete { filter { eq("post_id", postId); eq("user_id", userId) } }
                        ReactionToggleResult.REMOVED
                    } else {
                        client.from("reactions")
                            .update({
                                set("reaction_type", reactionType.name.lowercase())
                                set("updated_at", java.time.Instant.now().toString())
                            }) { filter { eq("post_id", postId); eq("user_id", userId) } }
                        ReactionToggleResult.UPDATED
                    }
                } else {
                    client.from("reactions").insert(buildJsonObject {
                        put("user_id", userId)
                        put("post_id", postId)
                        put("reaction_type", reactionType.name.lowercase())
                    })
                    ReactionToggleResult.ADDED
                }
            }
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    /**
     * Get aggregated reaction counts for a post.
     * 
     * @param postId The ID of the post
     * @return Result containing a map of ReactionType to count
     * 
     * Requirements: 3.5
     */
    suspend fun getPostReactionSummary(postId: String): Result<Map<ReactionType, Int>> = withContext(Dispatchers.IO) {
        try {
            val reactions = retryPolicy.executeWithRetry {
                client.from("reactions")
                    .select { filter { eq("post_id", postId) } }
                    .decodeList<JsonObject>()
            }
            
            val summary = reactions
                .groupBy { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                .mapValues { it.value.size }
            
            Result.Success(summary)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    /**
     * Get the current user's reaction for a post.
     * 
     * @param postId The ID of the post
     * @return Result containing the user's ReactionType or null if no reaction
     * 
     * Requirements: 3.3
     */
    suspend fun getUserPostReaction(postId: String): Result<ReactionType?> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser == null) {
                return@withContext Result.Success(null)
            }
            val userId = currentUser.id
            
            val reaction = retryPolicy.executeWithRetry {
                client.from("reactions")
                    .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                    .decodeSingleOrNull<JsonObject>()
            }
            
            val reactionType = reaction?.get("reaction_type")?.jsonPrimitive?.contentOrNull?.let {
                ReactionType.fromString(it)
            }
            
            Result.Success(reactionType)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    /**
     * Toggle a reaction on a comment.
     * - If no reaction exists, adds the reaction
     * - If same reaction type exists, removes it
     * - If different reaction type exists, updates to new type
     * 
     * @param commentId The ID of the comment
     * @param reactionType The type of reaction to toggle
     * @return Result indicating success or failure
     * 
     * Requirements: 6.2, 6.3, 6.4
     */
    suspend fun toggleCommentReaction(
        commentId: String,
        reactionType: ReactionType
    ): Result<ReactionToggleResult> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.Error(Exception("User not authenticated"), "User not authenticated")
            val userId = currentUser.id
            
            val result = retryPolicy.executeWithRetry {
                val existingReaction = client.from("comment_reactions")
                    .select { filter { eq("comment_id", commentId); eq("user_id", userId) } }
                    .decodeSingleOrNull<JsonObject>()

                if (existingReaction != null) {
                    val existingType = existingReaction["reaction_type"]?.jsonPrimitive?.contentOrNull
                    if (existingType == reactionType.name.lowercase()) {
                        client.from("comment_reactions")
                            .delete { filter { eq("comment_id", commentId); eq("user_id", userId) } }
                        ReactionToggleResult.REMOVED
                    } else {
                        client.from("comment_reactions")
                            .update({
                                set("reaction_type", reactionType.name.lowercase())
                                set("updated_at", java.time.Instant.now().toString())
                            }) { filter { eq("comment_id", commentId); eq("user_id", userId) } }
                        ReactionToggleResult.UPDATED
                    }
                } else {
                    client.from("comment_reactions").insert(buildJsonObject {
                        put("user_id", userId)
                        put("comment_id", commentId)
                        put("reaction_type", reactionType.name.lowercase())
                    })
                    ReactionToggleResult.ADDED
                }
            }
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    /**
     * Get aggregated reaction counts for a comment.
     * 
     * @param commentId The ID of the comment
     * @return Result containing a map of ReactionType to count
     * 
     * Requirements: 6.2
     */
    suspend fun getCommentReactionSummary(commentId: String): Result<Map<ReactionType, Int>> = withContext(Dispatchers.IO) {
        try {
            val reactions = retryPolicy.executeWithRetry {
                client.from("comment_reactions")
                    .select { filter { eq("comment_id", commentId) } }
                    .decodeList<JsonObject>()
            }
            
            val summary = reactions
                .groupBy { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                .mapValues { it.value.size }
            
            Result.Success(summary)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    /**
     * Get the current user's reaction for a comment.
     * 
     * @param commentId The ID of the comment
     * @return Result containing the user's ReactionType or null if no reaction
     * 
     * Requirements: 6.3
     */
    suspend fun getUserCommentReaction(commentId: String): Result<ReactionType?> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser == null) {
                return@withContext Result.Success(null)
            }
            val userId = currentUser.id
            
            val reaction = retryPolicy.executeWithRetry {
                client.from("comment_reactions")
                    .select { filter { eq("comment_id", commentId); eq("user_id", userId) } }
                    .decodeSingleOrNull<JsonObject>()
            }
            
            val reactionType = reaction?.get("reaction_type")?.jsonPrimitive?.contentOrNull?.let {
                ReactionType.fromString(it)
            }
            
            Result.Success(reactionType)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    fun determineToggleResult(
        existingReactionType: ReactionType?,
        newReactionType: ReactionType
    ): ReactionToggleResult {
        return when {
            existingReactionType == null -> ReactionToggleResult.ADDED
            existingReactionType == newReactionType -> ReactionToggleResult.REMOVED
            else -> ReactionToggleResult.UPDATED
        }
    }

    fun calculateReactionSummary(reactions: List<ReactionType>): Map<ReactionType, Int> {
        return reactions.groupingBy { it }.eachCount()
    }

    fun isReactionSummaryAccurate(summary: Map<ReactionType, Int>, totalReactions: Int): Boolean {
        return summary.values.sum() == totalReactions
    }
}

enum class ReactionToggleResult {
    ADDED,
    REMOVED,
    UPDATED
}
