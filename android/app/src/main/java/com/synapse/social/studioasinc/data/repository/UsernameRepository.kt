package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonObject

class UsernameRepository {
    private val client = SupabaseClient.client
    private val usernameCache = mutableMapOf<String, Boolean>()

    suspend fun checkAvailability(username: String): Result<Boolean> {
        return try {
            if (!SupabaseClient.isConfigured()) {
                return Result.failure(Exception("Supabase not configured"))
            }

            // Check cache first
            usernameCache[username]?.let { return Result.success(it) }

            val response = client
                .from("users")
                .select {
                    filter {
                        eq("username", username)
                    }
                }
                .decodeList<JsonObject>()

            val isAvailable = response.isEmpty()
            usernameCache[username] = isAvailable

            Result.success(isAvailable)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearCache() {
        usernameCache.clear()
    }
}
