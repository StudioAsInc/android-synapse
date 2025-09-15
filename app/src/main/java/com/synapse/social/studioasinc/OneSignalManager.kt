package com.synapse.social.studioasinc

import android.util.Log
import com.google.firebase.database.FirebaseDatabase

object OneSignalManager {

    private const val TAG = "OneSignalManager"
    private val db = FirebaseDatabase.getInstance().getReference("skyline/users")

    /**
     * Saves or updates the user's OneSignal Player ID in the Firebase Realtime Database.
     * This is now the primary method for storing the player ID.
     *
     * @param userUid The Firebase UID of the user.
     * @param playerId The OneSignal Player ID to save.
     */
    @JvmStatic
    fun savePlayerIdToRealtimeDatabase(userUid: String, playerId: String) {
        if (userUid.isBlank() || playerId.isBlank()) {
            Log.w(TAG, "User UID or Player ID is blank. Aborting save.")
            return
        }

        db.child(userUid).child("oneSignalPlayerId").setValue(playerId)
            .addOnSuccessListener {
                Log.i(TAG, "OneSignal Player ID saved to Realtime Database for user: $userUid")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save OneSignal Player ID to Realtime Database for user: $userUid", e)
            }
    }
}
