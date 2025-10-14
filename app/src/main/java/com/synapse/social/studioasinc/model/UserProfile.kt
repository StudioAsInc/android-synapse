
package com.synapse.social.studioasinc.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val nickname: String? = null,
    val biography: String? = null,
    val avatar_url: String? = null,
    val email: String,
    val account_premium: String? = "false",
    val user_level_xp: String? = "500",
    val verify: String? = "false",
    val account_type: String? = "user",
    val gender: String? = "hidden"
)
