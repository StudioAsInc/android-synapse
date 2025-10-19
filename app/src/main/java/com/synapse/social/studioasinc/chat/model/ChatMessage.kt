package com.synapse.social.studioasinc.chat.model

import com.synapse.social.studioasinc.model.Attachment

data class ChatMessage(
    val uid: String = "",
    val messageText: String = "",
    val timestamp: Long = 0,
    val user: ChatUser? = null,
    val key: String = "",
    val type: String = "MESSAGE",
    val attachments: List<Attachment>? = null,
    val messageState: String = "sent",
    val repliedMessageId: String? = null,
    val pushDate: Long = 0,
    val audioUrl: String? = null,
    val audioDuration: Long = 0
)
