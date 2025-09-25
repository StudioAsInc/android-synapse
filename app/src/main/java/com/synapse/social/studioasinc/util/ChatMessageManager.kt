package com.synapse.social.studioasinc.util

import com.synapse.social.studioasinc.util.SupabaseManager
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.HashMap

object ChatMessageManager {

    private const val CHATS_TABLE = "chats"
    private const val USER_CHATS_TABLE = "user_chats"
    private const val GROUP_CHATS_TABLE = "group_chats"
    private const val INBOX_TABLE = "inbox"
    private const val GROUPS_TABLE = "groups"

    private const val CHAT_ID_KEY = "chatID"
    private const val UID_KEY = "uid"
    private const val LAST_MESSAGE_UID_KEY = "last_message_uid"
    private const val LAST_MESSAGE_TEXT_KEY = "last_message_text"
    private const val LAST_MESSAGE_STATE_KEY = "last_message_state"
    private const val PUSH_DATE_KEY = "push_date"

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
        GlobalScope.launch {
            if (isGroup) {
                SupabaseManager.getClient().postgrest.from(GROUP_CHATS_TABLE).insert(messageMap)
            } else {
                val chatId = getChatId(senderUid, recipientUid)
                SupabaseManager.getClient().postgrest.from(CHATS_TABLE).insert(messageMap)
                SupabaseManager.getClient().postgrest.from(USER_CHATS_TABLE).insert(mapOf("user_id" to senderUid, "chat_id" to chatId))
                SupabaseManager.getClient().postgrest.from(USER_CHATS_TABLE).insert(mapOf("user_id" to recipientUid, "chat_id" to chatId))
            }
        }
    }

    fun updateInbox(lastMessage: String, recipientUid: String, isGroup: Boolean, groupName: String? = null) {
        val senderUid = SupabaseManager.getCurrentUserID() ?: return

        GlobalScope.launch {
            if (isGroup) {
                val group = SupabaseManager.getGroup(recipientUid)
                if (group != null) {
                    @Suppress("UNCHECKED_CAST")
                    val members = group["members"] as? List<String>
                    if (members != null) {
                        for (memberUid in members) {
                            val inboxUpdate = createInboxUpdate(
                                chatId = recipientUid,
                                conversationPartnerUid = recipientUid,
                                lastMessage = lastMessage,
                                isGroup = true
                            )
                            SupabaseManager.getClient().postgrest.from(INBOX_TABLE).insert(inboxUpdate)
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
                SupabaseManager.getClient().postgrest.from(INBOX_TABLE).insert(senderInboxUpdate)

                // Update inbox for the other user
                val recipientInboxUpdate = createInboxUpdate(
                    chatId = getChatId(senderUid, recipientUid),
                    conversationPartnerUid = senderUid,
                    lastMessage = lastMessage,
                    isGroup = false
                )
                SupabaseManager.getClient().postgrest.from(INBOX_TABLE).insert(recipientInboxUpdate)
            }
        }
    }

    private fun createInboxUpdate(
        chatId: String,
        conversationPartnerUid: String,
        lastMessage: String,
        isGroup: Boolean
    ): HashMap<String, Any> {
        val senderUid = SupabaseManager.getCurrentUserID() ?: ""
        return hashMapOf(
            CHAT_ID_KEY to chatId,
            UID_KEY to conversationPartnerUid,
            LAST_MESSAGE_UID_KEY to senderUid,
            LAST_MESSAGE_TEXT_KEY to lastMessage,
            LAST_MESSAGE_STATE_KEY to "sended",
            PUSH_DATE_KEY to System.currentTimeMillis().toString(),
            "chat_type" to if (isGroup) "group" else "single",
            "isGroup" to isGroup.toString()
        )
    }
}