package com.synapse.social.studioasinc.chat.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.backend.GeminiAIService
import com.synapse.social.studioasinc.backend.SupabaseChatService
import com.synapse.social.studioasinc.data.repository.MessageActionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * ViewModel for coordinating message action operations
 * Handles reply, forward, edit, delete, and AI summary actions
 */
class MessageActionsViewModel(context: Context) : ViewModel() {

    companion object {
        private const val TAG = "MessageActionsViewModel"
        private const val MAX_PREVIEW_LINES = 3
        private const val CHARS_PER_LINE = 50 // Approximate characters per line
    }

    private val repository = MessageActionRepository(context)
    private val geminiService = GeminiAIService(context)
    private val chatService = SupabaseChatService()

    // State flows for different action types
    private val _replyState = MutableStateFlow<ReplyState>(ReplyState.Idle)
    val replyState: StateFlow<ReplyState> = _replyState.asStateFlow()

    /**
     * Sealed class representing different states for message actions
     */
    sealed class MessageActionState {
        object Idle : MessageActionState()
        object Loading : MessageActionState()
        data class Success(val message: String) : MessageActionState()
        data class Error(val error: String) : MessageActionState()
    }

    /**
     * Data class for forward operation state
     */
    data class ForwardState(
        val selectedChats: List<String> = emptyList(),
        val isForwarding: Boolean = false,
        val forwardedCount: Int = 0,
        val error: String? = null
    )

    /**
     * Data class for AI summary state
     */
    data class AISummaryState(
        val isGenerating: Boolean = false,
        val summary: String? = null,
        val error: String? = null,
        val characterCount: Int = 0,
        val estimatedReadTime: Int = 0,
        val rateLimitResetTime: Long = 0
    )

    /**
     * Sealed class for reply state
     */
    sealed class ReplyState {
        object Idle : ReplyState()
        data class Active(
            val messageId: String,
            val messageText: String,
            val senderName: String,
            val previewText: String
        ) : ReplyState()
    }

    // ==================== Reply Operations ====================

    /**
     * Prepare reply to a message
     * Truncates message text to 3 lines for preview
     * 
     * @param messageId The ID of the message being replied to
     * @param messageText The text content of the message
     * @param senderName The name of the message sender
     */
    fun prepareReply(messageId: String, messageText: String, senderName: String) {
        Log.d(TAG, "Preparing reply to message: $messageId")

        // Truncate message text to 3 lines for preview
        val previewText = truncateToLines(messageText, MAX_PREVIEW_LINES)

        _replyState.value = ReplyState.Active(
            messageId = messageId,
            messageText = messageText,
            senderName = senderName,
            previewText = previewText
        )

        Log.d(TAG, "Reply prepared for message: $messageId")
    }

    /**
     * Clear reply state
     */
    fun clearReply() {
        _replyState.value = ReplyState.Idle
        Log.d(TAG, "Reply state cleared")
    }

    /**
     * Truncate text to specified number of lines
     */
    private fun truncateToLines(text: String, maxLines: Int): String {
        val maxChars = maxLines * CHARS_PER_LINE
        return if (text.length > maxChars) {
            text.take(maxChars) + "..."
        } else {
            text
        }
    }

    // ==================== Forward Operations ====================

    /**
     * Forward a message to multiple chats
     * Emits Loading, Success, or Error states
     * 
     * @param messageId The ID of the message to forward
     * @param messageData The complete message data
     * @param targetChatIds List of chat IDs to forward to
     * @return Flow emitting ForwardState updates
     */
    fun forwardMessage(
        messageId: String,
        messageData: Map<String, Any?>,
        targetChatIds: List<String>
    ): Flow<ForwardState> = flow {
        try {
            Log.d(TAG, "Forwarding message $messageId to ${targetChatIds.size} chats")

            // Emit loading state
            emit(ForwardState(isForwarding = true))

            // Call repository to forward message
            val result = repository.forwardMessageToMultipleChats(messageData, targetChatIds)

            result.fold(
                onSuccess = { forwardedCount ->
                    Log.d(TAG, "Message forwarded to $forwardedCount chats")
                    
                    // Check for partial failures
                    if (forwardedCount < targetChatIds.size) {
                        val failedCount = targetChatIds.size - forwardedCount
                        emit(
                            ForwardState(
                                isForwarding = false,
                                forwardedCount = forwardedCount,
                                error = "Forwarded to $forwardedCount of ${targetChatIds.size} chats. $failedCount failed."
                            )
                        )
                    } else {
                        // Complete success
                        emit(
                            ForwardState(
                                isForwarding = false,
                                forwardedCount = forwardedCount
                            )
                        )
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to forward message", error)
                    emit(
                        ForwardState(
                            isForwarding = false,
                            error = error.message ?: "Failed to forward message"
                        )
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding message", e)
            emit(
                ForwardState(
                    isForwarding = false,
                    error = e.message ?: "An unexpected error occurred"
                )
            )
        }
    }

    // ==================== Edit Operations ====================

    /**
     * Edit a message with validation
     * Validates message age (<48 hours) and non-empty content
     * 
     * @param messageId The ID of the message to edit
     * @param newContent The new message content
     * @return Flow emitting MessageActionState updates
     */
    fun editMessage(messageId: String, newContent: String): Flow<MessageActionState> = flow {
        try {
            Log.d(TAG, "Editing message: $messageId")

            // Validate non-empty content
            if (newContent.isBlank()) {
                Log.w(TAG, "Edit validation failed: empty content")
                emit(MessageActionState.Error("Message content cannot be empty"))
                return@flow
            }

            // Emit loading state
            emit(MessageActionState.Loading)

            // Call repository to edit message
            val result = repository.editMessage(messageId, newContent)

            result.fold(
                onSuccess = {
                    Log.d(TAG, "Message edited successfully: $messageId")
                    emit(MessageActionState.Success("Message edited successfully"))
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to edit message", error)
                    
                    // Provide specific error messages
                    val errorMessage = when {
                        error.message?.contains("too old", ignoreCase = true) == true ->
                            "This message is too old to edit (>48 hours)"
                        error.message?.contains("empty", ignoreCase = true) == true ->
                            "Message cannot be empty"
                        error.message?.contains("not found", ignoreCase = true) == true ->
                            "Message not found"
                        else -> error.message ?: "Failed to edit message"
                    }
                    
                    emit(MessageActionState.Error(errorMessage))
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error editing message", e)
            emit(MessageActionState.Error(e.message ?: "An unexpected error occurred"))
        }
    }

    // ==================== Delete Operations ====================

    /**
     * Delete a message with option for local-only or server-side deletion
     * 
     * @param messageId The ID of the message to delete
     * @param deleteForEveryone If true, deletes for all users; if false, deletes locally only
     * @return Flow emitting MessageActionState updates
     */
    fun deleteMessage(messageId: String, deleteForEveryone: Boolean): Flow<MessageActionState> = flow {
        try {
            Log.d(TAG, "Deleting message: $messageId (deleteForEveryone=$deleteForEveryone)")

            // Emit loading state
            emit(MessageActionState.Loading)

            // Call appropriate repository method based on deleteForEveryone flag
            val result = if (deleteForEveryone) {
                repository.deleteMessageForEveryone(messageId)
            } else {
                repository.deleteMessageLocally(messageId)
            }

            result.fold(
                onSuccess = {
                    val successMessage = if (deleteForEveryone) {
                        "Message deleted for everyone"
                    } else {
                        "Message deleted for you"
                    }
                    Log.d(TAG, successMessage)
                    emit(MessageActionState.Success(successMessage))
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to delete message", error)
                    emit(
                        MessageActionState.Error(
                            error.message ?: "Failed to delete message. Please try again."
                        )
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message", e)
            emit(MessageActionState.Error(e.message ?: "An unexpected error occurred"))
        }
    }

    // ==================== AI Summary Operations ====================

    /**
     * Generate AI summary for a message
     * Checks cache first, then calls Gemini API if needed
     * Handles rate limiting and caches successful results
     * 
     * @param messageId The ID of the message to summarize
     * @param messageText The text content to summarize
     * @return Flow emitting AISummaryState updates
     */
    fun generateAISummary(messageId: String, messageText: String): Flow<AISummaryState> = flow {
        try {
            Log.d(TAG, "Generating AI summary for message: $messageId")

            // Check if summary is cached first
            val cachedSummary = repository.getCachedSummary(messageId)
            if (cachedSummary != null) {
                Log.d(TAG, "Using cached summary for message: $messageId")
                
                // Calculate metadata for cached summary
                val characterCount = messageText.length
                val estimatedReadTime = calculateReadingTime(messageText)
                
                emit(
                    AISummaryState(
                        isGenerating = false,
                        summary = cachedSummary,
                        characterCount = characterCount,
                        estimatedReadTime = estimatedReadTime
                    )
                )
                return@flow
            }

            // Check if rate limited
            if (geminiService.isRateLimited()) {
                val resetTime = geminiService.getRateLimitResetTime()
                Log.w(TAG, "Rate limited. Reset time: $resetTime")
                emit(
                    AISummaryState(
                        isGenerating = false,
                        error = "Rate limit reached. Please try again later.",
                        rateLimitResetTime = resetTime
                    )
                )
                return@flow
            }

            // Emit loading state
            emit(AISummaryState(isGenerating = true))

            // Call Gemini API to generate summary
            val result = geminiService.generateSummary(messageText)

            result.fold(
                onSuccess = { summaryResult ->
                    Log.d(TAG, "AI summary generated successfully for message: $messageId")
                    
                    // Cache the summary
                    repository.cacheSummary(messageId, summaryResult.summary)
                    
                    emit(
                        AISummaryState(
                            isGenerating = false,
                            summary = summaryResult.summary,
                            characterCount = summaryResult.characterCount,
                            estimatedReadTime = summaryResult.estimatedReadTimeMinutes
                        )
                    )
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to generate AI summary", error)
                    
                    // Check if it's a rate limit error
                    val isRateLimitError = error.message?.contains("rate limit", ignoreCase = true) == true
                    val resetTime = if (isRateLimitError) {
                        geminiService.getRateLimitResetTime()
                    } else {
                        0L
                    }
                    
                    emit(
                        AISummaryState(
                            isGenerating = false,
                            error = error.message ?: "Failed to generate summary",
                            rateLimitResetTime = resetTime
                        )
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating AI summary", e)
            emit(
                AISummaryState(
                    isGenerating = false,
                    error = e.message ?: "An unexpected error occurred"
                )
            )
        }
    }

    /**
     * Calculate estimated reading time in minutes
     * Based on average reading speed of 200 words per minute
     */
    private fun calculateReadingTime(text: String): Int {
        val wordCount = text.split("\\s+".toRegex()).size
        val minutes = (wordCount.toDouble() / 200).toInt()
        return if (minutes < 1) 1 else minutes
    }

    /**
     * Get cached summary for a message
     * 
     * @param messageId The ID of the message
     * @return Cached summary text or null if not cached
     */
    fun getCachedSummary(messageId: String): String? {
        return repository.getCachedSummary(messageId)
    }

    /**
     * Clear all cached summaries
     */
    fun clearSummaryCache() {
        viewModelScope.launch {
            try {
                repository.clearSummaryCache()
                Log.d(TAG, "Summary cache cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing summary cache", e)
            }
        }
    }
}
