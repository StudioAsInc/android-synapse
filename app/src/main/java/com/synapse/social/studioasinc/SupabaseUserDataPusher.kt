package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Supabase implementation of UserDataPusher.
 * Handles user profile creation and updates using Supabase database.
 */
class SupabaseUserDataPusher {

    private val dbService = SupabaseDatabaseService()

    suspend fun pushData(
        username: String,
        nickname: String,
        biography: String,
        thedpurl: String,
        googleLoginAvatarUri: String?,
        email: String,
        uid: String,
        onComplete: (Boolean, String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val getJoinTime = Calendar.getInstance()
            val createUserMap = mutableMapOf<String, Any?>()
            
            createUserMap["uid"] = uid
            createUserMap["email"] = email
            createUserMap["profile_cover_image"] = null

            if (googleLoginAvatarUri != null) {
                createUserMap["avatar"] = googleLoginAvatarUri
            } else {
                createUserMap["avatar"] = if (thedpurl == "null") null else thedpurl
            }

            createUserMap["avatar_history_type"] = "local"
            createUserMap["username"] = username
            createUserMap["nickname"] = if (nickname.isEmpty()) null else nickname
            createUserMap["biography"] = if (biography.isEmpty()) null else biography

            // Set special privileges for admin email
            if (email == "mashikahamed0@gmail.com") {
                createUserMap["account_premium"] = true
                createUserMap["user_level_xp"] = 500
                createUserMap["verify"] = true
                createUserMap["account_type"] = "admin"
                createUserMap["gender"] = "hidden"
            } else {
                createUserMap["account_premium"] = false
                createUserMap["user_level_xp"] = 500
                createUserMap["verify"] = false
                createUserMap["account_type"] = "user"
                createUserMap["gender"] = "hidden"
            }

            createUserMap["banned"] = false
            createUserMap["status"] = "online"
            createUserMap["join_date"] = getJoinTime.time
            createUserMap["created_at"] = getJoinTime.time
            createUserMap["updated_at"] = getJoinTime.time

            // Insert or update user in users table
            val userResult = dbService.upsert("users", createUserMap)

            // Create username registry entry
            val usernameMap = mapOf(
                "username" to username,
                "uid" to uid,
                "email" to email,
                "user_id" to userResult["id"]
            )

            dbService.upsert("username_registry", usernameMap)

            withContext(Dispatchers.Main) {
                onComplete(true, null)
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onComplete(false, e.message)
            }
        }
    }
}