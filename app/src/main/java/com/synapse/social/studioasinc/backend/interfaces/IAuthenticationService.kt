package com.synapse.social.studioasinc.backend.interfaces

/**
 * Defines the contract for authentication operations, fully abstracted from the underlying provider.
 */
interface IAuthenticationService {
    /**
     * Gets the currently signed-in user.
     * @return The current user, or null if no user is signed in.
     */
    fun getCurrentUser(): IUser?

    /**
     * Signs in a user with the given email and password.
     */
    fun signIn(email: String, pass: String, listener: ICompletionListener<IAuthResult>)

    /**
     * Creates a new user account with the given email and password.
     */
    fun signUp(email: String, pass: String, listener: ICompletionListener<IAuthResult>)

    /**
     * Signs out the current user.
     */
    fun signOut()

    /**
     * Deletes the currently signed-in user's account.
     */
    fun deleteUser(listener: ICompletionListener<Unit>)
}