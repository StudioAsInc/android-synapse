package com.synapse.social.studioasinc.backend

import io.github.jan.tennert.supabase.realtime.PostgresAction
import io.github.jan.tennert.supabase.realtime.channel
import io.github.jan.tennert.supabase.realtime.postgresChangeFlow
import io.github.jan.tennert.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import com.synapse.social.studioasinc.SupabaseClient

/**
 * Supabase realtime service for handling real-time database changes.
 * Provides real-time subscriptions for messages, user status, and other live data.
 */
class SupabaseRealtimeService {
    
    private val realtime = SupabaseClient.client.realtime
    
    /**
     * Subscribe to real-time message updates for a specific chat.
     */
    fun subscribeToMessages(chatId: String): Flow<Map<String, Any?>> {
        val channel = realtime.channel("messages:$chatId")
        
        return channel.postgresChangeFlow<Map<String, Any?>>(
            schema = "public"
        ) {
            table = "messages"
            filter = "chat_id=eq.$chatId"
        }.filter { change ->
            // Only listen for INSERT events (new messages)
            change.eventType == PostgresAction.INSERT
        }.map { change ->
            change.record
        }
    }
    
    /**
     * Subscribe to real-time group message updates.
     */
    fun subscribeToGroupMessages(groupId: String): Flow<Map<String, Any?>> {
        val channel = realtime.channel("group_messages:$groupId")
        
        return channel.postgresChangeFlow<Map<String, Any?>>(
            schema = "public"
        ) {
            table = "group_messages"
            filter = "group_id=eq.$groupId"
        }.filter { change ->
            change.eventType == PostgresAction.INSERT
        }.map { change ->
            change.record
        }
    }
    
    /**
     * Subscribe to user status changes (online/offline).
     */
    fun subscribeToUserStatus(userId: String): Flow<Map<String, Any?>> {
        val channel = realtime.channel("user_status:$userId")
        
        return channel.postgresChangeFlow<Map<String, Any?>>(
            schema = "public"
        ) {
            table = "users"
            filter = "id=eq.$userId"
        }.filter { change ->
            change.eventType == PostgresAction.UPDATE
        }.map { change ->
            change.record
        }
    }
    
    /**
     * Subscribe to typing indicators for a chat.
     */
    fun subscribeToTypingStatus(chatId: String): Flow<Map<String, Any?>> {
        val channel = realtime.channel("typing:$chatId")
        
        return channel.postgresChangeFlow<Map<String, Any?>>(
            schema = "public"
        ) {
            table = "typing_status"
            filter = "chat_id=eq.$chatId"
        }.map { change ->
            change.record
        }
    }
    
    /**
     * Subscribe to inbox updates for a user.
     */
    fun subscribeToInbox(userId: String): Flow<Map<String, Any?>> {
        val channel = realtime.channel("inbox:$userId")
        
        return channel.postgresChangeFlow<Map<String, Any?>>(
            schema = "public"
        ) {
            table = "inbox"
            filter = "user_id=eq.$userId"
        }.map { change ->
            change.record
        }
    }
    
    /**
     * Join a realtime channel.
     */
    suspend fun joinChannel(channelName: String) {
        try {
            realtime.channel(channelName).join()
        } catch (e: Exception) {
            // Handle join error
        }
    }
    
    /**
     * Leave a realtime channel.
     */
    suspend fun leaveChannel(channelName: String) {
        try {
            realtime.channel(channelName).leave()
        } catch (e: Exception) {
            // Handle leave error
        }
    }
    
    /**
     * Send a presence update (for user online status).
     */
    suspend fun updatePresence(channelName: String, presenceData: Map<String, Any?>) {
        try {
            val channel = realtime.channel(channelName)
            channel.track(presenceData)
        } catch (e: Exception) {
            // Handle presence update error
        }
    }
    
    /**
     * Subscribe to presence changes in a channel.
     */
    fun subscribeToPresence(channelName: String): Flow<Map<String, Any?>> {
        val channel = realtime.channel(channelName)
        
        return channel.presenceChangeFlow().map { presenceChange ->
            mapOf(
                "joins" to presenceChange.joins,
                "leaves" to presenceChange.leaves
            )
        }
    }
}