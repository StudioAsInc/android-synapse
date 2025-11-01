package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import java.util.*

/**
 * Supabase Chat Service
 * Handles chat and messaging operations
 */
class SupabaseChatService {
    
    companion object {
        private const val TAG = "SupabaseChatService"
    }
    
    private val client = SupabaseClient.client
    private val databaseService = SupabaseDatabaseService()
    
    /**
     * Check if a Result contains a duplicate key constraint violation error
     */
    private fun isDuplicateKeyError(result: Result<Unit>): Boolean {
        return result.exceptionOrNull()?.message?.contains("duplicate key", ignoreCase = true) == true
    }
    
    /**
     * Ensure both participants exist in the chat
     * Handles duplicate key errors gracefully as participants may already exist
     */
    private suspend fun ensureParticipantsExist(chatId: String, userId1: String, userId2: String) {
        android.util.Log.d(TAG, "Ensuring participants exist for chat: $chatId")
        addChatParticipant(chatId, userId1)
        addChatParticipant(chatId, userId2)
    }
    
    /**
     * Create or get existing chat between two users
     * Uses try-insert-catch-retrieve pattern to handle race conditions gracefully
     */
    suspend fun getOrCreateDirectChat(userId1: String, userId2: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Validate inputs
                if (userId1.isEmpty() || userId2.isEmpty()) {
                    android.util.Log.e(TAG, "Invalid user IDs: userId1=$userId1, userId2=$userId2")
                    return@withContext Result.failure(Exception("Invalid user IDs"))
                }
                
                // Prevent self-messaging
                if (userId1 == userId2) {
                    android.util.Log.d(TAG, "Attempted self-messaging: $userId1")
                    return@withContext Result.failure(Exception("Cannot create chat with yourself"))
                }
                
                // Check if Supabase is properly configured
                if (!SupabaseClient.isConfigured()) {
                    android.util.Log.e(TAG, "Supabase not configured")
                    return@withContext Result.failure(Exception("Supabase not configured"))
                }
                
                // Generate consistent chat ID for direct chats
                val chatId = if (userId1 < userId2) {
                    "dm_${userId1}_${userId2}"
                } else {
                    "dm_${userId2}_${userId1}"
                }
                
                android.util.Log.d(TAG, "Creating/retrieving chat between $userId1 and $userId2: $chatId")
                
                // First check if chat already exists
                val existingChat = client.from("chats")
                    .select(columns = Columns.raw("chat_id")) {
                        filter {
                            eq("chat_id", chatId)
                        }
                        limit(1)
                    }
                    .decodeList<JsonObject>()
                
                if (existingChat.isNotEmpty()) {
                    android.util.Log.d(TAG, "Chat already exists: $chatId")
                    ensureParticipantsExist(chatId, userId1, userId2)
                    return@withContext Result.success(chatId)
                }
                
                // Try to insert new chat
                val chatData = mapOf(
                    "chat_id" to chatId,
                    "is_group" to false,
                    "created_by" to userId1,
                    "participants_count" to 2,
                    "is_active" to true,
                    "created_at" to System.currentTimeMillis()
                )
                
                val insertResult = databaseService.insert("chats", chatData)
                
                when {
                    insertResult.isSuccess -> {
                        // New chat created successfully
                        android.util.Log.d(TAG, "New chat created: $chatId")
                        ensureParticipantsExist(chatId, userId1, userId2)
                        Result.success(chatId)
                    }
                    isDuplicateKeyError(insertResult) -> {
                        // Chat already exists (race condition)
                        android.util.Log.d(TAG, "Chat already exists (race condition): $chatId")
                        ensureParticipantsExist(chatId, userId1, userId2)
                        Result.success(chatId)
                    }
                    else -> {
                        // Unexpected error
                        android.util.Log.e(TAG, "Failed to create chat: ${insertResult.exceptionOrNull()?.message}")
                        insertResult.map { chatId }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error in getOrCreateDirectChat", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Add participant to chat
     * Handles duplicate key errors gracefully as participant may already exist
     */
    private suspend fun addChatParticipant(chatId: String, userId: String): Result<Unit> {
        return try {
            // First check if participant already exists
            val existingParticipant = client.from("chat_participants")
                .select(columns = Columns.raw("user_id")) {
                    filter {
                        eq("chat_id", chatId)
                        eq("user_id", userId)
                    }
                    limit(1)
                }
                .decodeList<JsonObject>()
            
            if (existingParticipant.isNotEmpty()) {
                android.util.Log.d(TAG, "Participant already exists: $userId in $chatId")
                return Result.success(Unit)
            }
            
            val participantData = mapOf(
                "chat_id" to chatId,
                "user_id" to userId,
                "role" to "member",
                "is_admin" to false,
                "can_send_messages" to true,
                "joined_at" to System.currentTimeMillis()
            )
            
            val result = databaseService.insert("chat_participants", participantData)
            
            // If duplicate, that's fine - participant already exists
            if (isDuplicateKeyError(result)) {
                android.util.Log.d(TAG, "Participant already exists (race condition): $userId in $chatId")
                Result.success(Unit)
            } else {
                result
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error adding participant $userId to chat $chatId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Verify that a user is a participant in the chat
     */
    private suspend fun verifyUserIsParticipant(chatId: String, userId: String): Boolean {
        return try {
            val participants = client.from("chat_participants")
                .select(columns = Columns.raw("user_id")) {
                    filter {
                        eq("chat_id", chatId)
                        eq("user_id", userId)
                    }
                    limit(1)
                }
                .decodeList<JsonObject>()
            
            participants.isNotEmpty()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error verifying participant $userId in chat $chatId", e)
            false
        }
    }
    
    /**
     * Send a message
     */
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        content: String,
        messageType: String = "text",
        replyToId: String? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Supabase is properly configured
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.failure(Exception("Supabase not configured"))
                }
                
                // Validate that user is participant in this chat
                val isParticipant = verifyUserIsParticipant(chatId, senderId)
                if (!isParticipant) {
                    android.util.Log.e(TAG, "User $senderId is not a participant in chat $chatId")
                    return@withContext Result.failure(Exception("User is not a participant in this chat"))
                }
                
                val messageId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()
                
                val messageData = mutableMapOf<String, Any?>(
                    "chat_id" to chatId,
                    "sender_id" to senderId,
                    "content" to content,
                    "message_type" to messageType,
                    "created_at" to timestamp,
                    "updated_at" to timestamp,
                    "delivery_status" to "sent",
                    "is_deleted" to false,
                    "is_edited" to false
                )
                
                if (replyToId != null) {
                    messageData["reply_to_id"] = replyToId
                }
                
                databaseService.insert("messages", messageData).fold(
                    onSuccess = {
                        // Update chat's last message
                        updateChatLastMessage(chatId, content, timestamp, senderId)
                        Result.success(messageId)
                    },
                    onFailure = { error -> Result.failure(error) }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update chat's last message info
     */
    private suspend fun updateChatLastMessage(
        chatId: String,
        lastMessage: String,
        timestamp: Long,
        senderId: String
    ): Result<Unit> {
        return try {
            val updateData = mapOf(
                "last_message" to lastMessage,
                "last_message_time" to timestamp,
                "last_message_sender" to senderId
            )
            databaseService.update("chats", updateData, "chat_id", chatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get messages for a chat
     */
    suspend fun getMessages(chatId: String, limit: Int = 50): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Supabase is properly configured
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.success(emptyList())
                }
                val result = client.from("messages")
                    .select(columns = Columns.raw("*")) {
                        filter {
                            eq("chat_id", chatId)
                            eq("is_deleted", false)
                        }

                        limit(limit.toLong())
                    }
                    .decodeList<JsonObject>()
                
                val messages = result.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        value.toString().removeSurrounding("\"")
                    }
                }.reversed() // Reverse to show oldest first
                
                Result.success(messages)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get user's chats
     */
    suspend fun getUserChats(userId: String): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("SupabaseChatService", "Getting chats for user: $userId")
                
                // Check if Supabase is properly configured
                if (!SupabaseClient.isConfigured()) {
                    android.util.Log.w("SupabaseChatService", "Supabase not configured, returning empty chat list")
                    return@withContext Result.success(emptyList())
                }
                
                // Get chat IDs where user is a participant
                val participantResult = client.from("chat_participants")
                    .select(columns = Columns.raw("chat_id")) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<JsonObject>()
                
                android.util.Log.d("SupabaseChatService", "Found ${participantResult.size} participant records")
                
                val chatIds = participantResult.map { 
                    it["chat_id"].toString().removeSurrounding("\"") 
                }
                
                if (chatIds.isEmpty()) {
                    android.util.Log.d("SupabaseChatService", "No chats found for user")
                    return@withContext Result.success(emptyList())
                }
                
                android.util.Log.d("SupabaseChatService", "Chat IDs: $chatIds")
                
                // Get chat details
                val chatsResult = client.from("chats")
                    .select(columns = Columns.raw("*")) {
                        filter {
                            isIn("chat_id", chatIds)
                            eq("is_active", true)
                        }
                    }
                    .decodeList<JsonObject>()
                
                android.util.Log.d("SupabaseChatService", "Found ${chatsResult.size} active chats")
                
                val chats = chatsResult.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        value.toString().removeSurrounding("\"")
                    }
                }
                
                Result.success(chats)
            } catch (e: Exception) {
                android.util.Log.e("SupabaseChatService", "Failed to load chats", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Mark messages as read
     */
    suspend fun markMessagesAsRead(chatId: String, userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Update last_read_at for the participant
                val updateData = mapOf(
                    "last_read_at" to System.currentTimeMillis()
                )
                
                client.from("chat_participants").update(updateData) {
                    filter {
                        eq("chat_id", chatId)
                        eq("user_id", userId)
                    }
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Delete a message
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            val updateData = mapOf("is_deleted" to true)
            databaseService.update("messages", updateData, "id", messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Edit a message
     */
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit> {
        return try {
            val updateData = mapOf(
                "content" to newContent,
                "is_edited" to true,
                "edited_at" to System.currentTimeMillis(),
                "updated_at" to System.currentTimeMillis()
            )
            databaseService.update("messages", updateData, "id", messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update typing status for a user in a chat
     */
    suspend fun updateTypingStatus(chatId: String, userId: String, isTyping: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val typingData = mapOf(
                    "chat_id" to chatId,
                    "user_id" to userId,
                    "is_typing" to isTyping,
                    "timestamp" to System.currentTimeMillis()
                )
                
                if (isTyping) {
                    databaseService.upsert("typing_status", typingData)
                } else {
                    // Remove typing status when user stops typing
                    client.from("typing_status").delete {
                        filter {
                            eq("chat_id", chatId)
                            eq("user_id", userId)
                        }
                    }
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get typing users in a chat
     */
    suspend fun getTypingUsers(chatId: String, excludeUserId: String): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val fiveSecondsAgo = System.currentTimeMillis() - 5000
                
                val typingUsers = client.from("typing_status")
                    .select(columns = Columns.raw("user_id")) {
                        filter {
                            eq("chat_id", chatId)
                            eq("is_typing", true)
                            neq("user_id", excludeUserId)
                            gte("timestamp", fiveSecondsAgo)
                        }
                    }
                    .decodeList<JsonObject>()
                
                val userIds = typingUsers.map { 
                    it["user_id"].toString().removeSurrounding("\"") 
                }
                Result.success(userIds)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get chat participants
     */
    suspend fun getChatParticipants(chatId: String): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val participants = client.from("chat_participants")
                    .select(columns = Columns.raw("user_id")) {
                        filter { eq("chat_id", chatId) }
                    }
                    .decodeList<JsonObject>()
                
                val userIds = participants.map { 
                    it["user_id"].toString().removeSurrounding("\"") 
                }
                Result.success(userIds)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update message delivery status
     */
    suspend fun updateMessageDeliveryStatus(
        messageId: String,
        status: String
    ): Result<Unit> {
        return try {
            val updateData = mapOf(
                "delivery_status" to status,
                "updated_at" to System.currentTimeMillis()
            )
            databaseService.update("messages", updateData, "id", messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get unread message count for a chat
     */
    suspend fun getUnreadMessageCount(chatId: String, userId: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                // Get user's last read timestamp
                val participant = client.from("chat_participants")
                    .select(columns = Columns.raw("last_read_at")) {
                        filter {
                            eq("chat_id", chatId)
                            eq("user_id", userId)
                        }
                        limit(1)
                    }
                    .decodeList<JsonObject>()
                
                val lastReadAt = participant.firstOrNull()
                    ?.get("last_read_at")
                    ?.toString()
                    ?.removeSurrounding("\"")
                    ?.toLongOrNull() ?: 0L
                
                // Count messages after last read time
                val unreadMessages = client.from("messages")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            eq("chat_id", chatId)
                            neq("sender_id", userId)
                            gt("created_at", lastReadAt)
                            eq("is_deleted", false)
                        }
                    }
                    .decodeList<JsonObject>()
                
                Result.success(unreadMessages.size)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Add a reaction to a message
     */
    suspend fun addReaction(messageId: String, userId: String, emoji: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val reactionId = UUID.randomUUID().toString()
                val reactionData = mapOf(
                    "id" to reactionId,
                    "message_id" to messageId,
                    "user_id" to userId,
                    "emoji" to emoji,
                    "created_at" to System.currentTimeMillis()
                )
                
                databaseService.insert("message_reactions", reactionData).fold(
                    onSuccess = { Result.success(reactionId) },
                    onFailure = { error -> Result.failure(error) }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Remove a reaction from a message
     */
    suspend fun removeReaction(messageId: String, userId: String, emoji: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("message_reactions").delete {
                    filter {
                        eq("message_id", messageId)
                        eq("user_id", userId)
                        eq("emoji", emoji)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get reactions for a message
     */
    suspend fun getMessageReactions(messageId: String): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                val reactions = client.from("message_reactions")
                    .select(columns = Columns.raw("*")) {
                        filter { eq("message_id", messageId) }
                    }
                    .decodeList<JsonObject>()
                
                val reactionsList = reactions.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        value.toString().removeSurrounding("\"")
                    }
                }
                
                Result.success(reactionsList)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Toggle reaction on a message (add if not exists, remove if exists)
     */
    suspend fun toggleReaction(messageId: String, userId: String, emoji: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if reaction already exists
                val existingReactions = client.from("message_reactions")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            eq("message_id", messageId)
                            eq("user_id", userId)
                            eq("emoji", emoji)
                        }
                        limit(1)
                    }
                    .decodeList<JsonObject>()
                
                if (existingReactions.isNotEmpty()) {
                    // Remove reaction
                    removeReaction(messageId, userId, emoji)
                    Result.success(false) // Reaction removed
                } else {
                    // Add reaction
                    addReaction(messageId, userId, emoji)
                    Result.success(true) // Reaction added
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
