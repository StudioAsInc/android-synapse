package com.synapse.social.studioasinc.models

import kotlinx.serialization.Serializable

/**
 * Data models for Supabase database tables
 */

@Serializable
data class User(
    val id: String? = null,
    val uid: String? = null,
    val email: String? = null,
    val username: String? = null,
    val nickname: String? = null,
    val biography: String? = null,
    val avatar: String? = null,
    val avatar_history_type: String? = "local",
    val profile_cover_image: String? = null,
    val account_premium: Boolean = false,
    val user_level_xp: Int = 500,
    val verify: Boolean = false,
    val account_type: String = "user",
    val gender: String = "hidden",
    val banned: Boolean = false,
    val status: String = "offline",
    val join_date: String? = null,
    val one_signal_player_id: String? = null,
    val last_seen: String? = null,
    val chatting_with: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class Chat(
    val id: String? = null,
    val chat_id: String,
    val participant_1: String,
    val participant_2: String,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class Message(
    val id: String? = null,
    val message_key: String,
    val chat_id: String,
    val sender_id: String,
    val message_text: String? = null,
    val message_type: String = "text",
    val attachment_url: String? = null,
    val attachment_name: String? = null,
    val voice_duration: Int? = null,
    val reply_to_message_id: String? = null,
    val push_date: String? = null,
    val edited_at: String? = null,
    val deleted_at: String? = null,
    val created_at: String? = null
)

@Serializable
data class Story(
    val id: String? = null,
    val uid: String,
    val story_url: String,
    val story_type: String = "image",
    val created_at: String? = null,
    val expires_at: String? = null
)

@Serializable
data class Post(
    val id: String? = null,
    val post_id: String,
    val author_id: String,
    val uid: String, // For compatibility with existing code
    val content: String? = null,
    val postText: String? = null, // For compatibility
    val image_url: String? = null,
    val postImage: String? = null, // For compatibility
    val video_url: String? = null,
    val publishDate: String? = null, // For compatibility
    val postVisibility: String = "public", // For compatibility
    val postHideLikeCount: String = "false", // For compatibility
    val postDisableComments: String = "false", // For compatibility
    val postHideCommentsCount: String = "false", // For compatibility
    val post_visibility: String = "public", // Alternative naming
    val post_type: String = "text", // For compatibility
    val post_text: String? = null, // Alternative naming
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null
)