package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.model.Chat
import com.synapse.social.studioasinc.model.Message
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Supabase Chat Service
 * Handles all chat-related operations using Supabase Realtime and Database
 */
class SupabaseChatService {

    private val dbService = SupabaseDatabaseService()
    private val authService = SupabaseAuthenticationService()

    /**
     * Creates a new chat between users
     */
    suspend fun createChat(participantUids: List<String>, chatName: String? = null): Result<String> {
        return try {
            val currentUid = authService.getCurrentUserId() ?: return Result.failure(Exception("Not authenticated"))
            
            val chatData = mapOf(
                "id" to generateChatId(),
                "name" to chatName,
                "created_by" to currentUid,
                "created_at" to System.currentTimeMillis().toString(),
                "updated_at" to System.currentTimeMillis().toString(),
                "is_group" to (participantUids.size > 2).toString(),
                "participant_count" to participantUids.size.toString()
            )
            
            val chatId = chatData["id"] as String
            dbService.insert("chats", chatData)
            
            // Add participants
            participantUids.forEach { uid ->
                val participantData = mapOf(
                    "chat_id" to chatId,
                    "user_id" to uid,
                    "joined_at" to System.currentTimeMillis().toString(),
                    "role" to if (uid == currentUid) "admin" else "member"
                )
                dbService.insert("chat_participants", participantData)
            }
            
            Result.success(chatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends a message in a chat
     */
    suspend fun sendMessage(
        chatId: String,
        content: String,
        messageType: String = "text",
        mediaUrl: String? = null
    ): Result<String> {
        return try {
            val currentUid = authService.getCurrentUserId() ?: return Result.failure(Exception("Not authenticated"))
            
            val messageData = mapOf(
                "id" to generateMessageId(),
                "chat_id" to chatId,
                "sender_id" to currentUid,
                "content" to content,
                "message_type" to messageType,
                "media_url" to mediaUrl,
                "created_at" to System.currentTimeMillis().toString(),
                "updated_at" to System.currentTimeMillis().toString(),
                "is_deleted" to "false",
                "is_edited" to "false"
            )
            
            val messageId = messageData["id"] as String
            dbService.insert("messages", messageData)
            
            // Update chat's last message
            val chatUpdateData = mapOf(
                "last_message_id" to messageId,
                "last_message_at" to System.currentTimeMillis().toString(),
                "updated_at" to System.currentTimeMillis().toString()
            )
            dbService.update("chats", chatUpdateData, "id", chatId)
            
            Result.success(messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets messages for a chat
     */
    suspend fun getMessages(chatId: String, limit: Int = 50, offset: Int = 0): Result<List<Message>> {
        return try {
            val results = dbService.select("messages", "*").getOrNull() ?: emptyList()
            
            val messages = results
                .filter { (it["chat_id"] as? String) == chatId && (it["is_deleted"] as? String) != "true" }
                .sortedByDescending { (it["created_at"] as? String)?.toLongOrNull() ?: 0L }
                .drop(offset)
                .take(limit)
                .map { result ->
                    Message(
                        id = result["id"] as? String ?: "",
                        chatId = result["chat_id"] as? String ?: "",
                        senderId = result["sender_id"] as? String ?: "",
                        content = result["content"] as? String ?: "",
                        messageType = result["message_type"] as? String ?: "text",
                        mediaUrl = result["media_url"] as? String,
                        createdAt = (result["created_at"] as? String)?.toLongOrNull() ?: 0L,
                        updatedAt = (result["updated_at"] as? String)?.toLongOrNull() ?: 0L,
                        isDeleted = (result["is_deleted"] as? String) == "true",
                        isEdited = (result["is_edited"] as? String) == "true"
                    )
                }
            
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets user's chats
     */
    suspend fun getUserChats(userId: String): Result<List<Chat>> {
        return try {
            // Get chat IDs where user is a participant
            val participantResults = dbService.selectWithFilter("chat_participants", "*", "user_id", userId).getOrNull() ?: emptyList()
            val chatIds = participantResults.mapNotNull { it["chat_id"] as? String }
            
            if (chatIds.isEmpty()) {
                return Result.success(emptyList())
            }
            
            // Get chat details
            val chatResults = dbService.select("chats", "*").getOrNull() ?: emptyList()
            val chats = chatResults
                .filter { chatIds.contains(it["id"] as? String) }
                .map { result ->
                    Chat(
                        id = result["id"] as? String ?: "",
                        name = result["name"] as? String,
                        createdBy = result["created_by"] as? String ?: "",
                        createdAt = (result["created_at"] as? String)?.toLongOrNull() ?: 0L,
                        updatedAt = (result["updated_at"] as? String)?.toLongOrNull() ?: 0L,
                        isGroup = (result["is_group"] as? String) == "true",
                        participantCount = (result["participant_count"] as? String)?.toIntOrNull() ?: 0,
                        lastMessageId = result["last_message_id"] as? String,
                        lastMessageAt = (result["last_message_at"] as? String)?.toLongOrNull()
                    )
                }
                .sortedByDescending { it.lastMessageAt ?: it.updatedAt }
            
            Result.success(chats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a message
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            val updateData = mapOf(
                "is_deleted" to "true",
                "updated_at" to System.currentTimeMillis().toString()
            )
            dbService.update("messages", updateData, "id", messageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Edits a message
     */
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit> {
        return try {
            val updateData = mapOf(
                "content" to newContent,
                "is_edited" to "true",
                "updated_at" to System.currentTimeMillis().toString()
            )
            dbService.update("messages", updateData, "id", messageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets chat participants
     */
    suspend fun getChatParticipants(chatId: String): Result<List<String>> {
        return try {
            val results = dbService.selectWithFilter("chat_participants", "*", "chat_id", chatId).getOrNull() ?: emptyList()
            val participantIds = results.mapNotNull { it["user_id"] as? String }
            Result.success(participantIds)
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
                "joined_at" to System.currentTimeMillis().toString(),
                "role" to "member"
            )
            dbService.insert("chat_participants", participantData)
            
            // Update participant count
            val participants = getChatParticipants(chatId).getOrNull() ?: emptyList()
            val updateData = mapOf(
                "participant_count" to participants.size.toString(),
                "updated_at" to System.currentTimeMillis().toString()
            )
            dbService.update("chats", updateData, "id", chatId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Removes a participant from a chat
     */
    suspend fun removeParticipant(chatId: String, userId: String): Result<Unit> {
        return try {
            dbService.delete("chat_participants", "chat_id", chatId)
            
            // Update participant count
            val participants = getChatParticipants(chatId).getOrNull() ?: emptyList()
            val updateData = mapOf(
                "participant_count" to participants.size.toString(),
                "updated_at" to System.currentTimeMillis().toString()
            )
            dbService.update("chats", updateData, "id", chatId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observes messages in a chat (real-time)
     */
    fun observeMessages(chatId: String): Flow<List<Message>> = flow {
        // For now, we'll poll for messages
        // In a full implementation, you'd use Supabase Realtime subscriptions
        while (true) {
            val messages = getMessages(chatId).getOrNull() ?: emptyList()
            emit(messages)
            kotlinx.coroutines.delay(1000) // Poll every second
        }
    }

    /**
     * Observes user's chats (real-time)
     */
    fun observeUserChats(userId: String): Flow<List<Chat>> = flow {
        // For now, we'll poll for chats
        // In a full implementation, you'd use Supabase Realtime subscriptions
        while (true) {
            val chats = getUserChats(userId).getOrNull() ?: emptyList()
            emit(chats)
            kotlinx.coroutines.delay(2000) // Poll every 2 seconds
        }
    }

    private fun generateChatId(): String {
        return "chat_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}