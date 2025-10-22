package com.synapse.social.studioasinc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Chat model for Supabase
 */
@Serializable
data class Chat(
    val id: String = "",
    val name: String? = null,
    @SerialName("created_by")
    val createdBy: String = "",
    @SerialName("created_at")
    val createdAt: Long = 0L,
    @SerialName("updated_at")
    val updatedAt: Long = 0L,
    @SerialName("is_group")
    val isGroup: Boolean = false,
    @SerialName("participant_count")
    val participantCount: Int = 0,
    @SerialName("last_message_id")
    val lastMessageId: String? = null,
    @SerialName("last_message_at")
    val lastMessageAt: Long? = null,
    
    // UI-related properties
    var chatDisplayName: String? = null,
    var avatarUrl: String? = null,
    var lastMessage: String? = null,
    var unreadCount: Int = 0,
    var isOnline: Boolean = false
) {
    /**
     * Gets the display name for the chat
     */
    fun getDisplayName(): String {
        return chatDisplayName ?: name ?: "Chat"
    }
}

/**
 * Extension function to convert HashMap to Chat object
 */
fun HashMap<String, Any>.toChat(): Chat {
    return Chat(
        id = this["id"] as? String ?: "",
        name = this["name"] as? String,
        createdBy = this["created_by"] as? String ?: "",
        createdAt = (this["created_at"] as? String)?.toLongOrNull() ?: 0L,
        updatedAt = (this["updated_at"] as? String)?.toLongOrNull() ?: 0L,
        isGroup = (this["is_group"] as? String) == "true",
        participantCount = (this["participant_count"] as? String)?.toIntOrNull() ?: 0,
        lastMessageId = this["last_message_id"] as? String,
        lastMessageAt = (this["last_message_at"] as? String)?.toLongOrNull()
    )
}

/**
 * Extension function to convert Chat to HashMap for database operations
 */
fun Chat.toHashMap(): HashMap<String, Any?> {
    return hashMapOf(
        "id" to id,
        "name" to name,
        "created_by" to createdBy,
        "created_at" to createdAt.toString(),
        "updated_at" to updatedAt.toString(),
        "is_group" to isGroup.toString(),
        "participant_count" to participantCount.toString(),
        "last_message_id" to lastMessageId,
        "last_message_at" to lastMessageAt?.toString()
    )
}