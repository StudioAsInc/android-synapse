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
    val postType: String = "text", // For compatibility
    val post_text: String? = null, // Alternative naming
    val key: String? = null, // For Firebase compatibility
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null
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
        content = this["content"] as? String ?: this["postText"] as? String,
        postText = this["postText"] as? String ?: this["content"] as? String,
        image_url = this["image_url"] as? String ?: this["postImage"] as? String,
        postImage = this["postImage"] as? String ?: this["image_url"] as? String,
        video_url = this["video_url"] as? String,
        publishDate = this["publishDate"] as? String ?: this["publish_date"] as? String,
        postVisibility = this["postVisibility"] as? String ?: "public",
        postHideLikeCount = this["postHideLikeCount"] as? String ?: "false",
        postDisableComments = this["postDisableComments"] as? String ?: "false",
        postHideCommentsCount = this["postHideCommentsCount"] as? String ?: "false",
        post_visibility = this["post_visibility"] as? String ?: "public",
        post_type = this["post_type"] as? String ?: this["postType"] as? String ?: "text",
        postType = this["postType"] as? String ?: this["post_type"] as? String ?: "text",
        post_text = this["post_text"] as? String,
        key = this["key"] as? String,
        created_at = this["created_at"] as? String,
        updated_at = this["updated_at"] as? String,
        deleted_at = this["deleted_at"] as? String
    )
}