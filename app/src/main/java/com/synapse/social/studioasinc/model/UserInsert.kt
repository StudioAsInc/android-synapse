package com.synapse.social.studioasinc.model

import kotlinx.serialization.Serializable

@Serializable
data class UserInsert(
    val uid: String,
    val username: String,
    val display_name: String,
    val email: String,
In    val bio: String? = null,
    val profile_image_url: String? = null,
    val followers_count: Int = 0,
    val following_count: Int = 0,
    val posts_count: Int = 0,
    val status: String = "offline",
    val account_type: String = "user",
    val verify: Boolean = false,
    val banned: Boolean = false
)