package com.synapse.social.studioasinc.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val nickname: String? = null,
    val biography: String? = null,
    val avatar_url: String? = null
)
