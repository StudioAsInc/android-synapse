package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.model.ReactionType

@Deprecated(
    message = "Use PostRepository.toggleReaction() instead. The reactions table is now the single source of truth.",
    replaceWith = ReplaceWith("PostRepository().toggleReaction(postId, userId, ReactionType.LIKE)")
)
class LikeRepository {
    
    private val postRepository = PostRepository()
    
    @Deprecated("Use PostRepository.toggleReaction()", ReplaceWith("postRepository.toggleReaction(targetId, userId, ReactionType.LIKE)"))
    suspend fun toggleLike(userId: String, targetId: String, targetType: String = "post"): Result<Boolean> {
        if (targetType != "post") {
            android.util.Log.w("LikeRepository", "Non-post likes not supported in reactions table")
            return Result.failure(Exception("Only post reactions are supported"))
        }
        
        val result = postRepository.toggleReaction(targetId, userId, ReactionType.LIKE)
        return result.map { true }
    }
    
    @Deprecated("Use PostRepository.getUserReaction()", ReplaceWith("postRepository.getUserReaction(targetId, userId)"))
    suspend fun isLiked(userId: String, targetId: String, targetType: String = "post"): Result<Boolean> {
        if (targetType != "post") return Result.success(false)
        
        val result = postRepository.getUserReaction(targetId, userId)
        return result.map { it != null }
    }
    
    @Deprecated("Use PostRepository.getReactionSummary()", ReplaceWith("postRepository.getReactionSummary(targetId)"))
    suspend fun getLikeCount(targetId: String, targetType: String = "post"): Result<Int> {
        if (targetType != "post") return Result.success(0)
        
        val result = postRepository.getReactionSummary(targetId)
        return result.map { summary -> summary.values.sum() }
    }
    
    @Deprecated("Not supported in new schema")
    suspend fun getUserLikes(userId: String): Result<List<Nothing>> {
        android.util.Log.w("LikeRepository", "getUserLikes() not supported - use reactions table queries")
        return Result.success(emptyList())
    }
}
