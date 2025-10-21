package com.synapse.social.studioasinc

/**
 * Temporary stub for Supabase client during migration.
 * This will be replaced with actual Supabase implementation once dependencies are resolved.
 */
object SupabaseClient {
    // Temporary stub - will be replaced with actual Supabase client
    val client = object {
        val auth = object {
            fun currentUserOrNull(): Any? = null
            suspend fun signInWith(provider: Any, block: Any.() -> Unit): Any = Unit
            suspend fun signUpWith(provider: Any, block: Any.() -> Unit): Any = Unit
            suspend fun signOut() = Unit
            suspend fun deleteUser() = Unit
        }
        
        fun from(table: String) = object {
            fun select(columns: Any? = null) = this
            fun insert(data: Any) = this
            fun update(data: Any) = this
            fun upsert(data: Any) = this
            fun delete() = this
            suspend fun <T> decodeList(): List<T> = emptyList()
            suspend fun <T> decodeSingle(): T? = null
        }
        
        val realtime = object {
            fun channel(name: String) = object {
                suspend fun join() = Unit
                suspend fun leave() = Unit
            }
        }
    }
}