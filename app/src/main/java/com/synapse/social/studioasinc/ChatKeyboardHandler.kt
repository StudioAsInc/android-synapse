package com.synapse.social.studioasinc

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService
import com.synapse.social.studioasinc.util.ChatMessageManager

class ChatKeyboardHandler(
    private val activity: ChatActivity,
    private val messageEt: EditText,
    private val toolContainer: View,
    private val btn_sendMessage: MaterialButton,
    private val messageInputOutlinedRound: LinearLayout,
    private val messageInputOverallContainer: LinearLayout,
    private val authService: IAuthenticationService,
    private val dbService: IDatabaseService
) {



    fun setup() {
        messageEt.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val charSeq = s.toString()
                val chatID = ChatMessageManager(dbService, authService).getChatId(
                    authService.getCurrentUser()!!.uid,
                    activity.intent.getStringExtra(ChatConstants.UID_KEY)
                )
                val typingRef = dbService.getReference("chats").child(chatID).child(ChatConstants.TYPING_MESSAGE_REF)

                if (charSeq.isEmpty()) {
                    dbService.setValue(typingRef, null, (result, error) -> {})
                    activity._TransitionManager(messageInputOverallContainer, 150.0)
                    toolContainer.visibility = View.VISIBLE
                    btn_sendMessage.visibility = View.GONE
                    messageInputOutlinedRound.orientation = LinearLayout.HORIZONTAL
                } else {
                    val typingSnd = hashMapOf<String, Any>(
                        ChatConstants.UID_KEY to authService.getCurrentUser()!!.uid,
                        "typingMessageStatus" to "true"
                    )
                    dbService.updateChildren(typingRef, typingSnd, (result, error) -> {})
                    activity._TransitionManager(messageInputOverallContainer, 150.0)
                    toolContainer.visibility = View.GONE
                    btn_sendMessage.visibility = View.VISIBLE
                    messageInputOutlinedRound.orientation = LinearLayout.VERTICAL
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (messageEt.lineCount > 1) {
                    messageInputOutlinedRound.setBackgroundResource(R.drawable.bg_message_input_expanded)
                } else {
                    messageInputOutlinedRound.setBackgroundResource(R.drawable.bg_message_input)
                }
            }
        })
    }
}