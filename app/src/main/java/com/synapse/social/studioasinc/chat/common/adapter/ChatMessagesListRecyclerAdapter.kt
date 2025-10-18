package com.synapse.social.studioasinc.chat.common.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.chat.model.ChatMessage

class ChatMessagesListRecyclerAdapter(
    private val context: Context,
    private var messages: List<ChatMessage>,
    private val isGroup: Boolean
) : RecyclerView.Adapter<ChatMessagesListRecyclerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val v = inflater.inflate(R.layout.chat_msg_cv_synapse, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        val view = holder.itemView
        val senderName = view.findViewById<TextView>(R.id.sender_name)
        val messageText = view.findViewById<TextView>(R.id.message_text)

        messageText.text = message.message

        if (isGroup && message.uid != FirebaseAuth.getInstance().currentUser?.uid) {
            senderName.visibility = View.VISIBLE
            val user = message.user
            senderName.text = when {
                user != null && user.nickname.isNotEmpty() && user.nickname != "null" -> user.nickname
                user != null && user.username.isNotEmpty() -> "@${user.username}"
                else -> "Unknown User"
            }
        } else {
            senderName.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    fun updateMessages(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
}
