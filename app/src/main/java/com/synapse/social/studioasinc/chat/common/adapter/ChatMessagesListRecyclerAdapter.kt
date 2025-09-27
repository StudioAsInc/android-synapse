package com.synapse.social.studioasinc.chat.common.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.synapse.social.studioasinc.R
import java.util.HashMap

class ChatMessagesListRecyclerAdapter(
    private val context: Context,
    private val data: ArrayList<HashMap<String, Any>>,
    private val isGroup: Boolean,
    private val firebase: FirebaseDatabase
) : RecyclerView.Adapter<ChatMessagesListRecyclerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val v = inflater.inflate(R.layout.chat_msg_cv_synapse, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val view = holder.itemView
        val senderName = view.findViewById<TextView>(R.id.sender_name)

        if (isGroup && data[position]["uid"].toString() != FirebaseAuth.getInstance().currentUser?.uid) {
            senderName.visibility = View.VISIBLE
            val senderUid = data[position]["uid"].toString()
            val userRef = firebase.getReference("skyline/users").child(senderUid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val nickname = dataSnapshot.child("nickname").getValue(String::class.java)
                        val username = dataSnapshot.child("username").getValue(String::class.java)
                        senderName.text = when {
                            nickname != null && nickname != "null" -> nickname
                            username != null && username != "null" -> "@$username"
                            else -> "Unknown User"
                        }
                    } else {
                        senderName.text = "Unknown User"
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    senderName.text = "Unknown User"
                }
            })
        } else {
            senderName.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
}