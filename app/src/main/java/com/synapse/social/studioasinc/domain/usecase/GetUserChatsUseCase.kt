package com.synapse.social.studioasinc.domain.usecase

import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.model.Chat

/**
 * Use case for getting user's chats
 */
class GetUserChatsUseCase(
    private val chatRepository: ChatRepository = ChatRepository()
) {
    suspend operator fun invoke(userId: String): Result<List<Chat>> {
        return chatRepository.getUserChats(userId)
    }
}