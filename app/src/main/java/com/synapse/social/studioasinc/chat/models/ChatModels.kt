package com.synapse.social.studioasinc.chat.models

import com.synapse.social.studioasinc.chat.interfaces.*

/**
 * Chat Message Implementation
 */
data class ChatMessageImpl(
    override val id: String,
    override val chatId: String,
    override val senderId: String,
    override val receiverId: String? = null,
    override val messageText: String? = null,
    override val messageType: String,
    override val messageState: String,
    override val pushDate: Long,
    override val repliedMessageId: String? = null,
    override val attachments: List<ChatAttachment>? = null,
    override val isEdited: Boolean = false,
    override val editedAt: Long? = null
) : ChatMessage

/**
 * Chat Attachment Implementation
 */
data class ChatAttachmentImpl(
    override val id: String,
    override val url: String,
    override val type: String,
    override val fileName: String? = null,
    override val fileSize: Long? = null,
    override val thumbnailUrl: String? = null
) : ChatAttachment

/**
 * Chat Room Implementation
 */
data class ChatRoomImpl(
    override val id: String,
    override val participants: List<String>,
    override val isGroup: Boolean = false,
    override val groupName: String? = null,
    override val groupAvatar: String? = null,
    override val createdAt: Long,
    override val lastMessageId: String? = null,
    override val lastMessageText: String? = null,
    override val lastMessageTime: Long? = null,
    override val lastMessageSenderId: String? = null,
    override val unreadCount: Int = 0
) : ChatRoom

/**
 * Message States
 */
object MessageState {
    const val SENDING = "sending"
    const val SENT = "sent"
    const val DELIVERED = "delivered"
    const val READ = "read"
    const val FAILED = "failed"
}

/**
 * Message Types
 */
object MessageType {
    const val TEXT = "MESSAGE"
    const val ATTACHMENT = "ATTACHMENT_MESSAGE"
    const val VOICE = "VOICE_MESSAGE"
    const val SYSTEM = "SYSTEM_MESSAGE"
}

/**
 * Attachment Types
 */
object AttachmentType {
    const val IMAGE = "image"
    const val VIDEO = "video"
    const val AUDIO = "audio"
    const val DOCUMENT = "document"
}

/**
 * Typing Status
 */
data class TypingStatus(
    val userId: String,
    val chatId: String,
    val isTyping: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * User Status
 */
data class UserStatus(
    val userId: String,
    val isOnline: Boolean,
    val lastSeen: Long,
    val status: String? = null
)