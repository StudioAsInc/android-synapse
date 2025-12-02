package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.SynapseApplication
import com.synapse.social.studioasinc.data.Result
import com.synapse.social.studioasinc.util.ErrorHandler
import com.synapse.social.studioasinc.util.RetryPolicy
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class FollowRepository {

    private val client = SupabaseClient.client
    private val retryPolicy = RetryPolicy()

    suspend fun isFollowing(followerId: String, followingId: String): Result<Boolean> {
        return try {
            val result = retryPolicy.executeWithRetry {
                client.from("follows")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            eq("follower_id", followerId)
                            eq("following_id", followingId)
                        }
                    }
                    .decodeList<Map<String, Any>>()
            }
            Result.Success(result.isNotEmpty())
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun toggleFollow(followerId: String, followingId: String): Result<Boolean> {
        return try {
            val isFollowing = when (val result = isFollowing(followerId, followingId)) {
                is Result.Success -> result.data
                is Result.Error -> throw result.exception
                else -> false
            }

            retryPolicy.executeWithRetry {
                if (isFollowing) {
                    client.from("follows").delete {
                        filter {
                            eq("follower_id", followerId)
                            eq("following_id", followingId)
                        }
                    }
                } else {
                    client.from("follows").insert(buildJsonObject {
                        put("follower_id", followerId)
                        put("following_id", followingId)
                    })
                }
            }
            Result.Success(!isFollowing)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
}
