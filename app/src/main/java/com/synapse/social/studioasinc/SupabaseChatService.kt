package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.backend.SupabaseRealtimeService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*

/**
 * Supabase implementation of chat service.
 * Handles chat operations including sending messages, real-time updates, and chat management.
 */
class SupabaseChatService {
    
    private val dbService = SupabaseDatabaseService()
    private val realtimeService = SupabaseRealtimeService()
    
    /**
     * Send a text message to a chat.
     */
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        messageText: String,
        messageType: String = "text",
        replyToMessageId: String? = null
    ): Map<String, Any?> {
        val messageData = mapOf(
            "message_key" to UUID.randomUUID().toString(),
            "chat_id" to chatId,
            "sender_id" to senderId,
            "message_text" to messageText,
            "message_type" to messageType,
            "reply_to_message_id" to replyToMessageId,
            "push_date" to Date()
        )
        
        return dbService.insert("messages", messageData)
    }
    
    /**
     * Send a voice message to a chat.
     */
    suspend fun sendVoiceMessage(
        chatId: String,
        senderId: String,
        voiceUrl: String,
        duration: Long,
        replyToMessageId: String? = null
    ): Map<String, Any?> {
        val messageData = mapOf(
            "message_key" to UUID.randomUUID().toString(),
            "chat_id" to chatId,
            "sender_id" to senderId,
            "message_type" to "voice",
            "attachment_url" to voiceUrl,
            "voice_duration" to duration,
            "reply_to_message_id" to replyToMessageId,
            "push_date" to Date()
        )
        
        return dbService.insert("messages", messageData)
    }
    
    /**
     * Send an image message to a chat.
     */
    suspend fun sendImageMessage(
        chatId: String,
        senderId: String,
        imageUrl: String,
        replyToMessageId: String? = null
    ): Map<String, Any?> {
        val messageData = mapOf(
            "message_key" to UUID.randomUUID().toString(),
            "chat_id" to chatId,
            "sender_id" to senderId,
            "message_type" to "image",
            "attachment_url" to imageUrl,
            "reply_to_message_id" to replyToMessageId,
            "push_date" to Date()
        )
        
        return dbService.insert("messages", messageData)
    }
    
    /**
     * Get chat messages with pagination.
     */
    suspend fun getChatMessages(
        chatId: String,
        limit: Int = 50,
        offset: Int = 0
    ): List<Map<String, Any?>> {
        return dbService.selectWithFilter<Map<String, Any?>>(
            table = "messages",
            columns = """
                id, message_key, sender_id, message_text, message_type,
                attachment_url, voice_duration, reply_to_message_id, push_date,
                users!sender_id(username, nickname, avatar)
            """.trimIndent()
        ) { query ->
            // This would need proper implementation based on Supabase query builder
            // For now, this is a placeholder
            query
        }
    }
    
    /**
     * Subscribe to real-time message updates for a chat.
     */
    fun subscribeToMessages(chatId: String): Flow<Map<String, Any?>> {
        return realtimeService.subscribeToMessages(chatId)
    }
    
    /**
     * Create or get existing chat between two users.
     */
    suspend fun getOrCreateChat(user1Id: String, user2Id: String): Map<String, Any?> {
        // Generate consistent chat ID
        val chatId = if (user1Id < user2Id) "${user1Id}_${user2Id}" else "${user2Id}_${user1Id}"
        
        // Try to find existing chat
        val existingChat = dbService.selectWithFilter<Map<String, Any?>>(
            table = "chats",
            columns = "*"
        ) { query ->
            // Filter by chat_id
            query
        }.firstOrNull()
        
        return existingChat ?: run {
            // Create new chat
            val chatData = mapOf(
                "chat_id" to chatId,
                "participant_1" to user1Id,
                "participant_2" to user2Id
            )
            dbService.insert("chats", chatData)
        }
    }
    
    /**
     * Delete a message.
     */
    suspend fun deleteMessage(messageId: String): Boolean {
        return try {
            dbService.selectWithFilter<Map<String, Any?>>(
                table = "messages",
                columns = "id"
            ) { query ->
                // Filter by message_key = messageId and update deleted_at
                query
            }
            
            val updateData = mapOf("deleted_at" to Date())
            dbService.update("messages", updateData)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Update user's typing status.
     */
    suspend fun updateTypingStatus(chatId: String, userId: String, isTyping: Boolean) {
        // This would typically be handled through a separate typing_status table
        // or through real-time presence features
        val typingData = mapOf(
            "chat_id" to chatId,
            "user_id" to userId,
            "is_typing" to isTyping,
            "updated_at" to Date()
        )
        
        dbService.upsert("typing_status", typingData)
    }
    
    /**
     * Get user's chat list (inbox).
     */
    suspend fun getUserChats(userId: String): List<Map<String, Any?>> {
        return dbService.selectWithFilter<Map<String, Any?>>(
            table = "inbox",
            columns = """
                *,
                users!chat_partner_id(id, username, nickname, avatar, status),
                groups!group_id(id, name, avatar),
                messages!last_message_id(message_text, push_date),
                group_messages!last_group_message_id(message_text, push_date)
            """.trimIndent()
        ) { query ->
            // Filter by user_id and order by updated_at
            query
        }
    }
    
    /**
     * Update inbox entry when a new message is sent/received.
     */
    suspend fun updateInbox(
        userId: String,
        chatPartnerId: String? = null,
        groupId: String? = null,
        lastMessageId: String? = null,
        lastGroupMessageId: String? = null
    ) {
        val inboxData = mutableMapOf<String, Any?>(
            "user_id" to userId,
            "updated_at" to Date()
        )
        
        if (chatPartnerId != null) {
            inboxData["chat_partner_id"] = chatPartnerId
            inboxData["last_message_id"] = lastMessageId
        }
        
        if (groupId != null) {
            inboxData["group_id"] = groupId
            inboxData["last_group_message_id"] = lastGroupMessageId
        }
        
        dbService.upsert("inbox", inboxData)
    }
}