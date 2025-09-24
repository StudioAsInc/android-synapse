package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.Supabase.client
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object UserActivity {

    @JvmStatic
    fun setActivity(uid: String, activity: String) {
        updateUserActivity(uid, activity)
    }

    @JvmStatic
    fun clearActivity(uid: String) {
        updateUserActivity(uid, null)
    }

    private fun updateUserActivity(uid: String, activity: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.postgrest["profiles"].update({
                    set("activity", activity)
                }) {
                    filter {
                        eq("id", uid)
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
