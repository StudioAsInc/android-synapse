package com.synapse.social.studioasinc.domain.usecase

import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Use case for observing messages in real-time
 */
class ObserveMessagesUseCase(
    private val chatRepository: ChatRepository = ChatRepository()
) {
    operator fun invoke(chatId: String): Flow<List<Message>> {
        return chatRepository.observeMessages(chatId)
    }
}