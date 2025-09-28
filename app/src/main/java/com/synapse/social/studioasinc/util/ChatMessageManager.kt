package com.synapse.social.studioasinc.util

import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IDataListener
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService

class ChatMessageManager(
    private val dbService: IDatabaseService,
    private val authService: IAuthenticationService
) {

    companion object {
        const val SKYLINE_REF = "skyline"
        const val CHATS_REF = "chats"
        const val USER_CHATS_REF = "user-chats"
        const val GROUP_CHATS_REF = "group-chats"
        const val INBOX_REF = "inbox"

        const val CHAT_ID_KEY = "chatID"
        const val UID_KEY = "uid"
        const val LAST_MESSAGE_UID_KEY = "last_message_uid"
        const val LAST_MESSAGE_TEXT_KEY = "last_message_text"
        const val LAST_MESSAGE_STATE_KEY = "last_message_state"
        const val PUSH_DATE_KEY = "push_date"
    }

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
        val emptyListener = object : ICompletionListener<Unit> {
            override fun onComplete(result: Unit?, error: Exception?) {
                // Not implemented
            }
        }

        if (isGroup) {
            dbService.setValue(
                dbService.getReference(SKYLINE_REF).child(GROUP_CHATS_REF).child(recipientUid).child(uniqueMessageKey),
                messageMap,
                emptyListener
            )
        } else {
            val chatId = getChatId(senderUid, recipientUid)
            val fanOutObject = hashMapOf<String, Any?>(
                "/$CHATS_REF/$chatId/$uniqueMessageKey" to messageMap,
                "/$USER_CHATS_REF/$senderUid/$chatId" to true,
                "/$USER_CHATS_REF/$recipientUid/$chatId" to true
            )
            dbService.updateChildren(dbService.getReference(""), fanOutObject, emptyListener)
        }
    }

    fun updateInbox(lastMessage: String, recipientUid: String, isGroup: Boolean, groupName: String? = null) {
        val senderUid = authService.getCurrentUser()?.getUid() ?: return
        val emptyListener = object : ICompletionListener<Unit> {
            override fun onComplete(result: Unit?, error: Exception?) {
                // Not implemented
            }
        }

        if (isGroup) {
            val groupRef = dbService.getReference(SKYLINE_REF).child("groups").child(recipientUid)
            dbService.getData(groupRef.child("members"), object : IDataListener {
                override fun onDataChange(dataSnapshot: IDataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (memberSnapshot in dataSnapshot.children) {
                            val memberUid = memberSnapshot.key
                            if (memberUid != null) {
                                val inboxUpdate = createInboxUpdate(
                                    chatId = recipientUid,
                                    conversationPartnerUid = recipientUid,
                                    lastMessage = lastMessage,
                                    isGroup = true
                                )
                                dbService.setValue(
                                    dbService.getReference(INBOX_REF).child(memberUid).child(recipientUid),
                                    inboxUpdate,
                                    emptyListener
                                )
                            }
                        }
                    }
                }

                override fun onCancelled(databaseError: IDatabaseError) {
                    // Handle error
                }
            })
        } else {
            val senderInboxUpdate = createInboxUpdate(
                chatId = getChatId(senderUid, recipientUid),
                conversationPartnerUid = recipientUid,
                lastMessage = lastMessage,
                isGroup = false
            )
            dbService.setValue(
                dbService.getReference(INBOX_REF).child(senderUid).child(recipientUid),
                senderInboxUpdate,
                emptyListener
            )

            val recipientInboxUpdate = createInboxUpdate(
                chatId = getChatId(senderUid, recipientUid),
                conversationPartnerUid = senderUid,
                lastMessage = lastMessage,
                isGroup = false
            )
            dbService.setValue(
                dbService.getReference(INBOX_REF).child(recipientUid).child(senderUid),
                recipientInboxUpdate,
                emptyListener
            )
        }
    }

    private fun createInboxUpdate(
        chatId: String,
        conversationPartnerUid: String,
        lastMessage: String,
        isGroup: Boolean
    ): HashMap<String, Any> {
        val senderUid = authService.getCurrentUser()?.getUid() ?: ""
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
