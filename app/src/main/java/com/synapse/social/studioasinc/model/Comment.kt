package com.synapse.social.studioasinc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val uid: String = "",
    val text: String = "",  // Changed from 'comment' to match Supabase schema
    val timestamp: String = "",  // Changed from 'push_time' to match Supabase schema
    @SerialName("post_key")
    val postKey: String = "",  // Changed from 'key' to match Supabase schema
    @SerialName("reply_comment_key")
    val replyCommentKey: String? = null  // Fixed naming convention
)
