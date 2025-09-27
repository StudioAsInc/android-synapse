package com.synapse.social.studioasinc.backend

import com.google.firebase.auth.FirebaseAuth
import com.synapse.social.studioasinc.backend.interfaces.IAuthResult
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
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

    override fun signIn(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            val authResult = object : IAuthResult {
                override fun isSuccessful(): Boolean = task.isSuccessful
                override fun getUser(): IUser? {
                    return task.result?.user?.let { fbUser ->
                        object : IUser {
                            override fun getUid(): String = fbUser.uid
                        }
                    }
                }
            }
            listener.onComplete(authResult, task.exception)
        }
    }

    override fun signUp(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            val authResult = object : IAuthResult {
                override fun isSuccessful(): Boolean = task.isSuccessful
                override fun getUser(): IUser? {
                    return task.result?.user?.let { fbUser ->
                        object : IUser {
                            override fun getUid(): String = fbUser.uid
                        }
                    }
                }
            }
            listener.onComplete(authResult, task.exception)
        }
    }

    override fun signOut() {
        auth.signOut()
    }

    override fun deleteUser(listener: ICompletionListener<Unit>) {
        auth.currentUser?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                listener.onComplete(Unit, null)
            } else {
                listener.onComplete(null, task.exception)
            }
        }
    }
}