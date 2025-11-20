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
    @SerialName("media_items")
    var mediaItems: MutableList<MediaItem>? = null,
    // New reaction fields
    @kotlinx.serialization.Transient
    var reactions: Map<ReactionType, Int>? = null,
    @kotlinx.serialization.Transient
    var userReaction: ReactionType? = null,
    // Joined profile data
    @kotlinx.serialization.Transient
    var username: String? = null,
    @kotlinx.serialization.Transient
    var avatarUrl: String? = null,
    @kotlinx.serialization.Transient
    var isVerified: Boolean = false
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

    /**
     * Get total number of reactions across all types
     */
    fun getTotalReactionsCount(): Int {
        return reactions?.values?.sum() ?: likesCount
    }

    /**
     * Get top 3 reaction types by count
     */
    fun getTopReactions(): List<Pair<ReactionType, Int>> {
        return reactions?.entries
            ?.sortedByDescending { it.value }
            ?.take(3)
            ?.map { it.key to it.value }
            ?: emptyList()
    }

    /**
     * Check if current user has reacted
     */
    fun hasUserReacted(): Boolean {
        return userReaction != null
    }

    /**
     * Get reaction summary text (e.g., "❤️ 😂 and 12 others")
     */
    fun getReactionSummary(): String {
        val topReactions = getTopReactions()
        if (topReactions.isEmpty()) return ""

        val emojis = topReactions.take(2).joinToString(" ") { it.first.emoji }
        val total = getTotalReactionsCount()

        return when {
            total == 0 -> ""
            total == 1 -> "$emojis 1 person"
            topReactions.size == 1 -> "$emojis $total"
            else -> "$emojis $total"
        }
    }

    /**
     * Check if post has multiple media items
     */
    fun hasMultipleMedia(): Boolean {
        return (mediaItems?.size ?: 0) > 1
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