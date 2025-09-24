package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.Supabase.client
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages user online presence in Supabase, writing to the correct database path.
 * Handles online, offline (timestamp), and chat statuses.
 */
object PresenceManager {

    /**
     * Sets user status to "online".
     * @param uid The Supabase UID of the current user.
     */
    @JvmStatic
    fun goOnline(uid: String) {
        updateUserStatus(uid, "online")
    }

    /**
     * Explicitly sets the user's status to a timestamp (last seen).
     * @param uid The Supabase UID of the current user.
     */
    @JvmStatic
    fun goOffline(uid: String) {
        updateUserStatus(uid, System.currentTimeMillis().toString())
    }

    /**
     * Sets status to "chatting_with_<otherUserUid>".
     * @param currentUserUid The UID of the current user.
     * @param otherUserUid The UID of the user they are chatting with.
     */
    @JvmStatic
    fun setChattingWith(currentUserUid: String, otherUserUid: String) {
        updateUserStatus(currentUserUid, "chatting_with_$otherUserUid")
    }

    /**
     * Reverts the user's status back to "online".
     * @param currentUserUid The UID of the current user.
     */
    @JvmStatic
    fun stopChatting(currentUserUid: String) {
        updateUserStatus(currentUserUid, "online")
    }

    @JvmStatic
    fun setActivity(uid: String, activity: String) {
        updateUserStatus(uid, activity)
    }

    private fun updateUserStatus(uid: String, status: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.postgrest["profiles"].update({
                    set("status", status)
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
