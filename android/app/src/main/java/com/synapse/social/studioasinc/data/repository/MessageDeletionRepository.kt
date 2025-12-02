package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.SynapseApplication
import com.synapse.social.studioasinc.data.Result
import com.synapse.social.studioasinc.util.ErrorHandler
import com.synapse.social.studioasinc.util.RetryPolicy
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonObject
import java.util.UUID

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

class MessageDeletionRepository {

    companion object {
        private const val TAG = "MessageDeletionRepository"
    }

    private val client = SupabaseClient.client
    private val retryPolicy = RetryPolicy()
    
    private val userDeletedMessagesCache = mutableMapOf<String, MutableSet<String>>()

    suspend fun deleteForMe(messageIds: List<String>, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (messageIds.isEmpty()) {
            return@withContext Result.Error(Exception("Message IDs list cannot be empty"), "Message IDs list cannot be empty")
        }
        if (userId.isBlank()) {
            return@withContext Result.Error(Exception("User ID is required"), "User ID is required")
        }
        try {
            retryPolicy.executeWithRetry {
                val currentTimestamp = System.currentTimeMillis()
                val deletionRecords = messageIds.map { messageId ->
                    UserDeletedMessage(
                        userId = userId,
                        messageId = messageId,
                        deletedAt = currentTimestamp
                    )
                }
                client.from("user_deleted_messages").insert(deletionRecords) { select() }
                updateCacheForDeletedMessages(userId, messageIds)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun deleteForEveryone(messageIds: List<String>, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (messageIds.isEmpty()) {
            return@withContext Result.Error(Exception("Message IDs list cannot be empty"), "Message IDs list cannot be empty")
        }
        if (userId.isBlank()) {
            return@withContext Result.Error(Exception("User ID is required"), "User ID is required")
        }
        try {
            retryPolicy.executeWithRetry {
                val ownedMessageIds = getMessagesBySenderId(messageIds, userId)
                if (ownedMessageIds.size != messageIds.size) {
                    throw Exception("You can only delete your own messages for everyone")
                }
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
                            eq("sender_id", userId)
                        }
                    }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun getMessagesBySenderId(messageIds: List<String>, senderId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val messages = retryPolicy.executeWithRetry {
                client.from("messages")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            isIn("id", messageIds)
                            eq("sender_id", senderId)
                        }
                    }
                    .decodeList<JsonObject>()
            }
            messages.mapNotNull { it["id"]?.toString()?.removeSurrounding("\"") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUserDeletedMessageIds(
        userId: String, 
        chatId: String? = null,
        forceRefresh: Boolean = false
    ): Result<Set<String>> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            return@withContext Result.Error(Exception("User ID is required"), "User ID is required")
        }
        if (!forceRefresh && userDeletedMessagesCache.containsKey(userId)) {
            return@withContext Result.Success(userDeletedMessagesCache[userId]?.toSet() ?: emptySet())
        }
        try {
            val messageIds = retryPolicy.executeWithRetry {
                client.from("user_deleted_messages")
                    .select(columns = Columns.raw("message_id")) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<JsonObject>()
                    .mapNotNull { it["message_id"]?.toString()?.removeSurrounding("\"") }
                    .toSet()
            }
            userDeletedMessagesCache[userId] = messageIds.toMutableSet()
            Result.Success(messageIds)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    private fun updateCacheForDeletedMessages(userId: String, messageIds: List<String>) {
        userDeletedMessagesCache.computeIfAbsent(userId) { mutableSetOf() }.addAll(messageIds)
    }

    fun getCachedDeletedMessageIds(userId: String): Set<String>? {
        return userDeletedMessagesCache[userId]?.toSet()
    }

    fun clearCacheForUser(userId: String) {
        userDeletedMessagesCache.remove(userId)
    }

    fun clearAllCache() {
        userDeletedMessagesCache.clear()
    }
}
