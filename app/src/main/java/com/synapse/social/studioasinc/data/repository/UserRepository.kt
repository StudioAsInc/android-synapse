package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.UserProfile
import io.github.jan.supabase.postgrest.from

class UserRepository {
    
    private val client = SupabaseClient.client
    
    suspend fun getUserById(userId: String): Result<UserProfile?> {
        return try {
            android.util.Log.d("UserRepository", "Fetching user by ID: $userId")
            
            // First check if Supabase is configured
            if (!SupabaseClient.isConfigured()) {
                android.util.Log.e("UserRepository", "Supabase is not configured properly")
                return Result.failure(Exception("Supabase not configured. Please update gradle.properties with your Supabase credentials."))
            }
            
            val user = client.from("users")
                .select() {
                    filter {
                        eq("uid", userId)
                    }
                }
                .decodeSingleOrNull<UserProfile>()
            
            android.util.Log.d("UserRepository", "User fetch result: $user")
            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to fetch user by ID: $userId", e)
            
            // Provide more specific error messages
            val errorMessage = when {
                e.message?.contains("relation \"users\" does not exist", ignoreCase = true) == true -> 
                    "Database table 'users' does not exist. Please create the users table in your Supabase database."
                e.message?.contains("connection", ignoreCase = true) == true -> 
                    "Cannot connect to Supabase. Check your internet connection and Supabase configuration."
                e.message?.contains("unauthorized", ignoreCase = true) == true -> 
                    "Unauthorized access to Supabase. Check your API key and RLS policies."
                else -> "Database error: ${e.message}"
            }
            
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun getUserByUsername(username: String): Result<UserProfile?> {
        return try {
            val user = client.from("users")
                .select() {
                    filter {
                        eq("username", username)
                    }
                }
                .decodeSingleOrNull<UserProfile>()
            
            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to fetch user by username: $username", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateUser(user: UserProfile): Result<UserProfile> {
        return try {
            client.from("users")
                .update(user) {
                    filter {
                        eq("uid", user.uid)
                    }
                }
            
            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to update user: ${user.uid}", e)
            Result.failure(e)
        }
    }
    
    suspend fun searchUsers(query: String, limit: Int = 20): Result<List<UserProfile>> {
        return try {
            val users = client.from("users")
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
            
            Result.success(users)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to search users with query: $query", e)
            Result.failure(e)
        }
    }
    
    suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return try {
            val existingUser = client.from("users")
                .select() {
                    filter {
                        eq("username", username)
                    }
                }
                .decodeSingleOrNull<UserProfile>()
            
            // Return true if username is available (no existing user found)
            Result.success(existingUser == null)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to check username availability: $username", e)
            Result.failure(e)
        }
    }
}