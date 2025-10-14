
package com.synapse.social.studioasinc.model

import kotlinx.serialization.Serializable

@Serializable
data class InboxUpdate(
    val chat_id: String,
    val uid: String,
    val last_message_uid: String,
    val last_message_text: String,
    val last_message_state: String,
    val push_date: String,
    val chat_type: String,
    val isGroup: String
)
