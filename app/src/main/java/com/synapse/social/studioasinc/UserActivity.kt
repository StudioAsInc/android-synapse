package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.util.SupabaseManager
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object UserActivity {

    @JvmStatic
    fun setActivity(uid: String, activity: String) {
        GlobalScope.launch {
            SupabaseManager.getClient().postgrest.from("users").update(
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
            SupabaseManager.getClient().postgrest.from("users").update(
                {
                    set("activity", null as String?)
                }
            ) {
                filter {
                    eq("id", uid)
                }
            }
        }
    }
}