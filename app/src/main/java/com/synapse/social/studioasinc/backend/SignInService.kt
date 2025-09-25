package com.synapse.social.studioasinc.backend

import com.google.firebase.auth.FirebaseUser

class SignInService(private val authService: AuthenticationService) {

    interface SignInListener {
        fun onSignInSuccess(user: FirebaseUser?)
        fun onSignInFailure(exception: Exception)
    }

    fun signIn(email: String, pass: String, listener: SignInListener) {
        authService.signIn(email, pass, object : AuthenticationService.AuthListener {
            override fun onSuccess(user: FirebaseUser?) {
                listener.onSignInSuccess(user)
            }

            override fun onFailure(exception: Exception) {
                listener.onSignInFailure(exception)
            }
        })
    }
}