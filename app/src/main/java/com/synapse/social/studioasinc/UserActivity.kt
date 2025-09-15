package com.synapse.social.studioasinc

import com.google.firebase.database.FirebaseDatabase

object UserActivity {

    private val usersRef = FirebaseDatabase.getInstance().getReference("skyline/users")

    @JvmStatic
    fun setActivity(uid: String, activity: String) {
        usersRef.child(uid).child("activity").setValue(activity)
    }

    @JvmStatic
    fun clearActivity(uid: String) {
        usersRef.child(uid).child("activity").removeValue()
    }
}
