package com.synapse.social.studioasinc.chat.model

data class ChatMessage(
    val uid: String,
    val message: String,
    val timestamp: Long,
    val user: ChatUser? = null
)
