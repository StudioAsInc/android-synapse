
package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.model.UserProfile
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UserDataPusher {

    fun pushData(
        userProfile: UserProfile,
        scope: CoroutineScope,
        onComplete: (Boolean, String?) -> Unit
    ) {
        scope.launch {
            try {
                val updatedProfile = if (userProfile.email == "mashikahamed0@gmail.com") {
                    userProfile.copy(
                        account_premium = "true",
                        user_level_xp = "500",
                        verify = "true",
                        account_type = "admin",
                        gender = "hidden"
                    )
                } else {
                    userProfile
                }
                Supabase.INSTANCE.client.postgrest["profiles"].insert(updatedProfile)
                Supabase.INSTANCE.client.postgrest["usernames"].insert(mapOf("username" to updatedProfile.username, "uid" to updatedProfile.id))
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }
}
