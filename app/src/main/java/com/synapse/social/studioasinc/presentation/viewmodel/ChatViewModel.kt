package com.synapse.social.studioasinc.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.domain.usecase.*
import com.synapse.social.studioasinc.model.Chat
import com.synapse.social.studioasinc.model.Message
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel for chat functionality
 */
class ChatViewModel : ViewModel() {

    private val authService = SupabaseAuthenticationService()
    private val chatRepository = ChatRepository()
    
    // Use cases
    private val sendMessageUseCase = SendMessageUseCase(chatRepository)
    private val getMessagesUseCase = GetMessagesUseCase(chatRepository)
    private val observeMessagesUseCase = ObserveMessagesUseCase(chatRepository)
    private val getUserChatsUseCase = GetUserChatsUseCase(chatRepository)
    private val deleteMessageUseCase = DeleteMessageUseCase(chatRepository)
    private val editMessageUseCase = EditMessageUseCase(chatRepository)

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _chats = MutableLiveData<List<Chat>>()
    val chats: LiveData<List<Chat>> = _chats

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _messageSent = MutableLiveData<Boolean>()
    val messageSent: LiveData<Boolean> = _messageSent

    private var currentChatId: String? = null

    /**
     * Loads messages for a chat
     */
    fun loadMessages(chatId: String) {
        currentChatId = chatId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = getMessagesUseCase(chatId)
                result.onSuccess { messageList ->
                    _messages.value = messageList
                    _error.value = null
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Starts observing messages in real-time
     */
    fun startObservingMessages(chatId: String) {
        currentChatId = chatId
        observeMessagesUseCase(chatId)
            .onEach { messageList ->
                _messages.value = messageList
            }
            .launchIn(viewModelScope)
    }

    /**
     * Sends a message
     */
    fun sendMessage(chatId: String, content: String, messageType: String = "text", replyToId: String? = null) {
        if (content.isBlank()) return
        
        viewModelScope.launch {
            try {
                val currentUserId = authService.getCurrentUserId()
                if (currentUserId == null) {
                    _error.value = "User not authenticated"
                    _messageSent.value = false
                    return@launch
                }
                
                val result = sendMessageUseCase(chatId, currentUserId, content, messageType, replyToId)
                result.onSuccess {
                    _messageSent.value = true
                    _error.value = null
                    // Refresh messages
                    loadMessages(chatId)
                }.onFailure { exception ->
                    _error.value = exception.message
                    _messageSent.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _messageSent.value = false
            }
        }
    }

    /**
     * Loads user's chats
     */
    fun loadUserChats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUserId = authService.getCurrentUserId()
                if (currentUserId != null) {
                    val result = getUserChatsUseCase(currentUserId)
                    result.onSuccess { chatList ->
                        _chats.value = chatList
                        _error.value = null
                    }.onFailure { exception ->
                        _error.value = exception.message
                    }
                } else {
                    _error.value = "User not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Deletes a message
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                val result = deleteMessageUseCase(messageId)
                result.onSuccess {
                    _error.value = null
                    // Refresh messages
                    currentChatId?.let { loadMessages(it) }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Edits a message
     */
    fun editMessage(messageId: String, newContent: String) {
        if (newContent.isBlank()) return
        
        viewModelScope.launch {
            try {
                val result = editMessageUseCase(messageId, newContent)
                result.onSuccess {
                    _error.value = null
                    // Refresh messages
                    currentChatId?.let { loadMessages(it) }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Creates or gets a direct chat with another user
     */
    fun createOrGetDirectChat(otherUserId: String, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val currentUserId = authService.getCurrentUserId()
                if (currentUserId != null) {
                    val result = chatRepository.getOrCreateDirectChat(otherUserId, currentUserId)
                    result.onSuccess { chatId ->
                        onChatReady(chatId)
                        _error.value = null
                    }.onFailure { exception ->
                        _error.value = exception.message
                    }
                } else {
                    _error.value = "User not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Clears error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Resets message sent status
     */
    fun resetMessageSent() {
        _messageSent.value = false
    }
}