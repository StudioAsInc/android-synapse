package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.util.SupabaseManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.HashMap

class UserDataPusher {

    fun pushData(
        username: String,
        nickname: String,
        biography: String,
        thedpurl: String,
        googleLoginAvatarUri: String?,
        email: String,
        uid: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val getJoinTime = Calendar.getInstance()
        val createUserMap = HashMap<String, Any>()
        createUserMap["id"] = uid
        createUserMap["email"] = email
        createUserMap["profile_cover_image"] = "null"

        if (googleLoginAvatarUri != null) {
            createUserMap["avatar"] = googleLoginAvatarUri
        } else {
            createUserMap["avatar"] = thedpurl
        }

        createUserMap["avatar_history_type"] = "local"
        createUserMap["username"] = username
        createUserMap["nickname"] = if (nickname.isEmpty()) "null" else nickname
        createUserMap["biography"] = if (biography.isEmpty()) "null" else biography

        if (email == "mashikahamed0@gmail.com") {
            createUserMap["account_premium"] = "true"
            createUserMap["user_level_xp"] = "500"
            createUserMap["verify"] = "true"
            createUserMap["account_type"] = "admin"
            createUserMap["gender"] = "hidden"
        } else {
            createUserMap["account_premium"] = "false"
            createUserMap["user_level_xp"] = "500"
            createUserMap["verify"] = "false"
            createUserMap["account_type"] = "user"
            createUserMap["gender"] = "hidden"
        }

        createUserMap["banned"] = "false"
        createUserMap["status"] = "online"
        createUserMap["join_date"] = getJoinTime.timeInMillis.toString()

        GlobalScope.launch {
            try {
                SupabaseManager.getClient().postgrest.from("users").insert(createUserMap)
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }
}