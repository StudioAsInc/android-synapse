package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.SynapseApplication
import com.synapse.social.studioasinc.data.Result
import com.synapse.social.studioasinc.util.ErrorHandler
import com.synapse.social.studioasinc.util.RetryPolicy
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class BookmarkRepository {
    private val client = SupabaseClient.client
    private val retryPolicy = RetryPolicy()
    
    @Serializable
    private data class Favorite(
        val id: String? = null,
        @SerialName("post_id") val postId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("collection_id") val collectionId: String? = null
    )
    
    suspend fun isBookmarked(postId: String): Result<Boolean> {
        return try {
            val userId = client.auth.currentUserOrNull()?.id
                ?: return Result.Error(Exception("Not authenticated"), "Not authenticated")

            val favorites = retryPolicy.executeWithRetry {
                client.from("favorites")
                    .select(Columns.list("id")) {
                        filter {
                            eq("post_id", postId)
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<Favorite>()
            }

            Result.Success(favorites.isNotEmpty())
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    suspend fun toggleBookmark(postId: String, collectionId: String? = null): Result<Boolean> {
        return try {
            val userId = client.auth.currentUserOrNull()?.id
                ?: return Result.Error(Exception("Not authenticated"), "Not authenticated")

            val existing = retryPolicy.executeWithRetry {
                client.from("favorites")
                    .select(Columns.list("id")) {
                        filter {
                            eq("post_id", postId)
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<Favorite>()
                    .firstOrNull()
            }

            if (existing != null) {
                retryPolicy.executeWithRetry {
                    client.from("favorites")
                        .delete { filter { eq("id", existing.id!!) } }
                }
                Log.d(TAG, "Bookmark removed: $postId")
                Result.Success(false)
            } else {
                retryPolicy.executeWithRetry {
                    client.from("favorites")
                        .insert(Favorite(postId = postId, userId = userId, collectionId = collectionId))
                }
                Log.d(TAG, "Bookmark added: $postId")
                Result.Success(true)
            }
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    companion object {
        private const val TAG = "BookmarkRepository"
    }
}
