package com.synapse.social.studioasinc.backend

import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import com.synapse.social.studioasinc.SupabaseClient

/**
 * Service for handling Supabase authentication operations.
 * Provides methods for sign up, sign in, sign out, and user management.
 */
class SupabaseAuthenticationService {

    private val client = SupabaseClient.client

    /**
     * Signs up a new user with email and password.
     * @param email User's email address
     * @param password User's password
     * @return UserInfo object containing user details
     * @throws Exception if sign up fails
     */
    suspend fun signUp(email: String, password: String): UserInfo? {
        return client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }.user
    }

    /**
     * Signs in an existing user with email and password.
     * @param email User's email address
     * @param password User's password
     * @return UserInfo object containing user details
     * @throws Exception if sign in fails
     */
    suspend fun signIn(email: String, password: String): UserInfo? {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        return client.auth.currentUserOrNull()
    }

    /**
     * Signs out the current user.
     * @throws Exception if sign out fails
     */
    suspend fun signOut() {
        client.auth.signOut()
    }

    /**
     * Gets the current authenticated user.
     * @return UserInfo object if user is authenticated, null otherwise
     */
    fun getCurrentUser(): UserInfo? {
        return client.auth.currentUserOrNull()
    }

    /**
     * Gets the current user's ID.
     * @return User ID string if authenticated, null otherwise
     */
    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    /**
     * Checks if a user is currently authenticated.
     * @return true if user is authenticated, false otherwise
     */
    fun isUserAuthenticated(): Boolean {
        return client.auth.currentUserOrNull() != null
    }

    /**
     * Sends a password reset email to the specified email address.
     * @param email Email address to send reset link to
     * @throws Exception if password reset fails
     */
    suspend fun resetPassword(email: String) {
        client.auth.resetPasswordForEmail(email)
    }

    /**
     * Updates the current user's password.
     * @param newPassword New password to set
     * @throws Exception if password update fails
     */
    suspend fun updatePassword(newPassword: String) {
        client.auth.updateUser {
            password = newPassword
        }
    }

    /**
     * Updates the current user's email.
     * @param newEmail New email address to set
     * @throws Exception if email update fails
     */
    suspend fun updateEmail(newEmail: String) {
        client.auth.updateUser {
            email = newEmail
        }
    }
}