package com.synapse.social.studioasinc.model

data class Comment(
    val uid: String = "",
    val comment: String = "",
    val push_time: String = "",
    val key: String = "",
    val like: Long = 0,
    val replyCommentkey: String? = null
)
