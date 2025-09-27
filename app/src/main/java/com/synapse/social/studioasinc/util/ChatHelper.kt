package com.synapse.social.studioasinc.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.synapse.social.studioasinc.ChatActivity
import com.synapse.social.studioasinc.ChatAdapter
import java.util.ArrayList
import java.util.HashMap

class ChatHelper(
    private val context: Context,
    private val chatMessagesRef: DatabaseReference,
    private val chatAdapter: ChatAdapter?,
    private val chatMessagesList: ArrayList<HashMap<String, Object>>,
    private val repliedMessagesCache: HashMap<String, HashMap<String, Object>>,
    private val messageKeys: MutableSet<String>,
    private val recyclerView: RecyclerView
) {

    private var chatChildListener: ChildEventListener? = null

    fun onBackPressed() {
        val activity = context as Activity
        val intent = activity.intent
        if (intent.hasExtra("ORIGIN_KEY")) {
            val originSimpleName = intent.getStringExtra("ORIGIN_KEY")
            if (originSimpleName != null && !originSimpleName.equals("null") && !originSimpleName.trim().isEmpty()) {
                try {
                    val packageName = "com.synapse.social.studioasinc"
                    val fullClassName = "$packageName.${originSimpleName.trim()}"
                    val clazz = Class.forName(fullClassName)

                    val newIntent = Intent(context, clazz)
                    if ("ProfileActivity" == originSimpleName.trim()) {
                        if (intent.hasExtra("uid")) {
                            newIntent.putExtra("uid", intent.getStringExtra("uid"))
                        } else {
                            Toast.makeText(context, "Error: UID is required for ProfileActivity", Toast.LENGTH_SHORT).show()
                            activity.finish()
                            return
                        }
                    }
                    context.startActivity(newIntent)
                    activity.finish()
                    return
                } catch (e: ClassNotFoundException) {
                    Log.e("ChatActivity", "Activity class not found: $originSimpleName", e)
                    Toast.makeText(context, "Error: Activity not found", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("ChatActivity", "Failed to start activity: $originSimpleName", e)
                    Toast.makeText(context, "Error: Failed to start activity", Toast.LENGTH_SHORT).show()
                }
            }
        }
        activity.finish()
    }

    fun fetchRepliedMessages(messages: ArrayList<HashMap<String, Object>>) {
        val repliedIdsToFetch = HashSet<String>()
        for (message in messages) {
            if (message.containsKey("replied_message_id")) {
                val repliedId = message["replied_message_id"].toString()
                if (repliedId != "null" && !repliedMessagesCache.containsKey(repliedId)) {
                    repliedIdsToFetch.add(repliedId)
                }
            }
        }

        if (repliedIdsToFetch.isEmpty()) {
            return
        }

        for (messageKey in repliedIdsToFetch) {
            repliedMessagesCache[messageKey] = HashMap()

            chatMessagesRef.child(messageKey).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val repliedMessage = snapshot.getValue(object : GenericTypeIndicator<HashMap<String, Object>>() {})
                        if (repliedMessage != null) {
                            repliedMessagesCache[messageKey] = repliedMessage
                            updateMessageInRecyclerView(messageKey)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    repliedMessagesCache.remove(messageKey)
                }
            })
        }
    }

    private fun updateMessageInRecyclerView(repliedMessageKey: String) {
        if (chatAdapter == null || (context as Activity).isFinishing || context.isDestroyed) return
        for (i in chatMessagesList.indices) {
            val message = chatMessagesList[i]
            if (message.containsKey("replied_message_id") && repliedMessageKey == message["replied_message_id"].toString()) {
                val positionToUpdate = i
                (context as Activity).runOnUiThread {
                    if (chatAdapter != null && positionToUpdate < chatAdapter.itemCount) {
                        chatAdapter.notifyItemChanged(positionToUpdate)
                    }
                }
            }
        }
    }

    fun attachChatListener() {
        if (chatChildListener != null) {
            chatMessagesRef.removeEventListener(chatChildListener!!)
            chatChildListener = null
        }

        chatChildListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                if (dataSnapshot.exists()) {
                    val newMessage = dataSnapshot.getValue(object : GenericTypeIndicator<HashMap<String, Object>>() {})
                    if (newMessage != null && newMessage["key"] != null) {
                        val messageKey = newMessage["key"].toString()
                        if (!messageKeys.contains(messageKey)) {
                            messageKeys.add(messageKey)
                            val insertPosition = findCorrectInsertPosition(newMessage)
                            chatMessagesList.add(insertPosition, newMessage)
                            chatAdapter?.notifyItemInserted(insertPosition)
                            if (insertPosition > 0) chatAdapter?.notifyItemChanged(insertPosition - 1)
                            if (insertPosition < chatMessagesList.size - 1) chatAdapter?.notifyItemChanged(insertPosition + 1)
                            if (insertPosition == chatMessagesList.size - 1) {
                                recyclerView.post { recyclerView.smoothScrollToPosition(chatMessagesList.size - 1) }
                            }
                            if (newMessage.containsKey("replied_message_id")) {
                                val singleMessageList = ArrayList<HashMap<String, Object>>()
                                singleMessageList.add(newMessage)
                                fetchRepliedMessages(singleMessageList)
                            }
                        }
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.exists()) {
                    val updatedMessage = snapshot.getValue(object : GenericTypeIndicator<HashMap<String, Object>>() {})
                    if (updatedMessage != null && updatedMessage["key"] != null) {
                        val key = updatedMessage["key"].toString()
                        for (i in chatMessagesList.indices) {
                            if (chatMessagesList[i]["key"] != null && chatMessagesList[i]["key"].toString() == key) {
                                chatMessagesList[i] = updatedMessage
                                chatAdapter?.notifyItemChanged(i)
                                break
                            }
                        }
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val removedKey = snapshot.key
                    if (removedKey != null) {
                        for (i in chatMessagesList.indices) {
                            if (chatMessagesList[i]["key"] != null && chatMessagesList[i]["key"].toString() == removedKey) {
                                chatMessagesList.removeAt(i)
                                messageKeys.remove(removedKey)
                                chatAdapter?.notifyItemRemoved(i)
                                if (chatMessagesList.isNotEmpty() && i < chatMessagesList.size) {
                                    chatAdapter?.notifyItemChanged(Math.min(i, chatMessagesList.size - 1))
                                }
                                break
                            }
                        }
                    }
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatHelper", "Chat listener cancelled: " + error.message)
            }
        }
        chatMessagesRef.addChildEventListener(chatChildListener!!)
    }

    fun detachChatListener() {
        if (chatChildListener != null) {
            chatMessagesRef.removeEventListener(chatChildListener!!)
            chatChildListener = null
        }
    }

    private fun findCorrectInsertPosition(newMessage: HashMap<String, Object>): Int {
        if (chatMessagesList.isEmpty()) {
            return 0
        }
        val newMessageTime = getMessageTimestamp(newMessage)
        for (i in chatMessagesList.indices) {
            val existingMessageTime = getMessageTimestamp(chatMessagesList[i])
            if (newMessageTime <= existingMessageTime) {
                return i
            }
        }
        return chatMessagesList.size
    }

    private fun getMessageTimestamp(message: HashMap<String, Object>): Long {
        return try {
            when (val pushDateObj = message["push_date"]) {
                is Long -> pushDateObj
                is Double -> pushDateObj.toLong()
                is String -> pushDateObj.toLong()
                else -> System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}