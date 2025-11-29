package com.synapse.social.studioasinc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val uid: String = "",
    val comment: String = "",  // Comment text
    @SerialName("push_time")
    val push_time: String = "",  // Timestamp
    val key: String = "",  // Comment ID
    val like: Long? = null,  // Like count (optional - may not exist in schema)
    @SerialName("post_key")
    val postKey: String = "",  // Post this comment belongs to
    @SerialName("reply_comment_key")
    val replyCommentKey: String? = null  // If this is a reply, the parent comment key
)
