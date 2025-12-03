package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.SynapseApplication
import com.synapse.social.studioasinc.backend.SupabaseChatService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.data.Result
import com.synapse.social.studioasinc.model.Chat
import com.synapse.social.studioasinc.model.Message
import com.synapse.social.studioasinc.util.ErrorHandler
import com.synapse.social.studioasinc.util.RetryPolicy
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

class ChatRepository {

    private val chatService = SupabaseChatService()
    private val databaseService = SupabaseDatabaseService()
    private val client = SupabaseClient.client
    private val retryPolicy = RetryPolicy()
    
    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(expirationMs: Long = CACHE_EXPIRATION_MS): Boolean {
            return System.currentTimeMillis() - timestamp > expirationMs
        }
    }
    
    private val messagesCache = mutableMapOf<String, CacheEntry<List<Message>>>()
    
    companion object {
        private const val CACHE_EXPIRATION_MS = 5 * 60 * 1000L // 5 minutes
    }
    
    fun invalidateCache() {
        messagesCache.clear()
    }
    
    private fun getCacheKey(chatId: String, beforeTimestamp: Long?, limit: Int): String {
        return "messages_chat_${chatId}_before_${beforeTimestamp}_limit_${limit}"
    }

    suspend fun createChat(participantUids: List<String>, chatName: String? = null): Result<String> {
        return try {
            if (participantUids.size == 2) {
                val result = retryPolicy.executeWithRetry {
                    chatService.getOrCreateDirectChat(participantUids[0], participantUids[1])
                }
                if (result.isSuccess) {
                    Result.Success(result.getOrThrow())
                } else {
                    val exception = result.exceptionOrNull() as? Exception ?: Exception("Unknown error")
                    Result.Error(exception, ErrorHandler.getErrorMessage(exception, SynapseApplication.applicationContext()))
                }
            } else {
                Result.Error(Exception("Group chats not yet implemented"), "Group chats not yet implemented")
            }
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        content: String,
        messageType: String = "text",
        replyToId: String? = null
    ): Result<String> {
        return try {
            val result = retryPolicy.executeWithRetry {
                 chatService.sendMessage(
                    chatId = chatId,
                    senderId = senderId,
                    content = content,
                    messageType = messageType,
                    replyToId = replyToId
                )
            }
            if (result.isSuccess) {
                Result.Success(result.getOrThrow())
            } else {
                val exception = result.exceptionOrNull() as? Exception ?: Exception("Unknown error")
                Result.Error(exception, ErrorHandler.getErrorMessage(exception, SynapseApplication.applicationContext()))
            }
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun getMessages(
        chatId: String, 
        limit: Int = 50, 
        beforeTimestamp: Long? = null
    ): Result<List<Message>> {
        return try {
            val result = retryPolicy.executeWithRetry {
                chatService.getMessages(chatId, limit, beforeTimestamp)
            }
            if(result.isSuccess){
                val messages = result.getOrThrow().map { messageData ->
                    mapToMessage(messageData)
                }
                Result.Success(messages)
            } else {
                 val exception = result.exceptionOrNull() as? Exception ?: Exception("Unknown error")
                Result.Error(exception, ErrorHandler.getErrorMessage(exception, SynapseApplication.applicationContext()))
            }
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }
    
    suspend fun getMessagesPage(
        chatId: String,
        beforeTimestamp: Long? = null,
        limit: Int = 50
    ): Result<List<Message>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val cacheKey = getCacheKey(chatId, beforeTimestamp, limit)
            val cachedEntry = messagesCache[cacheKey]
            
            if (cachedEntry != null && !cachedEntry.isExpired()) {
                return@withContext Result.Success(cachedEntry.data)
            }
            
            val messages = retryPolicy.executeWithRetry {
                client.from("messages")
                    .select() {
                        filter {
                            eq("chat_id", chatId)
                            beforeTimestamp?.let {
                                lt("created_at", it)
                            }
                        }
                        if (limit < Int.MAX_VALUE) {
                            limit(limit.toLong())
                        }
                        order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<Message>()
            }
            
            messagesCache[cacheKey] = CacheEntry(messages)
            Result.Success(messages)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun getUserChats(userId: String): Result<List<Chat>> {
        return try {
            val result = retryPolicy.executeWithRetry {
                chatService.getUserChats(userId)
            }
            if(result.isSuccess) {
                val chats = result.getOrThrow().map { chatData ->
                    mapToChat(chatData)
                }
                Result.Success(chats)
            } else {
                 val exception = result.exceptionOrNull() as? Exception ?: Exception("Unknown error")
                Result.Error(exception, ErrorHandler.getErrorMessage(exception, SynapseApplication.applicationContext()))
            }
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            val result = retryPolicy.executeWithRetry {
                chatService.deleteMessage(messageId)
            }
             if (result.isSuccess) {
                Result.Success(Unit)
            } else {
                val exception = result.exceptionOrNull() as? Exception ?: Exception("Unknown error")
                Result.Error(exception, ErrorHandler.getErrorMessage(exception, SynapseApplication.applicationContext()))
            }
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun editMessage(messageId: String, newContent: String): Result<Unit> {
        return try {
            retryPolicy.executeWithRetry {
                val updateData = mapOf(
                    "content" to newContent,
                    "is_edited" to true,
                    "edited_at" to System.currentTimeMillis()
                )
                databaseService.update("messages", updateData, "id", messageId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun getChatParticipants(chatId: String): Result<List<String>> {
        return try {
            val participants = retryPolicy.executeWithRetry {
                client.from("chat_participants")
                    .select(columns = Columns.raw("user_id")) {
                        filter { eq("chat_id", chatId) }
                    }
                    .decodeList<JsonObject>()
            }
            
            val userIds = participants.map { 
                it["user_id"].toString().removeSurrounding("\"") 
            }
            Result.Success(userIds)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun addParticipant(chatId: String, userId: String): Result<Unit> {
        return try {
            retryPolicy.executeWithRetry {
                val participantData = mapOf(
                    "chat_id" to chatId,
                    "user_id" to userId,
                    "role" to "member",
                    "is_admin" to false,
                    "can_send_messages" to true,
                    "joined_at" to System.currentTimeMillis()
                )
                databaseService.insert("chat_participants", participantData)
            }
             Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun removeParticipant(chatId: String, userId: String): Result<Unit> {
        return try {
            retryPolicy.executeWithRetry {
                client.from("chat_participants").delete {
                    filter {
                        eq("chat_id", chatId)
                        eq("user_id", userId)
                    }
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    fun observeMessages(chatId: String): Flow<Result<List<Message>>> = kotlinx.coroutines.flow.flow {
        emit(Result.Loading)
        try {
            client.channel("messages:$chatId").postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "messages"
                filter = "chat_id=eq.$chatId"
            }.map {
                when (val result = getMessages(chatId)) {
                    is Result.Success -> Result.Success(result.data)
                    is Result.Error -> Result.Error(result.exception, result.message)
                    else -> Result.Loading
                }
            }.catch { e ->
                emit(Result.Error(e as? Exception ?: Exception(e.message), ErrorHandler.getErrorMessage(e as? Exception ?: Exception(e.message), SynapseApplication.applicationContext())))
            }.collect { emit(it) }
        } catch (e: Exception) {
            emit(Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext())))
        }
    }

    fun observeUserChats(userId: String): Flow<Result<List<Chat>>> = kotlinx.coroutines.flow.flow {
        emit(Result.Loading)
        try {
            client.channel("user_chats:$userId").postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "chats"
            }.map {
                when (val result = getUserChats(userId)) {
                    is Result.Success -> Result.Success(result.data)
                    is Result.Error -> Result.Error(result.exception, result.message)
                    else -> Result.Loading
                }
            }.catch { e ->
                emit(Result.Error(e as? Exception ?: Exception(e.message), ErrorHandler.getErrorMessage(e as? Exception ?: Exception(e.message), SynapseApplication.applicationContext())))
            }.collect { emit(it) }
        } catch (e: Exception) {
            emit(Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext())))
        }
    }

    suspend fun createDirectChat(currentUserId: String, otherUserId: String): Result<String> {
        return try {
            val result = retryPolicy.executeWithRetry {
                chatService.getOrCreateDirectChat(currentUserId, otherUserId)
            }
            if(result.isSuccess) {
                Result.Success(result.getOrThrow())
            } else {
                 val exception = result.exceptionOrNull() as? Exception ?: Exception("Unknown error")
                Result.Error(exception, ErrorHandler.getErrorMessage(exception, SynapseApplication.applicationContext()))
            }
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun findDirectChat(userId1: String, userId2: String): Result<String?> {
        return try {
            val chatId = if (userId1 < userId2) {
                "dm_${userId1}_${userId2}"
            } else {
                "dm_${userId2}_${userId1}"
            }
            
            val existingChat = retryPolicy.executeWithRetry {
                client.from("chats")
                    .select(columns = Columns.raw("chat_id")) {
                        filter { eq("chat_id", chatId) }
                        limit(1)
                    }
                    .decodeList<JsonObject>()
            }
            
            if (existingChat.isNotEmpty()) {
                Result.Success(chatId)
            } else {
                Result.Success(null)
            }
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun getOrCreateDirectChat(currentUserId: String, otherUserId: String): Result<String> {
        return try {
            val result = retryPolicy.executeWithRetry {
                chatService.getOrCreateDirectChat(currentUserId, otherUserId)
            }
            if(result.isSuccess) {
                Result.Success(result.getOrThrow())
            } else {
                 val exception = result.exceptionOrNull() as? Exception ?: Exception("Unknown error")
                Result.Error(exception, ErrorHandler.getErrorMessage(exception, SynapseApplication.applicationContext()))
            }
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun markMessagesAsRead(chatId: String, userId: String): Result<Unit> {
        return try {
            val result = retryPolicy.executeWithRetry {
                chatService.markMessagesAsRead(chatId, userId)
            }
            if(result.isSuccess) {
                Result.Success(Unit)
            } else {
                 val exception = result.exceptionOrNull() as? Exception ?: Exception("Unknown error")
                Result.Error(exception, ErrorHandler.getErrorMessage(exception, SynapseApplication.applicationContext()))
            }
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    private fun mapToMessage(data: Map<String, Any?>): Message {
        return Message(
            id = data["id"]?.toString() ?: "",
            chatId = data["chat_id"]?.toString() ?: "",
            senderId = data["sender_id"]?.toString() ?: "",
            content = data["content"]?.toString() ?: "",
            messageType = data["message_type"]?.toString() ?: "text",
            createdAt = data["created_at"]?.toString()?.toLongOrNull() ?: 0L,
            isDeleted = data["is_deleted"]?.toString()?.toBooleanStrictOrNull() ?: false,
            isEdited = data["is_edited"]?.toString()?.toBooleanStrictOrNull() ?: false,
            replyToId = data["reply_to_id"]?.toString()
        )
    }

    private fun mapToChat(data: Map<String, Any?>): Chat {
        return Chat(
            id = data["chat_id"]?.toString() ?: "",
            isGroup = data["is_group"]?.toString()?.toBooleanStrictOrNull() ?: false,
            lastMessage = data["last_message"]?.toString(),
            lastMessageTime = data["last_message_time"]?.toString()?.toLongOrNull(),
            lastMessageSender = data["last_message_sender"]?.toString(),
            createdAt = data["created_at"]?.toString()?.toLongOrNull() ?: 0L,
            isActive = data["is_active"]?.toString()?.toBooleanStrictOrNull() ?: true
        )
    }
}
