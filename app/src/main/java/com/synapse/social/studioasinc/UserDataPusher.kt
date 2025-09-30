package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.backend.IAuthenticationService
import com.synapse.social.studioasinc.backend.IDatabaseService
import com.synapse.social.studioasinc.backend.ICompletionListener
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import java.util.Calendar
import java.util.HashMap

class UserDataPusher(
    private val authService: IAuthenticationService,
    private val dbService: IDatabaseService
) {

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
        createUserMap["uid"] = uid
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
        // addOneSignalPlayerIdToMap(createUserMap) // This needs to be handled

        val main = dbService.getReference("skyline")
        val userRef = main.child("users").child(uid)
        dbService.updateChildren(userRef, createUserMap, object : ICompletionListener<Unit> {
            override fun onComplete(result: Unit?, error: Exception?) {
                if (error == null) {
                    val map = HashMap<String, Any>()
                    map["uid"] = uid
                    map["email"] = email
                    map["username"] = username
                    val pushusername = dbService.getReference("synapse/username")
                    val usernameRef = pushusername.child(username)
                    dbService.updateChildren(usernameRef, map, object : ICompletionListener<Unit> {
                        override fun onComplete(result: Unit?, error: Exception?) {
                            if (error == null) {
                                onComplete(true, null)
                            } else {
                                onComplete(false, error.message)
                            }
                        }
                    })
                } else {
                    onComplete(false, error.message)
                }
            }
        })
    }
}