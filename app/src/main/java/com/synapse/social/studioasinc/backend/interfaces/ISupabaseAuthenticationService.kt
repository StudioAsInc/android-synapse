package com.synapse.social.studioasinc.backend.interfaces

import io.github.jan.tennert.supabase.gotrue.user.UserInfo

/**
 * Defines the contract for Supabase authentication operations.
 */
interface ISupabaseAuthenticationService {
    /**
     * Gets the currently signed-in user.
     * @return The current user, or null if no user is signed in.
     */
    suspend fun getCurrentUser(): UserInfo?

    /**
     * Signs in a user with the given email and password.
     */
    suspend fun signIn(email: String, password: String): UserInfo

    /**
     * Creates a new user account with the given email and password.
     */
    suspend fun signUp(email: String, password: String): UserInfo

    /**
     * Signs out the current user.
     */
    suspend fun signOut()

    /**
     * Deletes the currently signed-in user's account.
     */
    suspend fun deleteUser()
}