package com.synapse.social.studioasinc.backend

import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.IUser

class AuthenticationService : IAuthenticationService {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun getCurrentUser(): IUser? {
        return auth.currentUser?.let { firebaseUser ->
            object : IUser {
                override fun getUid(): String = firebaseUser.uid
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun deleteUser(listener: OnCompleteListener<Void>) {
        auth.currentUser?.delete()?.addOnCompleteListener(listener)
    }
}