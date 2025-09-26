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
import com.synapse.social.studioasinc.chat.ChatConstants
import com.synapse.social.studioasinc.util.ChatMessageManager
import java.util.ArrayList
import java.util.HashMap
import android.widget.RelativeLayout

class MessageSendingHandler(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val _firebase: FirebaseDatabase,
    private val chatMessagesList: ArrayList<HashMap<String, Any>>,
    private val attactmentmap: ArrayList<HashMap<String, Any>>,
    private val chatAdapter: ChatAdapter,
    private val chatMessagesListRecycler: RecyclerView,
    private val rv_attacmentList: RecyclerView,
    private val attachmentLayoutListHolder: RelativeLayout,
    private val messageKeys: MutableSet<String>,
    private val recipientUid: String,
    internal var firstUserName: String,
    private val isGroup: Boolean
) {

    private val main = _firebase.getReference(ChatConstants.SKYLINE_REF)

    fun sendButtonAction(messageEt: EditText, replyMessageID: String, mMessageReplyLayout: LinearLayout) {
        val messageText = messageEt.text.toString().trim()
        val senderUid = auth.currentUser?.uid ?: return

        proceedWithMessageSending(messageText, senderUid, recipientUid, replyMessageID, messageEt, mMessageReplyLayout)
    }

    private fun proceedWithMessageSending(
        messageText: String,
        senderUid: String,
        recipientUid: String,
        replyMessageID: String,
        messageEt: EditText,
        mMessageReplyLayout: LinearLayout
    ) {
        auth.currentUser?.uid?.let { PresenceManager.setActivity(it, ChatConstants.PRESENCE_IDLE) }

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

            messageToSend[ChatConstants.TYPE_KEY] = ChatConstants.ATTACHMENT_MESSAGE_TYPE
            messageToSend[ChatConstants.ATTACHMENTS_KEY] = successfulAttachments
            lastMessageForInbox = if (messageText.isEmpty()) "${successfulAttachments.size} attachment(s)" else messageText
        } else { // Text-only message
            messageToSend[ChatConstants.TYPE_KEY] = ChatConstants.MESSAGE_TYPE
            lastMessageForInbox = messageText
        }

        messageToSend[ChatConstants.UID_KEY] = senderUid
        messageToSend[ChatConstants.MESSAGE_TEXT_KEY] = messageText
        messageToSend[ChatConstants.MESSAGE_STATE_KEY] = ChatConstants.MESSAGE_STATE_SENDED
        if (replyMessageID != "null") messageToSend[ChatConstants.REPLIED_MESSAGE_ID_KEY] = replyMessageID
        messageToSend[ChatConstants.KEY_KEY] = uniqueMessageKey
        messageToSend[ChatConstants.PUSH_DATE_KEY] = ServerValue.TIMESTAMP

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
        if (attactmentmap.isNotEmpty()) {
            resetAttachmentState()
        }

        // --- Background Action: Fetch recipient's notification ID and send notification ---
        val chatId = ChatMessageManager.getChatId(senderUid, recipientUid)
        val senderDisplayName = if (TextUtils.isEmpty(firstUserName)) "Someone" else firstUserName
        val notificationMessage = "$senderDisplayName: $lastMessageForInbox"

        _firebase.getReference(ChatConstants.SKYLINE_REF).child(ChatConstants.USERS_REF).child(recipientUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var recipientOneSignalPlayerId = "missing_id"
                    if (dataSnapshot.exists() && dataSnapshot.hasChild(ChatConstants.ONE_SIGNAL_PLAYER_ID_KEY)) {
                        val fetchedId = dataSnapshot.child(ChatConstants.ONE_SIGNAL_PLAYER_ID_KEY).getValue(String::class.java)
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
        chatSendMap[ChatConstants.UID_KEY] = senderUid
        chatSendMap[ChatConstants.TYPE_KEY] = ChatConstants.VOICE_MESSAGE_TYPE
        chatSendMap[ChatConstants.AUDIO_URL_KEY] = audioUrl
        chatSendMap[ChatConstants.AUDIO_DURATION_KEY] = duration
        chatSendMap[ChatConstants.MESSAGE_STATE_KEY] = ChatConstants.MESSAGE_STATE_SENDED
        if (replyMessageID != "null") chatSendMap[ChatConstants.REPLIED_MESSAGE_ID_KEY] = replyMessageID
        chatSendMap[ChatConstants.KEY_KEY] = uniqueMessageKey
        chatSendMap[ChatConstants.PUSH_DATE_KEY] = ServerValue.TIMESTAMP

        ChatMessageManager.sendMessageToDb(chatSendMap, senderUid, recipientUid, uniqueMessageKey, isGroup)

        chatSendMap["isLocalMessage"] = true
        messageKeys.add(uniqueMessageKey)
        chatMessagesList.add(chatSendMap)
        chatAdapter.notifyItemInserted(chatMessagesList.size - 1)
        chatMessagesListRecycler.post { chatMessagesListRecycler.smoothScrollToPosition(chatMessagesList.size - 1) }

        ChatMessageManager.updateInbox("Voice Message", recipientUid, isGroup, null)

        mMessageReplyLayout.visibility = View.GONE
    }

    private fun resetAttachmentState() {
        attachmentLayoutListHolder.visibility = View.GONE
        val oldSize = attactmentmap.size
        if (oldSize > 0) {
            attactmentmap.clear()
            rv_attacmentList.adapter?.notifyItemRangeRemoved(0, oldSize)
        }
    }
}