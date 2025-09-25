package com.synapse.social.studioasinc.util

import android.content.Context
import com.synapse.social.studioasinc.R
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.presence
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
            }
        }
    }

    fun getClient(): SupabaseClient {
        return client ?: throw IllegalStateException("Supabase has not been initialized.")
    }

    fun getCurrentUserID(): String? {
        return getClient().gotrue.currentUserOrNull()?.id
    }

    suspend fun listenForNewMessages(
        chatId: String,
        onNewMessage: (Map<*, *>) -> Unit
    ) {
        val client = getClient()
        val channel = client.channel("new_messages_for_$chatId")
        client.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
            filter("chat_id", "eq.$chatId")
        }.collect {
            @Suppress("UNCHECKED_CAST")
            onNewMessage(it.record as Map<String, Any>)
        }
        channel.subscribe()
    }

    suspend fun listenForPresenceChanges(
        channelId: String,
        onPresenceChange: (Map<*, *>) -> Unit
    ) {
        val client = getClient()
        val channel = client.channel(channelId)
        channel.presence.stateFlow.collect {
            @Suppress("UNCHECKED_CAST")
            onPresenceChange(it as Map<String, Any>)
        }
        channel.subscribe()
    }

    suspend fun deleteMessage(messageId: String) {
        val client = getClient()
        client.postgrest.from("messages").delete {
            filter("id", "eq.$messageId")
        }
    }

    suspend fun getUser(userId: String): Map<String, Any>? {
        val client = getClient()
        return try {
            client.postgrest.from("users").select {
                filter("id", "eq.$userId")
            }.data.let {
                @Suppress("UNCHECKED_CAST")
                (it as? List<Map<String, Any>>)?.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getGroup(groupId: String): Map<String, Any>? {
        val client = getClient()
        return try {
            client.postgrest.from("groups").select {
                filter("id", "eq.$groupId")
            }.data.let {
                @Suppress("UNCHECKED_CAST")
                (it as? List<Map<String, Any>>)?.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getChat(chatId: String): Map<String, Any>? {
        val client = getClient()
        return try {
            client.postgrest.from("chats").select {
                filter("id", "eq.$chatId")
            }.data.let {
                @Suppress("UNCHECKED_CAST")
                (it as? List<Map<String, Any>>)?.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getPost(postId: String): Map<String, Any>? {
        val client = getClient()
        return try {
            client.postgrest.from("posts").select {
                filter("id", "eq.$postId")
            }.data.let {
                @Suppress("UNCHECKED_CAST")
                (it as? List<Map<String, Any>>)?.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
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
            filter("message_id", "eq.$messageId")
            filter("user_id", "eq.$userId")
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
            filter("message_id", "eq.$messageId")
        }.collect {
            @Suppress("UNCHECKED_CAST")
            onNewReaction(it.record as Map<String, Any>)
        }
        channel.subscribe()
    }

    suspend fun getMessages(
        chatId: String,
        from: Long,
        to: Long
    ): List<Map<String, Any>>? {
        val client = getClient()
        return try {
            client.postgrest.from("messages").select {
                filter("chat_id", "eq.$chatId")
                order("created_at", Order.DESC)
                range(from, to)
            }.data.let {
                @Suppress("UNCHECKED_CAST")
                it as? List<Map<String, Any>>
            }
        } catch (e: Exception) {
            null
        }
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
            filter("user_id", "eq.$userId")
        }.collect {
            @Suppress("UNCHECKED_CAST")
            onNewPost(it.record as Map<String, Any>)
        }
        channel.subscribe()
    }

    suspend fun signOut() {
        val client = getClient()
        client.realtime.disconnect()
    }

    suspend fun getUserByUsername(username: String): Map<String, Any>? {
        val client = getClient()
        return try {
            client.postgrest.from("users").select {
                filter("username", "eq.$username")
            }.data.let {
                @Suppress("UNCHECKED_CAST")
                (it as? List<Map<String, Any>>)?.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun blockUser(blockerId: String, blockedId: String) {
        val client = getClient()
        client.postgrest.from("blocklist").insert(mapOf("blocker_id" to blockerId, "blocked_id" to blockedId))
    }

    suspend fun createGroup(group: Map<String, Any>) {
        val client = getClient()
        client.postgrest.from("groups").insert(group)
    }

    suspend fun getUsers(): List<Map<String, Any>>? {
        val client = getClient()
        return try {
            client.postgrest.from("users").select().data.let {
                @Suppress("UNCHECKED_CAST")
                it as? List<Map<String, Any>>
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getLike(postId: String, userId: String): Map<String, Any>? {
        val client = getClient()
        return try {
            client.postgrest.from("post-likes").select {
                filter("post_id", "eq.$postId")
                filter("user_id", "eq.$userId")
            }.data.let {
                @Suppress("UNCHECKED_CAST")
                (it as? List<Map<String, Any>>)?.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getLikeCount(postId: String): Long {
        val client = getClient()
        return try {
            client.postgrest.from("post-likes").select(params = mapOf("post_id" to postId)) {
                count(Count.EXACT)
            }.countOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun getCommentCount(postId: String): Long {
        val client = getClient()
        return try {
            client.postgrest.from("post-comments").select(params = mapOf("post_id" to postId)) {
                count(Count.EXACT)
            }.countOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun getFavorite(postId: String, userId: String): Map<String, Any>? {
        val client = getClient()
        return try {
            client.postgrest.from("favorite-posts").select {
                filter("post_id", "eq.$postId")
                filter("user_id", "eq.$userId")
            }.data.let {
                @Suppress("UNCHECKED_CAST")
                (it as? List<Map<String, Any>>)?.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addLike(postId: String, userId: String) {
        val client = getClient()
        client.postgrest.from("post-likes").insert(mapOf("post_id" to postId, "user_id" to userId))
    }

    suspend fun removeLike(postId: String, userId: String) {
        val client = getClient()
        client.postgrest.from("post-likes").delete {
            filter("post_id", "eq.$postId")
            filter("user_id", "eq.$userId")
        }
    }

    suspend fun addFavorite(postId: String, userId: String) {
        val client = getClient()
        client.postgrest.from("favorite-posts").insert(mapOf("post_id" to postId, "user_id" to userId))
    }

    suspend fun removeFavorite(postId: String, userId: String) {
        val client = getClient()
        client.postgrest.from("favorite-posts").delete {
            filter("post_id", "eq.$postId")
            filter("user_id", "eq.$userId")
        }
    }
}