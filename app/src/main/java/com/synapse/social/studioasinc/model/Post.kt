package com.synapse.social.studioasinc.model

import java.io.Serializable

data class Post(
    var key: String = "",
    var uid: String = "",
    var postText: String? = null,
    var postType: String = "TEXT", // TEXT, IMAGE, VIDEO, MIXED
    var mediaItems: MutableList<MediaItem> = mutableListOf(),
    var postHideViewsCount: String = "false",
    var postRegion: String = "none",
    var postHideLikeCount: String = "false",
    var postHideCommentsCount: String = "false",
    var postVisibility: String = "public",
    var postDisableFavorite: String = "false",
    var postDisableComments: String = "false",
    var publishDate: String = "",
    
    // Legacy single image field for backward compatibility
    var postImage: String? = null
) : Serializable {
    
    // Helper methods
    fun addMediaItem(item: MediaItem) {
        mediaItems.add(item)
    }
    
    fun hasMedia(): Boolean {
        return mediaItems.isNotEmpty() || !postImage.isNullOrEmpty()
    }
    
    fun getMediaCount(): Int {
        return when {
            mediaItems.isNotEmpty() -> mediaItems.size
            !postImage.isNullOrEmpty() -> 1
            else -> 0
        }
    }
    
    // Convert legacy single image to media items
    fun convertLegacyImage() {
        postImage?.let { url ->
            if (url.isNotEmpty() && mediaItems.isEmpty()) {
                mediaItems.add(MediaItem(url = url, type = MediaType.IMAGE))
            }
        }
    }
    
    // Determine post type based on content
    fun determinePostType() {
        postType = when {
            mediaItems.isEmpty() && postImage.isNullOrEmpty() -> "TEXT"
            mediaItems.all { it.type == MediaType.IMAGE } -> "IMAGE"
            mediaItems.all { it.type == MediaType.VIDEO } -> "VIDEO"
            mediaItems.any { it.type == MediaType.IMAGE } && mediaItems.any { it.type == MediaType.VIDEO } -> "MIXED"
            else -> "TEXT"
        }
    }
}

data class MediaItem(
    var url: String = "",
    var type: MediaType = MediaType.IMAGE,
    var thumbnailUrl: String? = null, // For videos
    var width: Int = 0,
    var height: Int = 0,
    var duration: Long = 0L // For videos in milliseconds
) : Serializable

enum class MediaType {
    IMAGE,
    VIDEO
}

// Extension function to convert HashMap to Post object
fun HashMap<String, Any>.toPost(): Post {
    val post = Post()
    
    // Map basic fields
    post.key = this["key"] as? String ?: ""
    post.uid = this["uid"] as? String ?: ""
    post.postText = this["post_text"] as? String
    post.postType = this["post_type"] as? String ?: "TEXT"
    post.postHideViewsCount = this["post_hide_views_count"] as? String ?: "false"
    post.postRegion = this["post_region"] as? String ?: "none"
    post.postHideLikeCount = this["post_hide_like_count"] as? String ?: "false"
    post.postHideCommentsCount = this["post_hide_comments_count"] as? String ?: "false"
    post.postVisibility = this["post_visibility"] as? String ?: "public"
    post.postDisableFavorite = this["post_disable_favorite"] as? String ?: "false"
    post.postDisableComments = this["post_disable_comments"] as? String ?: "false"
    post.publishDate = this["publish_date"] as? String ?: ""
    
    // Handle legacy image
    post.postImage = this["post_image"] as? String
    
    // Handle media items array
    val mediaItemsList = this["media_items"] as? List<*>
    if (mediaItemsList != null) {
        post.mediaItems = mediaItemsList.mapNotNull { item ->
            when (item) {
                is HashMap<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val mediaMap = item as HashMap<String, Any>
                    MediaItem(
                        url = mediaMap["url"] as? String ?: "",
                        type = MediaType.valueOf(mediaMap["type"] as? String ?: "IMAGE"),
                        thumbnailUrl = mediaMap["thumbnailUrl"] as? String,
                        width = (mediaMap["width"] as? Number)?.toInt() ?: 0,
                        height = (mediaMap["height"] as? Number)?.toInt() ?: 0,
                        duration = (mediaMap["duration"] as? Number)?.toLong() ?: 0L
                    )
                }
                else -> null
            }
        }.toMutableList()
    } else {
        // Convert legacy image if no media items
        post.convertLegacyImage()
    }
    
    return post
}

// Extension function to convert Post to HashMap for Firebase
fun Post.toHashMap(): HashMap<String, Any> {
    val map = HashMap<String, Any>()
    
    map["key"] = key
    map["uid"] = uid
    postText?.let { map["post_text"] = it }
    map["post_type"] = postType
    map["post_hide_views_count"] = postHideViewsCount
    map["post_region"] = postRegion
    map["post_hide_like_count"] = postHideLikeCount
    map["post_hide_comments_count"] = postHideCommentsCount
    map["post_visibility"] = postVisibility
    map["post_disable_favorite"] = postDisableFavorite
    map["post_disable_comments"] = postDisableComments
    map["publish_date"] = publishDate
    
    // Include legacy image for backward compatibility
    postImage?.let { map["post_image"] = it }
    
    // Convert media items to list of maps
    if (mediaItems.isNotEmpty()) {
        map["media_items"] = mediaItems.map { item ->
            hashMapOf<String, Any>(
                "url" to item.url,
                "type" to item.type.name,
                "width" to item.width,
                "height" to item.height
            ).apply {
                item.thumbnailUrl?.let { this["thumbnailUrl"] = it }
                if (item.duration > 0) {
                    this["duration"] = item.duration
                }
            }
        }
    }
    
    return map
}