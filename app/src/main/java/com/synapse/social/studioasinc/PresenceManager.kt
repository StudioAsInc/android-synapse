package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.util.SupabaseManager
import kotlinx.coroutines.GlobalScope
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
        GlobalScope.launch {
            SupabaseManager.trackUserPresence(uid, "online")
        }
    }

    /**
     * Explicitly sets the user's status to a timestamp (last seen).
     * @param uid The Supabase UID of the current user.
     */
    @JvmStatic
    fun goOffline(uid: String) {
        GlobalScope.launch {
            SupabaseManager.trackUserPresence(uid, System.currentTimeMillis().toString())
        }
    }

    /**
     * Sets status to "chatting_with_<otherUserUid>".
     * @param currentUserUid The UID of the current user.
     * @param otherUserUid The UID of the user they are chatting with.
     */
    @JvmStatic
    fun setChattingWith(currentUserUid: String, otherUserUid: String) {
        UserActivity.setActivity(currentUserUid, "chatting_with_$otherUserUid")
    }

    /**
     * Reverts the user's status back to "online".
     * @param currentUserUid The UID of the current user.
     */
    @JvmStatic
    fun stopChatting(currentUserUid: String) {
        UserActivity.clearActivity(currentUserUid)
    }

    @JvmStatic
    fun setActivity(uid: String, activity: String) {
        UserActivity.setActivity(uid, activity)
    }
}