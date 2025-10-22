package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class AuthRepository {
    
    private val client = SupabaseClient.client
    
    private fun isSupabaseConfigured(): Boolean = SupabaseClient.isConfigured()
    
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
    
    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            if (!isSupabaseConfigured()) {
                return Result.failure(Exception("Supabase not configured"))
            }
            val result = client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(client.auth.currentUserOrNull()?.id ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signOut(): Result<Unit> {
        return try {
            if (!isSupabaseConfigured()) {
                return Result.success(Unit)
            }
            client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getCurrentUserId(): String? {
        return if (isSupabaseConfigured()) {
            try {
                client.auth.currentUserOrNull()?.id
            } catch (e: Exception) {
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