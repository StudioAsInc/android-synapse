package com.synapse.social.studioasinc.backend.interfaces

/**
 * Represents a user, abstracted from the underlying provider.
 */
interface IUser {
    /**
     * Gets the unique ID of the user.
     */
    fun getUid(): String
}
