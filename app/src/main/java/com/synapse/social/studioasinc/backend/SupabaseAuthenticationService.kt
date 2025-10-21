package com.synapse.social.studioasinc.backend

import io.github.jan.tennert.supabase.gotrue.auth
import io.github.jan.tennert.supabase.gotrue.providers.builtin.Email
import io.github.jan.tennert.supabase.gotrue.user.UserInfo
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.backend.interfaces.ISupabaseAuthenticationService

/**
 * Supabase Authentication Service
 * Handles user authentication operations using Supabase Auth
 */
class SupabaseAuthenticationService : ISupabaseAuthenticationService {
    
    private val auth = SupabaseClient.client.auth
    
    override suspend fun getCurrentUser(): UserInfo? {
        return auth.currentUserOrNull()
    }
    
    override suspend fun signIn(email: String, password: String): UserInfo {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        return auth.currentUserOrNull() ?: throw Exception("Sign in failed")
    }
    
    override suspend fun signUp(email: String, password: String): UserInfo {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        return auth.currentUserOrNull() ?: throw Exception("Sign up failed")
    }
    
    override suspend fun signOut() {
        auth.signOut()
    }
    
    override suspend fun deleteUser() {
        auth.deleteUser()
    }
    
    fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }
}