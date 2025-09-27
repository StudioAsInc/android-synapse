package com.synapse.social.studioasinc.util

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.synapse.social.studioasinc.ChatAdapter
import com.synapse.social.studioasinc.ChatUIUpdater

class DatabaseHelper(
    private val context: Context,
    private val firebaseDatabase: FirebaseDatabase,
    private val chatAdapter: ChatAdapter?,
    private var firstUserName: String,
    private val chatUIUpdater: ChatUIUpdater,
    private val chatMessagesList: ArrayList<HashMap<String, Any>>,
    private val messageKeys: MutableSet<String>,
    private var oldestMessageKey: String?,
    private val chatMessagesRef: DatabaseReference,
    private val onMessagesLoaded: () -> Unit
) {

    fun getUserReference() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val getFirstUserNameRef = firebaseDatabase.getReference("skyline/users").child(currentUserUid)

        getFirstUserNameRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                try {
                    if (dataSnapshot.exists()) {
                        val nickname = dataSnapshot.child("nickname").getValue(String::class.java)
                        val username = dataSnapshot.child("username").getValue(String::class.java)

                        firstUserName = when {
                            nickname != null && nickname != "null" -> nickname
                            username != null && username != "null" -> "@$username"
                            else -> "Unknown User"
                        }
                    } else {
                        firstUserName = "Unknown User"
                    }
                    chatAdapter?.setFirstUserName(firstUserName)
                } catch (e: Exception) {
                    firstUserName = "Unknown User"
                }
                getChatMessagesRef()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                firstUserName = "Unknown User"
                getChatMessagesRef()
            }
        })
    }

    fun getChatMessagesRef() {
        val getChatsMessagesQuery = chatMessagesRef.limitToLast(80)

        getChatsMessagesQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                try {
                    if (dataSnapshot.exists()) {
                        chatUIUpdater.updateNoChatVisibility(false)
                        chatMessagesList.clear()
                        messageKeys.clear()
                        val initialMessages = ArrayList<HashMap<String, Any>>()

                        for (data in dataSnapshot.children) {
                            try {
                                val messageData = data.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})
                                if (messageData != null && messageData["key"] != null) {
                                    initialMessages.add(messageData)
                                    messageKeys.add(messageData["key"].toString())
                                }
                            } catch (e: Exception) {
                                // Log error
                            }
                        }

                        if (initialMessages.isNotEmpty()) {
                            initialMessages.sortBy { getMessageTimestamp(it) }
                            oldestMessageKey = initialMessages.firstOrNull()?.get("key")?.toString()
                            chatMessagesList.addAll(initialMessages)
                            chatAdapter?.notifyDataSetChanged()
                            onMessagesLoaded.invoke()
                        }
                    } else {
                        chatUIUpdater.updateNoChatVisibility(true)
                    }
                } catch (e: Exception) {
                    chatUIUpdater.updateNoChatVisibility(true)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                chatUIUpdater.updateNoChatVisibility(true)
            }
        })
    }

    private fun getMessageTimestamp(message: HashMap<String, Any>): Long {
        return when (val pushDateObj = message["push_date"]) {
            is Long -> pushDateObj
            is Double -> pushDateObj.toLong()
            is String -> pushDateObj.toLongOrNull() ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }
    }
}