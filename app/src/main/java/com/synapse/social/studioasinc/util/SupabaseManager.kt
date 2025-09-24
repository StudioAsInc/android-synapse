package com.synapse.social.studioasinc.util

import android.util.Log
import com.synapse.social.studioasinc.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object SupabaseManager {

    private const val TAG = "SupabaseManager"

    private val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Realtime)
            install(Auth)
        }
    }

    val auth: Auth
        get() = client.auth

    fun listenToUserProfileChanges(
        userId: String,
        scope: CoroutineScope,
        onUpdate: (Map<String, Any?>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val channel = client.channel("user_profile_changes")
                val changeFlow = channel.postgresChangeFlow<Postgrest.Channel.PostgresAction>(schema = "public") {
                    table = "users"
                    filter = "uid=eq.$userId"
                }

                changeFlow
                    .catch { error ->
                        Log.e(TAG, "Error listening to user profile changes", error)
                        onError(error)
                    }
                    .collect {
                        when (it) {
                            is Postgrest.Channel.PostgresAction.Update -> {
                                onUpdate(it.record)
                            }
                            is Postgrest.Channel.PostgresAction.Insert -> {
                                onUpdate(it.record)
                            }
                            else -> {
                                // Handle other cases if needed
                            }
                        }
                    }

                client.realtime.connect()
                channel.join()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to Supabase Realtime", e)
                onError(e)
            }
        }
    }

    suspend fun deleteMessage(messageKey: String) {
        client.postgrest["chats"].delete {
            filter("key", "eq", messageKey)
        }
    }

    suspend fun getUser(userId: String): Map<String, Any?>? {
        val response = client.postgrest["users"].select {
            filter("uid", "eq", userId)
            single()
        }
        return response.data?.jsonObject?.toMap()
    }

    suspend fun getGroup(groupId: String): Map<String, Any?>? {
        val response = client.postgrest["groups"].select {
            filter("id", "eq", groupId)
            single()
        }
        return response.data?.jsonObject?.toMap()
    }

    suspend fun getMessage(messageKey: String): Map<String, Any?>? {
        val response = client.postgrest["chats"].select {
            filter("key", "eq", messageKey)
            single()
        }
        return response.data?.jsonObject?.toMap()
    }

    suspend fun getOneSignalPlayerId(userId: String): String? {
        val response = client.postgrest["users"].select("oneSignalPlayerId") {
            filter("uid", "eq", userId)
            single()
        }
        return response.data?.jsonObject?.get("oneSignalPlayerId")?.jsonPrimitive?.content
    }


    suspend fun setTypingStatus(chatId: String, userId: String, isTyping: Boolean) {
        val channel = client.channel("typing_status")
        channel.subscribe()
        channel.track(
            "typing",
            mapOf("chat_id" to chatId, "user_id" to userId, "is_typing" to isTyping)
        )
    }

    fun listenToTypingStatus(
        chatId: String,
        scope: CoroutineScope,
        onTyping: (String, Boolean) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val channel = client.channel("typing_status")
            channel.subscribe()
            val flow = channel.presenceChangeFlow()
            flow.collect {
                val presences = it.currentPresences
                for (presence in presences) {
                    val state = presence.state
                    if (state["chat_id"]?.jsonPrimitive?.content == chatId) {
                        val userId = state["user_id"]?.jsonPrimitive?.content ?: ""
                        val isTyping = state["is_typing"]?.jsonPrimitive?.content?.toBoolean() ?: false
                        onTyping(userId, isTyping)
                    }
                }
            }
        }
    }

    suspend fun blockUser(blockerId: String, blockedId: String) {
        client.postgrest["blocklist"].insert(mapOf("blocker_id" to blockerId, "blocked_id" to blockedId))
    }

    suspend fun unblockUser(blockerId: String, blockedId: String) {
        client.postgrest["blocklist"].delete {
            filter("blocker_id", "eq", blockerId)
            filter("blocked_id", "eq", blockedId)
        }
    }

    fun listenToBlocklistChanges(
        userId: String,
        scope: CoroutineScope,
        onBlocklistUpdate: (List<String>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val channel = client.channel("blocklist_changes")
                val changeFlow = channel.postgresChangeFlow<Postgrest.Channel.PostgresAction>(schema = "public") {
                    table = "blocklist"
                    filter = "blocker_id=eq.$userId"
                }

                changeFlow
                    .catch { error ->
                        Log.e(TAG, "Error listening to blocklist changes", error)
                        onError(error)
                    }
                    .collect {
                        val response = client.postgrest["blocklist"].select {
                            filter("blocker_id", "eq", userId)
                        }
                        val blockedUsers = response.data.map { it.jsonObject["blocked_id"]?.jsonPrimitive?.content ?: "" }
                        onBlocklistUpdate(blockedUsers)
                    }

                client.realtime.connect()
                channel.join()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to Supabase Realtime", e)
                onError(e)
            }
        }
    }

    suspend fun getChatMessages(
        chatId: String,
        limit: Long,
        offset: Long
    ): List<Map<String, Any?>> {
        val response = client.postgrest["chats"].select {
            filter("chat_id", "eq", chatId)
            order("push_date", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            range(offset, offset + limit - 1)
        }
        return response.data.map { it.jsonObject.toMap() }
    }

    suspend fun sendMessage(
        chatId: String,
        message: Map<String, Any>
    ) {
        client.postgrest["chats"].insert(message)
    }

    fun listenToChatMessages(
        chatId: String,
        scope: CoroutineScope,
        onNewMessage: (Map<String, Any?>) -> Unit,
        onUpdateMessage: (Map<String, Any?>) -> Unit,
        onDeleteMessage: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val channel = client.channel("chat_messages")
                val changeFlow = channel.postgresChangeFlow<Postgrest.Channel.PostgresAction>(schema = "public") {
                    table = "chats"
                    filter = "chat_id=eq.$chatId"
                }

                changeFlow
                    .catch { error ->
                        Log.e(TAG, "Error listening to chat messages", error)
                        onError(error)
                    }
                    .collect {
                        when (it) {
                            is Postgrest.Channel.PostgresAction.Insert -> onNewMessage(it.record)
                            is Postgrest.Channel.PostgresAction.Update -> onUpdateMessage(it.record)
                            is Postgrest.Channel.PostgresAction.Delete -> onDeleteMessage(it.oldRecord["key"]?.jsonPrimitive?.content ?: "")
                            else -> {}
                        }
                    }

                client.realtime.connect()
                channel.join()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to Supabase Realtime", e)
                onError(e)
            }
        }
    }

    fun close() {
        client.realtime.disconnect()
    }
}