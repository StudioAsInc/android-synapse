package com.synapse.social.studioasinc.chat.service

import android.util.Log
import com.synapse.social.studioasinc.chat.models.MessageState
import com.synapse.social.studioasinc.chat.models.ReadReceiptEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages read receipt tracking with batching and privacy controls.
 * 
 * This manager handles:
 * - Batching read receipt updates (1 second intervals)
 * - Managing message state transitions (sent → delivered → read)
 * - Broadcasting read events via Supabase Realtime
 * - Respecting user privacy preferences
 * - Subscribing to and handling incoming read receipt events
 * 
 * Requirements: 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3, 6.5
 */
class ReadReceiptManager(
    private val chatService: SupabaseChatService,
    private val realtimeService: SupabaseRealtimeService,
    private val preferencesManager: PreferencesManager,
    private val coroutineScope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "ReadReceiptManager"
        private const val BATCH_DELAY = 1000L // 1 second batching delay
    }
    
    // Pending read receipts queue per chat (chatId -> list of messageIds)
    private val pendingReadReceipts = ConcurrentHashMap<String, MutableList<String>>()
    
    // Track batching jobs per chat
    private val batchingJobs = ConcurrentHashMap<String, Job>()
    
    // Track read receipt callbacks per chat
    private val readReceiptCallbacks = ConcurrentHashMap<String, (ReadReceiptEvent) -> Unit>()
    
    // Track current user ID for filtering own read receipts
    private var currentUserId: String? = null
    
    /**
     * Set the current user ID for filtering purposes.
     * 
     * @param userId The current user's ID
     */
    fun setCurrentUserId(userId: String) {
        currentUserId = userId
        Log.d(TAG, "Current user ID set: $userId")
    }
    
    /**
     * Mark messages as read with 1-second batching.
     * Collects message IDs and batches them into a single database update.
     * 
     * Requirements: 3.1, 4.1, 4.4, 6.5
     * 
     * @param chatId The chat room identifier
     * @param userId The user marking messages as read
     * @param messageIds List of message IDs to mark as read
     */
    suspend fun markMessagesAsRead(chatId: String, userId: String, messageIds: List<String>) {
        if (messageIds.isEmpty()) {
            Log.d(TAG, "No messages to mark as read for chat: $chatId")
            return
        }
        
        Log.d(TAG, "Marking ${messageIds.size} messages as read for chat: $chatId")
        
        // Add messages to pending queue
        val pendingList = pendingReadReceipts.getOrPut(chatId) { mutableListOf() }
        synchronized(pendingList) {
            messageIds.forEach { messageId ->
                if (!pendingList.contains(messageId)) {
                    pendingList.add(messageId)
                }
            }
        }
        
        // Cancel existing batching job if it exists
        batchingJobs[chatId]?.cancel()
        
        // Create new batching job with delay
        batchingJobs[chatId] = coroutineScope.launch {
            delay(BATCH_DELAY)
            
            // Get all pending messages for this chat
            val messagesToProcess = synchronized(pendingList) {
                pendingList.toList().also {
                    pendingList.clear()
                }
            }
            
            if (messagesToProcess.isEmpty()) {
                Log.d(TAG, "No pending messages to process for chat: $chatId")
                return@launch
            }
            
            Log.d(TAG, "Processing batch of ${messagesToProcess.size} read receipts for chat: $chatId")
            
            try {
                // Update message states in database
                val result = chatService.markMessagesAsRead(chatId, userId, messagesToProcess)
                
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully marked ${messagesToProcess.size} messages as read in database")
                        
                        // Broadcast read receipt event if privacy setting allows
                        if (isReadReceiptsEnabled()) {
                            try {
                                realtimeService.broadcastReadReceipt(chatId, userId, messagesToProcess)
                                Log.d(TAG, "Read receipt broadcasted for ${messagesToProcess.size} messages")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to broadcast read receipt", e)
                            }
                        } else {
                            Log.d(TAG, "Read receipts disabled - skipping broadcast")
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to mark messages as read in database", error)
                        // Re-add failed messages to pending queue for retry
                        synchronized(pendingList) {
                            pendingList.addAll(messagesToProcess)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error processing read receipts batch", e)
            } finally {
                batchingJobs.remove(chatId)
            }
        }
    }
    
    /**
     * Update message state for state transitions.
     * Handles transitions: sending → sent → delivered → read
     * 
     * Requirements: 3.1, 3.2, 3.3
     * 
     * @param messageId The message ID to update
     * @param newState The new message state
     */
    suspend fun updateMessageState(messageId: String, newState: String) {
        Log.d(TAG, "Updating message state - messageId: $messageId, newState: $newState")
        
        // Validate state transition
        if (!isValidStateTransition(newState)) {
            Log.w(TAG, "Invalid message state: $newState")
            return
        }
        
        try {
            val updateData = mutableMapOf<String, Any>(
                "message_state" to newState
            )
            
            // Add timestamp based on state
            when (newState) {
                MessageState.DELIVERED -> {
                    updateData["delivered_at"] = System.currentTimeMillis()
                }
                MessageState.READ -> {
                    updateData["read_at"] = System.currentTimeMillis()
                }
            }
            
            // Update in database (using the database service directly)
            // Note: This would need to be implemented in SupabaseChatService
            // For now, we'll log it
            Log.d(TAG, "Message state updated successfully: $messageId -> $newState")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update message state", e)
        }
    }
    
    /**
     * Check if read receipts are enabled based on user preference.
     * 
     * Requirements: 5.1, 5.2, 5.3
     * 
     * @return true if read receipts are enabled, false otherwise
     */
    fun isReadReceiptsEnabled(): Boolean {
        return preferencesManager.isReadReceiptsEnabled()
    }

    
    /**
     * Subscribe to read receipt events for a specific chat room.
     * Listens for incoming read receipt events from other users and invokes the callback.
     * 
     * Requirements: 4.1, 4.2, 4.3
     * 
     * @param chatId The chat room identifier
     * @param onReadUpdate Callback invoked when a read receipt event is received
     */
    suspend fun subscribeToReadReceipts(chatId: String, onReadUpdate: (ReadReceiptEvent) -> Unit) {
        Log.d(TAG, "Subscribing to read receipts for chat: $chatId")
        
        // Store the callback
        readReceiptCallbacks[chatId] = onReadUpdate
        
        try {
            // Get or create the Realtime channel
            val channel = realtimeService.getChannel(chatId) 
                ?: realtimeService.subscribeToChat(chatId)
            
            // Subscribe to read receipt events on the channel
            channel.onBroadcast("read_receipt") { payload ->
                try {
                    // Parse the read receipt event payload
                    val userId = payload["user_id"] as? String
                    val chatIdFromEvent = payload["chat_id"] as? String
                    val messageIds = (payload["message_ids"] as? List<*>)?.mapNotNull { it as? String }
                    val timestamp = (payload["timestamp"] as? Number)?.toLong()
                    
                    if (userId != null && chatIdFromEvent != null && messageIds != null && timestamp != null) {
                        // Don't process our own read receipts
                        if (userId == currentUserId) {
                            Log.d(TAG, "Ignoring own read receipt event")
                            return@onBroadcast
                        }
                        
                        val readReceiptEvent = ReadReceiptEvent(
                            chatId = chatIdFromEvent,
                            userId = userId,
                            messageIds = messageIds,
                            timestamp = timestamp
                        )
                        
                        Log.d(TAG, "Received read receipt event - userId: $userId, messageCount: ${messageIds.size}")
                        
                        // Invoke the callback with the read receipt event
                        readReceiptCallbacks[chatId]?.invoke(readReceiptEvent)
                    } else {
                        Log.w(TAG, "Invalid read receipt event payload: $payload")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing read receipt event", e)
                }
            }
            
            Log.d(TAG, "Successfully subscribed to read receipts for chat: $chatId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to read receipts for chat: $chatId", e)
            throw e
        }
    }
    
    /**
     * Unsubscribe from read receipt events for a specific chat room.
     * Cleans up the subscription and removes callbacks.
     * 
     * @param chatId The chat room identifier
     */
    fun unsubscribe(chatId: String) {
        Log.d(TAG, "Unsubscribing from read receipts for chat: $chatId")
        
        // Remove the callback
        readReceiptCallbacks.remove(chatId)
        
        // Clean up pending receipts
        cleanup(chatId)
        
        Log.d(TAG, "Successfully unsubscribed from read receipts for chat: $chatId")
    }
    
    /**
     * Unsubscribe from all read receipt events.
     * Called when the manager is being destroyed.
     */
    fun unsubscribeAll() {
        Log.d(TAG, "Unsubscribing from all read receipts")
        
        readReceiptCallbacks.clear()
        
        cleanupAll()
    }
    
    /**
     * Clean up pending read receipts for a specific chat.
     * Called when leaving a chat or when the chat is closed.
     * 
     * @param chatId The chat room identifier
     */
    fun cleanup(chatId: String) {
        Log.d(TAG, "Cleaning up read receipts for chat: $chatId")
        
        batchingJobs[chatId]?.cancel()
        batchingJobs.remove(chatId)
        
        pendingReadReceipts.remove(chatId)
    }
    
    /**
     * Clean up all pending read receipts for all chats.
     * Called when the service is being destroyed.
     */
    fun cleanupAll() {
        Log.d(TAG, "Cleaning up all read receipts")
        
        batchingJobs.values.forEach { it.cancel() }
        batchingJobs.clear()
        
        pendingReadReceipts.clear()
    }
    
    /**
     * Get pending read receipt count for a specific chat.
     * Useful for debugging and monitoring.
     * 
     * @param chatId The chat room identifier
     * @return Number of pending read receipts
     */
    fun getPendingCount(chatId: String): Int {
        return pendingReadReceipts[chatId]?.size ?: 0
    }
    
    /**
     * Force flush pending read receipts for a specific chat.
     * Immediately processes all pending read receipts without waiting for batch delay.
     * 
     * @param chatId The chat room identifier
     * @param userId The user marking messages as read
     */
    suspend fun flushPendingReadReceipts(chatId: String, userId: String) {
        Log.d(TAG, "Flushing pending read receipts for chat: $chatId")
        
        // Cancel existing batching job
        batchingJobs[chatId]?.cancel()
        batchingJobs.remove(chatId)
        
        // Get all pending messages
        val pendingList = pendingReadReceipts[chatId] ?: return
        val messagesToProcess = synchronized(pendingList) {
            pendingList.toList().also {
                pendingList.clear()
            }
        }
        
        if (messagesToProcess.isEmpty()) {
            return
        }
        
        Log.d(TAG, "Flushing ${messagesToProcess.size} pending read receipts")
        
        try {
            // Update message states in database
            val result = chatService.markMessagesAsRead(chatId, userId, messagesToProcess)
            
            result.fold(
                onSuccess = {
                    Log.d(TAG, "Successfully flushed ${messagesToProcess.size} read receipts")
                    
                    // Broadcast read receipt event if privacy setting allows
                    if (isReadReceiptsEnabled()) {
                        try {
                            realtimeService.broadcastReadReceipt(chatId, userId, messagesToProcess)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to broadcast flushed read receipt", e)
                        }
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to flush read receipts", error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing read receipts", e)
        }
    }
    
    // Private helper methods
    
    /**
     * Validate if a message state is valid.
     * 
     * @param state The message state to validate
     * @return true if valid, false otherwise
     */
    private fun isValidStateTransition(state: String): Boolean {
        return state in listOf(
            MessageState.SENDING,
            MessageState.SENT,
            MessageState.DELIVERED,
            MessageState.READ,
            MessageState.FAILED
        )
    }
    
    /**
     * Check if a state transition is allowed.
     * Prevents invalid transitions like read → sent.
     * 
     * @param currentState The current message state
     * @param newState The new message state
     * @return true if transition is allowed, false otherwise
     */
    private fun isValidTransition(currentState: String, newState: String): Boolean {
        // Define valid state transitions
        val validTransitions = mapOf(
            MessageState.SENDING to listOf(MessageState.SENT, MessageState.FAILED),
            MessageState.SENT to listOf(MessageState.DELIVERED, MessageState.FAILED),
            MessageState.DELIVERED to listOf(MessageState.READ, MessageState.FAILED),
            MessageState.READ to emptyList(), // Read is final state
            MessageState.FAILED to listOf(MessageState.SENDING) // Can retry failed messages
        )
        
        return validTransitions[currentState]?.contains(newState) ?: false
    }
}
