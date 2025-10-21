package com.synapse.social.studioasinc.backend

import io.github.jan.tennert.supabase.realtime.PostgresAction
import io.github.jan.tennert.supabase.realtime.channel
import io.github.jan.tennert.supabase.realtime.postgresChangeFlow
import io.github.jan.tennert.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import com.synapse.social.studioasinc.SupabaseClient

/**
 * Supabase Realtime Service
 * Handles real-time subscriptions using Supabase Realtime
 */
class SupabaseRealtimeService {
    
    private val realtime = SupabaseClient.client.realtime
    
    fun subscribeToMessages(chatId: String): Flow<Map<String, Any?>> {
        val channel = realtime.channel("messages:$chatId")
        
        return channel.postgresChangeFlow<Map<String, Any?>>(
            schema = "public"
        ) {
            table = "messages"
            filter = "chat_id=eq.$chatId"
        }
    }
    
    fun subscribeToUserStatus(userId: String): Flow<Map<String, Any?>> {
        val channel = realtime.channel("user_status:$userId")
        
        return channel.postgresChangeFlow<Map<String, Any?>>(
            schema = "public"
        ) {
            table = "users"
            filter = "id=eq.$userId"
        }
    }
    
    suspend fun joinChannel(channelName: String) {
        realtime.channel(channelName).join()
    }
    
    suspend fun leaveChannel(channelName: String) {
        realtime.channel(channelName).leave()
    }
}