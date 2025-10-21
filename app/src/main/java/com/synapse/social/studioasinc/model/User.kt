package com.synapse.social.studioasinc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User model for Supabase
 */
@Serializable
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
    val joinDate: String? = null,
    @SerialName("one_signal_player_id")
    val oneSignalPlayerId: String? = null,
    @SerialName("last_seen")
    val lastSeen: String? = null,
    @SerialName("chatting_with")
    val chattingWith: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)