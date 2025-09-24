
package com.synapse.social.studioasinc

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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

        val main = FirebaseDatabase.getInstance().getReference("skyline")
        main.child("users").child(uid).updateChildren(createUserMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val map = HashMap<String, Any>()
                    map["uid"] = uid
                    map["email"] = email
                    map["username"] = username
                    val pushusername = FirebaseDatabase.getInstance().getReference("synapse/username")
                    pushusername.child(username).updateChildren(map)
                        .addOnCompleteListener { pushTask ->
                            if (pushTask.isSuccessful) {
                                onComplete(true, null)
                            } else {
                                onComplete(false, pushTask.exception?.message)
                            }
                        }
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }
}
