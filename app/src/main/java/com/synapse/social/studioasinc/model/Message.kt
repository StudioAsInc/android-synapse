package com.synapse.social.studioasinc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String? = null,
    @SerialName("message_key")
    val messageKey: String,
    @SerialName("chat_id")
    val chatId: String,
    @SerialName("sender_id")
    val senderId: String,
    @SerialName("message_text")
    val messageText: String? = null,
    @SerialName("message_type")
    val messageType: String = "text", // text, image, voice, file, ai
    @SerialName("attachment_url")
    val attachmentUrl: String? = null,
    @SerialName("attachment_name")
    val attachmentName: String? = null,
    @SerialName("voice_duration")
    val voiceDuration: Int? = null,
    @SerialName("reply_to_message_id")
    val replyToMessageId: String? = null,
    @SerialName("push_date")
    val pushDate: String? = null,
    @SerialName("edited_at")
    val editedAt: String? = null,
    @SerialName("deleted_at")
    val deletedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)