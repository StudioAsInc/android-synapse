package com.synapse.social.studioasinc.util

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.ChatAdapter
import com.synapse.social.studioasinc.ChatUIUpdater
import java.util.ArrayList
import java.util.HashMap

class DatabaseHelper(
    private val context: Context,
    private val dbService: IDatabaseService,
    private val authService: IAuthenticationService,
    private val chatAdapter: ChatAdapter?,
    private var firstUserName: String,
    private val chatUIUpdater: ChatUIUpdater,
    private val chatMessagesList: ArrayList<HashMap<String, Any>>,
    private val messageKeys: MutableSet<String>,
    private var oldestMessageKey: String?,
    // TODO(supabase): Replace with Supabase Realtime channel or PostgREST query
    /* private val chatMessagesRef: DatabaseReference, */
    private val recyclerView: RecyclerView,
    private val repliedMessagesCache: HashMap<String, HashMap<String, Any>>,
    private val onMessagesLoaded: () -> Unit
) {

    /* private var chatChildListener: Any? = null // TODO(supabase): Replace with Supabase Realtime listener */
    private var isLoading = false

    companion object {
        private const val TAG = "DatabaseHelper"
    }

    fun getUserReference() {
        val currentUserUid = authService.getCurrentUser()?.uid
        if (currentUserUid == null) return

        dbService.getData(dbService.getReference("users/$currentUserUid"), object : IDataListener {
            override fun onDataChange(snapshot: IDataSnapshot) {
                if(snapshot.exists()){
                    val nickname = snapshot.child("nickname").getValue(String::class.java)
                    val username = snapshot.child("username").getValue(String::class.java)

                    firstUserName = when {
                        nickname != null && nickname != "null" -> nickname
                        username != null && username != "null" -> "@$username"
                        else -> "Unknown User"
                    }
                } else {
                    firstUserName = "Unknown User"
                }
                chatAdapter?.setFirstUserName(firstUserName)
                getChatMessagesRef()
            }

            override fun onCancelled(error: IDatabaseError) {
                firstUserName = "Unknown User"
                Log.e(TAG, "Failed to get user reference", error.toException())
                getChatMessagesRef()
            }
        })
    }

    fun getChatMessagesRef() {
        dbService.getData(dbService.getReference("chat_messages").orderByChild("timestamp").limitToLast(80), object : IDataListener {
            override fun onDataChange(snapshot: IDataSnapshot) {
                if (snapshot.exists()) {
                    chatUIUpdater.updateNoChatVisibility(false)
                    chatMessagesList.clear()
                    messageKeys.clear()
                    val initialMessages = ArrayList<HashMap<String, Any>>()

                    for (messageSnapshot in snapshot.children) {
                        try {
                            val messageMap = messageSnapshot.value as HashMap<String, Any>
                            if (messageMap["key"] != null) {
                                initialMessages.add(messageMap)
                                messageKeys.add(messageMap["key"].toString())
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing initial message data", e)
                        }
                    }

                    if (initialMessages.isNotEmpty()) {
                        initialMessages.sortBy { getMessageTimestamp(it) }
                        oldestMessageKey = initialMessages.firstOrNull()?.get("key")?.toString()
                        chatMessagesList.addAll(initialMessages)
                        chatAdapter?.notifyDataSetChanged()
                        fetchRepliedMessages(initialMessages)
                        onMessagesLoaded.invoke()
                    }
                } else {
                    chatUIUpdater.updateNoChatVisibility(true)
                }
            }

            override fun onCancelled(error: IDatabaseError) {
                Log.e(TAG, "Initial message load failed", error.toException())
                chatUIUpdater.updateNoChatVisibility(true)
            }
        })
    }

    fun getOldChatMessagesRef() {
        if (isLoading || oldestMessageKey == null || oldestMessageKey!!.isEmpty() || oldestMessageKey == "null") {
            return
        }
        isLoading = true
        chatUIUpdater.showLoadMoreIndicator()

        dbService.getData(dbService.getReference("chat_messages").orderByChild("key").endAt(oldestMessageKey).limitToLast(80), object : IDataListener {
            override fun onDataChange(snapshot: IDataSnapshot) {
                chatUIUpdater.hideLoadMoreIndicator()
                if (snapshot.exists()) {
                    val newMessages = ArrayList<HashMap<String, Any>>()
                    for (messageSnapshot in snapshot.children) {
                        try {
                            val messageMap = messageSnapshot.value as HashMap<String, Any>
                            if (messageMap["key"] != null) {
                                if (!messageKeys.contains(messageMap["key"].toString())) {
                                    newMessages.add(messageMap)
                                    messageKeys.add(messageMap["key"].toString())
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing old message data", e)
                        }
                    }

                    if (newMessages.isNotEmpty()) {
                        newMessages.sortBy { getMessageTimestamp(it) }
                        oldestMessageKey = newMessages.firstOrNull()?.get("key")?.toString()

                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                        val firstVisibleView = layoutManager.findViewByPosition(firstVisiblePosition)
                        val topOffset = firstVisibleView?.top ?: 0

                        chatMessagesList.addAll(0, newMessages)
                        chatAdapter?.notifyItemRangeInserted(0, newMessages.size)

                        if (firstVisibleView != null) {
                            layoutManager.scrollToPositionWithOffset(firstVisiblePosition + newMessages.size, topOffset)
                        }
                        fetchRepliedMessages(newMessages)
                    } else {
                        oldestMessageKey = null
                    }
                } else {
                    oldestMessageKey = null
                }
                isLoading = false
            }

            override fun onCancelled(error: IDatabaseError) {
                isLoading = false
                chatUIUpdater.hideLoadMoreIndicator()
                Log.e(TAG, "Error loading old messages", error.toException())
            }
        })
    }

    fun fetchRepliedMessages(messages: ArrayList<HashMap<String, Any>>) {
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

            dbService.getData(dbService.getReference("chat_messages/$messageKey"), object : IDataListener {
                override fun onDataChange(snapshot: IDataSnapshot) {
                    if (snapshot.exists()) {
                        val repliedMessage = snapshot.value as HashMap<String, Any>
                        repliedMessagesCache[messageKey] = repliedMessage
                        updateMessageInRecyclerView(messageKey)
                    }
                }

                override fun onCancelled(error: IDatabaseError) {
                    repliedMessagesCache.remove(messageKey)
                    Log.e(TAG, "Failed to fetch replied message", error.toException())
                }
            })
        }
    }

    private fun updateMessageInRecyclerView(repliedMessageKey: String) {
        val activity = context as? Activity
        if (chatAdapter == null || activity == null || activity.isFinishing || activity.isDestroyed) return
        for (i in chatMessagesList.indices) {
            val message = chatMessagesList[i]
            if (message.containsKey("replied_message_id") && repliedMessageKey == message["replied_message_id"].toString()) {
                val positionToUpdate = i
                activity.runOnUiThread {
                    if (positionToUpdate < chatAdapter!!.itemCount) {
                        chatAdapter.notifyItemChanged(positionToUpdate)
                    }
                }
            }
        }
    }

    fun attachChatListener() {
        if (chatChildListener != null) {
            detachChatListener()
        }

        chatChildListener = dbService.getReference("chat_messages").addChildEventListener(object : IChildEventListener {
            override fun onChildAdded(snapshot: IDataSnapshot, previousChildName: String?) {
                val newMessage = snapshot.value as? HashMap<String, Any>
                if (newMessage != null) {
                    handleChildAdded(newMessage)
                }
            }

            override fun onChildChanged(snapshot: IDataSnapshot, previousChildName: String?) {
                val updatedMessage = snapshot.value as? HashMap<String, Any>
                if (updatedMessage != null) {
                    handleChildChanged(updatedMessage)
                }
            }

            override fun onChildRemoved(snapshot: IDataSnapshot) {
                val removedMessage = snapshot.value as? HashMap<String, Any>
                if (removedMessage != null) {
                    handleChildRemoved(removedMessage)
                }
            }

            override fun onChildMoved(snapshot: IDataSnapshot, previousChildName: String?) {
                // Not implemented
            }

            override fun onCancelled(error: IDatabaseError) {
                Log.e(TAG, "Chat listener cancelled", error.toException())
            }
        })
    }

    fun detachChatListener() {
        if (chatChildListener != null) {
            dbService.getReference("chat_messages").removeEventListener(chatChildListener as IChildEventListener)
            chatChildListener = null
        }
    }

    private fun handleChildAdded(newMessage: HashMap<String, Any>) {
        if (newMessage["key"] != null) {
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
                    val singleMessageList = ArrayList<HashMap<String, Any>>()
                    singleMessageList.add(newMessage)
                    fetchRepliedMessages(singleMessageList)
                }
            }
        }
    }

    private fun handleChildChanged(updatedMessage: HashMap<String, Any>) {
        if (updatedMessage["key"] != null) {
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

    private fun handleChildRemoved(removedMessage: HashMap<String, Any>) {
        if (removedMessage["key"] != null) {
            val removedKey = removedMessage["key"].toString()
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

    private fun findCorrectInsertPosition(newMessage: HashMap<String, Any>): Int {
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

    private fun getMessageTimestamp(message: HashMap<String, Any>): Long {
        return when (val pushDateObj = message["push_date"]) {
            is Long -> pushDateObj
            is Double -> pushDateObj.toLong()
            is String -> pushDateObj.toLongOrNull() ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }
    }
}