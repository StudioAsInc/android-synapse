package com.synapse.social.studioasinc.model

/**
 * Story model for social media stories
 */
data class Story(
    val id: String? = null,
    val uid: String = "",
    val story_id: String = "",
    val content_url: String? = null,
    val story_type: String = "image", // image, video
    val publish_date: String? = null,
    val expires_at: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)