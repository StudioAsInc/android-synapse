package com.synapse.social.studioasinc.chat.service

import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.chat.models.ReadReceiptEvent
import com.synapse.social.studioasinc.chat.models.TypingStatus
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing Supabase Realtime WebSocket connections and events.
 * Handles channel lifecycle, typing indicators, read receipts, and connection state.
 */
class SupabaseRealtimeService {
    
    companion object {
        private const val TAG = "SupabaseRealtimeService"
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val INITIAL_RECONNECT_DELAY = 2000L
        private const val POLLING_INTERVAL = 5000L
        private const val CONNECTION_TIMEOUT = 10000L
    }
    
    // Thread-safe channel map for concurrent access
    private val channels = ConcurrentHashMap<String, RealtimeChannel>()
    
    // Connection state management per chat
    private val _connectionState = MutableStateFlow<RealtimeState>(RealtimeState.Disconnected)
    val connectionState: StateFlow<RealtimeState> = _connectionState.asStateFlow()
    
    private val chatReconnectAttempts = ConcurrentHashMap<String, Int>()
    private val chatPollingFallback = ConcurrentHashMap<String, Boolean>()
    
    // Callbacks for connection status updates
    private val connectionCallbacks = mutableListOf<(RealtimeState) -> Unit>()
    
    // Track last successful connection time
    private var lastSuccessfulConnection = 0L
    
    /**
     * Subscribe to a chat room's Realtime channel.
     * Creates and manages a channel for the specified chat ID.
     * 
     * @param chatId The unique identifier for the chat room
     * @return The created or existing RealtimeChannel
     */
    suspend fun subscribeToChat(chatId: String): RealtimeChannel {
        Log.d(TAG, "Subscribing to chat: $chatId")
        
        // Return existing channel if already subscribed
        channels[chatId]?.let {
            Log.d(TAG, "Reusing existing channel for chat: $chatId")
            return it
        }
        
        return try {
            updateConnectionState(RealtimeState.Connecting)
            
            val channelName = "chat:$chatId"
            val channel = SupabaseClient.client.realtime.channel(channelName)
            
            // Subscribe to the channel with timeout handling
            channel.subscribe()
            
            // Store the channel
            channels[chatId] = channel
            
            // Reset reconnection attempts on success
            chatReconnectAttempts[chatId] = 0
            chatPollingFallback[chatId] = false
            lastSuccessfulConnection = System.currentTimeMillis()
            
            updateConnectionState(RealtimeState.Connected)
            
            Log.d(TAG, "Successfully subscribed to chat: $chatId")
            channel
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to chat: $chatId", e)
            handleSubscriptionError(chatId, e)
            throw e
        }
    }
    
    /**
     * Broadcast a typing event to the chat room.
     * 
     * @param chatId The chat room identifier
     * @param userId The user who is typing
     * @param isTyping Whether the user is currently typing
     */
    suspend fun broadcastTyping(chatId: String, userId: String, isTyping: Boolean) {
        Log.d(TAG, "Broadcasting typing event - chatId: $chatId, userId: $userId, isTyping: $isTyping")
        
        val channel = channels[chatId]
        if (channel == null) {
            Log.w(TAG, "No channel found for chatId: $chatId. Subscribing first.")
            subscribeToChat(chatId)
            return broadcastTyping(chatId, userId, isTyping)
        }
        
        try {
            val typingStatus = TypingStatus(
                userId = userId,
                chatId = chatId,
                isTyping = isTyping,
                timestamp = System.currentTimeMillis()
            )
            
            // Broadcast the typing event using the correct API
            channel.broadcast(
                event = "typing",
                payload = buildMap {
                    put("user_id", userId)
                    put("chat_id", chatId)
                    put("is_typing", isTyping)
                    put("timestamp", typingStatus.timestamp)
                }
            )
            
            Log.d(TAG, "Typing event broadcasted successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast typing event", e)
            handleBroadcastError(chatId, e)
        }
    }
    
    /**
     * Broadcast a read receipt event to the chat room.
     * 
     * @param chatId The chat room identifier
     * @param userId The user who read the messages
     * @param messageIds List of message IDs that were read
     */
    suspend fun broadcastReadReceipt(chatId: String, userId: String, messageIds: List<String>) {
        Log.d(TAG, "Broadcasting read receipt - chatId: $chatId, userId: $userId, messageCount: ${messageIds.size}")
        
        val channel = channels[chatId]
        if (channel == null) {
            Log.w(TAG, "No channel found for chatId: $chatId. Subscribing first.")
            subscribeToChat(chatId)
            return broadcastReadReceipt(chatId, userId, messageIds)
        }
        
        try {
            val readReceiptEvent = ReadReceiptEvent(
                chatId = chatId,
                userId = userId,
                messageIds = messageIds,
                timestamp = System.currentTimeMillis()
            )
            
            // Broadcast the read receipt event using the correct API
            channel.broadcast(
                event = "read_receipt",
                payload = buildMap {
                    put("chat_id", chatId)
                    put("user_id", userId)
                    put("message_ids", messageIds)
                    put("timestamp", readReceiptEvent.timestamp)
                }
            )
            
            Log.d(TAG, "Read receipt broadcasted successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast read receipt", e)
            handleBroadcastError(chatId, e)
        }
    }
    
    /**
     * Unsubscribe from a chat room's Realtime channel and clean up resources.
     * 
     * @param chatId The chat room identifier
     */
    suspend fun unsubscribeFromChat(chatId: String) {
        Log.d(TAG, "Unsubscribing from chat: $chatId")
        
        val channel = channels.remove(chatId)
        if (channel != null) {
            try {
                SupabaseClient.client.realtime.removeChannel(channel)
                Log.d(TAG, "Successfully unsubscribed from chat: $chatId")
            } catch (e: Exception) {
                Log.e(TAG, "Error unsubscribing from chat: $chatId", e)
            }
        } else {
            Log.w(TAG, "No channel found for chatId: $chatId")
        }
        
        // Update connection state if no channels remain
        if (channels.isEmpty()) {
            _connectionState.value = RealtimeState.Disconnected
            notifyConnectionCallbacks(RealtimeState.Disconnected)
        }
    }
    
    /**
     * Clean up all channels and resources.
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up all channels")
        
        channels.keys.toList().forEach { chatId ->
            unsubscribeFromChat(chatId)
        }
        
        channels.clear()
        connectionCallbacks.clear()
        _connectionState.value = RealtimeState.Disconnected
    }
    
    /**
     * Register a callback for connection state changes.
     * 
     * @param callback Function to be called when connection state changes
     */
    fun addConnectionCallback(callback: (RealtimeState) -> Unit) {
        connectionCallbacks.add(callback)
    }
    
    /**
     * Remove a connection state callback.
     * 
     * @param callback The callback to remove
     */
    fun removeConnectionCallback(callback: (RealtimeState) -> Unit) {
        connectionCallbacks.remove(callback)
    }
    
    /**
     * Get the current channel for a chat room.
     * 
     * @param chatId The chat room identifier
     * @return The RealtimeChannel if it exists, null otherwise
     */
    fun getChannel(chatId: String): RealtimeChannel? {
        return channels[chatId]
    }
    
    /**
     * Check if a chat room has an active channel subscription.
     * 
     * @param chatId The chat room identifier
     * @return true if subscribed, false otherwise
     */
    fun isSubscribed(chatId: String): Boolean {
        return channels.containsKey(chatId)
    }
    
    // Private helper methods
    
    private fun notifyConnectionCallbacks(state: RealtimeState) {
        connectionCallbacks.forEach { callback ->
            try {
                callback(state)
            } catch (e: Exception) {
                Log.e(TAG, "Error in connection callback", e)
            }
        }
    }
    
    private suspend fun handleReconnection(chatId: String) {
        val attempts = chatReconnectAttempts.getOrDefault(chatId, 0)
        
        if (attempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnection attempts reached for chat: $chatId. Falling back to polling.")
            enablePollingFallback(chatId)
            return
        }
        
        chatReconnectAttempts[chatId] = attempts + 1
        
        // Exponential backoff: 2s, 4s, 8s, 16s, 32s
        val delayMs = INITIAL_RECONNECT_DELAY * (1 shl attempts)
        
        Log.d(TAG, "Attempting reconnection ${attempts + 1}/$MAX_RECONNECT_ATTEMPTS for chat: $chatId in ${delayMs}ms")
        updateConnectionState(RealtimeState.Connecting)
        
        delay(delayMs)
        
        try {
            // Remove failed channel before retrying
            channels.remove(chatId)?.let { oldChannel ->
                try {
                    SupabaseClient.client.realtime.removeChannel(oldChannel)
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing old channel during reconnection", e)
                }
            }
            
            subscribeToChat(chatId)
            Log.i(TAG, "Reconnection successful for chat: $chatId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Reconnection attempt ${attempts + 1} failed for chat: $chatId", e)
            handleReconnection(chatId)
        }
    }
    
    private fun handleSubscriptionError(chatId: String, error: Exception) {
        Log.e(TAG, "Subscription error for chat: $chatId", error)
        
        val errorMessage = when {
            error.message?.contains("timeout", ignoreCase = true) == true -> "Connection timeout"
            error.message?.contains("network", ignoreCase = true) == true -> "Network error"
            error.message?.contains("unauthorized", ignoreCase = true) == true -> "Authentication error"
            else -> error.message ?: "Unknown error"
        }
        
        updateConnectionState(RealtimeState.Error(errorMessage))
    }
    
    private fun handleBroadcastError(chatId: String, error: Exception) {
        Log.e(TAG, "Broadcast error for chat: $chatId", error)
        
        val errorMessage = when {
            error.message?.contains("not subscribed", ignoreCase = true) == true -> {
                "Channel not subscribed"
            }
            error.message?.contains("timeout", ignoreCase = true) == true -> {
                "Broadcast timeout"
            }
            else -> error.message ?: "Broadcast failed"
        }
        
        updateConnectionState(RealtimeState.Error(errorMessage))
        
        // If channel is not subscribed, attempt to resubscribe
        if (!isSubscribed(chatId)) {
            Log.w(TAG, "Channel not subscribed, will attempt reconnection on next broadcast")
        }
    }
    
    private fun enablePollingFallback(chatId: String) {
        chatPollingFallback[chatId] = true
        updateConnectionState(RealtimeState.Error("Using polling fallback"))
        Log.w(TAG, "Polling fallback enabled for chat: $chatId. Real-time features will poll every ${POLLING_INTERVAL}ms")
    }
    
    private fun updateConnectionState(newState: RealtimeState) {
        if (_connectionState.value != newState) {
            _connectionState.value = newState
            notifyConnectionCallbacks(newState)
        }
    }
    
    /**
     * Check if the service is using polling fallback instead of WebSocket for a specific chat.
     * 
     * @param chatId The chat room identifier
     * @return true if using polling, false if using WebSocket
     */
    fun isUsingPollingFallback(chatId: String): Boolean {
        return chatPollingFallback.getOrDefault(chatId, false)
    }
    
    /**
     * Check if any chat is using polling fallback.
     * 
     * @return true if any chat is using polling, false otherwise
     */
    fun isAnyUsingPollingFallback(): Boolean {
        return chatPollingFallback.values.any { it }
    }
    
    /**
     * Get the polling interval in milliseconds.
     * 
     * @return The polling interval
     */
    fun getPollingInterval(): Long {
        return POLLING_INTERVAL
    }
    
    /**
     * Manually trigger a reconnection attempt for a specific chat.
     * Useful for user-initiated retry actions.
     * 
     * @param chatId The chat room identifier
     */
    suspend fun reconnect(chatId: String) {
        Log.d(TAG, "Manual reconnection triggered for chat: $chatId")
        
        // Reset reconnection attempts to allow retry
        chatReconnectAttempts[chatId] = 0
        chatPollingFallback[chatId] = false
        
        // Remove existing channel
        unsubscribeFromChat(chatId)
        
        // Attempt to subscribe again
        try {
            subscribeToChat(chatId)
        } catch (e: Exception) {
            Log.e(TAG, "Manual reconnection failed for chat: $chatId", e)
            throw e
        }
    }
    
    /**
     * Reconnect all active channels.
     * Useful for recovering from network changes or app resume.
     */
    suspend fun reconnectAll() {
        Log.d(TAG, "Reconnecting all channels")
        
        val chatIds = channels.keys.toList()
        chatIds.forEach { chatId ->
            try {
                reconnect(chatId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reconnect chat: $chatId", e)
            }
        }
    }
    
    /**
     * Get the time since last successful connection in milliseconds.
     * 
     * @return Time in milliseconds, or -1 if never connected
     */
    fun getTimeSinceLastConnection(): Long {
        return if (lastSuccessfulConnection > 0) {
            System.currentTimeMillis() - lastSuccessfulConnection
        } else {
            -1
        }
    }
    
    /**
     * Check if the connection is healthy based on recent activity.
     * 
     * @return true if connection is healthy, false otherwise
     */
    fun isConnectionHealthy(): Boolean {
        val timeSinceConnection = getTimeSinceLastConnection()
        return _connectionState.value is RealtimeState.Connected && 
               timeSinceConnection >= 0 && 
               timeSinceConnection < CONNECTION_TIMEOUT
    }
    
    /**
     * Get reconnection attempts for a specific chat.
     * 
     * @param chatId The chat room identifier
     * @return Number of reconnection attempts
     */
    fun getReconnectionAttempts(chatId: String): Int {
        return chatReconnectAttempts.getOrDefault(chatId, 0)
    }
}

/**
 * Represents the connection state of the Realtime service.
 */
sealed class RealtimeState {
    object Connected : RealtimeState()
    object Connecting : RealtimeState()
    object Disconnected : RealtimeState()
    data class Error(val message: String) : RealtimeState()
}
