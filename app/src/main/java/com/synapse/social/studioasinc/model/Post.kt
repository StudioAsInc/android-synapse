package com.synapse.social.studioasinc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Post model for Supabase
 */
@Serializable
data class Post(
    val id: String = "",
    @SerialName("post_id")
    val postId: String = "",
    @SerialName("author_id")
    val authorId: String = "",
    val authorUid: String = "", // For compatibility
    val content: String = "",
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("video_url")
    val videoUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("deleted_at")
    val deletedAt: String? = null,
    // Additional properties for compatibility
    val timestamp: Long = 0L,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    var key: String? = null,
    val uid: String? = null,
    var postType: String? = null,
    var postText: String? = null,
    var postImage: String? = null,
    var mediaItems: MutableList<MediaItem>? = null,
    // Post settings
    val postHideViewsCount: String? = null,
    val postHideLikeCount: String? = null,
    val postHideCommentsCount: String? = null,
    val postDisableComments: String? = null,
    val postVisibility: String? = null,
    val publishDate: String? = null
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
        id = this["id"] as? String,
        postId = this["post_id"] as? String ?: this["postId"] as? String ?: "",
        authorId = this["author_id"] as? String ?: this["authorId"] as? String ?: this["uid"] as? String ?: "",
        content = this["content"] as? String ?: this["postText"] as? String ?: this["post_text"] as? String ?: "",
        imageUrl = this["image_url"] as? String ?: this["postImage"] as? String ?: this["post_image"] as? String,
        videoUrl = this["video_url"] as? String,
        createdAt = this["created_at"] as? String ?: this["publishDate"] as? String ?: this["publish_date"] as? String,
        updatedAt = this["updated_at"] as? String,
        deletedAt = this["deleted_at"] as? String
    )
}
/**

 * Extension function to convert Post to HashMap for Firebase compatibility
 */
fun Post.toHashMap(): HashMap<String, Any?> {
    return hashMapOf(
        "id" to id,
        "post_id" to postId,
        "author_id" to authorId,
        "authorUid" to authorUid,
        "content" to content,
        "image_url" to imageUrl,
        "video_url" to videoUrl,
        "created_at" to createdAt,
        "updated_at" to updatedAt,
        "deleted_at" to deletedAt,
        "timestamp" to timestamp,
        "likesCount" to likesCount,
        "commentsCount" to commentsCount,
        "key" to key,
        "uid" to uid,
        "postType" to postType,
        "postText" to postText,
        "postImage" to postImage,
        "mediaItems" to mediaItems,
        "postHideViewsCount" to postHideViewsCount,
        "postHideLikeCount" to postHideLikeCount,
        "postHideCommentsCount" to postHideCommentsCount,
        "postDisableComments" to postDisableComments,
        "postVisibility" to postVisibility,
        "publishDate" to publishDate
    )
}