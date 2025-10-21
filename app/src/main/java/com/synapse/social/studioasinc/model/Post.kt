package com.synapse.social.studioasinc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Post model for Supabase
 */
@Serializable
data class Post(
    val id: String? = null,
    @SerialName("post_id")
    val postId: String,
    @SerialName("author_id")
    val authorId: String,
    val content: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("video_url")
    val videoUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("deleted_at")
    val deletedAt: String? = null
)

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