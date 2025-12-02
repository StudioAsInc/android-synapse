package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.SynapseApplication
import com.synapse.social.studioasinc.data.Result
import com.synapse.social.studioasinc.util.ErrorHandler
import com.synapse.social.studioasinc.util.RetryPolicy
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ReportRepository {
    private val client = SupabaseClient.client
    private val retryPolicy = RetryPolicy()
    
    @Serializable
    private data class PostReport(
        val id: String? = null,
        @SerialName("post_id") val postId: String,
        @SerialName("reporter_id") val reporterId: String,
        val reason: String,
        val description: String? = null
    )
    
    suspend fun createReport(
        postId: String,
        reason: String,
        description: String? = null
    ): Result<Unit> {
        return try {
            val userId = client.auth.currentUserOrNull()?.id
                ?: return Result.Error(Exception("Not authenticated"), "Not authenticated")

            retryPolicy.executeWithRetry {
                client.from("post_reports")
                    .insert(PostReport(
                        postId = postId,
                        reporterId = userId,
                        reason = reason,
                        description = description
                    ))
            }

            Log.d(TAG, "Report created: post=$postId, reason=$reason")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    companion object {
        private const val TAG = "ReportRepository"
    }
}
