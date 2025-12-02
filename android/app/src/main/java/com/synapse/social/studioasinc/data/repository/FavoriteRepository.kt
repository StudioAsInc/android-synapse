package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.SynapseApplication
import com.synapse.social.studioasinc.data.Result
import com.synapse.social.studioasinc.util.ErrorHandler
import com.synapse.social.studioasinc.util.RetryPolicy
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class FavoriteRepository {

    private val client = SupabaseClient.client
    private val retryPolicy = RetryPolicy()

    suspend fun isFavorite(userId: String, postId: String): Result<Boolean> {
        return try {
            val result = retryPolicy.executeWithRetry {
                client.from("favorites")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            eq("user_id", userId)
                            eq("post_id", postId)
                        }
                    }
                    .decodeList<Map<String, Any>>()
            }
            Result.Success(result.isNotEmpty())
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun toggleFavorite(userId: String, postId: String): Result<Boolean> {
        return try {
            val isFavorite = when (val result = isFavorite(userId, postId)) {
                is Result.Success -> result.data
                is Result.Error -> throw result.exception
                else -> false
            }

            retryPolicy.executeWithRetry {
                if (isFavorite) {
                    client.from("favorites").delete {
                        filter {
                            eq("user_id", userId)
                            eq("post_id", postId)
                        }
                    }
                } else {
                    client.from("favorites").insert(mapOf(
                        "user_id" to userId,
                        "post_id" to postId
                    ))
                }
            }
            Result.Success(!isFavorite)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
}
