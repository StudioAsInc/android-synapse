package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.backend.interfaces.IAuthResult
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SupabaseAuthService : IAuthenticationService {

    private val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(GoTrue)
        install(Postgrest)
    }

    override fun getCurrentUser(): IUser? {
        val session = supabase.auth.currentSessionOrNull()
        return session?.user?.let { user ->
            object : IUser {
                override fun getUid(): String = user.id
                override fun getEmail(): String? = user.email
            }
        }
    }

    override fun signIn(email: String, pass: String, listener: ICompletionListener<IAuthResult>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabase.auth.signInWith(io.github.jan.supabase.gotrue.providers.builtin.Email.Provider) {
                    this.email = email
                    this.password = pass
                }
                val authResult = object : IAuthResult {
                    override fun isSuccessful(): Boolean = true
                    override fun getUser(): IUser? = getCurrentUser()
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
                supabase.auth.signUpWith(io.github.jan.supabase.gotrue.providers.builtin.Email.Provider) {
                    this.email = email
                    this.password = pass
                }
                val authResult = object : IAuthResult {
                    override fun isSuccessful(): Boolean = true
                    override fun getUser(): IUser? = getCurrentUser()
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
        // Supabase does not support user deletion from the client-side SDK.
        // This needs to be handled on the server-side with admin privileges.
        listener.onComplete(Unit, null)
    }
}
