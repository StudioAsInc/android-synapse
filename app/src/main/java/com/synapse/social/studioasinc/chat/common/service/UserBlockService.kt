package com.synapse.social.studioasinc.chat.common.service

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService

class UserBlockService(
    private val activity: Activity,
    private val authService: IAuthenticationService,
    private val dbService: IDatabaseService
) {



    fun blockUser(uid: String) {
        val myUid = authService.getCurrentUser()?.uid ?: return
        val blockData = mapOf(uid to "true")
        dbService.updateChildren(dbService.getReference("skyline/blocklist").child(myUid), blockData) { _, _ -> }
    }

    fun unblockUser(uid: String) {
        val myUid = authService.getCurrentUser()?.uid ?: return
        dbService.setValue(dbService.getReference("skyline/blocklist").child(myUid).child(uid), null) { _, error ->
            if (error == null) {
                val intent = activity.intent
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.finish()
                activity.startActivity(intent)
            } else {
                Log.e("UserBlockService", "Failed to unblock user", error)
            }
        }
    }
}