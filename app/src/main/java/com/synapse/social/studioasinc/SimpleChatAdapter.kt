package com.synapse.social.studioasinc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.gotrue.auth
import java.text.SimpleDateFormat
import java.util.*

class SimpleChatAdapter(
    private val messages: ArrayList<HashMap<String, Any?>>
) : RecyclerView.Adapter<SimpleChatAdapter.MessageViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_text)
        val messageTime: TextView? = itemView.findViewById(R.id.date)
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        val senderId = message["uid"]?.toString() ?: message["sender_id"]?.toString()
        val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id
        
        return if (senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_SENT -> R.layout.chat_bubble_text
            VIEW_TYPE_RECEIVED -> R.layout.chat_bubble_text
            else -> R.layout.chat_bubble_text
        }
        
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        
        // Set message text
        val messageText = message["message_text"]?.toString() ?: ""
        holder.messageText.text = messageText
        
        // Set message time
        val timestamp = message["push_date"] as? Long ?: System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.messageTime?.text = dateFormat.format(Date(timestamp))
    }

    override fun getItemCount(): Int = messages.size
}