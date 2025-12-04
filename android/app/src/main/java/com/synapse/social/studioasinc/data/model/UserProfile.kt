package com.synapse.social.studioasinc.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val uid: String,
    val username: String?,
    val email: String?,
    @SerialName("display_name")
    val displayName: String?,
    @SerialName("full_name")
    val fullName: String?,
    @SerialName("avatar_url")
    val avatarUrl: String?,
    @SerialName("cover_url")
    val coverUrl: String?,
    val bio: String?,
    val website: String?,
    val location: String?,
    @SerialName("join_date")
    val joinDate: String?,
    @SerialName("created_at")
    val createdAt: String?,
    @SerialName("followers_count")
    val followersCount: Int = 0,
    @SerialName("following_count")
    val followingCount: Int = 0,
    @SerialName("posts_count")
    val postsCount: Int = 0,
    @SerialName("is_verified")
    val isVerified: Boolean = false,
    val status: String? = null
)
