package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.BuildConfig
import io.supabase.kt.SupabaseClient
import io.supabase.kt.createSupabaseClient
import io.supabase.kt.gotrue.GoTrue
import io.supabase.kt.postgrest.Postgrest
import io.supabase.kt.realtime.Realtime

object SupabaseClient {

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(GoTrue)
        install(Postgrest)
        install(Realtime)
    }
}
