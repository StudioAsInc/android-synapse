package com.synapse.social.studioasinc.backend

import com.google.firebase.auth.FirebaseAuth
import com.synapse.social.studioasinc.backend.interfaces.IAuthResult
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.ISignInService
import com.synapse.social.studioasinc.backend.interfaces.IUser

class SignInService : ISignInService {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun signIn(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            val authResult = object : IAuthResult {
                override fun isSuccessful(): Boolean = task.isSuccessful
                override fun getUser(): IUser? {
                    return task.result?.user?.let { firebaseUser ->
                        object : IUser {
                            override fun getUid(): String = firebaseUser.uid
                        }
                    }
                }
            }
            listener.onComplete(authResult, task.exception)
        }
    }
}