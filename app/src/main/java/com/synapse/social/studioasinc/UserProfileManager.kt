package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.model.User
import io.github.jan.supabase.postgrest.query.PostgrestFilterBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages user profile operations and caching
 */
object UserProfileManager {

    private val dbService = SupabaseDatabaseService()
    private val authService = SupabaseAuthenticationService()
    private val profileCache = mutableMapOf<String, User>()

    /**
     * Gets a user profile by UID, with caching
     */
    suspend fun getUserProfile(uid: String): User? {
        // Check cache first
        profileCache[uid]?.let { return it }
        
        return try {
            val result = dbService.selectById("users", uid, "uid")
            if (result != null) {
                val user = User(
                    uid = result["uid"] as? String ?: "",
                    username = result["username"] as? String ?: "",
                    email = result["email"] as? String ?: "",
                    displayName = result["display_name"] as? String ?: "",
                    profileImageUrl = result["profile_image_url"] as? String,
                    bio = result["bio"] as? String,
                    followersCount = (result["followers_count"] as? Number)?.toInt() ?: 0,
                    followingCount = (result["following_count"] as? Number)?.toInt() ?: 0,
                    postsCount = (result["posts_count"] as? Number)?.toInt() ?: 0
                )
                // Cache the user
                profileCache[uid] = user
                user
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Updates a user profile
     */
    suspend fun updateUserProfile(uid: String, updates: Map<String, Any?>): Boolean {
        return try {
            dbService.update("users", updates) {
                eq("uid", uid)
            }
            // Clear cache for this user
            profileCache.remove(uid)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets the current user's profile
     */
    suspend fun getCurrentUserProfile(): User? {
        val currentUid = authService.getCurrentUserId() ?: return null
        return getUserProfile(currentUid)
    }

    /**
     * Updates the current user's profile
     */
    suspend fun updateCurrentUserProfile(updates: Map<String, Any?>): Boolean {
        val currentUid = authService.getCurrentUserId() ?: return false
        return updateUserProfile(currentUid, updates)
    }

    /**
     * Searches for users by username or display name
     */
    suspend fun searchUsers(query: String, limit: Int = 20): List<User> {
        return try {
            val results = dbService.select<Map<String, Any?>>("users") {
                or {
                    ilike("username", "%$query%")
                    ilike("display_name", "%$query%")
                }
            }
            
            results.take(limit).mapNotNull { result ->
                try {
                    User(
                        uid = result["uid"] as? String ?: "",
                        username = result["username"] as? String ?: "",
                        email = result["email"] as? String ?: "",
                        displayName = result["display_name"] as? String ?: "",
                        profileImageUrl = result["profile_image_url"] as? String,
                        bio = result["bio"] as? String,
                        followersCount = (result["followers_count"] as? Number)?.toInt() ?: 0,
                        followingCount = (result["following_count"] as? Number)?.toInt() ?: 0,
                        postsCount = (result["posts_count"] as? Number)?.toInt() ?: 0
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Gets multiple user profiles by UIDs
     */
    suspend fun getUserProfiles(uids: List<String>): List<User> {
        return try {
            val results = dbService.select<Map<String, Any?>>("users") {
                `in`("uid", uids)
            }
            
            results.mapNotNull { result ->
                try {
                    val user = User(
                        uid = result["uid"] as? String ?: "",
                        username = result["username"] as? String ?: "",
                        email = result["email"] as? String ?: "",
                        displayName = result["display_name"] as? String ?: "",
                        profileImageUrl = result["profile_image_url"] as? String,
                        bio = result["bio"] as? String,
                        followersCount = (result["followers_count"] as? Number)?.toInt() ?: 0,
                        followingCount = (result["following_count"] as? Number)?.toInt() ?: 0,
                        postsCount = (result["posts_count"] as? Number)?.toInt() ?: 0
                    )
                    // Cache the user
                    profileCache[user.uid] = user
                    user
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Clears the profile cache
     */
    fun clearCache() {
        profileCache.clear()
    }

    /**
     * Clears cache for a specific user
     */
    fun clearUserCache(uid: String) {
        profileCache.remove(uid)
    }

    /**
     * Checks if a user exists
     */
    suspend fun userExists(uid: String): Boolean {
        return try {
            val result = dbService.selectById("users", uid, "uid", "uid")
            result != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if a username is available
     */
    suspend fun isUsernameAvailable(username: String): Boolean {
        return try {
            val results = dbService.select<Map<String, Any?>>("users") {
                eq("username", username)
            }
            results.isEmpty()
        } catch (e: Exception) {
            false
        }
    }
}