package com.synapse.social.studioasinc.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatMessage(
    @SerializedName("key")
    var key: String = "",
    @SerializedName("uid")
    val uid: String = "",
    @SerializedName("message_text")
    val messageText: String? = null,
    @SerializedName("push_date")
    val pushDate: Long = 0,
    @SerializedName("message_state")
    val messageState: String? = null,
    @SerializedName("TYPE")
    val type: String = "MESSAGE",
    @SerializedName("attachments")
    val attachments: List<Attachment>? = null,
    @SerializedName("replied_message_id")
    val repliedMessageId: String? = null,
    var repliedMessage: ChatMessage? = null,
    var isLoadingMore: Boolean = false,
    var isTyping: Boolean = false
) : Parcelable

@Parcelize
data class Attachment(
    @SerializedName("publicId")
    val publicId: String = "",
    @SerializedName("url")
    val url: String = "",
    @SerializedName("width")
    val width: Int = 0,
    @SerializedName("height")
    val height: Int = 0,
    @SerializedName("resource_type")
    val resourceType: String = ""
) : Parcelable
