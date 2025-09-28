package com.synapse.social.studioasinc

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService
import com.synapse.social.studioasinc.backend.interfaces.IDataListener
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError
import com.synapse.social.studioasinc.util.ChatMessageManager

class MessageSendingHandler(
    private val context: Context,
    private val authService: IAuthenticationService,
    private val dbService: IDatabaseService,
    private val messageEt: EditText,
    private val attachmentLayoutListHolder: View,
    private val attactmentmap: ArrayList<HashMap<String, Any>>,
    private val repliedMessageId: String?,
    private val isGroup: Boolean,
    private val recipientUid: String
) {

    private val chatMessageManager = ChatMessageManager(dbService, authService)

    fun sendMessage() {
        val messageText = messageEt.text.toString().trim()
        val senderUid = authService.getCurrentUser()?.getUid() ?: return

        proceedWithMessageSending(messageText, senderUid, recipientUid, repliedMessageId ?: "null", messageEt)
    }

    private fun proceedWithMessageSending(
        messageText: String,
        senderUid: String,
        recipientUid: String,
        replyMessageID: String,
        messageEt: EditText
    ) {
        if (attactmentmap.isEmpty() && messageText.isEmpty()) {
            Log.w("MessageSendingHandler", "No message text and no attachments - nothing to send")
            return
        }

        val uniqueMessageKey = dbService.getReference("chats").push().key ?: ""
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
        messageToSend[ChatConstants.MESSAGE_STATE_KEY] = "sended"
        if (replyMessageID != "null") messageToSend[ChatConstants.REPLIED_MESSAGE_ID_KEY] = replyMessageID
        messageToSend[ChatConstants.KEY_KEY] = uniqueMessageKey
        messageToSend[ChatConstants.PUSH_DATE_KEY] = System.currentTimeMillis()

        chatMessageManager.sendMessageToDb(messageToSend, senderUid, recipientUid, uniqueMessageKey, isGroup)
        chatMessageManager.updateInbox(lastMessageForInbox, recipientUid, isGroup, null)

        messageEt.setText("")
        if (attactmentmap.isNotEmpty()) {
            resetAttachmentState()
        }
    }

    fun sendVoiceMessage(audioFilePath: String, duration: Long) {
        AsyncUploadService.uploadWithNotification(context, audioFilePath, java.io.File(audioFilePath).name, object : AsyncUploadService.UploadProgressListener {
            override fun onProgress(filePath: String, percent: Int) {}
            override fun onSuccess(filePath: String, url: String, publicId: String) {
                val senderUid = authService.getCurrentUser()?.getUid() ?: return
                val uniqueMessageKey = dbService.getReference("chats").push().key ?: ""

                val chatSendMap = HashMap<String, Any>()
                chatSendMap[ChatConstants.UID_KEY] = senderUid
                chatSendMap[ChatConstants.TYPE_KEY] = ChatConstants.VOICE_MESSAGE_TYPE
                chatSendMap["audio_url"] = url
                chatSendMap["audio_duration"] = duration
                chatSendMap[ChatConstants.MESSAGE_STATE_KEY] = "sended"
                if (repliedMessageId != "null") chatSendMap[ChatConstants.REPLIED_MESSAGE_ID_KEY] = repliedMessageId!!
                chatSendMap[ChatConstants.KEY_KEY] = uniqueMessageKey
                chatSendMap[ChatConstants.PUSH_DATE_KEY] = System.currentTimeMillis()

                chatMessageManager.sendMessageToDb(chatSendMap, senderUid, recipientUid, uniqueMessageKey, isGroup)
                chatMessageManager.updateInbox("Voice Message", recipientUid, isGroup, null)
            }

            override fun onFailure(filePath: String, error: String) {
                Toast.makeText(context, "Failed to upload audio.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun resetAttachmentState() {
        attachmentLayoutListHolder.visibility = View.GONE
        val rvAdapter = (attachmentLayoutListHolder.findViewById<RecyclerView>(R.id.rv_attacmentList)).adapter
        val oldSize = attactmentmap.size
        if (oldSize > 0) {
            attactmentmap.clear()
            rvAdapter?.notifyItemRangeRemoved(0, oldSize)
        }
    }
}