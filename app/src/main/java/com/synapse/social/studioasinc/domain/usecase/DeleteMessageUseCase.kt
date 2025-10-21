package com.synapse.social.studioasinc.domain.usecase

import com.synapse.social.studioasinc.data.repository.ChatRepository

/**
 * Use case for deleting messages
 */
class DeleteMessageUseCase(
    private val chatRepository: ChatRepository = ChatRepository()
) {
    suspend operator fun invoke(messageId: String): Result<Unit> {
        return chatRepository.deleteMessage(messageId)
    }
}