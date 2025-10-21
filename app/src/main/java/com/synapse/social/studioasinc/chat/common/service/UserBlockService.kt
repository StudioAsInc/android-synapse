package com.synapse.social.studioasinc.chat.common.service

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.synapse.social.studioasinc.compatibility.FirebaseAuth
import com.synapse.social.studioasinc.compatibility.FirebaseDatabase
import com.synapse.social.studioasinc.compatibility.DatabaseReference

class UserBlockService(private val activity: Activity) {

    private val blocklistRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("skyline/blocklist")
    private val myUid: String? = FirebaseAuth.getInstance().currentUser?.uid

    fun blockUser(uid: String) {
        if (myUid == null) return
        val blockData = mapOf(uid to "true")
        blocklistRef.child(myUid).updateChildren(blockData)
    }

    fun unblockUser(uid: String) {
        if (myUid == null) return
        blocklistRef.child(myUid).child(uid).removeValue()
            .addOnSuccessListener {
                val intent = activity.intent
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.finish()
                activity.startActivity(intent)
            }
            .addOnFailureListener { e ->
                Log.e("UserBlockService", "Failed to unblock user", e)
            }
    }
}