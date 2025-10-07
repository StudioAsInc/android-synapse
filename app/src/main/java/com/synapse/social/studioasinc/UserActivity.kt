package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService

object UserActivity {

    private val dbService: IDatabaseService by lazy { (SynapseApp.getContext().applicationContext as SynapseApp).getDatabaseService() }
    private val usersRef by lazy { dbService.getReference("skyline/users") }
    private val emptyListener = object : ICompletionListener<Unit> {
        override fun onComplete(result: Unit?, error: String?) {
            // No-op
        }
    }

    @JvmStatic
    fun setActivity(uid: String, activity: String) {
        val ref = usersRef.child(uid).child("activity")
        dbService.setValue(ref, activity, emptyListener)
    }

    @JvmStatic
    fun clearActivity(uid: String) {
        val ref = usersRef.child(uid).child("activity")
        dbService.setValue(ref, null, emptyListener)
    }
}