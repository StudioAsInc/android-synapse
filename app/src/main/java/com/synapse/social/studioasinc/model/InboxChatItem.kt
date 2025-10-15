package com.synapse.social.studioasinc.model

import com.google.gson.annotations.SerializedName

data class InboxChatItem(
    val uid: String,
    @SerializedName("chat_type") val chatType: String,
    @SerializedName("last_message_text") val lastMessageText: String?,
    @SerializedName("last_message_uid") val lastMessageUid: String?,
    @SerializedName("last_message_state") val lastMessageState: String?,
    @SerializedName("push_date") val pushDate: Double,
    // Renamed 'user' to 'users' and 'group' to 'groups' to match the table names for automatic deserialization.
    val users: User? = null,
    val groups: Group? = null,
    var unreadCount: Long = 0
)

data class User(
    val uid: String,
    val avatar: String?,
    val banned: Boolean = false,
    val username: String?,
    val nickname: String?,
    val status: String?,
    val gender: String?,
    @SerializedName("account_type") val accountType: String?,
    @SerializedName("account_premium") val isPremium: Boolean = false,
    @SerializedName("verify") val isVerified: Boolean = false
)

data class Group(
    val id: String,
    val name: String?,
    val icon: String?
)

data class UnreadMessage(
    @SerializedName("chat_id") val chatId: String
)
