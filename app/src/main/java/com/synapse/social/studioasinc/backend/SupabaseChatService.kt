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
    
    private val client = SupabaseClient.client
    private val databaseService = SupabaseDatabaseService()
    
    /**
     * Create or get existing chat between two users
     */
    suspend fun getOrCreateDirectChat(userId1: String, userId2: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Generate consistent chat ID for direct chats
                val chatId = if (userId1 < userId2) {
                    "dm_${userId1}_${userId2}"
                } else {
                    "dm_${userId2}_${userId1}"
                }
                
                // Check if chat exists
                val existingChat = databaseService.selectWhere("chats", "*", "chat_id", chatId)
                
                existingChat.fold(
                    onSuccess = { chats ->
                        if (chats.isNotEmpty()) {
                            // Chat exists
                            Result.success(chatId)
                        } else {
                            // Create new chat
                            val chatData = mapOf(
                                "chat_id" to chatId,
                                "is_group" to false,
                                "created_by" to userId1,
                                "participants_count" to 2,
                                "is_active" to true
                            )
                            
                            databaseService.insert("chats", chatData).fold(
                                onSuccess = {
                                    // Add participants
                                    addChatParticipant(chatId, userId1)
                                    addChatParticipant(chatId, userId2)
                                    Result.success(chatId)
                                },
                                onFailure = { error -> Result.failure(error) }
                            )
                        }
                    },
                    onFailure = { error -> Result.failure(error) }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Add participant to chat
     */
    private suspend fun addChatParticipant(chatId: String, userId: String): Result<Unit> {
        return try {
            val participantData = mapOf(
                "chat_id" to chatId,
                "user_id" to userId,
                "role" to "member",
                "is_admin" to false,
                "can_send_messages" to true
            )
            databaseService.insert("chat_participants", participantData)
        } catch (e: Exception) {
            Result.failure(e)
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
                // Get chat IDs where user is a participant
                val participantResult = client.from("chat_participants")
                    .select(columns = Columns.raw("chat_id")) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<JsonObject>()
                
                val chatIds = participantResult.map { 
                    it["chat_id"].toString().removeSurrounding("\"") 
                }
                
                if (chatIds.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }
                
                // Get chat details
                val chatsResult = client.from("chats")
                    .select(columns = Columns.raw("*")) {
                        filter {
                            isIn("chat_id", chatIds)
                            eq("is_active", true)
                        }

                    }
                    .decodeList<JsonObject>()
                
                val chats = chatsResult.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        value.toString().removeSurrounding("\"")
                    }
                }
                
                Result.success(chats)
            } catch (e: Exception) {
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
}
