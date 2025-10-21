package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.backend.SupabaseChatService
import com.synapse.social.studioasinc.model.Chat
import com.synapse.social.studioasinc.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Repository for chat operations
 */
class ChatRepository {

    private val chatService = SupabaseChatService()

    /**
     * Creates a new chat
     */
    suspend fun createChat(participantUids: List<String>, chatName: String? = null): Result<String> {
        return chatService.createChat(participantUids, chatName)
    }

    /**
     * Sends a message
     */
    suspend fun sendMessage(
        chatId: String,
        content: String,
        messageType: String = "text",
        mediaUrl: String? = null
    ): Result<String> {
        return chatService.sendMessage(chatId, content, messageType, mediaUrl)
    }

    /**
     * Gets messages for a chat
     */
    suspend fun getMessages(chatId: String, limit: Int = 50, offset: Int = 0): Result<List<Message>> {
        return chatService.getMessages(chatId, limit, offset)
    }

    /**
     * Gets user's chats
     */
    suspend fun getUserChats(userId: String): Result<List<Chat>> {
        return chatService.getUserChats(userId)
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
        return chatService.editMessage(messageId, newContent)
    }

    /**
     * Gets chat participants
     */
    suspend fun getChatParticipants(chatId: String): Result<List<String>> {
        return chatService.getChatParticipants(chatId)
    }

    /**
     * Adds a participant to a chat
     */
    suspend fun addParticipant(chatId: String, userId: String): Result<Unit> {
        return chatService.addParticipant(chatId, userId)
    }

    /**
     * Removes a participant from a chat
     */
    suspend fun removeParticipant(chatId: String, userId: String): Result<Unit> {
        return chatService.removeParticipant(chatId, userId)
    }

    /**
     * Observes messages in a chat (real-time)
     */
    fun observeMessages(chatId: String): Flow<List<Message>> {
        return chatService.observeMessages(chatId)
    }

    /**
     * Observes user's chats (real-time)
     */
    fun observeUserChats(userId: String): Flow<List<Chat>> {
        return chatService.observeUserChats(userId)
    }

    /**
     * Creates a direct chat between two users
     */
    suspend fun createDirectChat(otherUserId: String): Result<String> {
        return chatService.createChat(listOf(otherUserId), null)
    }

    /**
     * Finds existing direct chat between two users
     */
    suspend fun findDirectChat(userId1: String, userId2: String): Result<String?> {
        return try {
            val userChats = getUserChats(userId1).getOrNull() ?: emptyList()
            
            for (chat in userChats) {
                if (!chat.isGroup) {
                    val participants = getChatParticipants(chat.id).getOrNull() ?: emptyList()
                    if (participants.size == 2 && participants.contains(userId2)) {
                        return Result.success(chat.id)
                    }
                }
            }
            
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets or creates a direct chat between two users
     */
    suspend fun getOrCreateDirectChat(otherUserId: String, currentUserId: String): Result<String> {
        return try {
            val existingChat = findDirectChat(currentUserId, otherUserId).getOrNull()
            if (existingChat != null) {
                Result.success(existingChat)
            } else {
                createDirectChat(otherUserId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}