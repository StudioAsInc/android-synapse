package com.synapse.social.studioasinc

// Supabase: import io.github.janbarari.supabase.SupabaseClient
// Supabase: import io.github.janbarari.supabase.PostgrestClient

/**
 * Manages user online presence in Firebase, writing to the correct database path.
 * Handles online, offline (timestamp), and chat statuses.
 */
object PresenceManager {

    // Correct database reference to the 'users' node
    // Supabase: private val supabase = SupabaseClient.getInstance()

    /**
     * Returns the specific database reference for a user's status.
     * Path: /skyline/users/{uid}/status
     */
    // Supabase: private fun getUserStatusRef(uid: String) = supabase.from("users").select("status").eq("uid", uid)

    /**
     * Sets user status to "online".
     * Registers onDisconnect to set a timestamp for last seen.
     * @param uid The Firebase UID of the current user.
     */
    // Supabase: fun goOnline(uid: String) {
    // Supabase:     val statusUpdate = mapOf("status" to "online")
    // Supabase:     supabase.from("users").update(statusUpdate).eq("uid", uid).execute()
    // Supabase:     // Supabase: On disconnect, set the last seen time as a timestamp string
    // Supabase:     // Supabase: This would require a real-time presence solution in Supabase, not directly equivalent to Firebase's onDisconnect
    // Supabase:     // Supabase: For now, we'll just update the status to online.
    // Supabase: }

    /**
     * Explicitly sets the user's status to a timestamp (last seen).
     * @param uid The Firebase UID of the current user.
     */
    // Supabase: fun goOffline(uid: String) {
    // Supabase:     val statusUpdate = mapOf("status" to System.currentTimeMillis().toString())
    // Supabase:     supabase.from("users").update(statusUpdate).eq("uid", uid).execute()
    // Supabase: }

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
