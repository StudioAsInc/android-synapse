package com.synapse.social.studioasinc

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.chat.interfaces.ChatAdapterListener
import com.synapse.social.studioasinc.chat.interfaces.ChatMessage
import com.synapse.social.studioasinc.chat.models.MessageState
import com.synapse.social.studioasinc.chat.models.MessageType
import java.text.SimpleDateFormat
import java.util.*

class ChatMessagesAdapter(
    private val messages: List<ChatMessage>,
    private val listener: ChatAdapterListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val authService = SupabaseAuthenticationService()
    private val currentUserId = authService.getCurrentUserId()

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_SYSTEM = 3
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.messageType == MessageType.SYSTEM -> VIEW_TYPE_SYSTEM
            message.senderId == currentUserId -> VIEW_TYPE_SENT
            else -> VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)
                ReceivedMessageViewHolder(view)
            }
            VIEW_TYPE_SYSTEM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_system, parent, false)
                SystemMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
            is SystemMessageViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    // Sent Message ViewHolder
    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageLayout: LinearLayout = itemView.findViewById(R.id.messageLayout)
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        private val statusImageView: ImageView = itemView.findViewById(R.id.statusImageView)
        private val replyLayout: LinearLayout? = itemView.findViewById(R.id.replyLayout)
        private val replyTextView: TextView? = itemView.findViewById(R.id.replyTextView)

        fun bind(message: ChatMessage) {
            messageTextView.text = message.messageText
            timeTextView.text = formatTime(message.pushDate)
            
            // Set message background
            val background = createMessageBackground(0xFF2196F3.toInt(), true)
            messageLayout.background = background
            
            // Set text color for sent messages
            messageTextView.setTextColor(Color.WHITE)
            timeTextView.setTextColor(0xFFE3F2FD.toInt())
            
            // Set status icon
            when (message.messageState) {
                MessageState.SENDING -> {
                    statusImageView.setImageResource(R.drawable.ic_message_sending)
                    statusImageView.visibility = View.VISIBLE
                }
                MessageState.SENT -> {
                    statusImageView.setImageResource(R.drawable.ic_message_sent)
                    statusImageView.visibility = View.VISIBLE
                }
                MessageState.DELIVERED -> {
                    statusImageView.setImageResource(R.drawable.ic_message_delivered)
                    statusImageView.visibility = View.VISIBLE
                }
                MessageState.READ -> {
                    statusImageView.setImageResource(R.drawable.ic_message_read)
                    statusImageView.visibility = View.VISIBLE
                }
                else -> {
                    statusImageView.visibility = View.GONE
                }
            }
            
            // Handle reply
            if (message.repliedMessageId != null && replyLayout != null && replyTextView != null) {
                replyLayout.visibility = View.VISIBLE
                replyTextView.text = "Replied to a message" // TODO: Load actual replied message
            } else {
                replyLayout?.visibility = View.GONE
            }
            
            // Set click listeners
            itemView.setOnClickListener {
                listener.onMessageClick(message.id, adapterPosition)
            }
            
            itemView.setOnLongClickListener {
                listener.onMessageLongClick(message.id, adapterPosition)
            }
        }
    }

    // Received Message ViewHolder
    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageLayout: LinearLayout = itemView.findViewById(R.id.messageLayout)
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        private val senderNameTextView: TextView? = itemView.findViewById(R.id.senderNameTextView)
        private val replyLayout: LinearLayout? = itemView.findViewById(R.id.replyLayout)
        private val replyTextView: TextView? = itemView.findViewById(R.id.replyTextView)

        fun bind(message: ChatMessage) {
            messageTextView.text = message.messageText
            timeTextView.text = formatTime(message.pushDate)
            
            // Set message background
            val background = createMessageBackground(0xFFF5F5F5.toInt(), false)
            messageLayout.background = background
            
            // Set text color for received messages
            messageTextView.setTextColor(0xFF212121.toInt())
            timeTextView.setTextColor(0xFF757575.toInt())
            
            // Set sender name (for group chats)
            senderNameTextView?.let { nameView ->
                // TODO: Load sender name from user data
                nameView.text = "User" // Placeholder
                nameView.visibility = View.VISIBLE
            }
            
            // Handle reply
            if (message.repliedMessageId != null && replyLayout != null && replyTextView != null) {
                replyLayout.visibility = View.VISIBLE
                replyTextView.text = "Replied to a message" // TODO: Load actual replied message
            } else {
                replyLayout?.visibility = View.GONE
            }
            
            // Set click listeners
            itemView.setOnClickListener {
                listener.onMessageClick(message.id, adapterPosition)
            }
            
            itemView.setOnLongClickListener {
                listener.onMessageLongClick(message.id, adapterPosition)
            }
        }
    }

    // System Message ViewHolder
    inner class SystemMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)

        fun bind(message: ChatMessage) {
            messageTextView.text = message.messageText
        }
    }

    private fun createMessageBackground(color: Int, isSent: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadii = if (isSent) {
                floatArrayOf(20f, 20f, 20f, 20f, 20f, 20f, 5f, 5f) // Rounded except bottom-right
            } else {
                floatArrayOf(20f, 20f, 20f, 20f, 5f, 5f, 20f, 20f) // Rounded except bottom-left
            }
        }
    }

    private fun formatTime(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        
        val now = Calendar.getInstance()
        
        return if (isSameDay(calendar, now)) {
            // Same day - show time only
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        } else if (isYesterday(calendar, now)) {
            // Yesterday
            "Yesterday ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))}"
        } else {
            // Older - show date and time
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(messageTime: Calendar, now: Calendar): Boolean {
        val yesterday = Calendar.getInstance()
        yesterday.timeInMillis = now.timeInMillis
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        
        return isSameDay(messageTime, yesterday)
    }
}