package com.synapse.social.studioasinc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Post model for Supabase
 */
@Serializable
data class Post(
    val id: String = "",
    val key: String? = null,
    @SerialName("author_uid")
    val authorUid: String = "",
    @SerialName("post_text")
    val postText: String? = null,
    @SerialName("post_image")
    var postImage: String? = null,
    @SerialName("post_type")
    var postType: String? = null,
    @SerialName("post_hide_views_count")
    val postHideViewsCount: String? = null,
    @SerialName("post_hide_like_count")
    val postHideLikeCount: String? = null,
    @SerialName("post_hide_comments_count")
    val postHideCommentsCount: String? = null,
    @SerialName("post_disable_comments")
    val postDisableComments: String? = null,
    @SerialName("post_visibility")
    val postVisibility: String? = null,
    @SerialName("publish_date")
    val publishDate: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    @SerialName("likes_count")
    val likesCount: Int = 0,
    @SerialName("comments_count")
    val commentsCount: Int = 0,
    @SerialName("views_count")
    val viewsCount: Int = 0,
    @kotlinx.serialization.Transient
    var mediaItems: MutableList<MediaItem>? = null
) {
    /**
     * Determines post type based on media content
     */
    fun determinePostType() {
        postType = when {
            mediaItems?.any { it.type == MediaType.VIDEO } == true -> "VIDEO"
            mediaItems?.any { it.type == MediaType.IMAGE } == true -> "IMAGE"
            !postText.isNullOrEmpty() -> "TEXT"
            else -> "TEXT"
        }
    }
}

/**
 * Extension function to convert HashMap to Post object
 */
fun HashMap<String, Any>.toPost(): Post {
    return Post(
        id = this["id"] as? String ?: "",
        key = this["key"] as? String,
        authorUid = this["authorUid"] as? String ?: this["author_uid"] as? String ?: "",
        postText = this["postText"] as? String ?: this["post_text"] as? String,
        postImage = this["postImage"] as? String ?: this["post_image"] as? String,
        postType = this["postType"] as? String ?: this["post_type"] as? String,
        publishDate = this["publishDate"] as? String ?: this["publish_date"] as? String,
        timestamp = (this["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        likesCount = (this["likesCount"] as? Number)?.toInt() ?: 0,
        commentsCount = (this["commentsCount"] as? Number)?.toInt() ?: 0,
        viewsCount = (this["viewsCount"] as? Number)?.toInt() ?: 0,
        postHideViewsCount = this["postHideViewsCount"] as? String,
        postHideLikeCount = this["postHideLikeCount"] as? String,
        postHideCommentsCount = this["postHideCommentsCount"] as? String,
        postDisableComments = this["postDisableComments"] as? String,
        postVisibility = this["postVisibility"] as? String
    )
}