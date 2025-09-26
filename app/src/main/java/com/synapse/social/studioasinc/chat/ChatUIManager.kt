package com.synapse.social.studioasinc.chat

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.chat.ChatConstants.KEY_KEY

class ChatUIManager(
    private val context: Context,
    private val messageEt: EditText,
    private val toolContainer: View,
    private val messageInputOutlinedRound: LinearLayout,
    private val chatMessagesListRecycler: RecyclerView,
    private val replyLayout: View,
    private val replyUsername: TextView,
    private val replyMessage: TextView,
    private val listener: ChatUIListener
) {

    init {
        setupMessageEditTextWatcher()
        setupSwipeToReply()
    }

    private fun setupMessageEditTextWatcher() {
        messageEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    toolContainer.visibility = View.VISIBLE
                    messageInputOutlinedRound.orientation = LinearLayout.HORIZONTAL
                    listener.onTypingStateChanged(false)
                } else {
                    toolContainer.visibility = View.GONE
                    messageInputOutlinedRound.orientation = LinearLayout.VERTICAL
                    listener.onTypingStateChanged(true)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (messageEt.lineCount > 1) {
                    messageInputOutlinedRound.setBackgroundResource(R.drawable.bg_message_input_expanded)
                } else {
                    messageInputOutlinedRound.setBackgroundResource(R.drawable.bg_message_input)
                }
            }
        })
    }

    private fun setupSwipeToReply() {
        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onMessageSwiped(position)
                }
                // Reset the item view's translation
                viewHolder.itemView.animate().translationX(0f).setDuration(150).start()
            }
            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val icon = ContextCompat.getDrawable(context, R.drawable.ic_reply)
                    if (icon != null) {
                        icon.setColorFilter(0xFF616161.toInt(), PorterDuff.Mode.SRC_IN)
                        val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + icon.intrinsicHeight

                        if (dX > 0) { // Swiping right
                            val iconLeft = itemView.left + iconMargin
                            val iconRight = iconLeft + icon.intrinsicWidth
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        } else { // Swiping left
                            val iconRight = itemView.right - iconMargin
                            val iconLeft = iconRight - icon.intrinsicWidth
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        }
                        icon.draw(c)
                    }
                    itemView.translationX = dX * 0.75f // Damped translation
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }
        }
        ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(chatMessagesListRecycler)
    }

    fun showReplyUI(messageData: HashMap<String, Any>, senderName: String) {
        replyLayout.visibility = View.VISIBLE
        replyUsername.text = senderName
        replyMessage.text = messageData[ChatConstants.MESSAGE_TEXT_KEY]?.toString() ?: ""
    }

    fun hideReplyUI() {
        replyLayout.visibility = View.GONE
    }

    fun showDeleteMessageDialog(messageData: HashMap<String, Any>) {
        val messageKey = messageData[KEY_KEY]?.toString() ?: return
        MaterialAlertDialogBuilder(context)
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                listener.onDeleteMessageConfirmed(messageKey)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showLoadMoreIndicator() {
        listener.onShowLoadMore()
    }

    fun hideLoadMoreIndicator() {
        listener.onHideLoadMore()
    }

    interface ChatUIListener {
        fun onTypingStateChanged(isTyping: Boolean)
        fun onMessageSwiped(position: Int)
        fun onDeleteMessageConfirmed(messageKey: String)
        fun onShowLoadMore()
        fun onHideLoadMore()
    }
}