package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.SynapseApplication
import com.synapse.social.studioasinc.data.Result
import com.synapse.social.studioasinc.util.ErrorHandler
import com.synapse.social.studioasinc.util.RetryPolicy
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class AuthRepository {
    
    private val client = SupabaseClient.client
    private val retryPolicy = RetryPolicy()
    
    private fun isSupabaseConfigured(): Boolean = SupabaseClient.isConfigured()
    
    suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            if (!isSupabaseConfigured()) {
                return Result.Error(Exception("Supabase not configured"), "Supabase not configured")
            }
            retryPolicy.executeWithRetry {
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
            }
            Result.Success(client.auth.currentUserOrNull()?.id ?: "")
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            if (!isSupabaseConfigured()) {
                return Result.Error(Exception("Supabase not configured"), "Supabase not configured")
            }
            
            if (email.isBlank() || password.isBlank()) {
                return Result.Error(Exception("Email and password cannot be empty"), "Email and password cannot be empty")
            }
            
            retryPolicy.executeWithRetry {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            }
            Result.Success(client.auth.currentUserOrNull()?.id ?: "")
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    suspend fun signOut(): Result<Unit> {
        return try {
            if (!isSupabaseConfigured()) {
                return Result.Success(Unit)
            }
            retryPolicy.executeWithRetry {
                client.auth.signOut()
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
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
    
    suspend fun getCurrentUserUid(): String? {
        return if (isSupabaseConfigured()) {
            try {
                val authId = client.auth.currentUserOrNull()?.id ?: return null
                
                val result = retryPolicy.executeWithRetry {
                    client.from("users")
                        .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("uid")) {
                            filter {
                                eq("uid", authId)
                            }
                        }
                        .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()
                }
                
                if (result != null) {
                    authId
                } else {
                    null
                }
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
