package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.SynapseApplication
import com.synapse.social.studioasinc.data.Result
import com.synapse.social.studioasinc.model.UserProfile
import com.synapse.social.studioasinc.util.ErrorHandler
import com.synapse.social.studioasinc.util.RetryPolicy
import io.github.jan.supabase.postgrest.from

class UserRepository {
    
    private val client = SupabaseClient.client
    private val retryPolicy = RetryPolicy()
    
    suspend fun getUserById(userId: String): Result<UserProfile?> {
        return try {
            val user = retryPolicy.executeWithRetry {
                client.from("users")
                    .select() {
                        filter {
                            eq("uid", userId)
                        }
                    }
                    .decodeSingleOrNull<UserProfile>()
            }
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    suspend fun getUserByUsername(username: String): Result<UserProfile?> {
        return try {
            if (username.isBlank()) {
                return Result.Error(Exception("Username cannot be empty"), "Username cannot be empty")
            }
            val user = retryPolicy.executeWithRetry {
                client.from("users")
                    .select() {
                        filter {
                            eq("username", username)
                        }
                    }
                    .decodeSingleOrNull<UserProfile>()
            }
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    suspend fun updateUser(user: UserProfile): Result<UserProfile> {
        return try {
            if (user.uid.isBlank()) {
                return Result.Error(Exception("User ID cannot be empty"), "User ID cannot be empty")
            }
            retryPolicy.executeWithRetry {
                val updateData = mapOf(
                    "username" to user.username,
                    "display_name" to user.displayName,
                    "email" to user.email,
                    "bio" to user.bio,
                    "profile_image_url" to user.profileImageUrl,
                    "followers_count" to user.followersCount,
                    "following_count" to user.followingCount,
                    "posts_count" to user.postsCount,
                    "status" to user.status,
                    "account_type" to user.account_type,
                    "verify" to user.verify,
                    "banned" to user.banned
                )
                client.from("users")
                    .update(updateData) {
                        filter {
                            eq("uid", user.uid)
                        }
                    }
            }
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    suspend fun searchUsers(query: String, limit: Int = 20): Result<List<UserProfile>> {
        return try {
            if (query.isBlank()) {
                return Result.Success(emptyList())
            }
            val users = retryPolicy.executeWithRetry {
                client.from("users")
                    .select() {
                        filter {
                            or {
                                ilike("username", "%$query%")
                                ilike("display_name", "%$query%")
                            }
                        }
                        limit(limit.toLong())
                    }
                    .decodeList<UserProfile>()
            }
            Result.Success(users)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return try {
            val existingUser = retryPolicy.executeWithRetry {
                client.from("users")
                    .select() {
                        filter {
                            eq("username", username)
                        }
                    }
                    .decodeSingleOrNull<UserProfile>()
            }
            Result.Success(existingUser == null)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
}
