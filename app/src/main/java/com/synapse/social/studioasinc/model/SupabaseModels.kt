package com.synapse.social.studioasinc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data models for the application.
 * These models correspond to the database tables and are used for serialization.
 */

// @Serializable - Temporarily disabled for build fix
data class User(
    val id: String? = null,
    val uid: String,
    val email: String,
    val username: String,
    val nickname: String? = null,
    val biography: String? = null,
    val avatar: String? = null,
    @SerialName("avatar_history_type")
    val avatarHistoryType: String = "local",
    @SerialName("profile_cover_image")
    val profileCoverImage: String? = null,
    @SerialName("account_premium")
    val accountPremium: Boolean = false,
    @SerialName("user_level_xp")
    val userLevelXp: Int = 500,
    val verify: Boolean = false,
    @SerialName("account_type")
    val accountType: String = "user",
    val gender: String = "hidden",
    val banned: Boolean = false,
    val status: String = "offline",
    @SerialName("join_date")
    val joinDate: String,
    @SerialName("one_signal_player_id")
    val oneSignalPlayerId: String? = null,
    @SerialName("last_seen")
    val lastSeen: String? = null,
    @SerialName("chatting_with")
    val chattingWith: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

// @Serializable - Temporarily disabled for build fix
data class Chat(
    val id: String? = null,
    @SerialName("chat_id")
    val chatId: String,
    @SerialName("participant_1")
    val participant1: String,
    @SerialName("participant_2")
    val participant2: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

// @Serializable - Temporarily disabled for build fix
data class Message(
    val id: String? = null,
    @SerialName("message_key")
    val messageKey: String,
    @SerialName("chat_id")
    val chatId: String,
    @SerialName("sender_id")
    val senderId: String,
    @SerialName("message_text")
    val messageText: String? = null,
    @SerialName("message_type")
    val messageType: String = "text",
    @SerialName("attachment_url")
    val attachmentUrl: String? = null,
    @SerialName("attachment_name")
    val attachmentName: String? = null,
    @SerialName("voice_duration")
    val voiceDuration: Int? = null,
    @SerialName("reply_to_message_id")
    val replyToMessageId: String? = null,
    @SerialName("push_date")
    val pushDate: String,
    @SerialName("edited_at")
    val editedAt: String? = null,
    @SerialName("deleted_at")
    val deletedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String
)

// @Serializable - Temporarily disabled for build fix
data class Group(
    val id: String? = null,
    @SerialName("group_id")
    val groupId: String,
    val name: String,
    val description: String? = null,
    val avatar: String? = null,
    @SerialName("created_by")
    val createdBy: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

// @Serializable - Temporarily disabled for build fix
data class GroupMember(
    val id: String? = null,
    @SerialName("group_id")
    val groupId: String,
    @SerialName("user_id")
    val userId: String,
    val role: String = "member",
    @SerialName("joined_at")
    val joinedAt: String
)

// @Serializable - Temporarily disabled for build fix
data class GroupMessage(
    val id: String? = null,
    @SerialName("message_key")
    val messageKey: String,
    @SerialName("group_id")
    val groupId: String,
    @SerialName("sender_id")
    val senderId: String,
    @SerialName("message_text")
    val messageText: String? = null,
    @SerialName("message_type")
    val messageType: String = "text",
    @SerialName("attachment_url")
    val attachmentUrl: String? = null,
    @SerialName("attachment_name")
    val attachmentName: String? = null,
    @SerialName("voice_duration")
    val voiceDuration: Int? = null,
    @SerialName("reply_to_message_id")
    val replyToMessageId: String? = null,
    @SerialName("push_date")
    val pushDate: String,
    @SerialName("edited_at")
    val editedAt: String? = null,
    @SerialName("deleted_at")
    val deletedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String
)

// @Serializable - Temporarily disabled for build fix
data class Inbox(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("chat_partner_id")
    val chatPartnerId: String? = null,
    @SerialName("group_id")
    val groupId: String? = null,
    @SerialName("last_message_id")
    val lastMessageId: String? = null,
    @SerialName("last_group_message_id")
    val lastGroupMessageId: String? = null,
    @SerialName("unread_count")
    val unreadCount: Int = 0,
    @SerialName("updated_at")
    val updatedAt: String
)

// @Serializable - Temporarily disabled for build fix
data class BlockList(
    val id: String? = null,
    @SerialName("blocker_id")
    val blockerId: String,
    @SerialName("blocked_id")
    val blockedId: String,
    @SerialName("created_at")
    val createdAt: String
)

// @Serializable - Temporarily disabled for build fix
data class Post(
    val id: String? = null,
    @SerialName("post_id")
    val postId: String,
    @SerialName("author_id")
    val authorId: String,
    val content: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("video_url")
    val videoUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("deleted_at")
    val deletedAt: String? = null
)

// @Serializable - Temporarily disabled for build fix
data class UsernameRegistry(
    val id: String? = null,
    val username: String,
    @SerialName("user_id")
    val userId: String,
    val uid: String,
    val email: String,
    @SerialName("created_at")
    val createdAt: String
)

// @Serializable - Temporarily disabled for build fix
data class TypingStatus(
    val id: String? = null,
    @SerialName("chat_id")
    val chatId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("is_typing")
    val isTyping: Boolean,
    @SerialName("updated_at")
    val updatedAt: String
)