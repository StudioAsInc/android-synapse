package com.synapse.social.studioasinc.backend

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Compatibility service that bridges Firebase-style interfaces with Supabase implementations.
 * This allows gradual migration of existing code without breaking everything at once.
 */
class SupabaseCompatibilityService {
    
    private val supabaseDb = SupabaseDatabaseService()
    private val supabaseAuth = SupabaseAuthenticationService()
    
    // Authentication methods
    fun getCurrentUser(): Any? {
        var result: Any? = null
        CoroutineScope(Dispatchers.IO).launch {
            result = supabaseAuth.getCurrentUser()
        }
        return result
    }
    
    fun signIn(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = supabaseAuth.signIn(email, password)
                callback(true, user.id)
            } catch (e: Exception) {
                callback(false, e.message)
            }
        }
    }
    
    fun signUp(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = supabaseAuth.signUp(email, password)
                callback(true, user.id)
            } catch (e: Exception) {
                callback(false, e.message)
            }
        }
    }
    
    fun signOut() {
        CoroutineScope(Dispatchers.IO).launch {
            supabaseAuth.signOut()
        }
    }
}