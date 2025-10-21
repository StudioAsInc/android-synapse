package com.synapse.social.studioasinc.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.usecase.*
import com.synapse.social.studioasinc.model.Message
import com.synapse.social.studioasinc.model.Chat
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val getUserChatsUseCase: GetUserChatsUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val editMessageUseCase: EditMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private var currentChatId: String? = null

    fun loadChats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            getUserChatsUseCase()
                .onSuccess { chatList ->
                    _chats.value = chatList
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load chats"
                    )
                }
        }
    }

    fun loadMessages(chatId: String) {
        currentChatId = chatId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            getMessagesUseCase(chatId)
                .onSuccess { messageList ->
                    _messages.value = messageList
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    
                    // Start observing real-time messages
                    observeMessagesUseCase(chatId)
                        .collect { updatedMessages ->
                            _messages.value = updatedMessages
                        }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load messages"
                    )
                }
        }
    }

    fun sendMessage(
        recipientId: String,
        messageText: String,
        messageType: String = "text",
        attachmentUrl: String? = null,
        replyToMessageId: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            
            sendMessageUseCase(recipientId, messageText, messageType, attachmentUrl, replyToMessageId)
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(isSending = false)
                    // Message will be added via real-time subscription
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        error = error.message ?: "Failed to send message"
                    )
                }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            deleteMessageUseCase(messageId)
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to delete message"
                    )
                }
        }
    }

    fun editMessage(messageId: String, newText: String) {
        viewModelScope.launch {
            editMessageUseCase(messageId, newText)
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to edit message"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)