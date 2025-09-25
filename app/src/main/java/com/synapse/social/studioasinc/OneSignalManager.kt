package com.synapse.social.studioasinc

import android.util.Log
import com.synapse.social.studioasinc.util.SupabaseManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object OneSignalManager {

    private const val TAG = "OneSignalManager"

    /**
     * Saves or updates the user's OneSignal Player ID in the Supabase database.
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

        GlobalScope.launch {
            try {
                SupabaseManager.getClient().postgrest["users"].update(
                    {
                        set("oneSignalPlayerId", playerId)
                    }
                ) {
                    filter {
                        eq("id", userUid)
                    }
                }
                Log.i(TAG, "OneSignal Player ID saved to Supabase for user: $userUid")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save OneSignal Player ID to Supabase for user: $userUid", e)
            }
        }
    }
}