package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.util.SupabaseManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object UserActivity {

    @JvmStatic
    fun setActivity(uid: String, activity: String) {
        GlobalScope.launch {
            SupabaseManager.getClient().postgrest["users"].update(
                {
                    set("activity", activity)
                }
            ) {
                filter {
                    eq("id", uid)
                }
            }
        }
    }

    @JvmStatic
    fun clearActivity(uid: String) {
        GlobalScope.launch {
            SupabaseManager.getClient().postgrest["users"].update(
                {
                    set("activity", null)
                }
            ) {
                filter {
                    eq("id", uid)
                }
            }
        }
    }
}