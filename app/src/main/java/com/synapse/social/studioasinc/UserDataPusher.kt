
package com.synapse.social.studioasinc

// import com.google.firebase.auth.FirebaseAuth
// import com.google.firebase.database.FirebaseDatabase
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

        // TODO(supabase): Implement pushing user data to Supabase.
        // This will involve using the DatabaseService to insert or update the user's record in the `users` table.
        // Also, consider how to handle the `username` uniqueness, which was previously done in a separate `synapse/username` path.
        // You might need a separate table for usernames or use a database function to ensure uniqueness.
        //
        // Example:
        // val dbService = DatabaseService(SynapseApp.supabaseClient)
        // val userRef = dbService.getReference("users/$uid")
        // dbService.setValue(userRef, createUserMap, object : ICompletionListener<Unit> {
        //     override fun onSuccess(result: Unit) {
        //         // Handle username uniqueness check/insertion here
        //         onComplete(true, null)
        //     }
        //     override fun onFailure(error: Exception) {
        //         onComplete(false, error.message)
        //     }
        // })
    }
}
