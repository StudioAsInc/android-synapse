package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.SynapseApplication
import com.synapse.social.studioasinc.data.Result
import com.synapse.social.studioasinc.util.ErrorHandler
import com.synapse.social.studioasinc.util.RetryPolicy
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class ProfileLikeRepository {

    private val client = SupabaseClient.client
    private val retryPolicy = RetryPolicy()

    suspend fun isProfileLiked(likerId: String, profileId: String): Result<Boolean> {
        return try {
            val result = retryPolicy.executeWithRetry {
                client.from("profile_likes")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            eq("liker_uid", likerId)
                            eq("profile_uid", profileId)
                        }
                    }
                    .decodeList<Map<String, Any>>()
            }
            Result.Success(result.isNotEmpty())
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun toggleProfileLike(likerId: String, profileId: String): Result<Boolean> {
        return try {
            val isLiked = when (val result = isProfileLiked(likerId, profileId)) {
                is Result.Success -> result.data
                is Result.Error -> throw result.exception
                else -> false
            }

            retryPolicy.executeWithRetry {
                if (isLiked) {
                    client.from("profile_likes").delete {
                        filter {
                            eq("liker_uid", likerId)
                            eq("profile_uid", profileId)
                        }
                    }
                } else {
                    client.from("profile_likes").insert(mapOf(
                        "liker_uid" to likerId,
                        "profile_uid" to profileId
                    ))
                }
            }
            Result.Success(!isLiked)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
}
