package com.synapse.social.studioasinc.chat.service

import android.util.Log
import com.synapse.social.studioasinc.chat.models.TypingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages typing indicator events with debouncing and auto-stop functionality.
 * 
 * This manager handles:
 * - Debouncing typing events to prevent excessive broadcasts (500ms)
 * - Auto-stopping typing indicators after inactivity (3 seconds)
 * - Managing coroutine jobs per chat room
 * - Subscribing to and handling incoming typing events
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 6.1, 6.4
 */
class TypingIndicatorManager(
    private val realtimeService: SupabaseRealtimeService,
    private val coroutineScope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "TypingIndicatorManager"
        private const val DEBOUNCE_DELAY = 500L // 500ms debounce for typing events
        private const val TYPING_TIMEOUT = 3000L // 3 seconds auto-stop timeout
    }
    
    // Track typing jobs per chat room to manage debouncing
    private val typingJobs = ConcurrentHashMap<String, Job>()
    
    // Track auto-stop jobs per chat room
    private val autoStopJobs = ConcurrentHashMap<String, Job>()
    
    // Track last typing event time per chat
    private val lastTypingTime = ConcurrentHashMap<String, Long>()
    
    // Track if user is currently typing in each chat
    private val isTypingInChat = ConcurrentHashMap<String, Boolean>()
    
    // Track typing event callbacks per chat
    private val typingCallbacks = ConcurrentHashMap<String, (TypingStatus) -> Unit>()
    
    /**
     * Called when the user types in the message input field.
     * Implements debouncing to send typing events at most once per 500ms.
     * Also sets up auto-stop timer for 3 seconds of inactivity.
     * 
     * Requirements: 1.1, 1.3, 1.4, 6.1
     * 
     * @param chatId The chat room identifier
     * @param userId The current user's ID
     */
    fun onUserTyping(chatId: String, userId: String) {
        Log.d(TAG, "User typing in chat: $chatId")
        
        val currentTime = System.currentTimeMillis()
        val lastTime = lastTypingTime[chatId] ?: 0L
        val timeSinceLastEvent = currentTime - lastTime
        
        // Cancel existing typing job if it exists
        typingJobs[chatId]?.cancel()
        
        // Cancel existing auto-stop job
        autoStopJobs[chatId]?.cancel()
        
        // Check if we need to send a typing event (debounce logic)
        val shouldSendEvent = timeSinceLastEvent >= DEBOUNCE_DELAY || !isTypingInChat.getOrDefault(chatId, false)
        
        if (shouldSendEvent) {
            // Send typing event immediately
            typingJobs[chatId] = coroutineScope.launch {
                try {
                    realtimeService.broadcastTyping(chatId, userId, true)
                    lastTypingTime[chatId] = currentTime
                    isTypingInChat[chatId] = true
                    Log.d(TAG, "Typing event sent for chat: $chatId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to broadcast typing event for chat: $chatId", e)
                }
            }
        }
        
        // Set up auto-stop timer (3 seconds of inactivity)
        autoStopJobs[chatId] = coroutineScope.launch {
            delay(TYPING_TIMEOUT)
            Log.d(TAG, "Auto-stopping typing indicator for chat: $chatId after ${TYPING_TIMEOUT}ms inactivity")
            onUserStoppedTyping(chatId, userId)
        }
    }
    
    /**
     * Called when the user stops typing or sends a message.
     * Broadcasts a typing-stopped event and cleans up resources.
     * 
     * Requirements: 1.4, 1.5
     * 
     * @param chatId The chat room identifier
     * @param userId The current user's ID
     */
    fun onUserStoppedTyping(chatId: String, userId: String) {
        Log.d(TAG, "User stopped typing in chat: $chatId")
        
        // Cancel any pending typing jobs
        typingJobs[chatId]?.cancel()
        typingJobs.remove(chatId)
        
        // Cancel auto-stop job
        autoStopJobs[chatId]?.cancel()
        autoStopJobs.remove(chatId)
        
        // Only send stopped event if we were actually typing
        if (isTypingInChat.getOrDefault(chatId, false)) {
            coroutineScope.launch {
                try {
                    realtimeService.broadcastTyping(chatId, userId, false)
                    isTypingInChat[chatId] = false
                    lastTypingTime.remove(chatId)
                    Log.d(TAG, "Typing stopped event sent for chat: $chatId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to broadcast typing stopped event for chat: $chatId", e)
                }
            }
        }
    }
    
    /**
     * Clean up all typing jobs for a specific chat.
     * Called when leaving a chat or when the chat is closed.
     * 
     * @param chatId The chat room identifier
     */
    fun cleanup(chatId: String) {
        Log.d(TAG, "Cleaning up typing indicator for chat: $chatId")
        
        typingJobs[chatId]?.cancel()
        typingJobs.remove(chatId)
        
        autoStopJobs[chatId]?.cancel()
        autoStopJobs.remove(chatId)
        
        lastTypingTime.remove(chatId)
        isTypingInChat.remove(chatId)
    }
    
    /**
     * Clean up all typing jobs for all chats.
     * Called when the service is being destroyed.
     */
    fun cleanupAll() {
        Log.d(TAG, "Cleaning up all typing indicators")
        
        typingJobs.values.forEach { it.cancel() }
        typingJobs.clear()
        
        autoStopJobs.values.forEach { it.cancel() }
        autoStopJobs.clear()
        
        lastTypingTime.clear()
        isTypingInChat.clear()
    }
    
    /**
     * Check if the user is currently typing in a specific chat.
     * 
     * @param chatId The chat room identifier
     * @return true if user is typing, false otherwise
     */
    fun isUserTyping(chatId: String): Boolean {
        return isTypingInChat.getOrDefault(chatId, false)
    }
    
    /**
     * Subscribe to typing events for a specific chat room.
     * Listens for incoming typing events from other users and invokes the callback.
     * 
     * Requirements: 1.2, 6.4
     * 
     * @param chatId The chat room identifier
     * @param onTypingUpdate Callback invoked when a typing event is received
     */
    suspend fun subscribeToTypingEvents(chatId: String, onTypingUpdate: (TypingStatus) -> Unit) {
        Log.d(TAG, "Subscribing to typing events for chat: $chatId")
        
        // Store the callback
        typingCallbacks[chatId] = onTypingUpdate
        
        try {
            // Get or create the Realtime channel
            val channel = realtimeService.getChannel(chatId) 
                ?: realtimeService.subscribeToChat(chatId)
            
            // Subscribe to typing events on the channel
            channel.onBroadcast("typing") { payload ->
                try {
                    // Parse the typing event payload
                    val userId = payload["user_id"] as? String
                    val chatIdFromEvent = payload["chat_id"] as? String
                    val isTyping = payload["is_typing"] as? Boolean
                    val timestamp = (payload["timestamp"] as? Number)?.toLong()
                    
                    if (userId != null && chatIdFromEvent != null && isTyping != null && timestamp != null) {
                        val typingStatus = TypingStatus(
                            userId = userId,
                            chatId = chatIdFromEvent,
                            isTyping = isTyping,
                            timestamp = timestamp
                        )
                        
                        Log.d(TAG, "Received typing event - userId: $userId, isTyping: $isTyping")
                        
                        // Invoke the callback with the typing status
                        typingCallbacks[chatId]?.invoke(typingStatus)
                    } else {
                        Log.w(TAG, "Invalid typing event payload: $payload")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing typing event", e)
                }
            }
            
            Log.d(TAG, "Successfully subscribed to typing events for chat: $chatId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to typing events for chat: $chatId", e)
            throw e
        }
    }
    
    /**
     * Unsubscribe from typing events for a specific chat room.
     * Cleans up the subscription and removes callbacks.
     * 
     * Requirements: 6.4
     * 
     * @param chatId The chat room identifier
     */
    fun unsubscribe(chatId: String) {
        Log.d(TAG, "Unsubscribing from typing events for chat: $chatId")
        
        // Remove the callback
        typingCallbacks.remove(chatId)
        
        // Clean up typing state
        cleanup(chatId)
        
        Log.d(TAG, "Successfully unsubscribed from typing events for chat: $chatId")
    }
    
    /**
     * Unsubscribe from all typing events.
     * Called when the manager is being destroyed.
     */
    fun unsubscribeAll() {
        Log.d(TAG, "Unsubscribing from all typing events")
        
        typingCallbacks.clear()
        
        cleanupAll()
    }
}
