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

class ReshareRepository {
    private val client = SupabaseClient.client
    private val retryPolicy = RetryPolicy()
    
    @Serializable
    private data class Reshare(
        val id: String? = null,
        @SerialName("post_id") val postId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("reshare_text") val reshareText: String? = null
    )
    
    suspend fun hasReshared(postId: String): Result<Boolean> {
        return try {
            val userId = client.auth.currentUserOrNull()?.id
                ?: return Result.Error(Exception("Not authenticated"), "Not authenticated")

            val reshares = retryPolicy.executeWithRetry {
                client.from("reshares")
                    .select(Columns.list("id")) {
                        filter {
                            eq("post_id", postId)
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<Reshare>()
            }

            Result.Success(reshares.isNotEmpty())
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    suspend fun createReshare(postId: String, commentary: String? = null): Result<Unit> {
        return try {
            val userId = client.auth.currentUserOrNull()?.id
                ?: return Result.Error(Exception("Not authenticated"), "Not authenticated")

            retryPolicy.executeWithRetry {
                val existing = client.from("reshares")
                    .select(Columns.list("id")) {
                        filter {
                            eq("post_id", postId)
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<Reshare>()
                    .firstOrNull()

                if (existing != null) {
                    throw Exception("Already reshared")
                }

                client.from("reshares")
                    .insert(Reshare(postId = postId, userId = userId, reshareText = commentary))
            }

            Log.d(TAG, "Reshare created: $postId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    companion object {
        private const val TAG = "ReshareRepository"
    }
}
