package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.backend.interfaces.IAuthResult
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IUser
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SupabaseAuthService : IAuthenticationService {

    private val supabase = SupabaseClient.client

    override fun getCurrentUser(): IUser? {
        return supabase.auth.currentUserOrNull()?.let { user ->
            object : IUser {
                override fun getUid(): String = user.id
            }
        }
    }

    override fun signIn(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        CoroutineScope(Dispatchers.IO).launch {
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
        CoroutineScope(Dispatchers.IO).launch {
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
        CoroutineScope(Dispatchers.IO).launch {
            supabase.auth.signOut()
        }
    }

    override fun deleteUser(listener: ICompletionListener<Unit>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Supabase admin client is required to delete users.
                // This is a placeholder for the actual implementation.
                // val result = supabase.auth.admin.deleteUser(uid)
                listener.onComplete(Unit, null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }
}
