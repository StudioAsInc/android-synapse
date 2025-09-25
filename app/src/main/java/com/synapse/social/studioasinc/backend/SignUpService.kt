package com.synapse.social.studioasinc.backend

import com.google.firebase.auth.FirebaseUser

class SignUpService(private val authService: AuthenticationService) {

    interface SignUpListener {
        fun onSignUpSuccess(user: FirebaseUser?)
        fun onSignUpFailure(exception: Exception)
    }

    fun signUp(email: String, pass: String, listener: SignUpListener) {
        authService.signUp(email, pass, object : AuthenticationService.AuthListener {
            override fun onSuccess(user: FirebaseUser?) {
                listener.onSignUpSuccess(user)
            }

            override fun onFailure(exception: Exception) {
                listener.onSignUpFailure(exception)
            }
        })
    }
}