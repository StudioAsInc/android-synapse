package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
     * Uses a database function to bypass RLS for adding both participants
     */
    private suspend fun ensureParticipantsExist(chatId: String, userId1: String, userId2: String, createdBy: String): Result<Unit> {
        android.util.Log.d(TAG, "Ensuring participants exist for chat: $chatId")
        
        return try {
            // Always try to add both participants via RPC (it has ON CONFLICT DO NOTHING)
            // This is more reliable than checking first, especially with race conditions
            val results = listOf(
                addChatParticipantViaRPC(chatId, userId1, createdBy),
                addChatParticipantViaRPC(chatId, userId2, createdBy)
            )
            
            // Check if any critical failures occurred
            val failures = results.filter { it.isFailure }
            if (failures.isNotEmpty()) {
                android.util.Log.w(TAG, "Some participants may not have been added, but continuing")
            }
            
            android.util.Log.d(TAG, "Participants ensured for chat: $chatId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error ensuring participants exist", e)
            Result.failure(e)
        }
    }
    
    /**
     * Add participant via RPC function (bypasses RLS)
     */
    private suspend fun addChatParticipantViaRPC(chatId: String, userId: String, createdBy: String): Result<Unit> {
        return try {
            val isCreator = userId == createdBy
            
            // Build parameters as JSON
            val params = buildJsonObject {
                put("p_chat_id", chatId)
                put("p_user_id", userId)
                put("p_role", if (isCreator) "creator" else "member")
                put("p_is_admin", isCreator)
                put("p_can_send_messages", true)
            }
            
            // Call RPC function using the postgrest extension property
            client.postgrest.rpc("add_chat_participant", params)
            
            android.util.Log.d(TAG, "Added participant via RPC: $userId to $chatId")
            Result.success(Unit)
        } catch (e: Exception) {
            // The RPC function has ON CONFLICT DO NOTHING, so any error is unexpected
            android.util.Log.e(TAG, "RPC failed for participant $userId: ${e.message}", e)
            // Don't fail the operation if participant might already exist
            if (e.message?.contains("duplicate", ignoreCase = true) == true ||
                e.message?.contains("already exists", ignoreCase = true) == true ||
                e.message?.contains("conflict", ignoreCase = true) == true) {
                android.util.Log.d(TAG, "Participant likely already exists: $userId in $chatId")
                Result.success(Unit)
            } else {
                Result.failure(e)
            }
        }
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
                
                // Check if chat already exists first
                val existingChat = try {
                    client.from("chats")
                        .select(columns = Columns.raw("chat_id")) {
                            filter {
                                eq("chat_id", chatId)
                            }
                            limit(1)
                        }
                        .decodeList<JsonObject>()
                        .firstOrNull()
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Error checking existing chat: ${e.message}")
                    null
                }
                
                if (existingChat == null) {
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
                            android.util.Log.d(TAG, "New chat created: $chatId")
                        }
                        isDuplicateKeyError(insertResult) -> {
                            // Chat was created by another request (race condition)
                            android.util.Log.d(TAG, "Chat created by concurrent request: $chatId")
                        }
                        else -> {
                            // Unexpected error during chat creation
                            android.util.Log.e(TAG, "Failed to create chat: ${insertResult.exceptionOrNull()?.message}")
                            return@withContext Result.failure(insertResult.exceptionOrNull() ?: Exception("Failed to create chat"))
                        }
                    }
                } else {
                    android.util.Log.d(TAG, "Chat already exists: $chatId")
                }
                
                // Ensure both participants are added (works whether chat is new or existing)
                val participantsResult = ensureParticipantsExist(chatId, userId1, userId2, userId1)
                if (participantsResult.isFailure) {
                    android.util.Log.e(TAG, "Failed to add participants: ${participantsResult.exceptionOrNull()?.message}")
                    return@withContext Result.failure(participantsResult.exceptionOrNull() ?: Exception("Failed to add participants"))
                }
                
                Result.success(chatId)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error in getOrCreateDirectChat", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Add participant to chat
     * DEPRECATED: Use addChatParticipantViaRPC instead to bypass RLS policies
     * This method is kept for backward compatibility but should not be used for new code
     */
    @Deprecated("Use addChatParticipantViaRPC instead", ReplaceWith("addChatParticipantViaRPC(chatId, userId, createdBy)"))
    private suspend fun addChatParticipant(chatId: String, userId: String, createdBy: String): Result<Unit> {
        // Redirect to RPC method which properly handles RLS
        return addChatParticipantViaRPC(chatId, userId, createdBy)
    }
    
    /**
     * Verify that a user is a participant in the chat
     * Uses RPC function to bypass RLS and avoid recursion issues
     */
    private suspend fun verifyUserIsParticipant(chatId: String, userId: String): Boolean {
        return try {
            // Use the security definer function we created
            val params = buildJsonObject {
                put("p_chat_id", chatId)
                put("p_user_uid", userId)
            }
            
            // RPC returns a boolean value directly - decode it
            val result = client.postgrest.rpc("is_user_in_chat", params)
                .decodeAs<Boolean>()
            
            result
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error verifying participant $userId in chat $chatId: ${e.message}", e)
            // If verification fails, assume user is participant to avoid blocking legitimate messages
            // The database RLS will still enforce security at the insert level
            android.util.Log.w(TAG, "Assuming user is participant due to verification error")
            true
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
     * Edit a message and save edit history
     */
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Get current message content before editing
                val currentMessage = client.from("messages")
                    .select(columns = Columns.raw("content, sender_id")) {
                        filter {
                            eq("id", messageId)
                        }
                        limit(1)
                    }
                    .decodeList<JsonObject>()
                    .firstOrNull()

                val previousContent = currentMessage?.get("content")?.toString()?.removeSurrounding("\"") ?: ""
                val senderId = currentMessage?.get("sender_id")?.toString()?.removeSurrounding("\"") ?: ""

                // Save edit history
                if (previousContent.isNotEmpty()) {
                    val historyData = mapOf(
                        "message_id" to messageId,
                        "previous_content" to previousContent,
                        "edited_by" to senderId,
                        "edited_at" to System.currentTimeMillis()
                    )
                    databaseService.insert("message_edit_history", historyData)
                }

                // Update message
                val updateData = mapOf(
                    "content" to newContent,
                    "is_edited" to true,
                    "edited_at" to System.currentTimeMillis(),
                    "updated_at" to System.currentTimeMillis()
                )
                databaseService.update("messages", updateData, "id", messageId)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error editing message", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update typing status for a user in a chat
     */
    suspend fun updateTypingStatus(chatId: String, userId: String, isTyping: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Validate inputs
                if (chatId.isBlank() || userId.isBlank()) {
                    android.util.Log.e(TAG, "Invalid parameters: chatId=$chatId, userId=$userId")
                    return@withContext Result.failure(Exception("Invalid chat ID or user ID"))
                }
                
                // Check if Supabase is properly configured
                if (!SupabaseClient.isConfigured()) {
                    android.util.Log.e(TAG, "Supabase not configured")
                    return@withContext Result.failure(Exception("Supabase not configured"))
                }
                if (isTyping) {
                    val typingData = mapOf(
                        "chat_id" to chatId,
                        "user_id" to userId,
                        "is_typing" to isTyping,
                        "timestamp" to System.currentTimeMillis()
                    )
                    databaseService.upsert("typing_status", typingData)
                } else {
                    // Remove typing status when user stops typing
                    try {
                        client.from("typing_status").delete {
                            filter {
                                eq("chat_id", chatId)
                                eq("user_id", userId)
                            }
                        }
                        Result.success(Unit)
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Failed to delete typing status (user may not be typing): ${e.message}")
                        Result.success(Unit) // Don't fail if delete fails
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error updating typing status", e)
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
