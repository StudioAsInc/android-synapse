package com.synapse.social.studioasinc.backend

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.android.*

object SupabaseClient {

    private const val SUPABASE_URL = "https://apqvyyphlrtmuyjnzmuq.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFwcXZ5eXBobHJ0bXV5am56bXVxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTg3MDUwODcsImV4cCI6MjA3NDI4MTA4N30.On7kjijj7bUg_xzr2HwCTYvLaV-f_1aDYqVTfKai7gc"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(GoTrue)
        install(Postgrest)
        install(Storage)
        httpEngine = Android.createEngine()
    }
}
