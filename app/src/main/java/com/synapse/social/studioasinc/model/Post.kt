package com.synapse.social.studioasinc.model

import kotlinx.serialization.Serializable

/**
 * Unified Post model for Supabase with Firebase compatibility
 */
@Serializable
data class Post(
    val id: String? = null,
    val post_id: String = "",
    val author_id: String = "",
    val uid: String = "", // For compatibility with existing code
    val content: String? = null,
    val postText: String? = null, // For compatibility
    val post_text: String? = null, // Alternative naming
    val image_url: String? = null,
    val postImage: String? = null, // For compatibility
    val post_image: String? = null, // Alternative naming
    val video_url: String? = null,
    val publishDate: String? = null, // For compatibility
    val publish_date: String? = null, // Alternative naming
    val postVisibility: String = "public", // For compatibility
    val post_visibility: String = "public", // Alternative naming
    val postHideLikeCount: String = "false", // For compatibility
    val post_hide_like_count: String = "false", // Alternative naming
    val postDisableComments: String = "false", // For compatibility
    val post_disable_comments: String = "false", // Alternative naming
    val postHideCommentsCount: String = "false", // For compatibility
    val post_hide_comments_count: String = "false", // Alternative naming
    val post_hide_views_count: String = "false", // Additional field
    val post_region: String = "none", // Additional field
    val post_disable_favorite: String = "false", // Additional field
    val post_type: String = "text", // For compatibility
    val postType: String = "text", // For compatibility
    val key: String? = null, // For Firebase compatibility
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null,
    // Media items for new media system
    val mediaItems: List<MediaItem>? = null
) {
    // Helper function to convert legacy image to MediaItem
    fun convertLegacyImage(): List<MediaItem> {
        val imageUrl = post_image ?: postImage ?: image_url
        return if (imageUrl != null) {
            listOf(MediaItem(url = imageUrl, type = MediaType.IMAGE))
        } else {
            emptyList()
        }
    }
}

/**
 * Extension function to convert HashMap to Post object
 */
fun HashMap<String, Any>.toPost(): Post {
    return Post(
        id = this["id"] as? String,
        post_id = this["post_id"] as? String ?: this["postId"] as? String ?: "",
        author_id = this["author_id"] as? String ?: this["authorId"] as? String ?: "",
        uid = this["uid"] as? String ?: "",
        content = this["content"] as? String ?: this["postText"] as? String ?: this["post_text"] as? String,
        postText = this["postText"] as? String ?: this["content"] as? String ?: this["post_text"] as? String,
        post_text = this["post_text"] as? String ?: this["postText"] as? String ?: this["content"] as? String,
        image_url = this["image_url"] as? String ?: this["postImage"] as? String ?: this["post_image"] as? String,
        postImage = this["postImage"] as? String ?: this["image_url"] as? String ?: this["post_image"] as? String,
        post_image = this["post_image"] as? String ?: this["postImage"] as? String ?: this["image_url"] as? String,
        video_url = this["video_url"] as? String,
        publishDate = this["publishDate"] as? String ?: this["publish_date"] as? String,
        publish_date = this["publish_date"] as? String ?: this["publishDate"] as? String,
        postVisibility = this["postVisibility"] as? String ?: this["post_visibility"] as? String ?: "public",
        post_visibility = this["post_visibility"] as? String ?: this["postVisibility"] as? String ?: "public",
        postHideLikeCount = this["postHideLikeCount"] as? String ?: this["post_hide_like_count"] as? String ?: "false",
        post_hide_like_count = this["post_hide_like_count"] as? String ?: this["postHideLikeCount"] as? String ?: "false",
        postDisableComments = this["postDisableComments"] as? String ?: this["post_disable_comments"] as? String ?: "false",
        post_disable_comments = this["post_disable_comments"] as? String ?: this["postDisableComments"] as? String ?: "false",
        postHideCommentsCount = this["postHideCommentsCount"] as? String ?: this["post_hide_comments_count"] as? String ?: "false",
        post_hide_comments_count = this["post_hide_comments_count"] as? String ?: this["postHideCommentsCount"] as? String ?: "false",
        post_hide_views_count = this["post_hide_views_count"] as? String ?: "false",
        post_region = this["post_region"] as? String ?: "none",
        post_disable_favorite = this["post_disable_favorite"] as? String ?: "false",
        post_type = this["post_type"] as? String ?: this["postType"] as? String ?: "text",
        postType = this["postType"] as? String ?: this["post_type"] as? String ?: "text",
        key = this["key"] as? String,
        created_at = this["created_at"] as? String,
        updated_at = this["updated_at"] as? String,
        deleted_at = this["deleted_at"] as? String
    )
}