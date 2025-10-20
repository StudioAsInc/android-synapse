package com.synapse.social.studioasinc.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.synapse.social.studioasinc.chat.common.repository.ChatRepository
import com.synapse.social.studioasinc.chat.common.viewmodel.ChatViewModel

class ChatViewModelFactory(private val chatRepository: ChatRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
