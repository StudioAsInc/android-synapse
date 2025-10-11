package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.backend.interfaces.IAuthResult
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

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

class AuthenticationService(private val supabase: SupabaseClient) : IAuthenticationService {

    override fun getCurrentUser(): IUser? {
        return supabase.gotrue.currentUserOrNull()?.let { SupabaseUser(it) }
    }

    override fun signIn(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        supabase.gotrue.runCatching { 
            signInWith(io.github.jan.supabase.gotrue.providers.AuthOtp(email)) { 
                password = pass 
            } 
        }.onSuccess { 
            listener.onSuccess(SupabaseAuthResult(true)) 
        }.onFailure { 
            listener.onFailure(SupabaseAuthResult(false, it.message)) 
        }
    }

    override fun signUp(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        supabase.gotrue.runCatching { 
            signUpWith(io.github.jan.supabase.gotrue.providers.AuthOtp(email)) { 
                password = pass 
            } 
        }.onSuccess { 
            listener.onSuccess(SupabaseAuthResult(true)) 
        }.onFailure { 
            listener.onFailure(SupabaseAuthResult(false, it.message)) 
        }
    }

    override fun signOut() {
        supabase.gotrue.runCatching { 
            signOut()
        }
    }

    override fun deleteUser(listener: ICompletionListener<Unit>) {
        supabase.gotrue.runCatching { 
            deleteUser()
        }.onSuccess { 
            listener.onSuccess(Unit)
        }.onFailure { 
            listener.onFailure(SupabaseAuthResult(false, it.message))
        }
    }
}

class SupabaseAuthResult(private val success: Boolean, private val error: String? = null) : IAuthResult {
    override fun isSuccessful(): Boolean = success
    override fun getException(): Exception? = error?.let { Exception(it) }
}

class SupabaseUser(private val userInfo: UserInfo) : IUser {
    override fun getUid(): String = userInfo.id
    override fun getEmail(): String? = userInfo.email
}
