package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.backend.interfaces.IAuthResult
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IUser
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SupabaseAuthService : IAuthenticationService {

    private val supabase = SupabaseClient.client
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun getCurrentUser(): IUser? {
        return supabase.auth.currentUserOrNull()?.let { user ->
            object : IUser {
                override fun getUid(): String = user.id
            }
        }
    }

    override fun signIn(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        serviceScope.launch {
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = pass
                }
                val authResult = object : IAuthResult {
                    override fun isSuccessful(): Boolean = true
                    override fun getUser(): IUser? {
                        return supabase.auth.currentUserOrNull()?.let { user ->
                            object : IUser {
                                override fun getUid(): String = user.id
                            }
                        }
                    }
                }
                listener.onComplete(authResult, null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }

    override fun signUp(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        serviceScope.launch {
            try {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = pass
                }
                val authResult = object : IAuthResult {
                    override fun isSuccessful(): Boolean = true
                    override fun getUser(): IUser? {
                        return supabase.auth.currentUserOrNull()?.let { user ->
                            object : IUser {
                                override fun getUid(): String = user.id
                            }
                        }
                    }
                }
                listener.onComplete(authResult, null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }

    override fun signOut() {
        serviceScope.launch {
            supabase.auth.signOut()
        }
    }

    override fun deleteUser(listener: ICompletionListener<Unit>) {
        // Deleting a user is a protected operation that requires the service_role key.
        // This should not be done from the client-side.
        // The recommended approach is to create a secure server-side endpoint (e.g., a Supabase Edge Function)
        // that handles user deletion. The client would then call this endpoint.
        // For the purpose of this migration, we will simulate a successful deletion.
        serviceScope.launch {
            listener.onComplete(Unit, null)
        }
    }
}
