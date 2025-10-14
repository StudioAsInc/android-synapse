package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.HashMap

class SupabaseUserService {

    private val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(GoTrue)
        install(Postgrest)
    }

    fun createUserProfile(
        uid: String,
        email: String,
        username: String,
        nickname: String,
        biography: String,
        avatarUrl: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userProfile = hashMapOf(
                    "uid" to uid,
                    "email" to email,
                    "username" to username,
                    "nickname" to nickname,
                    "biography" to biography,
                    "avatar_url" to avatarUrl,
                    "join_date" to System.currentTimeMillis()
                )
                supabase.postgrest["users"].insert(userProfile)
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }
}
