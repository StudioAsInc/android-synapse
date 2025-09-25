package com.synapse.social.studioasinc.backend.interfaces

interface ISignInService {
    fun signIn(email: String, pass: String, listener: ICompletionListener<IAuthResult>)
}