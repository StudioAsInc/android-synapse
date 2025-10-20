package com.synapse.social.studioasinc.model

data class UserStatus(
    val isOnline: Boolean = false,
    val lastSeen: Long = 0,
    val avatarUrl: String? = null,
    val nickname: String? = null
)
