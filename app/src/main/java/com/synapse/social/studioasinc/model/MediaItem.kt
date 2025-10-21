package com.synapse.social.studioasinc.model

import kotlinx.serialization.Serializable

/**
 * Represents a media item (image or video) for upload
 */
@Serializable
data class MediaItem(
    val id: String = "",
    val url: String,
    val type: MediaType,
    val thumbnailUrl: String? = null,
    val duration: Long? = null, // For videos
    val size: Long? = null,
    val mimeType: String? = null
)

/**
 * Types of media supported
 */
@Serializable
enum class MediaType {
    IMAGE,
    VIDEO
}