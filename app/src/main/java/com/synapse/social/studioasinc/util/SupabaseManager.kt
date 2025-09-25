package com.synapse.social.studioasinc.util

import android.content.Context
import com.synapse.social.studioasinc.R
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.cio.CIO

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
                httpEngine = CIO.create()
            }
        }
    }

    fun getClient(): SupabaseClient {
        return client ?: throw IllegalStateException("Supabase has not been initialized.")
    }

    suspend fun listenForNewMessages(
        chatId: String,
        onNewMessage: (Map<String, Any>) -> Unit
    ) {
        val client = getClient()
        val channel = client.realtime.channel("new_messages_for_$chatId")
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public", table = "messages") {
            filter = "chat_id=eq.$chatId"
        }.collect {
            onNewMessage(it.record)
        }
        client.realtime.connect()
        channel.join()
    }

    suspend fun listenForPresenceChanges(
        channelId: String,
        onPresenceChange: (Map<String, Any>) -> Unit
    ) {
        val client = getClient()
        val channel = client.realtime.channel(channelId)
        channel.presenceChangeFlow().collect {
            onPresenceChange(it)
        }
        client.realtime.connect()
        channel.join()
    }

    suspend fun deleteMessage(messageId: String) {
        val client = getClient()
        client.postgrest["messages"].delete {
            filter {
                eq("id", messageId)
            }
        }
    }

    suspend fun getUser(userId: String): Map<String, Any>? {
        val client = getClient()
        val response = client.postgrest["users"].select {
            filter {
                eq("id", userId)
            }
        }.single()
        return response.data
    }

    suspend fun getChat(chatId: String): Map<String, Any>? {
        val client = getClient()
        val response = client.postgrest["chats"].select {
            filter {
                eq("id", chatId)
            }
        }.single()
        return response.data
    }

    suspend fun getPost(postId: String): Map<String, Any>? {
        val client = getClient()
        val response = client.postgrest["posts"].select {
            filter {
                eq("id", postId)
            }
        }.single()
        return response.data
    }

    suspend fun trackUserPresence(userId: String, presenceStatus: String) {
        val client = getClient()
        val channel = client.realtime.channel("presence_$userId")
        channel.presence {
            track(buildJsonObject { put("status", presenceStatus) })
        }
        client.realtime.connect()
        channel.join()
    }

    suspend fun addMessage(message: Map<String, Any>) {
        val client = getClient()
        client.postgrest["messages"].insert(message)
    }

    suspend fun addReaction(reaction: Map<String, Any>) {
        val client = getClient()
        client.postgrest["reactions"].insert(reaction)
    }

    suspend fun removeReaction(messageId: String, userId: String) {
        val client = getClient()
        client.postgrest["reactions"].delete {
            filter {
                eq("message_id", messageId)
                eq("user_id", userId)
            }
        }
    }

    suspend fun listenForReactions(
        messageId: String,
        onNewReaction: (Map<String, Any>) -> Unit
    ) {
        val client = getClient()
        val channel = client.realtime.channel("reactions_for_$messageId")
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public", table = "reactions") {
            filter = "message_id=eq.$messageId"
        }.collect {
            onNewReaction(it.record)
        }
        client.realtime.connect()
        channel.join()
    }

    suspend fun getMessages(
        chatId: String,
        from: Long,
        to: Long
    ): List<Map<String, Any>>? {
        val client = getClient()
        val response = client.postgrest["messages"].select {
            filter {
                eq("chat_id", chatId)
            }
            order("created_at", Order.DESC)
            range(from, to)
        }
        return response.data
    }

    suspend fun createPost(post: Map<String, Any>) {
        val client = getClient()
        client.postgrest["posts"].insert(post)
    }

    suspend fun listenForNewPosts(
        userId: String,
        onNewPost: (Map<String, Any>) -> Unit
    ) {
        val client = getClient()
        val channel = client.realtime.channel("new_posts_for_$userId")
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public", table = "posts") {
            filter = "user_id=eq.$userId"
        }.collect {
            onNewPost(it.record)
        }
        client.realtime.connect()
        channel.join()
    }

    suspend fun signOut() {
        val client = getClient()
        client.realtime.disconnect()
    }

    suspend fun getUserByUsername(username: String): Map<String, Any>? {
        val client = getClient()
        val response = client.postgrest["users"].select {
            filter {
                eq("username", username)
            }
        }.single()
        return response.data
    }

    suspend fun blockUser(blockerId: String, blockedId: String) {
        val client = getClient()
        client.postgrest["blocklist"].insert(mapOf("blocker_id" to blockerId, "blocked_id" to blockedId))
    }

    suspend fun createGroup(group: Map<String, Any>) {
        val client = getClient()
        client.postgrest["groups"].insert(group)
    }

    suspend fun getUsers(): List<Map<String, Any>>? {
        val client = getClient()
        val response = client.postgrest["users"].select()
        return response.data
    }

    suspend fun getLike(postId: String, userId: String): Map<String, Any>? {
        val client = getClient()
        val response = client.postgrest["post-likes"].select {
            filter {
                eq("post_id", postId)
                eq("user_id", userId)
            }
        }.single()
        return response.data
    }

    suspend fun getLikeCount(postId: String): Long {
        val client = getClient()
        val response = client.postgrest["post-likes"].select(count = Count.EXACT) {
            filter {
                eq("post_id", postId)
            }
        }
        return response.count
    }

    suspend fun getCommentCount(postId: String): Long {
        val client = getClient()
        val response = client.postgrest["post-comments"].select(count = Count.EXACT) {
            filter {
                eq("post_id", postId)
            }
        }
        return response.count
    }

    suspend fun getFavorite(postId: String, userId: String): Map<String, Any>? {
        val client = getClient()
        val response = client.postgrest["favorite-posts"].select {
            filter {
                eq("post_id", postId)
                eq("user_id", userId)
            }
        }.single()
        return response.data
    }

    suspend fun addLike(postId: String, userId: String) {
        val client = getClient()
        client.postgrest["post-likes"].insert(mapOf("post_id" to postId, "user_id" to userId))
    }

    suspend fun removeLike(postId: String, userId: String) {
        val client = getClient()
        client.postgrest["post-likes"].delete {
            filter {
                eq("post_id", postId)
                eq("user_id", userId)
            }
        }
    }

    suspend fun addFavorite(postId: String, userId: String) {
        val client = getClient()
        client.postgrest["favorite-posts"].insert(mapOf("post_id" to postId, "user_id" to userId))
    }

    suspend fun removeFavorite(postId: String, userId: String) {
        val client = getClient()
        client.postgrest["favorite-posts"].delete {
            filter {
                eq("post_id", postId)
                eq("user_id", userId)
            }
        }
    }
}