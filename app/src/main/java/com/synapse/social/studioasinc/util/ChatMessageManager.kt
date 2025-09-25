package com.synapse.social.studioasinc.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.util.Calendar
import java.util.HashMap

object ChatMessageManager {

    private val firebaseDatabase = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
            firebaseDatabase.getReference(SKYLINE_REF).child(GROUP_CHATS_REF).child(recipientUid).child(uniqueMessageKey)
                .setValue(messageMap)
        } else {
            val chatId = getChatId(senderUid, recipientUid)
            val fanOutObject = hashMapOf<String, Any?>(
                "/$CHATS_REF/$chatId/$uniqueMessageKey" to messageMap,
                "/$USER_CHATS_REF/$senderUid/$chatId" to true,
                "/$USER_CHATS_REF/$recipientUid/$chatId" to true
            )
            firebaseDatabase.reference.updateChildren(fanOutObject)
        }
    }

    fun updateInbox(lastMessage: String, recipientUid: String, isGroup: Boolean, groupName: String? = null) {
        val senderUid = auth.currentUser?.uid ?: return

        if (isGroup) {
            val groupRef = firebaseDatabase.getReference(SKYLINE_REF).child("groups").child(recipientUid)
            groupRef.child("members").get().addOnSuccessListener { dataSnapshot ->
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
                            firebaseDatabase.getReference(INBOX_REF).child(memberUid).child(recipientUid)
                                .setValue(inboxUpdate)
                        }
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
            firebaseDatabase.getReference(INBOX_REF).child(senderUid).child(recipientUid)
                .setValue(senderInboxUpdate)

            // Update inbox for the other user
            val recipientInboxUpdate = createInboxUpdate(
                chatId = getChatId(senderUid, recipientUid),
                conversationPartnerUid = senderUid,
                lastMessage = lastMessage,
                isGroup = false
            )
            firebaseDatabase.getReference(INBOX_REF).child(recipientUid).child(senderUid)
                .setValue(recipientInboxUpdate)
        }
    }

    private fun createInboxUpdate(
        chatId: String,
        conversationPartnerUid: String,
        lastMessage: String,
        isGroup: Boolean
    ): HashMap<String, Any> {
        val senderUid = auth.currentUser?.uid ?: ""
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
