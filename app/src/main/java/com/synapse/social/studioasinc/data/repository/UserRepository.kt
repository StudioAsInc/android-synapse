package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.User
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


class UserRepository {
    
    private val client = SupabaseClient.client
    
    suspend fun createUser(user: User): Result<User> {
        return try {
            val result = client.from("users").insert(user) {
                select()
            }.decodeSingle<User>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserById(userId: String): Result<User?> {
        return try {
            val user = client.from("users")
                .select()
                .eq("uid", userId)
                .decodeSingleOrNull<User>()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserByUsername(username: String): Result<User?> {
        return try {
            val user = client.from("users")
                .select()
                .eq("username", username)
                .decodeSingleOrNull<User>()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUser(userId: String, updates: Map<String, Any?>): Result<User> {
        return try {
            val result = client.from("users")
                .update(updates) {
                    select()
                }
                .eq("uid", userId)
                .decodeSingle<User>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUserStatus(userId: String, status: String): Result<Unit> {
        return try {
            client.from("users")
                .update(mapOf("status" to status, "last_seen" to "now()"))
                .eq("uid", userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val users = client.from("users")
                .select()
                .or("username.ilike.%$query%,nickname.ilike.%$query%")
                .decodeList<User>()
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeUser(userId: String): Flow<User?> = flow {
        try {
            val user = client.from("users")
                .select()
                .eq("uid", userId)
                .decodeSingleOrNull<User>()
            emit(user)
        } catch (e: Exception) {
            emit(null)
        }
    }
}