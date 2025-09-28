package com.synapse.social.studioasinc

import android.util.Log
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService

object OneSignalManager {

    private const val TAG = "OneSignalManager"
    private val dbService: IDatabaseService = SupabaseDatabaseService()
    private val db = dbService.getReference("skyline/users")

    /**
     * Saves or updates the user's OneSignal Player ID in the database.
     * This is now the primary method for storing the player ID.
     *
     * @param userUid The UID of the user.
     * @param playerId The OneSignal Player ID to save.
     */
    @JvmStatic
    fun savePlayerIdToRealtimeDatabase(userUid: String, playerId: String) {
        if (userUid.isBlank() || playerId.isBlank()) {
            Log.w(TAG, "User UID or Player ID is blank. Aborting save.")
            return
        }

        db.child(userUid).child("oneSignalPlayerId").setValue(playerId) { _, error ->
            if (error == null) {
                Log.i(TAG, "OneSignal Player ID saved to Database for user: $userUid")
            } else {
                Log.e(TAG, "Failed to save OneSignal Player ID to Database for user: $userUid", error)
            }
        }
    }
}