package com.synapse.social.studioasinc.chat.common.service

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.synapse.social.studioasinc.backend.AuthenticationService
import com.synapse.social.studioasinc.backend.DatabaseService
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService

class UserBlockService(
    private val activity: Activity,
    private val dbService: IDatabaseService,
    private val authService: IAuthenticationService
) {

    private val blocklistRef = dbService.getReference("blocklist")
    private val myUid: String? = authService.getCurrentUser()?.uid

    fun blockUser(uid: String) {
        if (myUid == null) return
        blocklistRef.child(myUid).child(uid).setValue("true", null)
    }

    fun unblockUser(uid: String) {
        if (myUid == null) return
        blocklistRef.child(myUid).child(uid).setValue(null, null)
        val intent = activity.intent
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.finish()
        activity.startActivity(intent)
    }
}