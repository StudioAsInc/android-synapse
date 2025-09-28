package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService

/**
 * Manages user online presence, writing to the correct database path.
 * Handles online, offline (timestamp), and chat statuses.
 */
object PresenceManager {

    private val dbService: IDatabaseService = SupabaseDatabaseService()
    private val usersRef = dbService.getReference("skyline/users")

    /**
     * Returns the specific database reference for a user's status.
     * Path: /skyline/users/{uid}/status
     */
    private fun getUserStatusRef(uid: String) = usersRef.child(uid).child("status")

    /**
     * Sets user status to "online".
     * @param uid The UID of the current user.
     */
    @JvmStatic
    fun goOnline(uid: String) {
        val statusRef = getUserStatusRef(uid)
        statusRef.setValue("online") { _, _ -> }
        /*
         * TODO: onDisconnect functionality needs to be re-implemented using Supabase Realtime and Presence.
         *
         * Firebase's `onDisconnect` is not directly available in Supabase. The recommended approach is:
         * 1.  **Use Supabase Presence:** When a user comes online, they should join a specific Realtime channel
         *     (e.g., a "presence" channel) and track their state.
         * 2.  **Server-side Logic (Edge Function):** When the user's client disconnects abruptly, the Supabase
         *     Realtime service will automatically detect this and broadcast a `LEAVE` event to the channel.
         * 3.  **Create a Supabase Edge Function:** This function would subscribe to presence events on the server.
         *     When it receives a `LEAVE` event for a user, it should execute logic to update that user's
         *     status in the database to the current timestamp (e.g., `System.currentTimeMillis()`).
         *
         * This provides a robust way to handle user presence and last-seen status.
         * See Supabase docs for Realtime and Edge Functions for implementation details.
         */
    }

    /**
     * Explicitly sets the user's status to a timestamp (last seen).
     * @param uid The UID of the current user.
     */
    @JvmStatic
    fun goOffline(uid: String) {
        getUserStatusRef(uid).setValue(System.currentTimeMillis().toString()) { _, _ -> }
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