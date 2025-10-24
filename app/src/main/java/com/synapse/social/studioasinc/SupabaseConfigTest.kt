package com.synapse.social.studioasinc

import android.util.Log

/**
 * Simple utility to test Supabase configuration
 */
object SupabaseConfigTest {
    private const val TAG = "SupabaseConfigTest"
    
    fun testConfiguration() {
        Log.d(TAG, "=== Supabase Configuration Test ===")
        
        if (SupabaseClient.isConfigured()) {
            Log.d(TAG, "✅ Supabase is properly configured")
            Log.d(TAG, "URL: ${BuildConfig.SUPABASE_URL}")
            Log.d(TAG, "Key: ${BuildConfig.SUPABASE_ANON_KEY.take(10)}...")
        } else {
            Log.e(TAG, "❌ Supabase is NOT configured properly")
            Log.e(TAG, "Please update gradle.properties with your actual Supabase credentials")
            Log.e(TAG, "Current URL: ${BuildConfig.SUPABASE_URL}")
            Log.e(TAG, "Current Key: ${BuildConfig.SUPABASE_ANON_KEY}")
        }
        
        Log.d(TAG, "=== End Configuration Test ===")
    }
}