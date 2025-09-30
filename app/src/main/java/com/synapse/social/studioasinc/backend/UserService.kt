package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.backend.IDataListener
import com.synapse.social.studioasinc.backend.IDataSnapshot
import com.synapse.social.studioasinc.backend.IDatabaseError
import com.synapse.social.studioasinc.backend.IDatabaseService

class UserService(private val dbService: IDatabaseService) {

    interface UserProfileListener {
        fun onProfileReceived(profile: IDataSnapshot?)
        fun onError(databaseError: IDatabaseError)
    }

    fun getUserProfile(uid: String, listener: UserProfileListener) {
        val userRef = dbService.getReference("skyline/users/$uid")
        dbService.getData(userRef, object : IDataListener {
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                listener.onProfileReceived(dataSnapshot)
            }

            override fun onCancelled(databaseError: IDatabaseError) {
                listener.onError(databaseError)
            }
        })
    }
}