package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.Message
import com.synapse.social.studioasinc.model.Chat
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor() {
    
    private val client = SupabaseClient.client
    
    suspend fun createChat(participantId1: String, participantId2: String): Result<Chat> {
        return try {
            val chatId = "${minOf(participantId1, participantId2)}_${maxOf(participantId1, participantId2)}"
            val chat = Chat(
                chatId = chatId,
                participantId1 = participantId1,
                participantId2 = participantId2
            )
            
            val result = client.from("chats").upsert(chat) {
                select()
            }.decodeSingle<Chat>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getChat(participantId1: String, participantId2: String): Result<Chat?> {
        return try {
            val chat = client.from("chats")
                .select()
                .or("and(participant_1.eq.$participantId1,participant_2.eq.$participantId2),and(participant_1.eq.$participantId2,participant_2.eq.$participantId1)")
                .decodeSingleOrNull<Chat>()
            Result.success(chat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendMessage(message: Message): Result<Message> {
        return try {
            val result = client.from("messages").insert(message) {
                select()
            }.decodeSingle<Message>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMessages(chatId: String, limit: Int = 50): Result<List<Message>> {
        return try {
            val messages = client.from("messages")
                .select()
                .eq("chat_id", chatId)
                .order("created_at", ascending = false)
                .limit(limit.toLong())
                .decodeList<Message>()
            Result.success(messages.reversed())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateMessage(messageId: String, updates: Map<String, Any?>): Result<Message> {
        return try {
            val result = client.from("messages")
                .update(updates) {
                    select()
                }
                .eq("id", messageId)
                .decodeSingle<Message>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            client.from("messages")
                .update(mapOf("deleted_at" to "now()"))
                .eq("id", messageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeMessages(chatId: String): Flow<List<Message>> {
        return client.channel("messages_$chatId")
            .postgresChangeFlow<PostgresAction.Insert>(schema = "public", table = "messages") {
                eq("chat_id", chatId)
            }
            .map { 
                // Return updated messages list
                getMessages(chatId).getOrElse { emptyList() }
            }
    }
    
    suspend fun getUserChats(userId: String): Result<List<Chat>> {
        return try {
            val chats = client.from("chats")
                .select()
                .or("participant_1.eq.$userId,participant_2.eq.$userId")
                .order("updated_at", ascending = false)
                .decodeList<Chat>()
            Result.success(chats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}