package com.synapse.social.studioasinc.domain.usecase

import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.model.Message

/**
 * Use case for getting messages
 */
class GetMessagesUseCase(
    private val chatRepository: ChatRepository = ChatRepository()
) {
    suspend operator fun invoke(chatId: String, limit: Int = 50, beforeTimestamp: Long? = null): Result<List<Message>> {
        return chatRepository.getMessages(chatId, limit, beforeTimestamp)
    }
}