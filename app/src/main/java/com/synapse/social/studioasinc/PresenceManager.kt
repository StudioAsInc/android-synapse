
package com.synapse.social.studioasinc

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.presence.Presence
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object PresenceManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val channel = Supabase.client.realtime.channel("presence")

    @JvmStatic
    fun goOnline(uid: String) {
        scope.launch {
            try {
                channel.subscribe()
                channel.presence.track(uid)
                channel.presence.onJoin {
                    updateUserStatus(it.userId, "online")
                }
                channel.presence.onLeave {
                    updateUserStatus(it.userId, System.currentTimeMillis().toString())
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    @JvmStatic
    fun goOffline(uid: String) {
        scope.launch {
            try {
                channel.presence.untrack()
                channel.unsubscribe()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun updateUserStatus(uid: String, status: String) {
        scope.launch {
            try {
                Supabase.client.postgrest["profiles"].update(
                    {
                        set("status", status)
                    }
                ) {
                    filter("id", "eq", uid)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    @JvmStatic
    fun setActivity(uid: String, activity: String) {
        scope.launch {
            try {
                Supabase.client.postgrest["profiles"].update(
                    {
                        set("activity", activity)
                    }
                ) {
                    filter("id", "eq", uid)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    @JvmStatic
    fun clearActivity(uid: String) {
        scope.launch {
            try {
                Supabase.client.postgrest["profiles"].update(
                    {
                        set("activity", null)
                    }
                ) {
                    filter("id", "eq", uid)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
