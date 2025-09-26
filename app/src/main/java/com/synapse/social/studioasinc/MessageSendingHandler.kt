package com.synapse.social.studioasinc

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.synapse.social.studioasinc.ChatAdapter
import com.synapse.social.studioasinc.NotificationHelper
import com.synapse.social.studioasinc.PresenceManager
import java.util.ArrayList
import java.util.HashMap

class MessageSendingHandler(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val _firebase: FirebaseDatabase,
    private val chatMessagesList: ArrayList<HashMap<String, Any>>,
    private val chatAdapter: ChatAdapter,
    private val chatMessagesListRecycler: RecyclerView,
    private val messageKeys: MutableSet<String>,
    private val recipientUid: String,
    private var firstUserName: String,
    private val isGroup: Boolean
) {

    private val main = _firebase.getReference("skyline")

    fun setFirstUserName(name: String) {
        this.firstUserName = name
    }

    fun sendButtonAction(messageEt: EditText, replyMessageID: String, attactmentmap: ArrayList<HashMap<String, Any>>, mMessageReplyLayout: LinearLayout) {
        val messageText = messageEt.text.toString().trim()
        val senderUid = auth.currentUser?.uid ?: return

        proceedWithMessageSending(messageText, senderUid, recipientUid, replyMessageID, attactmentmap, messageEt, mMessageReplyLayout)
    }

    private fun proceedWithMessageSending(
        messageText: String,
        senderUid: String,
        recipientUid: String,
        replyMessageID: String,
        attactmentmap: ArrayList<HashMap<String, Any>>,
        messageEt: EditText,
        mMessageReplyLayout: LinearLayout
    ) {
        auth.currentUser?.uid?.let { PresenceManager.setActivity(it, "Idle") }

        if (attactmentmap.isEmpty() && messageText.isEmpty()) {
            Log.w("MessageSendingHandler", "No message text and no attachments - nothing to send")
            return
        }

        val uniqueMessageKey = main.push().key ?: ""
        val messageToSend = HashMap<String, Any>()
        val lastMessageForInbox: String

        if (attactmentmap.isNotEmpty()) {
            val successfulAttachments = ArrayList<HashMap<String, Any>>()
            var allUploadsSuccessful = true
            for (item in attactmentmap) {
                if ("success" == item["uploadState"]) {
                    val attachmentData = HashMap<String, Any>()
                    attachmentData["url"] = item["cloudinaryUrl"] as Any
                    attachmentData["publicId"] = item["publicId"] as Any
                    attachmentData["width"] = item["width"] as Any
                    attachmentData["height"] = item["height"] as Any
                    successfulAttachments.add(attachmentData)
                } else {
                    allUploadsSuccessful = false
                }
            }

            if (!allUploadsSuccessful) {
                Toast.makeText(context, "Waiting for uploads to complete...", Toast.LENGTH_SHORT).show()
                return
            }

            messageToSend["TYPE"] = "ATTACHMENT_MESSAGE"
            messageToSend["attachments"] = successfulAttachments
            lastMessageForInbox = if (messageText.isEmpty()) "${successfulAttachments.size} attachment(s)" else messageText
        } else { // Text-only message
            messageToSend["TYPE"] = "MESSAGE"
            lastMessageForInbox = messageText
        }

        messageToSend["uid"] = senderUid
        messageToSend["message_text"] = messageText
        messageToSend["message_state"] = "sended"
        if (replyMessageID != "null") messageToSend["replied_message_id"] = replyMessageID
        messageToSend["key"] = uniqueMessageKey
        messageToSend["push_date"] = ServerValue.TIMESTAMP

        ChatMessageManager.sendMessageToDb(messageToSend, senderUid, recipientUid, uniqueMessageKey, isGroup)

        val localMessage = HashMap(messageToSend)
        localMessage["isLocalMessage"] = true
        messageKeys.add(uniqueMessageKey)
        chatMessagesList.add(localMessage)

        val newPosition = chatMessagesList.size - 1
        chatAdapter.notifyItemInserted(newPosition)
        if (newPosition > 0) chatAdapter.notifyItemChanged(newPosition - 1)

        chatMessagesListRecycler.post { chatMessagesListRecycler.smoothScrollToPosition(chatMessagesList.size - 1) }

        ChatMessageManager.updateInbox(lastMessageForInbox, recipientUid, isGroup, null)

        messageEt.setText("")
        mMessageReplyLayout.visibility = View.GONE

        // --- Background Action: Fetch recipient's notification ID and send notification ---
        val chatId = ChatMessageManager.getChatId(senderUid, recipientUid)
        val senderDisplayName = if (TextUtils.isEmpty(firstUserName)) "Someone" else firstUserName
        val notificationMessage = "$senderDisplayName: $lastMessageForInbox"

        _firebase.getReference("skyline/users").child(recipientUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var recipientOneSignalPlayerId = "missing_id"
                    if (dataSnapshot.exists() && dataSnapshot.hasChild("oneSignalPlayerId")) {
                        val fetchedId = dataSnapshot.child("oneSignalPlayerId").getValue(String::class.java)
                        if (fetchedId != null && !fetchedId.isEmpty()) {
                            recipientOneSignalPlayerId = fetchedId
                        }
                    }
                    NotificationHelper.sendMessageAndNotifyIfNeeded(senderUid, recipientUid, recipientOneSignalPlayerId, notificationMessage, chatId)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("ChatActivity", "Failed to fetch recipient's data for notification.", databaseError.toException())
                    NotificationHelper.sendMessageAndNotifyIfNeeded(senderUid, recipientUid, "missing_id", notificationMessage, chatId)
                }
            })
    }

    fun sendVoiceMessage(audioUrl: String, duration: Long, replyMessageID: String, mMessageReplyLayout: LinearLayout) {
        val senderUid = auth.currentUser?.uid ?: return
        val uniqueMessageKey = main.push().key ?: ""

        val chatSendMap = HashMap<String, Any>()
        chatSendMap["uid"] = senderUid
        chatSendMap["TYPE"] = "VOICE_MESSAGE"
        chatSendMap["audio_url"] = audioUrl
        chatSendMap["audio_duration"] = duration
        chatSendMap["message_state"] = "sended"
        if (replyMessageID != "null") chatSendMap["replied_message_id"] = replyMessageID
        chatSendMap["key"] = uniqueMessageKey
        chatSendMap["push_date"] = ServerValue.TIMESTAMP

        ChatMessageManager.sendMessageToDb(chatSendMap, senderUid, recipientUid, uniqueMessageKey, isGroup)

        chatSendMap["isLocalMessage"] = true
        messageKeys.add(uniqueMessageKey)
        chatMessagesList.add(chatSendMap)
        chatAdapter.notifyItemInserted(chatMessagesList.size - 1)
        chatMessagesListRecycler.post { chatMessagesListRecycler.smoothScrollToPosition(chatMessagesList.size - 1) }

        ChatMessageManager.updateInbox("Voice Message", recipientUid, isGroup, null)

        mMessageReplyLayout.visibility = View.GONE
    }
}