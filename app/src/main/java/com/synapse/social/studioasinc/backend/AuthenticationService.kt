package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.backend.interfaces.IAuthResult
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IUser

// TODO(supabase): Implement Supabase Authentication Service
// This service should implement the IAuthenticationService interface and provide a concrete
// implementation using the Supabase Kotlin client library.
// See: https://supabase.com/docs/guides/auth/kotlin
//
// Key tasks:
// 1.  Initialize the Supabase client.
// 2.  Implement getCurrentUser to return the current Supabase user.
// 3.  Implement signIn, signUp, signOut, and deleteUser using Supabase Auth.
// 4.  Create Supabase-specific wrappers for IAuthResult and IUser to map the Supabase API to the existing interfaces.

class AuthenticationService : IAuthenticationService {

    override fun getCurrentUser(): IUser? {
        // TODO(supabase): Implement this method to return the current Supabase user.
        throw NotImplementedError("Supabase authentication service is not yet implemented.")
    }

    override fun signIn(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        // TODO(supabase): Implement this method to sign in a user with Supabase.
        throw NotImplementedError("Supabase authentication service is not yet implemented.")
    }

    override fun signUp(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        // TODO(supabase): Implement this method to sign up a user with Supabase.
        throw NotImplementedError("Supabase authentication service is not yet implemented.")
    }

    override fun signOut() {
        // TODO(supabase): Implement this method to sign out the current user from Supabase.
        throw NotImplementedError("Supabase authentication service is not yet implemented.")
    }

    override fun deleteUser(listener: ICompletionListener<Unit>) {
        // TODO(supabase): Implement this method to delete the current user from Supabase.
        throw NotImplementedError("Supabase authentication service is not yet implemented.")
    }
}
