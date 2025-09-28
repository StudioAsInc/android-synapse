package com.synapse.social.studioasinc.util

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService
import com.synapse.social.studioasinc.ChatAdapter
import com.synapse.social.studioasinc.ChatUIUpdater
import com.synapse.social.studioasinc.backend.interfaces.*
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
    private val chatMessagesRef: IDatabaseReference,
    private val recyclerView: RecyclerView,
    private val repliedMessagesCache: HashMap<String, HashMap<String, Any>>,
    private val onMessagesLoaded: () -> Unit
) {

    private var isLoading = false
    private var realtimeChannel: IRealtimeChannel? = null

    companion object {
        private const val TAG = "DatabaseHelper"
    }

    fun getUserReference() {
        val currentUserUid = authService.getCurrentUser()?.getUid() ?: return
        val getFirstUserNameRef = dbService.getReference("skyline/users").child(currentUserUid)

        dbService.getData(getFirstUserNameRef, object : IDataListener {
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                try {
                    if (dataSnapshot.exists()) {
                        val userList = dataSnapshot.getValue(List::class.java) as? List<Map<String, Any?>>
                        val user = userList?.firstOrNull()
                        if (user != null) {
                            val nickname = user["nickname"] as? String
                            val username = user["username"] as? String
                            firstUserName = when {
                                nickname != null && nickname != "null" -> nickname
                                username != null && username != "null" -> "@$username"
                                else -> "Unknown User"
                            }
                        } else {
                            firstUserName = "Unknown User"
                        }
                    } else {
                        firstUserName = "Unknown User"
                    }
                    chatAdapter?.setFirstUserName(firstUserName)
                } catch (e: Exception) {
                    firstUserName = "Unknown User"
                    Log.e(TAG, "Error processing user data", e)
                }
                getChatMessagesRef()
            }

            override fun onCancelled(databaseError: IDatabaseError) {
                firstUserName = "Unknown User"
                Log.e(TAG, "Failed to get user reference", Exception(databaseError.message))
                getChatMessagesRef()
            }
        })
    }

    fun getChatMessagesRef() {
        val getChatsMessagesQuery = chatMessagesRef.limitToLast(80)

        dbService.getData(getChatsMessagesQuery, object : IDataListener {
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                try {
                    if (dataSnapshot.exists()) {
                        chatUIUpdater.updateNoChatVisibility(false)
                        chatMessagesList.clear()
                        messageKeys.clear()
                        val initialMessages = ArrayList<HashMap<String, Any>>()
                        val messagesList = dataSnapshot.getValue(List::class.java) as? List<Map<String, Any>>

                        messagesList?.forEach { messageMap ->
                            try {
                                val mutableMessageData = HashMap(messageMap)
                                if (mutableMessageData["key"] != null) {
                                    initialMessages.add(mutableMessageData)
                                    messageKeys.add(mutableMessageData["key"].toString())
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
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing initial chat messages", e)
                    chatUIUpdater.updateNoChatVisibility(true)
                }
            }

            override fun onCancelled(databaseError: IDatabaseError) {
                Log.e(TAG, "Initial message load failed", Exception(databaseError.message))
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

        val getChatsMessagesQuery = chatMessagesRef
            .orderByKey()
            .endBefore(oldestMessageKey)
            .limitToLast(80)

        dbService.getData(getChatsMessagesQuery, object : IDataListener {
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                chatUIUpdater.hideLoadMoreIndicator()
                try {
                    if (dataSnapshot.exists()) {
                        val newMessages = ArrayList<HashMap<String, Any>>()
                        val messagesList = dataSnapshot.getValue(List::class.java) as? List<Map<String, Any>>
                        messagesList?.forEach { messageMap ->
                            try {
                                val mutableMessageData = HashMap(messageMap)
                                if (mutableMessageData["key"] != null) {
                                    if (!messageKeys.contains(mutableMessageData["key"].toString())) {
                                        newMessages.add(mutableMessageData)
                                        messageKeys.add(mutableMessageData["key"].toString())
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
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing old messages", e)
                } finally {
                    isLoading = false
                }
            }

            override fun onCancelled(databaseError: IDatabaseError) {
                isLoading = false
                chatUIUpdater.hideLoadMoreIndicator()
                Log.e(TAG, "Error loading old messages", Exception(databaseError.message))
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

            dbService.getData(chatMessagesRef.child(messageKey), object : IDataListener {
                override fun onDataChange(snapshot: IDataSnapshot) {
                    if (snapshot.exists()) {
                        val repliedMessage = snapshot.getValue(HashMap::class.java) as HashMap<String, Any>
                        if (repliedMessage != null) {
                            repliedMessagesCache[messageKey] = repliedMessage
                            updateMessageInRecyclerView(messageKey)
                        }
                    }
                }

                override fun onCancelled(error: IDatabaseError) {
                    repliedMessagesCache.remove(messageKey)
                    Log.e(TAG, "Failed to fetch replied message", Exception(error.message))
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
        if (realtimeChannel != null) {
            detachChatListener()
        }

        realtimeChannel = dbService.addRealtimeListener(chatMessagesRef, object : IRealtimeListener {
            override fun onInsert(snapshot: IDataSnapshot) {
                handleChildAdded(snapshot)
            }

            override fun onUpdate(snapshot: IDataSnapshot) {
                handleChildChanged(snapshot)
            }

            override fun onDelete(snapshot: IDataSnapshot) {
                handleChildRemoved(snapshot)
            }
        })
    }

    fun detachChatListener() {
        if (realtimeChannel != null) {
            dbService.removeRealtimeListener(realtimeChannel!!)
            realtimeChannel = null
        }
    }

    private fun handleChildAdded(dataSnapshot: IDataSnapshot) {
        if (dataSnapshot.exists()) {
            val newMessage = dataSnapshot.getValue(HashMap::class.java) as HashMap<String, Any>
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
                        val singleMessageList = ArrayList<HashMap<String, Any>>()
                        singleMessageList.add(newMessage)
                        fetchRepliedMessages(singleMessageList)
                    }
                }
            }
        }
    }

    private fun handleChildChanged(snapshot: IDataSnapshot) {
        if (snapshot.exists()) {
            val updatedMessage = snapshot.getValue(HashMap::class.java) as HashMap<String, Any>
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

    private fun handleChildRemoved(snapshot: IDataSnapshot) {
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