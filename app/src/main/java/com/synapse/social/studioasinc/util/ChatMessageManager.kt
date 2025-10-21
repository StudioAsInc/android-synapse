package com.synapse.social.studioasinc.util

import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.model.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Utility class for managing chat messages with Supabase backend.
 * Handles message sending, retrieval, and chat management operations.
 */
class ChatMessageManager(
    private val authRepository: AuthRepository = AuthRepository(),
    private val chatRepository: ChatRepository = ChatRepository()
) {

    /**
     * Generate a consistent chat ID for two users
     */
    fun getChatId(userId1: String, userId2: String): String {
        return "${minOf(userId1, userId2)}_${maxOf(userId1, userId2)}"
    }

    /**
     * Send a message to a recipient
     */
    suspend fun sendMessage(
        recipientId: String,
        messageText: String,
        messageType: String = "text",
        attachmentUrl: String? = null
    ): Result<Message> {
        val senderId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated"))

        val chatId = getChatId(senderId, recipientId)
        
        // Create or get chat first
        chatRepository.createChat(senderId, recipientId)
            .onFailure { return Result.failure(it) }

        val message = Message(
            messageKey = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = senderId,
            messageText = messageText,
            messageType = messageType,
            attachmentUrl = attachmentUrl
        )

        return chatRepository.sendMessage(message)
    }

    /**
     * Get messages for a specific chat
     */
    suspend fun getMessages(chatId: String, limit: Int = 50): Result<List<Message>> {
        return chatRepository.getMessages(chatId, limit)
    }

    /**
     * Update inbox with latest message information
     */
    suspend fun updateInbox(senderUid: String, recipientUid: String, lastMessage: String, isGroup: Boolean = false) {
        // Update the conversation list with the latest message
        try {
            val chatId = getChatId(senderUid, recipientUid)
            chatRepository.updateLastMessage(chatId, lastMessage, System.currentTimeMillis())
        } catch (e: Exception) {
            // Log error but don't fail the message send
            android.util.Log.e("ChatMessageManager", "Failed to update inbox: ${e.message}")
        }
    }

    /**
     * Legacy method for compatibility with existing code
     */
    suspend fun sendMessageToDb(
        messageMap: HashMap<String, Any>,
        senderUid: String,
        recipientUid: String,
        uniqueMessageKey: String,
        isGroup: Boolean
    ) {
        val messageText = messageMap["message_text"] as? String ?: ""
        val messageType = messageMap["message_type"] as? String ?: "text"
        val attachmentUrl = messageMap["attachment_url"] as? String

        sendMessage(recipientUid, messageText, messageType, attachmentUrl)
    }

    companion object {
        /**
         * Static method for generating chat IDs
         */
        fun getChatId(userId1: String, userId2: String): String {
            return "${minOf(userId1, userId2)}_${maxOf(userId1, userId2)}"
        }
    }
}