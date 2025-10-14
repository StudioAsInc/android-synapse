
package com.synapse.social.studioasinc.util

import com.synapse.social.studioasinc.Supabase
import com.synapse.social.studioasinc.model.InboxUpdate
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object ChatMessageManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        scope.launch {
            try {
                if (isGroup) {
                    Supabase.client.postgrest["group_chats"].insert(messageMap)
                } else {
                    val chatId = getChatId(senderUid, recipientUid)
                    Supabase.client.postgrest["chats"].insert(messageMap)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateInbox(lastMessage: String, recipientUid: String, isGroup: Boolean, groupName: String? = null) {
        val senderUid = Supabase.client.auth.currentUserOrNull()?.id ?: return

        scope.launch {
            try {
                if (isGroup) {
                    val members = Supabase.client.postgrest["groups"].select {
                        filter("id", "eq", recipientUid)
                    }.data[0].get("members").toString()

                    for (member in members.split(",")) {
                        val inboxUpdate = createInboxUpdate(
                            chatId = recipientUid,
                            conversationPartnerUid = recipientUid,
                            lastMessage = lastMessage,
                            isGroup = true
                        )
                        Supabase.client.postgrest["inbox"].insert(inboxUpdate)
                    }
                } else {
                    val senderInboxUpdate = createInboxUpdate(
                        chatId = getChatId(senderUid, recipientUid),
                        conversationPartnerUid = recipientUid,
                        lastMessage = lastMessage,
                        isGroup = false
                    )
                    Supabase.client.postgrest["inbox"].insert(senderInboxUpdate)

                    val recipientInboxUpdate = createInboxUpdate(
                        chatId = getChatId(senderUid, recipientUid),
                        conversationPartnerUid = senderUid,
                        lastMessage = lastMessage,
                        isGroup = false
                    )
                    Supabase.client.postgrest["inbox"].insert(recipientInboxUpdate)
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
    ): InboxUpdate {
        val senderUid = Supabase.client.auth.currentUserOrNull()?.id ?: ""
        return InboxUpdate(
            chat_id = chatId,
            uid = conversationPartnerUid,
            last_message_uid = senderUid,
            last_message_text = lastMessage,
            last_message_state = "sended",
            push_date = System.currentTimeMillis().toString(),
            chat_type = if (isGroup) "group" else "single",
            isGroup = isGroup.toString()
        )
    }
}
