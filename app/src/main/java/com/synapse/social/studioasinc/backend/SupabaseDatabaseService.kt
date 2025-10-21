package com.synapse.social.studioasinc.backend

import io.github.jan.tennert.supabase.postgrest.from
import io.github.jan.tennert.supabase.postgrest.query.PostgrestQueryBuilder
import kotlinx.serialization.json.JsonObject
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.backend.interfaces.ISupabaseDatabaseService
import com.synapse.social.studioasinc.model.*

/**
 * Supabase implementation of database service.
 * Handles database operations using Supabase Postgrest with typed models.
 */
class SupabaseDatabaseService : ISupabaseDatabaseService {
    
    private val client = SupabaseClient.client
    
    override suspend fun <T> select(table: String, columns: String): List<T> {
        return client.from(table).select(columns = columns).decodeList()
    }
    
    override suspend fun <T> selectSingle(table: String, columns: String): T? {
        return try {
            client.from(table).select(columns = columns).decodeSingle<T>()
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun insert(table: String, data: Map<String, Any?>): Map<String, Any?> {
        return client.from(table).insert(data).decodeSingle()
    }
    
    override suspend fun update(table: String, data: Map<String, Any?>): Map<String, Any?> {
        return client.from(table).update(data).decodeSingle()
    }
    
    override suspend fun delete(table: String): Boolean {
        return try {
            client.from(table).delete()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun upsert(table: String, data: Map<String, Any?>): Map<String, Any?> {
        return client.from(table).upsert(data).decodeSingle()
    }
    
    override suspend fun <T> selectWithFilter(
        table: String, 
        columns: String,
        filter: (query: Any) -> Any
    ): List<T> {
        val query = client.from(table).select(columns = columns)
        val filteredQuery = filter(query) as PostgrestQueryBuilder
        return filteredQuery.decodeList()
    }
    
    // Typed methods for specific models
    
    suspend fun getUserByUid(uid: String): User? {
        return try {
            client.from("users")
                .select()
                .eq("uid", uid)
                .decodeSingle<User>()
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getUserByUsername(username: String): User? {
        return try {
            client.from("users")
                .select()
                .eq("username", username)
                .decodeSingle<User>()
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun insertUser(user: User): User {
        return client.from("users").insert(user).decodeSingle()
    }
    
    suspend fun updateUser(uid: String, updates: Map<String, Any?>): User {
        return client.from("users")
            .update(updates)
            .eq("uid", uid)
            .decodeSingle()
    }
    
    suspend fun getChatMessages(chatId: String, limit: Int = 50, offset: Int = 0): List<Message> {
        return client.from("messages")
            .select("""
                *,
                users!sender_id(username, nickname, avatar)
            """.trimIndent())
            .eq("chat_id", chatId)
            .isNull("deleted_at")
            .order("push_date", ascending = true)
            .limit(limit.toLong())
            .range(offset.toLong(), (offset + limit - 1).toLong())
            .decodeList()
    }
    
    suspend fun insertMessage(message: Message): Message {
        return client.from("messages").insert(message).decodeSingle()
    }
    
    suspend fun getOrCreateChat(user1Id: String, user2Id: String): Chat {
        val chatId = if (user1Id < user2Id) "${user1Id}_${user2Id}" else "${user2Id}_${user1Id}"
        
        return try {
            client.from("chats")
                .select()
                .eq("chat_id", chatId)
                .decodeSingle<Chat>()
        } catch (e: Exception) {
            // Chat doesn't exist, create it
            val newChat = Chat(
                chatId = chatId,
                participant1 = user1Id,
                participant2 = user2Id,
                createdAt = java.time.Instant.now().toString(),
                updatedAt = java.time.Instant.now().toString()
            )
            client.from("chats").insert(newChat).decodeSingle()
        }
    }
    
    suspend fun getUserChats(userId: String): List<Inbox> {
        return client.from("inbox")
            .select("""
                *,
                users!chat_partner_id(id, username, nickname, avatar, status),
                groups!group_id(id, name, avatar),
                messages!last_message_id(message_text, push_date, message_type),
                group_messages!last_group_message_id(message_text, push_date, message_type)
            """.trimIndent())
            .eq("user_id", userId)
            .order("updated_at", ascending = false)
            .decodeList()
    }
    
    suspend fun updateInbox(inbox: Inbox): Inbox {
        return client.from("inbox").upsert(inbox).decodeSingle()
    }
    
    suspend fun checkUsernameAvailability(username: String): Boolean {
        return try {
            val result = client.from("users")
                .select("id")
                .eq("username", username)
                .decodeSingle<Map<String, Any?>>()
            false // Username exists
        } catch (e: Exception) {
            true // Username available
        }
    }
    
    suspend fun insertUsernameRegistry(registry: UsernameRegistry): UsernameRegistry {
        return client.from("username_registry").insert(registry).decodeSingle()
    }
}