package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Supabase Authentication Service
 * Handles user authentication using Supabase Auth
 */
class SupabaseAuthenticationService : com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService {
    
    private val client = SupabaseClient.client
    
    /**
     * Sign up a new user with email and password
     */
    override suspend fun signUp(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                
                val user = client.auth.currentUserOrNull()
                if (user != null) {
                    Result.success(User(user.id, user.email ?: ""))
                } else {
                    Result.failure(Exception("Failed to create user"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Sign in with email and password
     */
    override suspend fun signIn(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                
                val user = client.auth.currentUserOrNull()
                if (user != null) {
                    Result.success(User(user.id, user.email ?: ""))
                } else {
                    Result.failure(Exception("Failed to sign in"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Sign out the current user
     */
    override suspend fun signOut(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.signOut()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get current user
     */
    override fun getCurrentUser(): User? {
        val user = client.auth.currentUserOrNull()
        return user?.let { User(it.id, it.email ?: "") }
    }
    
    /**
     * Get current user ID
     */
    override fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }
    
    /**
     * Update user password
     */
    override suspend fun updatePassword(newPassword: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.updateUser {
                    password = newPassword
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update user email
     */
    override suspend fun updateEmail(newEmail: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.updateUser {
                    email = newEmail
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

/**
 * User data class
 */
data class User(
    val id: String,
    val email: String
)