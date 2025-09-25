package com.synapse.social.studioasinc.backend

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthenticationService {

    private val fauth: FirebaseAuth = FirebaseAuth.getInstance()

    interface AuthListener {
        fun onSuccess(user: FirebaseUser?)
        fun onFailure(exception: Exception)
    }

    fun signIn(email: String, pass: String, listener: AuthListener) {
        fauth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    listener.onSuccess(fauth.currentUser)
                } else {
                    task.exception?.let { listener.onFailure(it) }
                }
            }
    }

    fun signUp(email: String, pass: String, listener: AuthListener) {
        fauth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    listener.onSuccess(fauth.currentUser)
                } else {
                    task.exception?.let { listener.onFailure(it) }
                }
            }
    }

    fun getCurrentUser(): FirebaseUser? {
        return fauth.currentUser
    }

    fun signOut() {
        fauth.signOut()
    }
}