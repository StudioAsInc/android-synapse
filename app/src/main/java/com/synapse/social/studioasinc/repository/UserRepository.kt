package com.synapse.social.studioasinc.repository

import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.models.User
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Repository for fetching and caching user data.
 */
object UserRepository {

    private val userCache = mutableMapOf<String, User>()
    private val dbService = SupabaseDatabaseService()

    /**
     * Gets a user from the cache or fetches it from Supabase.
     *
     * @param uid The ID of the user to get.
     * @return A [User] object or null if the user is not found.
     */
    suspend fun getUser(uid: String): User? {
        if (userCache.containsKey(uid)) {
            return userCache[uid]
        }

        return try {
            val user = dbService.selectSingle<User>("users", "*")
            user?.let {
                userCache[uid] = it
                it
            }
        } catch (e: Exception) {
            null
        }
    }
}
