package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.backend.interfaces.IAuthResult
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SupabaseAuthService : IAuthenticationService {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val supabase: SupabaseClient = createSupabaseClient(
        // TODO: IMPORTANT! Replace these placeholder credentials.
        // It is strongly recommended to load these from a secure configuration file (e.g., local.properties)
        // rather than hardcoding them in the source code.
        supabaseUrl = "YOUR_SUPABASE_URL",
        supabaseKey = "YOUR_SUPABASE_KEY"
    ) {
        install(Auth)
    }

    override fun getCurrentUser(): IUser? {
        return supabase.auth.currentUserOrNull()?.let { SupabaseUser(it) }
    }

    override fun signIn(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        serviceScope.launch {
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = pass
                }
                listener.onComplete(SupabaseAuthResult(true, getCurrentUser()), null)
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
                listener.onComplete(SupabaseAuthResult(true, getCurrentUser()), null)
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
        // Supabase does not have a direct client-side user deletion API.
        // This should be handled by a server-side function.
        listener.onComplete(Unit, null)
    }
}

class SupabaseUser(private val user: UserInfo) : IUser {
    override fun getUid(): String {
        return user.id
    }
}

class SupabaseAuthResult(
    private val isSuccess: Boolean,
    private val user: IUser?
) : IAuthResult {
    override fun isSuccessful(): Boolean {
        return isSuccess
    }

    override fun getUser(): IUser? {
        return user
    }
}
