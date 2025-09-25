package com.synapse.social.studioasinc.backend.interfaces

interface ISignUpService {
    fun signUp(email: String, pass: String, listener: ICompletionListener<IAuthResult>)
}