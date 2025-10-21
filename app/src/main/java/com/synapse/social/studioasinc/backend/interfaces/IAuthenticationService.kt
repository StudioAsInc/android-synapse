package com.synapse.social.studioasinc.backend.interfaces

import com.synapse.social.studioasinc.backend.User

/**
 * Authentication Service Interface
 */
interface IAuthenticationService {
    suspend fun signUp(email: String, password: String): Result<User>
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signOut(): Result<Unit>
    fun getCurrentUser(): User?
    fun getCurrentUserId(): String?
    suspend fun updatePassword(newPassword: String): Result<Unit>
    suspend fun updateEmail(newEmail: String): Result<Unit>
}