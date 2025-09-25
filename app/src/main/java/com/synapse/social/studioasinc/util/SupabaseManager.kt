package com.synapse.social.studioasinc.util

import android.content.Context
import com.synapse.social.studioasinc.R
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.presence.presence
import io.ktor.client.engine.android.Android
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object SupabaseManager {

    private var client: SupabaseClient? = null

    fun initialize(context: Context) {
        if (client == null) {
            val supabaseUrl = context.getString(R.string.supabase_url)
            val supabaseKey = context.getString(R.string.supabase_anon_key)
            client = createSupabaseClient(
                supabaseUrl = supabaseUrl,
                supabaseKey = supabaseKey
            ) {
                install(GoTrue)
                install(Postgrest)
                install(Realtime)
                httpEngine = Android.create()
            }
        }
    }

    fun getClient(): SupabaseClient {
        return client ?: throw IllegalStateException("Supabase has not been initialized.")
    }

    fun getCurrentUserID(): String? {
        return getClient().auth.currentUserOrNull()?.id
    }

    suspend fun listenForNewMessages(
        chatId: String,
        onNewMessage: (Map<*, *>) -> Unit
    ) {
        val client = getClient()
        val channel = client.channel("new_messages_for_$chatId")
        client.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
        }.collect {
            onNewMessage(it.record)
        }
        channel.subscribe()
    }

    suspend fun listenForPresenceChanges(
        channelId: String,
        onPresenceChange: (Map<*, *>) -> Unit
    ) {
        val client = getClient()
        val channel = client.channel(channelId)
        channel.presence.state.collect {
            onPresenceChange(it)
        }
        channel.subscribe()
    }

    suspend fun deleteMessage(messageId: String) {
        val client = getClient()
        client.postgrest.from("messages").delete {
            filter {
                eq("id", messageId)
            }
        }
    }

    suspend fun getUser(userId: String): Map<*, *>? {
        val client = getClient()
        val response = client.postgrest.from("users").select {
            filter {
                eq("id", userId)
            }
        }.body<List<Map<String, Any>>>()
        return response.firstOrNull()
    }

    suspend fun getGroup(groupId: String): Map<*, *>? {
        val client = getClient()
        val response = client.postgrest.from("groups").select {
            filter {
                eq("id", groupId)
            }
        }.body<List<Map<String, Any>>>()
        return response.firstOrNull()
    }

    suspend fun getChat(chatId: String): Map<*, *>? {
        val client = getClient()
        val response = client.postgrest.from("chats").select {
            filter {
                eq("id", chatId)
            }
        }.body<List<Map<String, Any>>>()
        return response.firstOrNull()
    }

    suspend fun getPost(postId: String): Map<*, *>? {
        val client = getClient()
        val response = client.postgrest.from("posts").select {
            filter {
                eq("id", postId)
            }
        }.body<List<Map<String, Any>>>()
        return response.firstOrNull()
    }

    suspend fun trackUserPresence(userId: String, presenceStatus: String) {
        val client = getClient()
        val channel = client.channel("presence_$userId")
        channel.presence.track(buildJsonObject { put("status", presenceStatus) })
        channel.subscribe()
    }

    suspend fun addMessage(message: Map<String, Any>) {
        val client = getClient()
        client.postgrest.from("messages").insert(message)
    }

    suspend fun addReaction(reaction: Map<String, Any>) {
        val client = getClient()
        client.postgrest.from("reactions").insert(reaction)
    }

    suspend fun removeReaction(messageId: String, userId: String) {
        val client = getClient()
        client.postgrest.from("reactions").delete {
            filter {
                eq("message_id", messageId)
                eq("user_id", userId)
            }
        }
    }

    suspend fun listenForReactions(
        messageId: String,
        onNewReaction: (Map<*, *>) -> Unit
    ) {
        val client = getClient()
        val channel = client.channel("reactions_for_$messageId")
        client.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "reactions"
        }.collect {
            onNewReaction(it.record)
        }
        channel.subscribe()
    }

    suspend fun getMessages(
        chatId: String,
        from: Long,
        to: Long
    ): List<Map<*, *>>? {
        val client = getClient()
        val response = client.postgrest.from("messages").select {
            filter {
                eq("chat_id", chatId)
            }
            order("created_at", Order.DESC)
            range(from, to)
        }.body<List<Map<String, Any>>>()
        return response
    }

    suspend fun createPost(post: Map<String, Any>) {
        val client = getClient()
        client.postgrest.from("posts").insert(post)
    }

    suspend fun listenForNewPosts(
        userId: String,
        onNewPost: (Map<*, *>) -> Unit
    ) {
        val client = getClient()
        val channel = client.channel("new_posts_for_$userId")
        client.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "posts"
        }.collect {
            onNewPost(it.record)
        }
        channel.subscribe()
    }

    suspend fun signOut() {
        val client = getClient()
        client.realtime.disconnect()
    }

    suspend fun getUserByUsername(username: String): Map<*, *>? {
        val client = getClient()
        val response = client.postgrest.from("users").select {
            filter {
                eq("username", username)
            }
        }.body<List<Map<String, Any>>>()
        return response.firstOrNull()
    }

    suspend fun blockUser(blockerId: String, blockedId: String) {
        val client = getClient()
        client.postgrest.from("blocklist").insert(mapOf("blocker_id" to blockerId, "blocked_id" to blockedId))
    }

    suspend fun createGroup(group: Map<String, Any>) {
        val client = getClient()
        client.postgrest.from("groups").insert(group)
    }

    suspend fun getUsers(): List<Map<*, *>>? {
        val client = getClient()
        val response = client.postgrest.from("users").select().body<List<Map<String, Any>>>()
        return response
    }

    suspend fun getLike(postId: String, userId: String): Map<*, *>? {
        val client = getClient()
        val response = client.postgrest.from("post-likes").select {
            filter {
                eq("post_id", postId)
                eq("user_id", userId)
            }
        }.body<List<Map<String, Any>>>()
        return response.firstOrNull()
    }

    suspend fun getLikeCount(postId: String): Long {
        val client = getClient()
        val response = client.postgrest.from("post-likes").select(columns = Columns.list("count"), count = Count.EXACT) {
            filter {
                eq("post_id", postId)
            }
        }
        return response.count ?: 0L
    }

    suspend fun getCommentCount(postId: String): Long {
        val client = getClient()
        val response = client.postgrest.from("post-comments").select(columns = Columns.list("count"), count = Count.EXACT) {
            filter {
                eq("post_id", postId)
            }
        }
        return response.count ?: 0L
    }

    suspend fun getFavorite(postId: String, userId: String): Map<*, *>? {
        val client = getClient()
        val response = client.postgrest.from("favorite-posts").select {
            filter {
                eq("post_id", postId)
                eq("user_id", userId)
            }
        }.body<List<Map<String, Any>>>()
        return response.firstOrNull()
    }

    suspend fun addLike(postId: String, userId: String) {
        val client = getClient()
        client.postgrest.from("post-likes").insert(mapOf("post_id" to postId, "user_id" to userId))
    }

    suspend fun removeLike(postId: String, userId: String) {
        val client = getClient()
        client.postgrest.from("post-likes").delete {
            filter {
                eq("post_id", postId)
                eq("user_id", userId)
            }
        }
    }

    suspend fun addFavorite(postId: String, userId: String) {
        val client = getClient()
        client.postgrest.from("favorite-posts").insert(mapOf("post_id" to postId, "user_id" to userId))
    }

    suspend fun removeFavorite(postId: String, userId: String) {
        val client = getClient()
        client.postgrest.from("favorite-posts").delete {
            filter {
                eq("post_id", postId)
                eq("user_id", userId)
            }
        }
    }
}