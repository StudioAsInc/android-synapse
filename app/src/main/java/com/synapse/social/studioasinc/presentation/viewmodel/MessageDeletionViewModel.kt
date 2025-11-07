package com.synapse.social.studioasinc.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.MessageDeletionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for message deletion operations
 * Manages deletion state and coordinates repository operations
 * 
 * Requirements: 1.1, 2.1, 2.3, 5.4, 6.5
 */
class MessageDeletionViewModel : ViewModel() {

    private val repository = MessageDeletionRepository()

    // Deletion state management
    private val _deletionState = MutableStateFlow<DeletionState>(DeletionState.Idle)
    val deletionState: StateFlow<DeletionState> = _deletionState.asStateFlow()

    // Error state management
    private val _errorState = MutableSharedFlow<String>()
    val errorState: SharedFlow<String> = _errorState.asSharedFlow()

    /**
     * Delete messages for the current user only
     * Marks messages as deleted in user_deleted_messages table
     * 
     * Requirements: 1.1, 1.4, 6.5
     * 
     * @param messageIds List of message IDs to delete
     * @param userId Current user ID
     */
    fun deleteMessagesForMe(messageIds: List<String>, userId: String) {
        if (messageIds.isEmpty()) {
            viewModelScope.launch {
                _errorState.emit("No messages selected")
            }
            return
        }

        if (userId.isBlank()) {
            viewModelScope.launch {
                _errorState.emit("User ID is required")
            }
            return
        }

        viewModelScope.launch {
            _deletionState.value = DeletionState.Deleting

            val result = repository.deleteForMe(messageIds, userId)

            result.onSuccess {
                _deletionState.value = DeletionState.Success(messageIds.size)
            }.onFailure { exception ->
                val errorMessage = when {
                    exception.message?.contains("network", ignoreCase = true) == true ->
                        "Unable to delete messages. Please check your connection."
                    else ->
                        "Failed to delete messages. Please try again."
                }
                _deletionState.value = DeletionState.Error(errorMessage)
                _errorState.emit(errorMessage)
            }
        }
    }

    /**
     * Delete messages for everyone in the chat
     * Updates is_deleted and delete_for_everyone fields in messages table
     * Only message owners can delete for everyone
     * 
     * Requirements: 2.1, 2.4, 2.5, 6.5
     * 
     * @param messageIds List of message IDs to delete
     * @param userId Current user ID (must be the sender)
     */
    fun deleteMessagesForEveryone(messageIds: List<String>, userId: String) {
        if (messageIds.isEmpty()) {
            viewModelScope.launch {
                _errorState.emit("No messages selected")
            }
            return
        }

        if (userId.isBlank()) {
            viewModelScope.launch {
                _errorState.emit("User ID is required")
            }
            return
        }

        viewModelScope.launch {
            // Validate ownership before attempting deletion
            val ownsAllMessages = validateMessageOwnership(messageIds, userId)
            
            if (!ownsAllMessages) {
                val errorMessage = "You can only delete your own messages for everyone."
                _deletionState.value = DeletionState.Error(errorMessage)
                _errorState.emit(errorMessage)
                return@launch
            }

            _deletionState.value = DeletionState.Deleting

            val result = repository.deleteForEveryone(messageIds, userId)

            result.onSuccess {
                _deletionState.value = DeletionState.Success(messageIds.size)
            }.onFailure { exception ->
                val errorMessage = when {
                    exception.message?.contains("network", ignoreCase = true) == true ->
                        "Unable to delete messages. Please check your connection."
                    exception.message?.contains("own messages", ignoreCase = true) == true ->
                        "You can only delete your own messages for everyone."
                    else ->
                        "Failed to delete messages. Please try again."
                }
                _deletionState.value = DeletionState.Error(errorMessage)
                _errorState.emit(errorMessage)
            }
        }
    }

    /**
     * Validate that the user owns all specified messages
     * Used for ownership validation before delete for everyone
     * 
     * Requirements: 2.3, 5.4
     * 
     * @param messageIds List of message IDs to validate
     * @param userId User ID to validate against
     * @return true if user owns all messages, false otherwise
     */
    private suspend fun validateMessageOwnership(messageIds: List<String>, userId: String): Boolean {
        if (messageIds.isEmpty() || userId.isBlank()) {
            return false
        }

        val ownedMessageIds = repository.getMessagesBySenderId(messageIds, userId)
        return ownedMessageIds.size == messageIds.size
    }

    /**
     * Reset deletion state to idle
     * Called after handling deletion result
     */
    fun resetState() {
        _deletionState.value = DeletionState.Idle
    }
}

/**
 * Sealed class representing deletion operation states
 * 
 * Requirements: 6.5
 */
sealed class DeletionState {
    /**
     * Idle state - no deletion operation in progress
     */
    object Idle : DeletionState()

    /**
     * Deleting state - deletion operation in progress
     */
    object Deleting : DeletionState()

    /**
     * Success state - deletion completed successfully
     * @param deletedCount Number of messages deleted
     */
    data class Success(val deletedCount: Int) : DeletionState()

    /**
     * Error state - deletion failed
     * @param message User-friendly error message
     */
    data class Error(val message: String) : DeletionState()
}
