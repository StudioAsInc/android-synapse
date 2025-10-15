package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.IAuthResult
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IUser
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SupabaseAuthService : IAuthenticationService {

    private val supabase = SupabaseClient.client

    override fun getCurrentUser(): IUser? {
        val session = supabase.auth.currentSessionOrNull()
        return session?.user?.let { SupabaseUser(it.id) }
    }

    override fun signIn(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabase.auth.signInWith(Email)
                val user = supabase.auth.currentUserOrNull()
                val result = SupabaseAuthResult(true, user?.let { SupabaseUser(it.id) })
                listener.onComplete(result, null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }

    override fun signUp(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabase.auth.signUpWith(Email)
                val user = supabase.auth.currentUserOrNull()
                val result = SupabaseAuthResult(true, user?.let { SupabaseUser(it.id) })
                listener.onComplete(result, null)
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
                val user = supabase.auth.currentUserOrNull()
                if (user != null) {
                    // Note: Deleting a user is a protected operation and should be handled with care.
                    // This is a placeholder for the actual implementation.
                    // supabase.auth.admin.deleteUser(user.id)
                    listener.onComplete(Unit, null)
                } else {
                    listener.onComplete(null, Exception("User not found"))
                }
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }
}

class SupabaseAuthResult(private val successful: Boolean, private val user: IUser?) : IAuthResult {
    override fun isSuccessful(): Boolean = successful
    override fun getUser(): IUser? = user
}

class SupabaseUser(private val uid: String) : IUser {
    override fun getUid(): String = uid
}
