package com.synapse.social.studioasinc.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.SynapseApplication
import com.synapse.social.studioasinc.backend.SupabaseChatService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.data.Result
import com.synapse.social.studioasinc.model.MessageEdit
import com.synapse.social.studioasinc.util.ErrorHandler
import com.synapse.social.studioasinc.util.RetryPolicy
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MessageActionRepository(private val context: Context) {

    companion object {
        private const val TAG = "MessageActionRepository"
        private const val PREFS_NAME = "message_action_prefs"
        private const val SUMMARY_CACHE_PREFIX = "ai_summary_"
        private const val SUMMARY_EXPIRY_PREFIX = "ai_summary_expiry_"
        private const val SUMMARY_CACHE_MAX_SIZE = 100
        private const val SUMMARY_EXPIRY_DAYS = 7
        private const val FORTY_EIGHT_HOURS_MS = 48 * 60 * 60 * 1000L
    }

    private val chatService = SupabaseChatService()
    private val databaseService = SupabaseDatabaseService()
    private val client = SupabaseClient.client
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val retryPolicy = RetryPolicy()

    suspend fun forwardMessageToChat(
        messageData: Map<String, Any?>,
        targetChatId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val originalMessageId = messageData["id"]?.toString()
                ?: return@withContext Result.Error(Exception("Message ID is required"), "Message ID is required")
            val originalChatId = messageData["chat_id"]?.toString()
                ?: return@withContext Result.Error(Exception("Chat ID is required"), "Chat ID is required")
            val senderId = messageData["sender_id"]?.toString()
                ?: return@withContext Result.Error(Exception("Sender ID is required"), "Sender ID is required")

            val newMessageId = retryPolicy.executeWithRetry {
                val content = messageData["content"]?.toString() ?: ""
                val messageType = messageData["message_type"]?.toString() ?: "text"
                val mediaUrl = messageData["media_url"]?.toString()
                val newMessageId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()
                val forwardedMessageData = mutableMapOf<String, Any?>(
                    "id" to newMessageId,
                    "chat_id" to targetChatId,
                    "sender_id" to senderId,
                    "content" to content,
                    "message_type" to messageType,
                    "media_url" to mediaUrl,
                    "created_at" to timestamp,
                    "updated_at" to timestamp,
                    "delivery_status" to "sent",
                    "is_deleted" to false,
                    "is_edited" to false,
                    "forwarded_from_message_id" to originalMessageId,
                    "forwarded_from_chat_id" to originalChatId
                )
                databaseService.insert("messages", forwardedMessageData)
                val forwardId = UUID.randomUUID().toString()
                val forwardData = mapOf(
                    "id" to forwardId,
                    "original_message_id" to originalMessageId,
                    "original_chat_id" to originalChatId,
                    "forwarded_message_id" to newMessageId,
                    "forwarded_chat_id" to targetChatId,
                    "forwarded_by" to senderId,
                    "forwarded_at" to timestamp
                )
                databaseService.insert("message_forwards", forwardData)
                updateChatLastMessage(targetChatId, content, timestamp, senderId)
                newMessageId
            }
            Result.Success(newMessageId)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun forwardMessageToMultipleChats(
        messageData: Map<String, Any?>,
        targetChatIds: List<String>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var successCount = 0
            for (chatId in targetChatIds) {
                when (forwardMessageToChat(messageData, chatId)) {
                    is Result.Success -> successCount++
                    is Result.Error -> {}
                    else -> {}
                }
            }
            if (successCount == 0) {
                Result.Error(Exception("Failed to forward message to any chat"), "Failed to forward message to any chat")
            } else {
                Result.Success(successCount)
            }
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    private suspend fun updateChatLastMessage(
        chatId: String,
        lastMessage: String,
        timestamp: Long,
        senderId: String
    ) {
        try {
            val updateData = mapOf(
                "last_message" to lastMessage,
                "last_message_time" to timestamp,
                "last_message_sender" to senderId
            )
            databaseService.update("chats", updateData, "chat_id", chatId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update chat last message", e)
        }
    }

    suspend fun editMessage(messageId: String, newContent: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (newContent.isBlank()) {
            return@withContext Result.Error(Exception("Message content cannot be empty"), "Message content cannot be empty")
        }
        
        try {
            retryPolicy.executeWithRetry {
                val messageResult = client.from("messages")
                    .select(columns = Columns.raw("*")) {
                        filter { eq("id", messageId) }
                        limit(1)
                    }
                    .decodeList<JsonObject>()
                if (messageResult.isEmpty()) {
                    throw Exception("Message not found")
                }
                val message = messageResult.first()
                val createdAt = message["created_at"]?.toString()?.removeSurrounding("\"")?.toLongOrNull() ?: 0L
                val currentContent = message["content"]?.toString()?.removeSurrounding("\"") ?: ""
                val senderId = message["sender_id"]?.toString()?.removeSurrounding("\"") ?: ""
                val messageAge = System.currentTimeMillis() - createdAt
                if (messageAge > FORTY_EIGHT_HOURS_MS) {
                    throw Exception("Message is too old to edit (>48 hours)")
                }
                val editHistoryJson = message["edit_history"]?.toString()?.removeSurrounding("\"")
                val editHistory = try {
                    if (editHistoryJson != null && editHistoryJson != "null" && editHistoryJson.isNotEmpty()) {
                        JSONArray(editHistoryJson)
                    } else {
                        JSONArray()
                    }
                } catch (e: Exception) {
                    JSONArray()
                }
                val editEntry = JSONObject().apply {
                    put("edited_at", System.currentTimeMillis())
                    put("previous_content", currentContent)
                    put("edited_by", senderId)
                }
                editHistory.put(editEntry)
                val updateData = mapOf(
                    "content" to newContent,
                    "is_edited" to true,
                    "edited_at" to System.currentTimeMillis(),
                    "updated_at" to System.currentTimeMillis(),
                    "edit_history" to editHistory.toString()
                )
                databaseService.update("messages", updateData, "id", messageId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun getEditHistory(messageId: String): Result<List<MessageEdit>> = withContext(Dispatchers.IO) {
        try {
            val messageResult = retryPolicy.executeWithRetry {
                 client.from("messages")
                    .select(columns = Columns.raw("edit_history")) {
                        filter { eq("id", messageId) }
                        limit(1)
                    }
                    .decodeList<JsonObject>()
            }
            if (messageResult.isEmpty()) {
                Result.Error(Exception("Message not found"), "Message not found")
            } else {
                val message = messageResult.first()
                val editHistoryJson = message["edit_history"]?.toString()?.removeSurrounding("\"")
                val editHistory = try {
                    if (editHistoryJson != null && editHistoryJson != "null" && editHistoryJson.isNotEmpty()) {
                        val jsonArray = JSONArray(editHistoryJson)
                        val edits = mutableListOf<MessageEdit>()
                        for (i in 0 until jsonArray.length()) {
                            val editObj = jsonArray.getJSONObject(i)
                            edits.add(
                                MessageEdit(
                                    editedAt = editObj.getLong("edited_at"),
                                    previousContent = editObj.getString("previous_content"),
                                    editedBy = editObj.getString("edited_by")
                                )
                            )
                        }
                        edits
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    emptyList()
                }
                Result.Success(editHistory)
            }
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun deleteMessageLocally(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            prefs.edit().putBoolean("deleted_locally_$messageId", true).apply()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    fun isMessageDeletedLocally(messageId: String): Boolean {
        return prefs.getBoolean("deleted_locally_$messageId", false)
    }

    suspend fun deleteMessageForEveryone(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            retryPolicy.executeWithRetry {
                chatService.deleteMessage(messageId, deleteForEveryone = true)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    fun getCachedSummary(messageId: String): String? {
        try {
            val summaryKey = SUMMARY_CACHE_PREFIX + messageId
            val expiryKey = SUMMARY_EXPIRY_PREFIX + messageId
            val summary = prefs.getString(summaryKey, null)
            if (summary.isNullOrEmpty()) {
                return null
            }
            val expiryTime = prefs.getLong(expiryKey, 0L)
            if (System.currentTimeMillis() > expiryTime) {
                prefs.edit()
                    .remove(summaryKey)
                    .remove(expiryKey)
                    .apply()
                return null
            }
            return summary
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cached summary", e)
            return null
        }
    }

    fun cacheSummary(messageId: String, summary: String) {
        try {
            evictOldestCacheEntriesIfNeeded()
            val summaryKey = SUMMARY_CACHE_PREFIX + messageId
            val expiryKey = SUMMARY_EXPIRY_PREFIX + messageId
            val expiryTime = System.currentTimeMillis() + (SUMMARY_EXPIRY_DAYS * 24 * 60 * 60 * 1000L)
            prefs.edit()
                .putString(summaryKey, summary)
                .putLong(expiryKey, expiryTime)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error caching summary", e)
        }
    }

    fun clearSummaryCache() {
        try {
            val editor = prefs.edit()
            val allKeys = prefs.all.keys
            allKeys.forEach { key ->
                if (key.startsWith(SUMMARY_CACHE_PREFIX) || key.startsWith(SUMMARY_EXPIRY_PREFIX)) {
                    editor.remove(key)
                }
            }
            editor.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing summary cache", e)
        }
    }

    private fun getCachedSummaryCount(): Int {
        return prefs.all.keys.count { it.startsWith(SUMMARY_CACHE_PREFIX) }
    }

    private fun evictOldestCacheEntriesIfNeeded() {
        try {
            val currentCount = getCachedSummaryCount()
            if (currentCount < SUMMARY_CACHE_MAX_SIZE) {
                return
            }
            val entries = mutableListOf<Pair<String, Long>>()
            prefs.all.forEach { (key, value) ->
                if (key.startsWith(SUMMARY_CACHE_PREFIX)) {
                    val messageId = key.removePrefix(SUMMARY_CACHE_PREFIX)
                    val expiryKey = SUMMARY_EXPIRY_PREFIX + messageId
                    val expiryTime = prefs.getLong(expiryKey, 0L)
                    entries.add(Pair(messageId, expiryTime))
                }
            }
            entries.sortBy { it.second }
            val entriesToRemove = (currentCount * 0.1).toInt().coerceAtLeast(1)
            val editor = prefs.edit()
            entries.take(entriesToRemove).forEach { (messageId, _) ->
                editor.remove(SUMMARY_CACHE_PREFIX + messageId)
                editor.remove(SUMMARY_EXPIRY_PREFIX + messageId)
            }
            editor.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error evicting cache entries", e)
        }
    }

    fun cleanupExpiredCacheEntries() {
        try {
            val currentTime = System.currentTimeMillis()
            val editor = prefs.edit()
            var removedCount = 0
            prefs.all.forEach { (key, _) ->
                if (key.startsWith(SUMMARY_EXPIRY_PREFIX)) {
                    val expiryTime = prefs.getLong(key, 0L)
                    if (currentTime > expiryTime) {
                        val messageId = key.removePrefix(SUMMARY_EXPIRY_PREFIX)
                        editor.remove(SUMMARY_CACHE_PREFIX + messageId)
                        editor.remove(key)
                        removedCount++
                    }
                }
            }
            editor.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up expired cache entries", e)
        }
    }
}
