package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.util.RetryHandler
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonObject
import java.util.UUID

/**
 * Data model for user-deleted messages
 */
@Serializable
data class UserDeletedMessage(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id")
    val userId: String,
    @SerialName("message_id")
    val messageId: String,
    @SerialName("deleted_at")
    val deletedAt: Long = System.currentTimeMillis()
)

/**
 * Repository for message deletion operations
 * Handles delete for me and delete for everyone operations
 */
class MessageDeletionRepository {

    companion object {
        private const val TAG = "MessageDeletionRepository"
    }

    private val client = SupabaseClient.client

    // ==================== Delete For Me Operations ====================

    /**
     * Delete messages for the current user only
     * Inserts records into user_deleted_messages table
     * @param messageIds List of message IDs to delete
     * @param userId Current user ID
     * @return Result indicating success or failure
     */
    suspend fun deleteForMe(messageIds: List<String>, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (messageIds.isEmpty()) {
            return@withContext Result.failure(Exception("Message IDs list cannot be empty"))
        }

        if (userId.isBlank()) {
            return@withContext Result.failure(Exception("User ID is required"))
        }

        // Use RetryHandler for network resilience
        val result = RetryHandler.executeWithRetryResult { attemptNumber ->
            Log.d(TAG, "Deleting messages for user - Attempt: $attemptNumber, MessageCount: ${messageIds.size}, UserId: $userId")

            // Prepare batch insert data
            val deletionRecords = messageIds.map { messageId ->
                UserDeletedMessage(
                    userId = userId,
                    messageId = messageId,
                    deletedAt = System.currentTimeMillis()
                )
            }

            // Batch insert into user_deleted_messages table
            try {
                client.from("user_deleted_messages")
                    .insert(deletionRecords)

                Log.d(TAG, "Successfully deleted ${messageIds.size} messages for user: $userId")
                Unit
            } catch (e: Exception) {
                Log.e(TAG, "Failed to insert user deleted messages", e)
                throw e
            }
        }

        return@withContext result
    }

    // ==================== Delete For Everyone Operations ====================

    /**
     * Delete messages for everyone in the chat
     * Updates is_deleted and delete_for_everyone fields in messages table
     * Only message owners can delete for everyone
     * @param messageIds List of message IDs to delete
     * @param userId Current user ID (must be the sender)
     * @return Result indicating success or failure
     */
    suspend fun deleteForEveryone(messageIds: List<String>, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (messageIds.isEmpty()) {
            return@withContext Result.failure(Exception("Message IDs list cannot be empty"))
        }

        if (userId.isBlank()) {
            return@withContext Result.failure(Exception("User ID is required"))
        }

        // Use RetryHandler for network resilience
        val result = RetryHandler.executeWithRetryResult { attemptNumber ->
            Log.d(TAG, "Deleting messages for everyone - Attempt: $attemptNumber, MessageCount: ${messageIds.size}, UserId: $userId")

            // First, validate ownership - ensure user owns all messages
            val ownedMessageIds = getMessagesBySenderId(messageIds, userId)
            
            if (ownedMessageIds.size != messageIds.size) {
                val unauthorizedCount = messageIds.size - ownedMessageIds.size
                Log.w(TAG, "User $userId does not own $unauthorizedCount of ${messageIds.size} messages")
                throw Exception("You can only delete your own messages for everyone")
            }

            // Batch update messages - set is_deleted and delete_for_everyone to true
            try {
                // Update all messages in a single query
                client.from("messages")
                    .update(
                        mapOf(
                            "is_deleted" to true,
                            "delete_for_everyone" to true,
                            "updated_at" to System.currentTimeMillis()
                        )
                    ) {
                        filter {
                            isIn("id", messageIds)
                        }
                    }

                Log.d(TAG, "Successfully deleted ${messageIds.size} messages for everyone")
                Unit
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update messages as deleted", e)
                throw e
            }
        }

        return@withContext result
    }

    // ==================== Helper Functions ====================

    /**
     * Get message IDs that belong to a specific sender
     * Used for ownership validation
     * @param messageIds List of message IDs to check
     * @param senderId Sender ID to validate against
     * @return List of message IDs owned by the sender
     */
    suspend fun getMessagesBySenderId(messageIds: List<String>, senderId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Validating message ownership - MessageCount: ${messageIds.size}, SenderId: $senderId")

            val messages = client.from("messages")
                .select(columns = Columns.raw("id, sender_id")) {
                    filter {
                        isIn("id", messageIds)
                        eq("sender_id", senderId)
                    }
                }
                .decodeList<JsonObject>()

            val ownedMessageIds = messages.mapNotNull { message ->
                message["id"]?.toString()?.removeSurrounding("\"")
            }

            Log.d(TAG, "User owns ${ownedMessageIds.size} of ${messageIds.size} messages")
            ownedMessageIds
        } catch (e: Exception) {
            Log.e(TAG, "Error validating message ownership", e)
            emptyList()
        }
    }

    /**
     * Fetch user-deleted message IDs for the current user
     * Used to determine which messages should be hidden in the UI
     * @param userId Current user ID
     * @param chatId Optional chat ID to filter by specific chat
     * @return Result with set of deleted message IDs
     */
    suspend fun getUserDeletedMessageIds(userId: String, chatId: String? = null): Result<Set<String>> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            return@withContext Result.failure(Exception("User ID is required"))
        }

        // Use RetryHandler for network resilience
        val result = RetryHandler.executeWithRetryResult { attemptNumber ->
            Log.d(TAG, "Fetching user-deleted messages - Attempt: $attemptNumber, UserId: $userId, ChatId: $chatId")

            try {
                val deletedMessages = client.from("user_deleted_messages")
                    .select(columns = Columns.raw("message_id")) {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<JsonObject>()

                val messageIds = deletedMessages.mapNotNull { record ->
                    record["message_id"]?.toString()?.removeSurrounding("\"")
                }.toSet()

                Log.d(TAG, "Retrieved ${messageIds.size} user-deleted message IDs")
                messageIds
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch user-deleted messages", e)
                throw e
            }
        }

        return@withContext result
    }
}
