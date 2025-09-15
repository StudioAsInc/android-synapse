package com.synapse.social.studioasinc

import com.google.firebase.database.FirebaseDatabase

/**
 * Manages user online presence in Firebase, writing to the correct database path.
 * Handles online, offline (timestamp), and chat statuses.
 */
object PresenceManager {

    // Correct database reference to the 'users' node
    private val usersRef = FirebaseDatabase.getInstance().getReference("skyline/users")

    /**
     * Returns the specific database reference for a user's status.
     * Path: /skyline/users/{uid}/status
     */
    private fun getUserStatusRef(uid: String) = usersRef.child(uid).child("status")

    /**
     * Sets user status to "online".
     * Registers onDisconnect to set a timestamp for last seen.
     * @param uid The Firebase UID of the current user.
     */
    @JvmStatic
    fun goOnline(uid: String) {
        val statusRef = getUserStatusRef(uid)
        val activityRef = usersRef.child(uid).child("activity")
        statusRef.setValue("online")
        // On disconnect, set the last seen time as a timestamp string
        statusRef.onDisconnect().setValue(System.currentTimeMillis().toString())
        activityRef.onDisconnect().removeValue()
    }

    /**
     * Explicitly sets the user's status to a timestamp (last seen).
     * @param uid The Firebase UID of the current user.
     */
    @JvmStatic
    fun goOffline(uid: String) {
        // Set the last seen time as a timestamp string
        getUserStatusRef(uid).setValue(System.currentTimeMillis().toString())
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
