package com.synapse.social.studioasinc.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val uid: String,
    val username: String,
    val display_name: String,
    val email: String,
    val bio: String,
    val profile_image_url: String? = null,
    val followers_count: Int = 0,
    val following_count: Int = 0,
    val posts_count: Int = 0
)
