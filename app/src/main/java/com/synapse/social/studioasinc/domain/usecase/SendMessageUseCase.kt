package com.synapse.social.studioasinc.domain.usecase

import com.synapse.social.studioasinc.data.repository.ChatRepository

/**
 * Use case for sending messages
 */
class SendMessageUseCase(
    private val chatRepository: ChatRepository = ChatRepository()
) {
    suspend operator fun invoke(
        chatId: String,
        content: String,
        messageType: String = "text",
        mediaUrl: String? = null
    ): Result<String> {
        return chatRepository.sendMessage(chatId, content, messageType, mediaUrl)
    }
}