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
import com.synapse.social.studioasinc.chat.common.viewmodel.ChatViewModel
import com.synapse.social.studioasinc.model.ChatMessage

class MessageSendingHandler(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val chatViewModel: ChatViewModel,
    private val chatId: String,
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
        auth.currentUser?.uid?.let { PresenceManager.setActivity(it, "Idle") }

        if (attactmentmap.isEmpty() && messageText.isEmpty()) {
            Log.w("MessageSendingHandler", "No message text and no attachments - nothing to send")
            return
        }

        val uniqueMessageKey = FirebaseDatabase.getInstance().reference.push().key ?: ""
        val message = ChatMessage(
            key = uniqueMessageKey,
            uid = senderUid,
            messageText = messageText,
            pushDate = System.currentTimeMillis(),
            messageState = "sended",
            repliedMessageId = if (replyMessageID != "null") replyMessageID else null,
            attachments = attactmentmap.mapNotNull {
                if ("success" == it["uploadState"]) {
                    com.synapse.social.studioasinc.model.Attachment(
                        publicId = it["publicId"] as String,
                        url = it["cloudinaryUrl"] as String,
                        width = it["width"] as Int,
                        height = it["height"] as Int
                    )
                } else {
                    null
                }
            }
        )
        chatViewModel.sendMessage(chatId, message)

        messageEt.setText("")
        mMessageReplyLayout.visibility = View.GONE
        if (attactmentmap.isNotEmpty()) {
            resetAttachmentState()
        }

        val senderDisplayName = if (TextUtils.isEmpty(firstUserName)) "Someone" else firstUserName
        val notificationMessage = "$senderDisplayName: ${message.messageText}"
        NotificationHelper.sendMessageAndNotifyIfNeeded(auth.currentUser!!.uid, recipientUid, "missing_id", notificationMessage, chatId)
    }

    fun sendVoiceMessage(audioUrl: String, duration: Long, replyMessageID: String, mMessageReplyLayout: LinearLayout) {
        val senderUid = auth.currentUser?.uid ?: return
        val uniqueMessageKey = FirebaseDatabase.getInstance().reference.push().key ?: ""
        val message = ChatMessage(
            key = uniqueMessageKey,
            uid = senderUid,
            pushDate = System.currentTimeMillis(),
            messageState = "sended",
            type = VOICE_MESSAGE_TYPE,
            attachments = listOf(
                com.synapse.social.studioasinc.model.Attachment(
                    url = audioUrl
                )
            ),
            repliedMessageId = if (replyMessageID != "null") replyMessageID else null
        )
        chatViewModel.sendMessage(chatId, message)

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