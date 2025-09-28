package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.IAuthResult
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SupabaseAuthService : IAuthenticationService {

    private val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = "YOUR_SUPABASE_URL",
        supabaseKey = "YOUR_SUPABASE_KEY"
    ) {
        install(Auth)
    }

    override fun getCurrentUser(): IUser? {
        val user = supabase.auth.currentUserOrNull()
        return if (user != null) {
            SupabaseUser(user)
        } else {
            null
        }
    }

    override fun signIn(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        GlobalScope.launch {
            try {
                supabase.auth.signInWith(io.github.jan.supabase.gotrue.providers.builtin.Email.Provider) {
                    this.email = email
                    this.password = pass
                }
                listener.onComplete(SupabaseAuthResult(true), null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }

    override fun signUp(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        GlobalScope.launch {
            try {
                supabase.auth.signUpWith(io.github.jan.supabase.gotrue.providers.builtin.Email.Provider) {
                    this.email = email
                    this.password = pass
                }
                listener.onComplete(SupabaseAuthResult(true), null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }

    override fun signOut() {
        GlobalScope.launch {
            supabase.auth.signOut()
        }
    }

    override fun deleteUser(listener: ICompletionListener<Unit>) {
        // Supabase does not have a direct client-side user deletion API.
        // This should be handled by a server-side function.
        listener.onComplete(Unit, null)
    }
}

class SupabaseUser(private val user: io.github.jan.supabase.gotrue.user.User) : IUser {
    override fun getUid(): String {
        return user.id
    }
}

class SupabaseAuthResult(private val isSuccess: Boolean) : IAuthResult {
    override fun isSuccessful(): Boolean {
        return isSuccess
    }
}
