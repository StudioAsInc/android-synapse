package com.synapse.social.studioasinc.backend

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService

/**
 * Compatibility service that bridges Firebase-style interfaces with Supabase implementations.
 * This allows gradual migration of existing code without breaking everything at once.
 */
class SupabaseCompatibilityService : IDatabaseService, IAuthenticationService {
    
    private val supabaseDb = SupabaseDatabaseService()
    private val supabaseAuth = SupabaseAuthenticationService()
    
    // Authentication methods
    override fun getCurrentUser(): Any? {
        var result: Any? = null
        CoroutineScope(Dispatchers.IO).launch {
            result = supabaseAuth.getCurrentUser()
        }
        return result
    }
    
    override fun signIn(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = supabaseAuth.signIn(email, password)
                callback(true, user.id)
            } catch (e: Exception) {
                callback(false, e.message)
            }
        }
    }
    
    override fun signUp(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = supabaseAuth.signUp(email, password)
                callback(true, user.id)
            } catch (e: Exception) {
                callback(false, e.message)
            }
        }
    }
    
    override fun signOut() {
        CoroutineScope(Dispatchers.IO).launch {
            supabaseAuth.signOut()
        }
    }
    
    // Database methods - simplified implementations for compatibility
    override fun getData(path: String, callback: (Any?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Parse path to determine table and filters
                val pathParts = path.split("/")
                when {
                    pathParts.contains("users") -> {
                        val uid = pathParts.lastOrNull()
                        if (uid != null) {
                            val user = supabaseDb.getUserByUid(uid)
                            callback(user)
                        } else {
                            callback(null)
                        }
                    }
                    pathParts.contains("username_registry") -> {
                        val username = pathParts.lastOrNull()
                        if (username != null) {
                            val available = supabaseDb.checkUsernameAvailability(username)
                            callback(if (available) null else mapOf("exists" to true))
                        } else {
                            callback(null)
                        }
                    }
                    else -> callback(null)
                }
            } catch (e: Exception) {
                callback(null)
            }
        }
    }
    
    override fun setData(path: String, data: Map<String, Any?>, callback: ((Boolean) -> Unit)?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pathParts = path.split("/")
                when {
                    pathParts.contains("users") -> {
                        val uid = pathParts.lastOrNull()
                        if (uid != null) {
                            supabaseDb.updateUser(uid, data)
                            callback?.invoke(true)
                        } else {
                            callback?.invoke(false)
                        }
                    }
                    else -> callback?.invoke(false)
                }
            } catch (e: Exception) {
                callback?.invoke(false)
            }
        }
    }
    
    override fun pushData(path: String, data: Map<String, Any?>, callback: ((Boolean, String?) -> Unit)?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pathParts = path.split("/")
                when {
                    pathParts.contains("messages") -> {
                        // Handle message insertion
                        val messageKey = java.util.UUID.randomUUID().toString()
                        val messageData = data.toMutableMap()
                        messageData["message_key"] = messageKey
                        
                        supabaseDb.insert("messages", messageData)
                        callback?.invoke(true, messageKey)
                    }
                    else -> callback?.invoke(false, null)
                }
            } catch (e: Exception) {
                callback?.invoke(false, null)
            }
        }
    }
    
    override fun removeData(path: String, callback: ((Boolean) -> Unit)?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Implement soft delete for messages
                val pathParts = path.split("/")
                if (pathParts.contains("messages")) {
                    val messageKey = pathParts.lastOrNull()
                    if (messageKey != null) {
                        supabaseDb.update("messages", mapOf(
                            "deleted_at" to java.time.Instant.now().toString()
                        ))
                        callback?.invoke(true)
                    } else {
                        callback?.invoke(false)
                    }
                } else {
                    callback?.invoke(false)
                }
            } catch (e: Exception) {
                callback?.invoke(false)
            }
        }
    }
    
    override fun addListener(path: String, callback: (Any?) -> Unit): Any? {
        // For now, return a dummy listener
        // Real-time functionality will be implemented separately
        return object {
            fun remove() {
                // Placeholder for listener removal
            }
        }
    }
    
    override fun removeListener(listener: Any?) {
        // Placeholder for listener removal
    }
}