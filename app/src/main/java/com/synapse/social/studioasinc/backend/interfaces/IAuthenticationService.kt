package com.synapse.social.studioasinc.backend.interfaces

interface IAuthenticationService {
    fun getCurrentUser(): IUser?
}