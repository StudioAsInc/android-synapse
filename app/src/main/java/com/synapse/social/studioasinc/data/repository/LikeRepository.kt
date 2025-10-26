package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.Like
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LikeRepository {
    
    private val client = SupabaseClient.client
    
    /**
     * Toggle like on a post (like if not liked, unlike if already liked)
     */
    suspend fun toggleLike(userId: String, targetId: String, targetType: String = "post"): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("LikeRepository", "Toggling like for user=$userId, target=$targetId, type=$targetType")
                
                // Check if already liked
                val existingLike = client.from("likes")
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("target_id", targetId)
                            eq("target_type", targetType)
                        }
                    }
                    .decodeSingleOrNull<Like>()
                
                if (existingLike != null) {
                    // Unlike - delete the like
                    client.from("likes").delete {
                        filter {
                            eq("user_id", userId)
                            eq("target_id", targetId)
                            eq("target_type", targetType)
                        }
                    }
                    android.util.Log.d("LikeRepository", "Unliked successfully")
                    Result.success(false) // false = unliked
                } else {
                    // Like - insert new like
                    val like = Like(
                        userId = userId,
                        targetId = targetId,
                        targetType = targetType
                    )
                    client.from("likes").insert(like)
                    android.util.Log.d("LikeRepository", "Liked successfully")
                    Result.success(true) // true = liked
                }
            } catch (e: Exception) {
                android.util.Log.e("LikeRepository", "Failed to toggle like", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Check if user has liked a target
     */
    suspend fun isLiked(userId: String, targetId: String, targetType: String = "post"): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val like = client.from("likes")
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("target_id", targetId)
                            eq("target_type", targetType)
                        }
                    }
                    .decodeSingleOrNull<Like>()
                
                Result.success(like != null)
            } catch (e: Exception) {
                android.util.Log.e("LikeRepository", "Failed to check like status", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get like count for a target
     */
    suspend fun getLikeCount(targetId: String, targetType: String = "post"): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val likes = client.from("likes")
                    .select {
                        filter {
                            eq("target_id", targetId)
                            eq("target_type", targetType)
                        }
                    }
                    .decodeList<Like>()
                
                Result.success(likes.size)
            } catch (e: Exception) {
                android.util.Log.e("LikeRepository", "Failed to get like count", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get all likes for a user's posts
     */
    suspend fun getUserLikes(userId: String): Result<List<Like>> {
        return withContext(Dispatchers.IO) {
            try {
                val likes = client.from("likes")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<Like>()
                
                Result.success(likes)
            } catch (e: Exception) {
                android.util.Log.e("LikeRepository", "Failed to get user likes", e)
                Result.failure(e)
            }
        }
    }
}
