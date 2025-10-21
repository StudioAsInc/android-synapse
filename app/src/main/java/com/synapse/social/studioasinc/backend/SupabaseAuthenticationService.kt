package com.synapse.social.studioasinc.backend

import io.github.jan.tennert.supabase.gotrue.auth
import io.github.jan.tennert.supabase.gotrue.providers.builtin.Email
import io.github.jan.tennert.supabase.gotrue.user.UserInfo
import com.synapse.social.studioasinc.SupabaseClient

/**
 * Supabase Authentication Service
 * Handles user authentication operations using Supabase Auth
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
        auth.deleteUser()
    }
    
    fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }
}