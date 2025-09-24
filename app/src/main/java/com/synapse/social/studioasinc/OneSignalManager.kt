package com.synapse.social.studioasinc

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object OneSignalManager {
    // This function assumes that there is a table named `profiles` with a column named `one_signal_player_id`.
    fun savePlayerIdToSupabase(userId: String, playerId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Supabase.client.postgrest["profiles"].update(
                    {
                        set("one_signal_player_id", playerId)
                    }
                ) {
                    filter {
                        eq("id", userId)
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
