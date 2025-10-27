package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Repository for handling authentication operations with Supabase.
 * Provides methods for sign up, sign in, sign out, and user session management.
 */
class AuthRepository {
    
    private val client = SupabaseClient.client
    
    private fun isSupabaseConfigured(): Boolean = SupabaseClient.isConfigured()
    
    /**
     * Register a new user with email and password.
     * @param email User's email address
     * @param password User's password
     * @return Result containing user ID on success, or error on failure
     */
    suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            if (!isSupabaseConfigured()) {
                return Result.failure(Exception("Supabase not configured"))
            }
            val result = client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(client.auth.currentUserOrNull()?.id ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign in an existing user with email and password.
     * @param email User's email address
     * @param password User's password
     * @return Result containing user ID on success, or error on failure
     */
    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            if (!isSupabaseConfigured()) {
                return Result.failure(Exception("Supabase not configured"))
            }
            
            if (email.isBlank() || password.isBlank()) {
                return Result.failure(Exception("Email and password cannot be empty"))
            }
            
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(client.auth.currentUserOrNull()?.id ?: "")
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Sign in failed for email: $email", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sign out the current user.
     * @return Result indicating success or failure
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            if (!isSupabaseConfigured()) {
                return Result.success(Unit)
            }
            client.auth.signOut()
            android.util.Log.d("AuthRepository", "User signed out successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Sign out failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get the current authenticated user's ID.
     * @return User ID if authenticated, null otherwise
     */
    fun getCurrentUserId(): String? {
        return if (isSupabaseConfigured()) {
            try {
                client.auth.currentUserOrNull()?.id
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Failed to get current user ID", e)
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Get the current user's UID from the users table (not the auth UUID)
     * This is needed for RLS policies that check against the users.uid field
     */
    suspend fun getCurrentUserUid(): String? {
        return if (isSupabaseConfigured()) {
            try {
                val authId = client.auth.currentUserOrNull()?.id ?: return null
                val result = client.from("users")
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("uid")) {
                        filter {
                            eq("id", authId)
                        }
                    }
                    .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()
                
                result?.get("uid")?.toString()?.removeSurrounding("\"")
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Failed to get user UID", e)
                null
            }
        } else {
            null
        }
    }
    
    fun getCurrentUserEmail(): String? {
        return if (isSupabaseConfigured()) {
            try {
                client.auth.currentUserOrNull()?.email
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    fun isUserLoggedIn(): Boolean {
        return if (isSupabaseConfigured()) {
            try {
                client.auth.currentUserOrNull() != null
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }
    
    fun observeAuthState(): Flow<Boolean> {
        return if (isSupabaseConfigured()) {
            try {
                client.auth.sessionStatus.map { status ->
                    client.auth.currentUserOrNull() != null
                }
            } catch (e: Exception) {
                flowOf(false)
            }
        } else {
            flowOf(false)
        }
    }
}