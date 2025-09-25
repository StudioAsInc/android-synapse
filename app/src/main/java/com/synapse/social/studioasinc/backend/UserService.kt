package com.synapse.social.studioasinc.backend

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

class UserService(private val dbService: DatabaseService) {

    interface UserProfileListener {
        fun onProfileReceived(profile: DataSnapshot?)
        fun onError(databaseError: DatabaseError)
    }

    fun getUserProfile(uid: String, listener: UserProfileListener) {
        dbService.getData("skyline/users/$uid", object : DatabaseService.DataListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                listener.onProfileReceived(dataSnapshot)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                listener.onError(databaseError)
            }
        })
    }
}