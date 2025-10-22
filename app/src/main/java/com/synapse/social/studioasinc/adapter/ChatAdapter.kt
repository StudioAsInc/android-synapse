package com.synapse.social.studioasinc.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.model.Message
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying chat messages
 */
class ChatAdapter(
    private val context: Context,
    private val onMessageLongClick: (Message) -> Unit = {}
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    private val authService = SupabaseAuthenticationService()
    private val currentUserId = authService.getCurrentUserId()

    companion object {
        private const val VIEW_TYPE_MESSAGE_SENT = 1
        private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_MESSAGE_SENT
        } else {
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_MESSAGE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_2, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_MESSAGE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_2, parent, false)
                ReceivedMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(android.R.id.text1)
        private val timeText: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(message: Message) {
            messageText.text = message.getDisplayContent()
            timeText.text = formatTime(message.createdAt)
            
            itemView.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(android.R.id.text1)
        private val timeText: TextView = itemView.findViewById(android.R.id.text2)
        private val senderName: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(message: Message) {
            messageText.text = "${message.senderName ?: "Unknown"}: ${message.getDisplayContent()}"
            timeText.text = formatTime(message.createdAt)
            
            itemView.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}