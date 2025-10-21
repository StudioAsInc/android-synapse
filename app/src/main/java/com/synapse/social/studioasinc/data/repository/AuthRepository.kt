package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository {
    
    private val client = SupabaseClient.client
    
    suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            val result = client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(result.user?.id ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val result = client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(result.user?.id ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signOut(): Result<Unit> {
        return try {
            client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }
    
    fun getCurrentUserEmail(): String? {
        return client.auth.currentUserOrNull()?.email
    }
    
    fun isUserLoggedIn(): Boolean {
        return client.auth.currentUserOrNull() != null
    }
    
    fun observeAuthState(): Flow<Boolean> {
        return client.auth.sessionStatus.map { status ->
            client.auth.currentUserOrNull() != null
        }
    }
}