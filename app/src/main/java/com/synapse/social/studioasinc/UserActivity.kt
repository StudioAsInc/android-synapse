package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object UserActivity {

    private val dbService = SupabaseDatabaseService()

    @JvmStatic
    fun setActivity(uid: String, activity: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                dbService.update("users", mapOf("activity" to activity))
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    @JvmStatic
    fun clearActivity(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                dbService.update("users", mapOf("activity" to null))
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
