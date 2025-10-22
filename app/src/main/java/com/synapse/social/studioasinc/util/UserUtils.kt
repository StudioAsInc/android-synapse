package com.synapse.social.studioasinc.util

import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Utility class for user-related operations with Supabase backend.
 * Provides helper methods for user data retrieval and management.
 */
object UserUtils {
    
    interface Callback<T> {
        fun onSuccess(result: T)
        fun onError(error: String)
    }
    
    /**
     * Get user display name (nickname if available, otherwise username)
     */
    fun getUserDisplayName(userId: String, callback: Callback<String>) {
        if (userId.isBlank()) {
            callback.onError("Invalid user ID")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository()
                val userResult = userRepository.getUserById(userId)
                
                withContext(Dispatchers.Main) {
                    userResult.fold(
                        onSuccess = { user ->
                            if (user != null) {
                                // Return nickname if available, otherwise username
                                val displayName = user.nickname?.takeIf { it.isNotBlank() } ?: user.username
                                
                                if (displayName != null) {
                                    callback.onSuccess(displayName)
                                } else {
                                    callback.onError("User display name not found")
                                }
                            } else {
                                callback.onError("User not found")
                            }
                        },
                        onFailure = { error ->
                            callback.onError("Error fetching user: ${error.message}")
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Error fetching user: ${e.message}")
                }
            }
        }
    }

    /**
     * Suspend version of getUserDisplayName for use in coroutines
     */
    suspend fun getUserDisplayName(userId: String): Result<String> {
        return try {
            if (userId.isBlank()) {
                Result.failure(Exception("Invalid user ID"))
            } else {
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository()
                val userResult = userRepository.getUserById(userId)
                
                userResult.fold(
                    onSuccess = { user ->
                        if (user != null) {
                            val displayName = user.nickname?.takeIf { it.isNotBlank() } ?: user.username
                            if (displayName != null) {
                                Result.success(displayName)
                            } else {
                                Result.failure(Exception("User display name not found"))
                            }
                        } else {
                            Result.failure(Exception("User not found"))
                        }
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user profile image URL
     */
    fun getUserProfileImage(userId: String, callback: Callback<String?>) {
        if (userId.isBlank()) {
            callback.onError("Invalid user ID")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository()
                val userResult = userRepository.getUserById(userId)
                
                withContext(Dispatchers.Main) {
                    userResult.fold(
                        onSuccess = { user ->
                            if (user != null) {
                                callback.onSuccess(user.profileImageUrl)
                            } else {
                                callback.onError("User not found")
                            }
                        },
                        onFailure = { error ->
                            callback.onError("Error fetching user profile: ${error.message}")
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Error fetching user profile: ${e.message}")
                }
            }
        }
    }

    /**
     * Check if a username is available
     */
    suspend fun isUsernameAvailable(username: String): Result<Boolean> {
        return try {
            if (username.isBlank()) {
                Result.failure(Exception("Username cannot be empty"))
            } else {
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository()
                val userResult = userRepository.getUserByUsername(username)
                userResult.fold(
                    onSuccess = { user -> Result.success(user == null) },
                    onFailure = { error -> Result.failure(error) }
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user by username
     */
    suspend fun getUserByUsername(username: String): Result<com.synapse.social.studioasinc.model.User?> {
        return try {
            if (username.isBlank()) {
                Result.failure(Exception("Username cannot be empty"))
            } else {
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository()
                userRepository.getUserByUsername(username)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Format user count (followers, following, etc.)
     */
    fun formatUserCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }

    /**
     * Validate username format
     */
    fun isValidUsername(username: String): Boolean {
        return username.matches(Regex("^[a-zA-Z0-9_]{3,20}$"))
    }

    /**
     * Generate a suggested username from display name
     */
    fun generateSuggestedUsername(displayName: String): String {
        return displayName
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(20)
            .ifBlank { "user_${System.currentTimeMillis() % 10000}" }
    }
}