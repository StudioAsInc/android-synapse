package com.synapse.social.studioasinc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Chat(
    val id: String? = null,
    @SerialName("chat_id")
    val chatId: String,
    @SerialName("participant_1")
    val participantId1: String,
    @SerialName("participant_2")
    val participantId2: String,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)