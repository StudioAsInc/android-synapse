package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.backend.interfaces.IAuthResult
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Supabase-based authentication service that implements the existing interface
 * for backward compatibility during migration.
 */
class AuthenticationService : IAuthenticationService {

    private val supabaseAuthService = SupabaseAuthenticationService()

    override fun getCurrentUser(): IUser? {
        // This needs to be handled differently since Supabase is async
        // For now, return null and handle auth state through coroutines
        return null
    }

    override fun signIn(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val user = supabaseAuthService.signIn(email, pass)
                val authResult = object : IAuthResult {
                    override fun isSuccessful(): Boolean = true
                    override fun getUser(): IUser? {
                        return object : IUser {
                            override fun getUid(): String = user.id
                        }
                    }
                }
                listener.onComplete(authResult, null)
            } catch (e: Exception) {
                val authResult = object : IAuthResult {
                    override fun isSuccessful(): Boolean = false
                    override fun getUser(): IUser? = null
                }
                listener.onComplete(authResult, e)
            }
        }
    }

    override fun signUp(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val user = supabaseAuthService.signUp(email, pass)
                val authResult = object : IAuthResult {
                    override fun isSuccessful(): Boolean = true
                    override fun getUser(): IUser? {
                        return object : IUser {
                            override fun getUid(): String = user.id
                        }
                    }
                }
                listener.onComplete(authResult, null)
            } catch (e: Exception) {
                val authResult = object : IAuthResult {
                    override fun isSuccessful(): Boolean = false
                    override fun getUser(): IUser? = null
                }
                listener.onComplete(authResult, e)
            }
        }
    }

    override fun signOut() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabaseAuthService.signOut()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override fun deleteUser(listener: ICompletionListener<Unit>) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                supabaseAuthService.deleteUser()
                listener.onComplete(Unit, null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }
}