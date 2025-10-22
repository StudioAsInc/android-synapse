package com.synapse.social.studioasinc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Message model for Supabase
 */
@Serializable
data class Message(
    val id: String = "",
    @SerialName("chat_id")
    val chatId: String = "",
    @SerialName("sender_id")
    val senderId: String = "",
    val content: String = "",
    @SerialName("message_type")
    val messageType: String = "text", // text, image, video, audio, file
    @SerialName("media_url")
    val mediaUrl: String? = null,
    @SerialName("created_at")
    val createdAt: Long = 0L,
    @SerialName("updated_at")
    val updatedAt: Long = 0L,
    @SerialName("is_deleted")
    val isDeleted: Boolean = false,
    @SerialName("is_edited")
    val isEdited: Boolean = false,
    @SerialName("reply_to_id")
    val replyToId: String? = null,
    
    // UI-related properties
    var senderName: String? = null,
    var senderAvatarUrl: String? = null,
    var isFromCurrentUser: Boolean = false,
    var deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.SENT,
    var timestamp: String? = null
) {
    /**
     * Checks if this is a media message
     */
    fun isMediaMessage(): Boolean {
        return messageType in listOf("image", "video", "audio", "file")
    }
    
    /**
     * Gets the display content for the message
     */
    fun getDisplayContent(): String {
        return when {
            isDeleted -> "This message was deleted"
            messageType == "image" -> "📷 Image"
            messageType == "video" -> "🎥 Video"
            messageType == "audio" -> "🎵 Audio"
            messageType == "file" -> "📎 File"
            else -> content
        }
    }
}

/**
 * Message delivery status
 */
enum class MessageDeliveryStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

/**
 * Extension function to convert HashMap to Message object
 */
fun HashMap<String, Any>.toMessage(): Message {
    return Message(
        id = this["id"] as? String ?: "",
        chatId = this["chat_id"] as? String ?: "",
        senderId = this["sender_id"] as? String ?: "",
        content = this["content"] as? String ?: "",
        messageType = this["message_type"] as? String ?: "text",
        mediaUrl = this["media_url"] as? String,
        createdAt = (this["created_at"] as? String)?.toLongOrNull() ?: 0L,
        updatedAt = (this["updated_at"] as? String)?.toLongOrNull() ?: 0L,
        isDeleted = (this["is_deleted"] as? String) == "true",
        isEdited = (this["is_edited"] as? String) == "true",
        replyToId = this["reply_to_id"] as? String
    )
}

/**
 * Extension function to convert Message to HashMap for database operations
 */
fun Message.toHashMap(): HashMap<String, Any?> {
    return hashMapOf(
        "id" to id,
        "chat_id" to chatId,
        "sender_id" to senderId,
        "content" to content,
        "message_type" to messageType,
        "media_url" to mediaUrl,
        "created_at" to createdAt.toString(),
        "updated_at" to updatedAt.toString(),
        "is_deleted" to isDeleted.toString(),
        "is_edited" to isEdited.toString(),
        "reply_to_id" to replyToId
    )
}