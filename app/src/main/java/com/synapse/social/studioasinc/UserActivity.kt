package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService

object UserActivity {

    private val dbService: IDatabaseService = SupabaseDatabaseService()
    private val usersRef = dbService.getReference("skyline/users")
    private val emptyListener = object : ICompletionListener<Unit> {
        override fun onComplete(result: Unit?, error: Exception?) {
            // No-op
        }
    }

    @JvmStatic
    fun setActivity(uid: String, activity: String) {
        usersRef.child(uid).child("activity").setValue(activity, emptyListener)
    }

    @JvmStatic
    fun clearActivity(uid: String) {
        usersRef.child(uid).child("activity").setValue(null, emptyListener)
    }
}