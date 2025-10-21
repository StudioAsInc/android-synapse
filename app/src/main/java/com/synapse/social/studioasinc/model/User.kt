package com.synapse.social.studioasinc.model

/**
 * Compatibility User model for existing code
 * This is a temporary bridge while migrating to Supabase
 */
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