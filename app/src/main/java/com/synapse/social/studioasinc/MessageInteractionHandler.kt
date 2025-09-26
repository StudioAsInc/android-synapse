package com.synapse.social.studioasinc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.synapse.social.studioasinc.util.ChatMessageManager
import java.util.HashMap

internal class MessageInteractionHandler(
    private val activity: AppCompatActivity,
    private val listener: ChatInteractionListener,
    private val auth: FirebaseAuth,
    private val _firebase: FirebaseDatabase,
    private val chatMessagesList: ArrayList<HashMap<String, Any>>,
    private val chatMessagesListRecycler: RecyclerView,
    private val vbr: android.os.Vibrator,
    private val aiFeatureHandler: AiFeatureHandler,
    private var firstUserName: String,
    private var secondUserName: String
) {

    fun setFirstUserName(name: String) {
        this.firstUserName = name
    }

    fun setSecondUserName(name: String) {
        this.secondUserName = name
    }

    fun showMessageOverviewPopup(view: View, position: Int) {
        if (position >= chatMessagesList.size || position < 0) {
            return
        }

        val messageData = chatMessagesList[position]
        val currentUser = auth.currentUser
        val senderUid = messageData[ChatActivity.UID_KEY]?.toString()
        val isMine = currentUser != null && senderUid != null && senderUid == currentUser.uid
        val messageText = messageData[ChatActivity.MESSAGE_TEXT_KEY]?.toString() ?: ""

        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.chat_msg_options_popup_cv_synapse, null)

        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindow.elevation = 24f

        val editLayout = popupView.findViewById<LinearLayout>(R.id.edit)
        val replyLayout = popupView.findViewById<LinearLayout>(R.id.reply)
        val summaryLayout = popupView.findViewById<LinearLayout>(R.id.summary)
        val explainLayout = popupView.findViewById<LinearLayout>(R.id.explain)
        val copyLayout = popupView.findViewById<LinearLayout>(R.id.copy)
        val deleteLayout = popupView.findViewById<LinearLayout>(R.id.delete)

        editLayout.visibility = if (isMine) View.VISIBLE else View.GONE
        deleteLayout.visibility = if (isMine) View.VISIBLE else View.GONE
        summaryLayout.visibility = if (messageText.length > 200) View.VISIBLE else View.GONE

        replyLayout.setOnClickListener {
            listener.onReplySelected(messageData[ChatActivity.KEY_KEY].toString())
            vbr.vibrate(48)
            popupWindow.dismiss()
        }

        copyLayout.setOnClickListener {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("clipboard", messageText)
            clipboard.setPrimaryClip(clip)
            vbr.vibrate(48)
            popupWindow.dismiss()
        }

        deleteLayout.setOnClickListener {
            listener.onDeleteMessage(messageData)
            popupWindow.dismiss()
        }

        editLayout.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(activity)
            dialog.setTitle("Edit message")
            val dialogView = LayoutInflater.from(activity).inflate(R.layout.single_et, null)
            dialog.setView(dialogView)
            val editText = dialogView.findViewById<EditText>(R.id.edittext1)
            editText.setText(messageText)
            dialog.setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString()
                val cu = auth.currentUser
                val myUid = cu?.uid
                if (myUid == null) return@setPositiveButton
                val otherUid = activity.intent.getStringExtra(ChatActivity.UID_KEY)
                val msgKey = messageData[ChatActivity.KEY_KEY]?.toString()
                if (otherUid == null || msgKey == null) return@setPositiveButton
                val chatID = ChatMessageManager.getChatId(myUid, otherUid)
                val msgRef = _firebase.getReference(ChatActivity.CHATS_REF).child(chatID).child(msgKey)
                msgRef.child(ChatActivity.MESSAGE_TEXT_KEY).setValue(newText)
            }
            dialog.setNegativeButton("Cancel", null)
            val shownDialog = dialog.show()

            editText.requestFocus()
            shownDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

            popupWindow.dismiss()
        }

        summaryLayout.setOnClickListener {
            val prompt = "Summarize the following text in a few sentences:\n\n$messageText"
            val vh = chatMessagesListRecycler.findViewHolderForAdapterPosition(position)
            if (vh is BaseMessageViewHolder) {
                aiFeatureHandler.callGeminiForSummary(prompt, vh)
            }
            popupWindow.dismiss()
        }

        explainLayout.setOnClickListener {
            val prompt = buildExplanationPrompt(position, messageText, messageData)
            val vh = chatMessagesListRecycler.findViewHolderForAdapterPosition(position)
            if (vh is BaseMessageViewHolder) {
                aiFeatureHandler.callGeminiForExplanation(prompt, vh)
            }
            popupWindow.dismiss()
        }

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = popupView.measuredWidth
        val popupHeight = popupView.measuredHeight

        val location = IntArray(2)
        view.getLocationOnScreen(location)

        val xInitial = location[0] + view.width / 2 - popupWidth / 2
        val yAbove = location[1] - popupHeight - 8
        val yBelow = location[1] + view.height + 8

        val visibleFrame = Rect()
        view.getWindowVisibleDisplayFrame(visibleFrame)

        val x = xInitial.coerceIn(visibleFrame.left + 16, visibleFrame.right - popupWidth - 16)
        var y = if (yAbove >= visibleFrame.top + 16) yAbove else yBelow
        y = y.coerceIn(visibleFrame.top + 16, visibleFrame.bottom - popupHeight - 16)


        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isOutsideTouchable = true
        popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, x, y)
    }

    private fun buildExplanationPrompt(position: Int, messageText: String, messageData: HashMap<String, Any>): String {
        val beforeContext = StringBuilder()
        val startIndex = kotlin.math.max(0, position - 5)
        for (i in startIndex until position) {
            appendMessageToContext(beforeContext, chatMessagesList[i])
        }

        val afterContext = StringBuilder()
        val endIndex = kotlin.math.min(chatMessagesList.size, position + 3)
        for (i in position + 1 until endIndex) {
            appendMessageToContext(afterContext, chatMessagesList[i])
        }

        val senderOfMessageToExplain = getSenderNameForMessage(messageData)

        return activity.getString(
            R.string.gemini_explanation_prompt,
            secondUserName,
            beforeContext.toString(),
            senderOfMessageToExplain,
            messageText,
            afterContext.toString()
        )
    }

    private fun getSenderNameForMessage(message: HashMap<String, Any>): String {
        val isMyMessage = message[ChatActivity.UID_KEY].toString() == auth.currentUser?.uid
        return if (isMyMessage) firstUserName else secondUserName
    }

    private fun appendMessageToContext(contextBuilder: StringBuilder, message: HashMap<String, Any>) {
        val messageText = message[ChatActivity.MESSAGE_TEXT_KEY]?.toString() ?: ""
        contextBuilder.append(getSenderNameForMessage(message))
            .append(": ")
            .append(messageText)
            .append("\n")
    }
}