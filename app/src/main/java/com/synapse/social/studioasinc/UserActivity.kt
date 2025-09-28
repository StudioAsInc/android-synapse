package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService

object UserActivity {

    private val dbService: IDatabaseService = SupabaseDatabaseService()
    private val usersRef = dbService.getReference("skyline/users")

    @JvmStatic
    fun setActivity(uid: String, activity: String) {
        usersRef.child(uid).child("activity").setValue(activity) { _, _ -> }
    }

    @JvmStatic
    fun clearActivity(uid: String) {
        usersRef.child(uid).child("activity").removeValue { _, _ -> }
    }
}