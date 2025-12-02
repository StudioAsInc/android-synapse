package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.SynapseApplication
import com.synapse.social.studioasinc.data.Result
import com.synapse.social.studioasinc.model.PollOptionResult
import com.synapse.social.studioasinc.util.ErrorHandler
import com.synapse.social.studioasinc.util.RetryPolicy
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

class PollRepository {
    private val client = SupabaseClient.client
    private val retryPolicy = RetryPolicy()
    
    @Serializable
    private data class PollVote(
        val id: String? = null,
        @SerialName("post_id") val postId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("option_index") val optionIndex: Int,
        @SerialName("created_at") val createdAt: String? = null
    )
    
    @Serializable
    private data class PostPollData(
        val id: String,
        @SerialName("poll_options") val pollOptions: List<String>,
        @SerialName("poll_end_time") val pollEndTime: String?
    )
    
    suspend fun getUserVote(postId: String): Result<Int?> {
        return try {
            val userId = client.auth.currentUserOrNull()?.id
                ?: return Result.Error(Exception("Not authenticated"), "Not authenticated")

            val votes = retryPolicy.executeWithRetry {
                client.from("poll_votes")
                    .select(Columns.list("option_index")) {
                        filter {
                            eq("post_id", postId)
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<PollVote>()
            }

            Result.Success(votes.firstOrNull()?.optionIndex)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    suspend fun submitVote(postId: String, optionIndex: Int): Result<Unit> {
        return try {
            val userId = client.auth.currentUserOrNull()?.id
                ?: return Result.Error(Exception("Not authenticated"), "Not authenticated")

            retryPolicy.executeWithRetry {
                val post = client.from("posts")
                    .select(Columns.list("id", "poll_options", "poll_end_time")) {
                        filter { eq("id", postId) }
                    }
                    .decodeSingle<PostPollData>()

                post.pollEndTime?.let { endTime ->
                    if (Instant.parse(endTime).isBefore(Instant.now())) {
                        throw Exception("Poll has ended")
                    }
                }

                if (optionIndex < 0 || optionIndex >= post.pollOptions.size) {
                    throw Exception("Invalid option index")
                }

                val existingVote = client.from("poll_votes")
                    .select(Columns.list("id")) {
                        filter {
                            eq("post_id", postId)
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<PollVote>()
                    .firstOrNull()

                if (existingVote != null) {
                    client.from("poll_votes")
                        .update({
                            set("option_index", optionIndex)
                        }) {
                            filter { eq("id", existingVote.id!!) }
                        }
                } else {
                    client.from("poll_votes")
                        .insert(PollVote(
                            postId = postId,
                            userId = userId,
                            optionIndex = optionIndex
                        ))
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    suspend fun getPollResults(postId: String): Result<List<PollOptionResult>> {
        return try {
            val post = retryPolicy.executeWithRetry {
                client.from("posts")
                    .select(Columns.list("poll_options")) {
                        filter { eq("id", postId) }
                    }
                    .decodeSingle<PostPollData>()
            }

            val votes = retryPolicy.executeWithRetry {
                client.from("poll_votes")
                    .select(Columns.list("option_index")) {
                        filter { eq("post_id", postId) }
                    }
                    .decodeList<PollVote>()
            }

            val voteCounts = votes.groupingBy { it.optionIndex }.eachCount()

            Result.Success(PollOptionResult.calculateResults(post.pollOptions, voteCounts))
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    companion object {
        private const val TAG = "PollRepository"
    }
}
