package com.synapse.social.studioasinc.backend.interfaces

/**
 * A generic interface representing the result of an authentication operation.
 * This abstracts the underlying BaaS provider's specific AuthResult class.
 */
interface IAuthResult {
    fun isSuccessful(): Boolean
    fun getUser(): IUser?
}
