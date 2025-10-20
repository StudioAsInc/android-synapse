package com.synapse.social.studioasinc.chat.common.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.model.ChatMessage
import com.synapse.social.studioasinc.model.UserStatus
import com.synapse.social.studioasinc.chat.common.repository.ChatRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _chatMessages = MutableLiveData<List<ChatMessage>>()
    val chatMessages: LiveData<List<ChatMessage>> = _chatMessages

    private val _newMessageSent = MutableLiveData<Boolean>()
    val newMessageSent: LiveData<Boolean> = _newMessageSent

    private val _userStatus = MutableLiveData<UserStatus>()
    val userStatus: LiveData<UserStatus> = _userStatus

    fun loadChatMessages(chatId: String) {
        viewModelScope.launch {
            chatRepository.getChatMessages(chatId).collect { messages ->
                val deferreds = messages.map { message ->
                    if (message.repliedMessageId != null) {
                        async {
                            message.repliedMessage = chatRepository.getRepliedMessage(chatId, message.repliedMessageId!!)
                        }
                    } else {
                        null
                    }
                }
                deferreds.filterNotNull().awaitAll()
                _chatMessages.postValue(messages)
            }
        }
    }

    fun loadUserStatus(userId: String) {
        viewModelScope.launch {
            chatRepository.getUserStatus(userId).collect { status ->
                _userStatus.postValue(status)
            }
        }
    }

    fun sendMessage(chatId: String, message: ChatMessage) {
        viewModelScope.launch {
            val success = chatRepository.sendMessage(chatId, message)
            _newMessageSent.postValue(success)
        }
    }

    fun deleteMessage(chatId: String, message: ChatMessage) {
        viewModelScope.launch {
            chatRepository.deleteMessage(chatId, message)
        }
    }

    fun blockUser(userId: String, targetId: String) {
        viewModelScope.launch {
            chatRepository.blockUser(userId, targetId)
        }
    }

    fun unblockUser(userId: String, targetId: String) {
        viewModelScope.launch {
            chatRepository.unblockUser(userId, targetId)
        }
    }

    private val _blockedUsers = MutableLiveData<List<String>>()
    val blockedUsers: LiveData<List<String>> = _blockedUsers

    fun getBlockedUsers(userId: String) {
        viewModelScope.launch {
            chatRepository.getBlockedUsers(userId).collect {
                _blockedUsers.postValue(it)
            }
        }
    }

    fun loadMoreMessages(chatId: String, lastMessageKey: String) {
        viewModelScope.launch {
            chatRepository.getMoreMessages(chatId, lastMessageKey).collect {
                val currentMessages = _chatMessages.value ?: emptyList()
                _chatMessages.postValue(it + currentMessages)
            }
        }
    }

    fun markMessageAsSeen(chatId: String, message: ChatMessage) {
        viewModelScope.launch {
            chatRepository.markMessageAsSeen(chatId, message)
        }
    }

    fun setTyping(chatId: String, isTyping: Boolean) {
        viewModelScope.launch {
            chatRepository.setTyping(chatId, isTyping)
        }
    }

    private val _typingStatus = MutableLiveData<Boolean>()
    val typingStatus: LiveData<Boolean> = _typingStatus

    fun getTypingStatus(chatId: String) {
        viewModelScope.launch {
            chatRepository.getTypingStatus(chatId).collect {
                _typingStatus.postValue(it)
            }
        }
    }
}
