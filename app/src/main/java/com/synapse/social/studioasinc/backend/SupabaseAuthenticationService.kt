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
                // Check if Supabase is configured
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.failure(Exception("Supabase not configured. Please set up your credentials."))
                }
                
                // Clear any existing session first
                try {
                    client.auth.signOut()
                } catch (e: Exception) {
                    // Ignore sign out errors
                }
                
                // Attempt sign up
                val authResult = client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                
                // Verify the user was created and authenticated
                val user = client.auth.currentUserOrNull()
                if (user != null && user.id.isNotEmpty()) {
                    Result.success(User(user.id, user.email ?: ""))
                } else {
                    Result.failure(Exception("Account creation failed"))
                }
            } catch (e: Exception) {
                // Make sure to clear any partial session on error
                try {
                    client.auth.signOut()
                } catch (signOutError: Exception) {
                    // Ignore sign out errors
                }
                Result.failure(Exception("Sign up failed: ${e.message}"))
            }
        }
    }
    
    /**
     * Sign in with email and password
     */
    override suspend fun signIn(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Supabase is configured
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.failure(Exception("Supabase not configured. Please set up your credentials."))
                }
                
                // Clear any existing session first
                try {
                    client.auth.signOut()
                } catch (e: Exception) {
                    // Ignore sign out errors
                }
                
                // Attempt sign in
                val authResult = client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                
                // Verify the user is actually authenticated
                val user = client.auth.currentUserOrNull()
                if (user != null && user.id.isNotEmpty()) {
                    Result.success(User(user.id, user.email ?: ""))
                } else {
                    Result.failure(Exception("Authentication failed - invalid credentials"))
                }
            } catch (e: Exception) {
                // Make sure to clear any partial session on error
                try {
                    client.auth.signOut()
                } catch (signOutError: Exception) {
                    // Ignore sign out errors
                }
                Result.failure(Exception("Sign in failed: ${e.message}"))
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
        return try {
            // Check if Supabase is configured
            if (!SupabaseClient.isConfigured()) {
                return null
            }
            
            val user = client.auth.currentUserOrNull()
            if (user != null && user.id.isNotEmpty()) {
                User(user.id, user.email ?: "")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
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