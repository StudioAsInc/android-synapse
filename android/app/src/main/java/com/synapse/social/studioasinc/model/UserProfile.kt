package com.synapse.social.studioasinc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val uid: String,
    val username: String,
    @SerialName("display_name") val displayName: String? = null,
    val email: String? = null,
    val bio: String? = null,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    @SerialName("profile_cover_image") val profileCoverImage: String? = null,
    @SerialName("followers_count") val followersCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0,
    @SerialName("posts_count") val postsCount: Int = 0,
    val status: String = "offline",
    @SerialName("account_type") val account_type: String = "user",
    val verify: Boolean = false,
    val banned: Boolean = false
) {
    val isVerified: Boolean get() = verify
    val isPremium: Boolean get() = account_type == "premium"
}
