package com.synapse.social.studioasinc

import io.github.jan.tennert.supabase.createSupabaseClient
import io.github.jan.tennert.supabase.gotrue.GoTrue
import io.github.jan.tennert.supabase.postgrest.Postgrest
import io.github.jan.tennert.supabase.realtime.Realtime
import io.github.jan.tennert.supabase.storage.Storage

/**
 * Singleton Supabase client for the entire application.
 * Provides a centralized access point to all Supabase services.
 */
object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(GoTrue) {
            // Authentication configuration
            autoRefreshToken = true
            autoSaveToStorage = true
        }
        install(Postgrest) {
            // Database configuration
        }
        install(Realtime) {
            // Realtime configuration
        }
        install(Storage) {
            // Storage configuration
        }
    }
}