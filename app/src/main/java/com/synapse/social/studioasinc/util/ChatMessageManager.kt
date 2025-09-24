package com.synapse.social.studioasinc.util

import com.synapse.social.studioasinc.Supabase.client
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.HashMap
import java.util.UUID

object ChatMessageManager {

    private const val CHATS_REF = "chats"
    private const val GROUP_CHATS_REF = "group-chats"
    private const val INBOX_REF = "inbox"

    fun getChatId(uid1: String?, uid2: String?): String {
        if (uid1 == null || uid2 == null) {
            return ""
        }
        return if (uid1.compareTo(uid2) > 0) {
            uid1 + uid2
        } else {
            uid2 + uid1
        }
    }

    fun sendMessageToDb(
        messageMap: HashMap<String, Any>,
        senderUid: String,
        recipientUid: String,
        uniqueMessageKey: String,
        isGroup: Boolean
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isGroup) {
                    client.postgrest[GROUP_CHATS_REF].insert(messageMap)
                } else {
                    client.postgrest[CHATS_REF].insert(messageMap)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateInbox(lastMessage: String, recipientUid: String, isGroup: Boolean, groupName: String? = null) {
        val senderUid = client.auth.currentUserOrNull()?.id ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isGroup) {
                    val group = client.postgrest["groups"].select {
                        filter {
                            eq("id", recipientUid)
                        }
                    }.decodeList<Map<String, Any>>().firstOrNull()

                    if (group != null) {
                        val members = group["members"] as? List<String>
                        if (members != null) {
                            members.forEach { memberUid ->
                                val inboxUpdate = createInboxUpdate(
                                    chatId = recipientUid,
                                    conversationPartnerUid = recipientUid,
                                    lastMessage = lastMessage,
                                    isGroup = true
                                )
                                client.postgrest[INBOX_REF].insert(inboxUpdate, upsert = true)
                            }
                        }
                    }
                } else {
                    // Update inbox for the current user
                    val senderInboxUpdate = createInboxUpdate(
                        chatId = getChatId(senderUid, recipientUid),
                        conversationPartnerUid = recipientUid,
                        lastMessage = lastMessage,
                        isGroup = false
                    )
                    client.postgrest[INBOX_REF].insert(senderInboxUpdate, upsert = true)

                    // Update inbox for the other user
                    val recipientInboxUpdate = createInboxUpdate(
                        chatId = getChatId(senderUid, recipientUid),
                        conversationPartnerUid = senderUid,
                        lastMessage = lastMessage,
                        isGroup = false
                    )
                    client.postgrest[INBOX_REF].insert(recipientInboxUpdate, upsert = true)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun createInboxUpdate(
        chatId: String,
        conversationPartnerUid: String,
        lastMessage: String,
        isGroup: Boolean
    ): HashMap<String, Any> {
        val senderUid = client.auth.currentUserOrNull()?.id ?: ""
        return hashMapOf(
            "id" to UUID.randomUUID().toString(),
            "chatID" to chatId,
            "uid" to conversationPartnerUid,
            "last_message_uid" to senderUid,
            "last_message_text" to lastMessage,
            "last_message_state" to "sended",
            "push_date" to System.currentTimeMillis(),
            "chat_type" to if (isGroup) "group" else "single",
            "isGroup" to isGroup
        )
    }
}
