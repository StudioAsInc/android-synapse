package com.synapse.social.studioasinc

import android.util.Log
// import com.google.firebase.database.FirebaseDatabase
import com.synapse.social.studioasinc.backend.DatabaseService

object OneSignalManager {

    private const val TAG = "OneSignalManager"
    // TODO(supabase): Replace with Supabase database call
    // private val db = FirebaseDatabase.getInstance().getReference("skyline/users")

    /**
     * Saves or updates the user's OneSignal Player ID in the Supabase Database.
     * This is now the primary method for storing the player ID.
     *
     * @param userUid The Supabase UID of the user.
     * @param playerId The OneSignal Player ID to save.
     */
    @JvmStatic
    fun savePlayerIdToSupabase(userUid: String, playerId: String) {
        if (userUid.isBlank() || playerId.isBlank()) {
            Log.w(TAG, "User UID or Player ID is blank. Aborting save.")
            return
        }

        // TODO(supabase): Implement saving the player ID to the Supabase user profile.
        // This will likely involve using the DatabaseService to update the user's record.
        // Example:
        // val dbService = DatabaseService(SynapseApp.supabaseClient)
        // val userRef = dbService.getReference("users/$userUid")
        // val updates = mapOf("oneSignalPlayerId" to playerId)
        // dbService.updateChildren(userRef, updates, object : ICompletionListener<Unit> {
        //     override fun onSuccess(result: Unit) {
        //         Log.i(TAG, "OneSignal Player ID saved to Supabase for user: $userUid")
        //     }
        //     override fun onFailure(error: Exception) {
        //         Log.e(TAG, "Failed to save OneSignal Player ID to Supabase for user: $userUid", error)
        //     }
        // })
    }
}
