// To-do: Migrate Firebase to Supabase
// This class handles the logic for showing and hiding the typing indicator.
// 1. **Remove Firebase Dependencies**:
//    - Remove the `FirebaseAuth` and `FirebaseDatabase` imports.
//    - The `auth` and `firebase` properties should be replaced with a Supabase client instance.
//
// 2. **Refactor Typing Indicator Logic**:
//    - The current implementation writes to a specific path in the Firebase Realtime Database (`chats/{chatID}/typing-message`).
//    - This should be replaced with Supabase's broadcast feature, which is designed for sending ephemeral messages to clients on the same channel.
//    - When the user starts typing, broadcast a "typing" event on the chat channel.
//    - When the user stops typing (text is empty), broadcast a "stopped-typing" event or a similar message.
//
// 3. **Authentication**:
//    - Replace `auth.currentUser!!.uid` with the method for getting the current user's ID from the Supabase client, which should be passed into this class's constructor.

package com.synapse.social.studioasinc

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.synapse.social.studioasinc.util.ChatMessageManager

class ChatKeyboardHandler(
    private val activity: ChatActivity,
    private val messageEt: EditText,
    private val toolContainer: View,
    private val btn_sendMessage: MaterialButton,
    private val messageInputOutlinedRound: LinearLayout,
    private val messageInputOverallContainer: LinearLayout,
    private val auth: FirebaseAuth
) {

    private val firebase = FirebaseDatabase.getInstance()

    fun setup() {
        messageEt.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val charSeq = s.toString()
                val chatID = ChatMessageManager.getChatId(
                    auth.currentUser!!.uid,
                    activity.intent.getStringExtra(ChatConstants.UID_KEY)
                )
                val typingRef = firebase.getReference("chats").child(chatID).child(ChatConstants.TYPING_MESSAGE_REF)

                if (charSeq.isEmpty()) {
                    typingRef.removeValue()
                    activity._TransitionManager(messageInputOverallContainer, 150.0)
                    toolContainer.visibility = View.VISIBLE
                    btn_sendMessage.visibility = View.GONE
                    messageInputOutlinedRound.orientation = LinearLayout.HORIZONTAL
                } else {
                    val typingSnd = hashMapOf<String, Any>(
                        ChatConstants.UID_KEY to auth.currentUser!!.uid,
                        "typingMessageStatus" to "true"
                    )
                    typingRef.updateChildren(typingSnd)
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