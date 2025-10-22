package com.synapse.social.studioasinc.domain.usecase

import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.model.Message
import com.synapse.social.studioasinc.model.Chat
import kotlinx.coroutines.flow.Flow
import java.util.UUID


class SendMessageUseCase(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        recipientId: String,
        messageText: String,
        messageType: String = "text",
        attachmentUrl: String? = null,
        replyToMessageId: String? = null
    ): Result<Message> {
        val senderId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated"))
        
        // Create or get chat
        val chatResult = chatRepository.createChat(senderId, recipientId)
        if (chatResult.isFailure) {
            return Result.failure(chatResult.exceptionOrNull()!!)
        }
        
        val chat = chatResult.getOrThrow()
        val message = Message(
            messageKey = UUID.randomUUID().toString(),
            chatId = chat.id!!,
            senderId = senderId,
            messageText = messageText,
            messageType = messageType,
            attachmentUrl = attachmentUrl,
            replyToMessageId = replyToMessageId
        )
        
        return chatRepository.sendMessage(message)
    }
}

class GetMessagesUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, limit: Int = 50): Result<List<Message>> {
        return chatRepository.getMessages(chatId, limit)
    }
}

class ObserveMessagesUseCase(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(chatId: String): Flow<List<Message>> {
        return chatRepository.observeMessages(chatId)
    }
}

class GetUserChatsUseCase(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<List<Chat>> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated"))
        
        return chatRepository.getUserChats(userId)
    }
}

class DeleteMessageUseCase(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(messageId: String): Result<Unit> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated"))
        
        return chatRepository.deleteMessage(messageId)
    }
}

class EditMessageUseCase(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(messageId: String, newText: String): Result<Message> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated"))
        
        val updates = mapOf(
            "message_text" to newText,
            "edited_at" to "now()"
        )
        
        return chatRepository.updateMessage(messageId, updates)
    }
}