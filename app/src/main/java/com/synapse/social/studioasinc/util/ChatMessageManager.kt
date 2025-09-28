package com.synapse.social.studioasinc.util

import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService

class ChatMessageManager(
    private val dbService: IDatabaseService,
    private val authService: IAuthenticationService
) {


    private const val SKYLINE_REF = "skyline"
    private const val CHATS_REF = "chats"
    private const val USER_CHATS_REF = "user-chats"
    private const val GROUP_CHATS_REF = "group-chats"
    private const val INBOX_REF = "inbox"

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
        if (isGroup) {
            dbService.setValue(dbService.getReference(SKYLINE_REF).child(GROUP_CHATS_REF).child(recipientUid).child(uniqueMessageKey),
                messageMap, (result, error) -> {})
        } else {
            val chatId = getChatId(senderUid, recipientUid)
            val fanOutObject = hashMapOf<String, Any?>(
                "/$CHATS_REF/$chatId/$uniqueMessageKey" to messageMap,
                "/$USER_CHATS_REF/$senderUid/$chatId" to true,
                "/$USER_CHATS_REF/$recipientUid/$chatId" to true
            )
            dbService.updateChildren(dbService.getReference(""), fanOutObject, (result, error) -> {})
        }
    }

    fun updateInbox(lastMessage: String, recipientUid: String, isGroup: Boolean, groupName: String? = null) {
        val senderUid = authService.getCurrentUser()?.uid ?: return

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
                                dbService.setValue(dbService.getReference(INBOX_REF).child(memberUid).child(recipientUid),
                                    inboxUpdate, (result, error) -> {})
                            }
                        }
                    }
                }

                override fun onCancelled(databaseError: IDatabaseError) {
                    // Handle error
                }
            })
        } else {
            // Update inbox for the current user
            val senderInboxUpdate = createInboxUpdate(
                chatId = getChatId(senderUid, recipientUid),
                conversationPartnerUid = recipientUid,
                lastMessage = lastMessage,
                isGroup = false
            )
            dbService.setValue(dbService.getReference(INBOX_REF).child(senderUid).child(recipientUid),
                senderInboxUpdate, (result, error) -> {})

            // Update inbox for the other user
            val recipientInboxUpdate = createInboxUpdate(
                chatId = getChatId(senderUid, recipientUid),
                conversationPartnerUid = senderUid,
                lastMessage = lastMessage,
                isGroup = false
            )
            dbService.setValue(dbService.getReference(INBOX_REF).child(recipientUid).child(senderUid),
                recipientInboxUpdate, (result, error) -> {})
        }
    }

    private fun createInboxUpdate(
        chatId: String,
        conversationPartnerUid: String,
        lastMessage: String,
        isGroup: Boolean
    ): HashMap<String, Any> {
        val senderUid = authService.getCurrentUser()?.uid ?: ""
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
