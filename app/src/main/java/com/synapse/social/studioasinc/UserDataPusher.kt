
package com.synapse.social.studioasinc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Supabase-based UserDataPusher that replaces Firebase implementation.
 */
class UserDataPusher {

    private val supabaseUserDataPusher = SupabaseUserDataPusher()

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
        CoroutineScope(Dispatchers.Main).launch {
            supabaseUserDataPusher.pushData(
                username = username,
                nickname = nickname,
                biography = biography,
                thedpurl = thedpurl,
                googleLoginAvatarUri = googleLoginAvatarUri,
                email = email,
                uid = uid,
                onComplete = onComplete
            )
        }
    }
}
