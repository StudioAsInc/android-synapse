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
    val authorUid: String = "",
    val postText: String? = null,
    var postImage: String? = null,
    var postType: String? = null,
    val postHideViewsCount: String? = null,
    val postHideLikeCount: String? = null,
    val postHideCommentsCount: String? = null,
    val postDisableComments: String? = null,
    val postVisibility: String? = null,
    val publishDate: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
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