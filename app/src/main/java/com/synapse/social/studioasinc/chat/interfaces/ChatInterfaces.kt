package com.synapse.social.studioasinc.chat.interfaces

/**
 * Chat Adapter Listener Interface
 * Handles interactions with chat messages in the RecyclerView
 */
interface ChatAdapterListener {
    fun onMessageClick(messageId: String, position: Int)
    fun onMessageLongClick(messageId: String, position: Int): Boolean
    fun onReplyClick(messageId: String, messageText: String, senderName: String)
    fun onAttachmentClick(attachmentUrl: String, attachmentType: String)
    fun onUserProfileClick(userId: String)
}

/**
 * Chat Interaction Listener Interface
 * Handles chat-level interactions and events
 */
interface ChatInteractionListener {
    fun onTypingStart()
    fun onTypingStop()
    fun onMessageSent(messageId: String)
    fun onMessageDelivered(messageId: String)
    fun onMessageRead(messageId: String)
    fun onUserBlocked(userId: String)
    fun onUserUnblocked(userId: String)
    fun onChatDeleted(chatId: String)
}

/**
 * Voice Message Handler Interface
 * Handles voice message recording and playback
 */
interface VoiceMessageListener {
    fun onRecordingStart()
    fun onRecordingStop(audioFilePath: String, duration: Long)
    fun onRecordingCancel()
    fun onPlaybackStart(messageId: String)
    fun onPlaybackStop(messageId: String)
    fun onPlaybackComplete(messageId: String)
}

/**
 * Chat Message Interface
 * Represents a chat message data structure
 */
interface ChatMessage {
    val id: String
    val chatId: String
    val senderId: String
    val receiverId: String?
    val messageText: String?
    val messageType: String
    val messageState: String
    val pushDate: Long
    val repliedMessageId: String?
    val attachments: List<ChatAttachment>?
    val isEdited: Boolean
    val editedAt: Long?
}

/**
 * Chat Attachment Interface
 * Represents a file attachment in a chat message
 */
interface ChatAttachment {
    val id: String
    val url: String
    val type: String // image, video, audio, document
    val fileName: String?
    val fileSize: Long?
    val thumbnailUrl: String?
}

/**
 * Chat Room Interface
 * Represents a chat conversation
 */
interface ChatRoom {
    val id: String
    val participants: List<String>
    val isGroup: Boolean
    val groupName: String?
    val groupAvatar: String?
    val createdAt: Long
    val lastMessageId: String?
    val lastMessageText: String?
    val lastMessageTime: Long?
    val lastMessageSenderId: String?
    val unreadCount: Int
}