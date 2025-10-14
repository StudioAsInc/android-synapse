package com.synapse.social.studioasinc

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object Supabase {
    private const val SUPABASE_URL = "YOUR_SUPABASE_URL"
    private const val SUPABASE_KEY = "YOUR_SUPABASE_KEY"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY,
    ) {
        install(GoTrue)
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
}
