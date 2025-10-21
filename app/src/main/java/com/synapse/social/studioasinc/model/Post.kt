package com.synapse.social.studioasinc.model

/**
 * Compatibility Post model for existing code
 * This is a temporary bridge while migrating to Supabase
 */
data class Post(
    val id: String? = null,
    val post_id: String = "",
    val author_id: String = "",
    val uid: String = "", // For compatibility with existing code
    val content: String? = null,
    val postText: String? = null, // For compatibility
    val image_url: String? = null,
    val postImage: String? = null, // For compatibility
    val video_url: String? = null,
    val publishDate: String? = null, // For compatibility
    val postVisibility: String = "public", // For compatibility
    val postHideLikeCount: String = "false", // For compatibility
    val postDisableComments: String = "false", // For compatibility
    val postHideCommentsCount: String = "false", // For compatibility
    val post_visibility: String = "public", // Alternative naming
    val post_type: String = "text", // For compatibility
    val post_text: String? = null, // Alternative naming
    val key: String? = null, // For Firebase compatibility
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null
)