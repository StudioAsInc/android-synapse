package com.synapse.social.studioasinc

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.plugins.addDefaultResponseValidation
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Supabase client singleton for the application
 */
object SupabaseClient {
    private const val TAG = "SupabaseClient"
    
    val client by lazy {
        try {
            // Check if credentials are properly configured
            if (BuildConfig.SUPABASE_URL.isBlank() || 
                BuildConfig.SUPABASE_URL == "https://your-project.supabase.co" ||
                BuildConfig.SUPABASE_ANON_KEY.isBlank() || 
                BuildConfig.SUPABASE_ANON_KEY == "your-anon-key-here") {
                
                Log.e(TAG, "Supabase credentials not configured properly!")
                Log.e(TAG, "Please update gradle.properties with your actual Supabase URL and key")
                
                // Create a dummy client with placeholder values to prevent crashes
                createSupabaseClient(
                    supabaseUrl = "https://placeholder.supabase.co",
                    supabaseKey = "placeholder-key"
                ) {
                    install(Auth)
                    install(Postgrest)
                    install(Realtime)
                    install(Storage)
                }
            } else {
                createSupabaseClient(
                    supabaseUrl = BuildConfig.SUPABASE_URL,
                    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
                ) {
                    install(Auth)
                    install(Postgrest)
                    install(Realtime)
                    install(Storage) {
                        customUrl = BuildConfig.SUPABASE_SYNAPSE_S3_ENDPOINT_URL
                    }
                    // httpEngine = OkHttp.create {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase client: ${e.message}", e)
            // Return a dummy client to prevent app crashes
            createSupabaseClient(
                supabaseUrl = "https://placeholder.supabase.co",
                supabaseKey = "placeholder-key"
            ) {
                install(Auth)
                install(Postgrest)
                install(Realtime)
            }
        }
    }
    
    /**
     * Check if Supabase is properly configured
     */
    fun isConfigured(): Boolean {
        return BuildConfig.SUPABASE_URL.isNotBlank() && 
               BuildConfig.SUPABASE_URL != "https://your-project.supabase.co" &&
               BuildConfig.SUPABASE_ANON_KEY.isNotBlank() && 
               BuildConfig.SUPABASE_ANON_KEY != "your-anon-key-here"
    }
}