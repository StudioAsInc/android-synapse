package com.synapse.social.studioasinc

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import com.synapse.social.studioasinc.ChatAdapter
import com.synapse.social.studioasinc.NotificationHelper
import com.synapse.social.studioasinc.PresenceManager
import com.synapse.social.studioasinc.util.ChatMessageManager
import java.util.ArrayList
import java.util.HashMap
import android.widget.RelativeLayout
import com.synapse.social.studioasinc.ChatConstants.SKYLINE_REF
import com.synapse.social.studioasinc.ChatConstants.TYPE_KEY
import com.synapse.social.studioasinc.ChatConstants.ATTACHMENT_MESSAGE_TYPE
import com.synapse.social.studioasinc.ChatConstants.ATTACHMENTS_KEY
import com.synapse.social.studioasinc.ChatConstants.MESSAGE_TYPE
import com.synapse.social.studioasinc.ChatConstants.UID_KEY
import com.synapse.social.studioasinc.ChatConstants.MESSAGE_TEXT_KEY
import com.synapse.social.studioasinc.ChatConstants.MESSAGE_STATE_KEY
import com.synapse.social.studioasinc.ChatConstants.REPLIED_MESSAGE_ID_KEY
import com.synapse.social.studioasinc.ChatConstants.KEY_KEY
import com.synapse.social.studioasinc.ChatConstants.PUSH_DATE_KEY
import com.synapse.social.studioasinc.ChatConstants.USERS_REF
import com.synapse.social.studioasinc.ChatConstants.VOICE_MESSAGE_TYPE

// TODO: Migrate to Supabase
// This class is responsible for sending messages.
// The following needs to be done:
// 1. Replace all Firebase database calls with calls to the `DatabaseService` interface.
// 2. Replace all Firebase auth calls with calls to the `AuthenticationService` interface.
class MessageSendingHandler(
    private val context: Context,
    private val chatMessagesList: ArrayList<HashMap<String, Any>>,
    private val attactmentmap: ArrayList<HashMap<String, Any>>,
    private val chatAdapter: ChatAdapter,
    private val chatMessagesListRecycler: RecyclerView,
    private val rv_attacmentList: RecyclerView,
    private val attachmentLayoutListHolder: RelativeLayout,
    private val messageKeys: MutableSet<String>,
    private val recipientUid: String,
    private var firstUserName: String,
    private val isGroup: Boolean
) {

    fun setFirstUserName(name: String) {
        this.firstUserName = name
    }

    fun sendButtonAction(messageEt: EditText, replyMessageID: String, mMessageReplyLayout: LinearLayout) {
        val messageText = messageEt.text.toString().trim()
        // TODO: Replace with Supabase Auth
        // Supabase: val senderUid = supabase.auth.currentUser()?.id ?: return

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
        / Implement with Supabase
        // auth.currentUser?.uid?.let { PresenceManager.setActivity(it, "Idle") }/ TODO:

        if (attactmentmap.isEmpty() && messageText.isEmpty()) {
            Log.w("MessageSendingHandler", "No message text and no attachments - nothing to send")
            return
        }

        // Supabase: val uniqueMessageKey = UUID.randomUUID().toString()
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

            messageToSend[TYPE_KEY] = ATTACHMENT_MESSAGE_TYPE
            messageToSend[ATTACHMENTS_KEY] = successfulAttachments
            lastMessageForInbox = if (messageText.isEmpty()) "${successfulAttachments.size} attachment(s)" else messageText
        } else { // Text-only message
            messageToSend[TYPE_KEY] = MESSAGE_TYPE
            lastMessageForInbox = messageText
        }

        messageToSend[UID_KEY] = senderUid
        messageToSend[MESSAGE_TEXT_KEY] = messageText
        messageToSend[MESSAGE_STATE_KEY] = "sended"
        if (replyMessageID != "null") messageToSend[REPLIED_MESSAGE_ID_KEY] = replyMessageID
        messageToSend[KEY_KEY] = uniqueMessageKey
        // messageToSend[PUSH_DATE_KEY] = ServerValue.TIMESTAMP

        // Supabase: ChatMessageManager.sendMessageToDb(messageToSend, senderUid, recipientUid, uniqueMessageKey, isGroup)

        val localMessage = HashMap(messageToSend)
        localMessage["isLocalMessage"] = true
        messageKeys.add(uniqueMessageKey)
        chatMessagesList.add(localMessage)

        val newPosition = chatMessagesList.size - 1
        chatAdapter.notifyItemInserted(newPosition)
        if (newPosition > 0) chatAdapter.notifyItemChanged(newPosition - 1)

        chatMessagesListRecycler.post { chatMessagesListRecycler.smoothScrollToPosition(chatMessagesList.size - 1) }

        // Supabase: ChatMessageManager.updateInbox(lastMessageForInbox, recipientUid, isGroup, null)

        messageEt.setText("")
        mMessageReplyLayout.visibility = View.GONE
        if (attactmentmap.isNotEmpty()) {
            resetAttachmentState()
        }

        // --- Background Action: Fetch recipient's notification ID and send notification ---
        val chatId = ChatMessageManager.getChatId(senderUid, recipientUid)
        val senderDisplayName = if (TextUtils.isEmpty(firstUserName)) "Someone" else firstUserName
        val notificationMessage = "$senderDisplayName: $lastMessageForInbox"

        // TODO: Get recipient's OneSignal player ID from Supabase
    }

    // Supabase: fun sendVoiceMessage(audioUrl: String, duration: Long, replyMessageID: String, mMessageReplyLayout: LinearLayout) {
    // Supabase:     // Supabase: Implement with Supabase
    // Supabase: }

    private fun resetAttachmentState() {
        attachmentLayoutListHolder.visibility = View.GONE
        val oldSize = attactmentmap.size
        if (oldSize > 0) {
            attactmentmap.clear()
            rv_attacmentList.adapter?.notifyItemRangeRemoved(0, oldSize)
        }
    }
}