package com.synapse.social.studioasinc.chat.common.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.R
import java.util.HashMap

import com.synapse.social.studioasinc.backend.AuthenticationService
import com.synapse.social.studioasinc.backend.DatabaseService
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService
import com.synapse.social.studioasinc.backend.interfaces.IDataListener
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError

class ChatMessagesListRecyclerAdapter(
    private val context: Context,
    private val data: ArrayList<HashMap<String, Any>>,
    private val isGroup: Boolean,
    private val dbService: IDatabaseService,
    private val authService: IAuthenticationService
) : RecyclerView.Adapter<ChatMessagesListRecyclerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val v = inflater.inflate(R.layout.chat_msg_cv_synapse, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val view = holder.itemView
        val senderName = view.findViewById<TextView>(R.id.sender_name)

        // TODO: Replace Firebase calls with Supabase to fetch user information.
        // 1. Get the current user's ID from `Supabase.client.auth.currentUser.id`.
        // 2. Query the `users` table in Supabase to get the sender's profile.
        // 3. Use the retrieved data to display the sender's name.
        if (isGroup && data[position]["uid"].toString() != authService.getCurrentUser()?.uid) {
            senderName.visibility = View.VISIBLE
            val senderUid = data[position]["uid"].toString()
            dbService.getData(dbService.getReference("users/$senderUid"), object : IDataListener {
                override fun onDataChange(dataSnapshot: IDataSnapshot) {
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

                override fun onCancelled(databaseError: IDatabaseError) {
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