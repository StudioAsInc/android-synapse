package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo

/**
 * Supabase Authentication Service
 */
class SupabaseAuthenticationService {
    
    private val auth = SupabaseClient.client.auth
    
    suspend fun getCurrentUser(): UserInfo? {
        return auth.currentUserOrNull()
    }
    
    suspend fun signIn(email: String, password: String): UserInfo {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        return auth.currentUserOrNull() ?: throw Exception("Sign in failed")
    }
    
    suspend fun signUp(email: String, password: String): UserInfo {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        return auth.currentUserOrNull() ?: throw Exception("Sign up failed")
    }
    
    suspend fun signOut() {
        auth.signOut()
    }
    
    suspend fun deleteUser() {
        // Note: User deletion should be handled through Supabase dashboard or custom function
        // For now, we'll just sign out the user
        auth.signOut()
    }
    
    fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }
}