package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.backend.SupabaseChatService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.model.Chat
import com.synapse.social.studioasinc.model.Message
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject

/**
 * Repository for chat operations
 * Provides a clean API for chat functionality with proper data mapping
 */
class ChatRepository {

    private val chatService = SupabaseChatService()
    private val databaseService = SupabaseDatabaseService()
    private val client = SupabaseClient.client

    /**
     * Creates a new chat or gets existing one
     */
    suspend fun createChat(participantUids: List<String>, chatName: String? = null): Result<String> {
        return if (participantUids.size == 2) {
            chatService.getOrCreateDirectChat(participantUids[0], participantUids[1])
        } else {
            Result.failure(Exception("Group chats not yet implemented"))
        }
    }

    /**
     * Sends a message
     */
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        content: String,
        messageType: String = "text",
        replyToId: String? = null
    ): Result<String> {
        return chatService.sendMessage(
            chatId = chatId,
            senderId = senderId,
            content = content,
            messageType = messageType,
            replyToId = replyToId
        )
    }

    /**
     * Gets messages for a chat with proper data mapping
     */
    suspend fun getMessages(chatId: String, limit: Int = 50, offset: Int = 0): Result<List<Message>> {
        return try {
            val result = chatService.getMessages(chatId, limit)
            result.map { messagesList ->
                messagesList.map { messageData ->
                    mapToMessage(messageData)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets user's chats with proper data mapping
     */
    suspend fun getUserChats(userId: String): Result<List<Chat>> {
        return try {
            val result = chatService.getUserChats(userId)
            result.map { chatsList ->
                chatsList.map { chatData ->
                    mapToChat(chatData)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a message
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return chatService.deleteMessage(messageId)
    }

    /**
     * Edits a message
     */
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit> {
        return try {
            val updateData = mapOf(
                "content" to newContent,
                "is_edited" to true,
                "edited_at" to System.currentTimeMillis()
            )
            databaseService.update("messages", updateData, "id", messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets chat participants
     */
    suspend fun getChatParticipants(chatId: String): Result<List<String>> {
        return try {
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

    /**
     * Adds a participant to a chat
     */
    suspend fun addParticipant(chatId: String, userId: String): Result<Unit> {
        return try {
            val participantData = mapOf(
                "chat_id" to chatId,
                "user_id" to userId,
                "role" to "member",
                "is_admin" to false,
                "can_send_messages" to true,
                "joined_at" to System.currentTimeMillis()
            )
            databaseService.insert("chat_participants", participantData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Removes a participant from a chat
     */
    suspend fun removeParticipant(chatId: String, userId: String): Result<Unit> {
        return try {
            client.from("chat_participants").delete {
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

    /**
     * Observes messages in a chat (real-time)
     */
    fun observeMessages(chatId: String): Flow<List<Message>> {
        return try {
            val channel = client.channel("messages:$chatId")
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "messages"
                filter = "chat_id=eq.$chatId"
            }.map { action ->
                when (action) {
                    is PostgresAction.Insert, is PostgresAction.Update, is PostgresAction.Delete -> {
                        // Reload messages when changes occur
                        val result = chatService.getMessages(chatId)
                        result.getOrNull()?.map { mapToMessage(it) } ?: emptyList()
                    }
                    else -> emptyList()
                }
            }.catch { e ->
                android.util.Log.e("ChatRepository", "Error observing messages", e)
                emit(emptyList())
            }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    /**
     * Observes user's chats (real-time)
     */
    fun observeUserChats(userId: String): Flow<List<Chat>> {
        return try {
            val channel = client.channel("user_chats:$userId")
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "chats"
            }.map { action ->
                when (action) {
                    is PostgresAction.Insert, is PostgresAction.Update, is PostgresAction.Delete -> {
                        // Reload chats when changes occur
                        val result = chatService.getUserChats(userId)
                        result.getOrNull()?.map { mapToChat(it) } ?: emptyList()
                    }
                    else -> emptyList()
                }
            }.catch { e ->
                android.util.Log.e("ChatRepository", "Error observing chats", e)
                emit(emptyList())
            }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    /**
     * Creates a direct chat between two users
     */
    suspend fun createDirectChat(currentUserId: String, otherUserId: String): Result<String> {
        return chatService.getOrCreateDirectChat(currentUserId, otherUserId)
    }

    /**
     * Finds existing direct chat between two users
     */
    suspend fun findDirectChat(userId1: String, userId2: String): Result<String?> {
        return try {
            val chatId = if (userId1 < userId2) {
                "dm_${userId1}_${userId2}"
            } else {
                "dm_${userId2}_${userId1}"
            }
            
            val existingChat = client.from("chats")
                .select(columns = Columns.raw("chat_id")) {
                    filter { eq("chat_id", chatId) }
                    limit(1)
                }
                .decodeList<JsonObject>()
            
            if (existingChat.isNotEmpty()) {
                Result.success(chatId)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets or creates a direct chat between two users
     */
    suspend fun getOrCreateDirectChat(currentUserId: String, otherUserId: String): Result<String> {
        return chatService.getOrCreateDirectChat(currentUserId, otherUserId)
    }

    /**
     * Marks messages as read
     */
    suspend fun markMessagesAsRead(chatId: String, userId: String): Result<Unit> {
        return chatService.markMessagesAsRead(chatId, userId)
    }

    // Helper methods for data mapping
    private fun mapToMessage(data: Map<String, Any?>): Message {
        return Message(
            id = data["id"]?.toString() ?: "",
            chatId = data["chat_id"]?.toString() ?: "",
            senderId = data["sender_id"]?.toString() ?: "",
            content = data["content"]?.toString() ?: "",
            messageType = data["message_type"]?.toString() ?: "text",
            createdAt = data["created_at"]?.toString()?.toLongOrNull() ?: 0L,
            isDeleted = data["is_deleted"]?.toString()?.toBooleanStrictOrNull() ?: false,
            isEdited = data["is_edited"]?.toString()?.toBooleanStrictOrNull() ?: false,
            replyToId = data["reply_to_id"]?.toString()
        )
    }

    private fun mapToChat(data: Map<String, Any?>): Chat {
        return Chat(
            id = data["chat_id"]?.toString() ?: "",
            isGroup = data["is_group"]?.toString()?.toBooleanStrictOrNull() ?: false,
            lastMessage = data["last_message"]?.toString(),
            lastMessageTime = data["last_message_time"]?.toString()?.toLongOrNull(),
            lastMessageSender = data["last_message_sender"]?.toString(),
            createdAt = data["created_at"]?.toString()?.toLongOrNull() ?: 0L,
            isActive = data["is_active"]?.toString()?.toBooleanStrictOrNull() ?: true
        )
    }
}