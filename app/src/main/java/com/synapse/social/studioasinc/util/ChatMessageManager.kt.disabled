package com.synapse.social.studioasinc.util

import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.model.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class ChatMessageManager(
    private val authRepository: AuthRepository = AuthRepository(),
    private val chatRepository: ChatRepository = ChatRepository()
) {

    fun getChatId(userId1: String, userId2: String): String {
        return "${minOf(userId1, userId2)}_${maxOf(userId1, userId2)}"
    }

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

    suspend fun getMessages(chatId: String, limit: Int = 50): Result<List<Message>> {
        return chatRepository.getMessages(chatId, limit)
    }

    suspend fun updateInbox(senderUid: String, recipientUid: String, lastMessage: String, isGroup: Boolean = false) {
        // This would update the inbox/conversation list
        // Implementation depends on your inbox structure
    }

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
}