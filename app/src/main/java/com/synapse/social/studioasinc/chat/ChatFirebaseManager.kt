package com.synapse.social.studioasinc.chat

import android.util.Log
import androidx.annotation.NonNull
import com.google.firebase.database.*
import com.synapse.social.studioasinc.chat.ChatConstants.KEY_KEY

class ChatFirebaseManager(
    private var chatMessagesRef: DatabaseReference,
    private val listener: ChatFirebaseListener
) {

    private var chatChildEventListener: ChildEventListener? = null
    private var oldestMessageKey: String? = null
    private var isLoading = false
    private val messageKeys = mutableSetOf<String>()

    companion object {
        private const val CHAT_PAGE_SIZE = 80
    }

    fun loadInitialMessages() {
        val getChatsMessages = chatMessagesRef.limitToLast(CHAT_PAGE_SIZE)
        getChatsMessages.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(@NonNull dataSnapshot: DataSnapshot) {
                try {
                    if (dataSnapshot.exists()) {
                        val initialMessages = ArrayList<HashMap<String, Object>>()
                        for (_data in dataSnapshot.children) {
                            val messageData =
                                _data.getValue(object :
                                    GenericTypeIndicator<HashMap<String, Object>>() {})
                            if (messageData != null && messageData.containsKey(KEY_KEY) && messageData[KEY_KEY] != null) {
                                initialMessages.add(messageData)
                                messageKeys.add(messageData[KEY_KEY].toString())
                            }
                        }

                        if (initialMessages.isNotEmpty()) {
                            initialMessages.sortBy { getMessageTimestamp(it) }
                            oldestMessageKey = initialMessages.firstOrNull()?.get(KEY_KEY)?.toString()
                            listener.onInitialMessagesLoaded(initialMessages)
                            fetchRepliedMessages(initialMessages)
                        }
                    } else {
                        listener.onNoMessagesFound()
                    }
                } catch (e: Exception) {
                    Log.e("ChatFirebaseManager", "Error processing initial chat messages: ${e.message}")
                    listener.onNoMessagesFound()
                }
            }

            override fun onCancelled(@NonNull databaseError: DatabaseError) {
                Log.e(
                    "ChatFirebaseManager",
                    "Initial message load failed: ${databaseError.message}"
                )
                listener.onNoMessagesFound()
            }
        })
    }

    fun loadOldMessages() {
        if (isLoading || oldestMessageKey == null) {
            return
        }
        isLoading = true
        listener.onLoadMoreStarted()

        val getChatsMessages = chatMessagesRef
            .orderByKey()
            .endBefore(oldestMessageKey)
            .limitToLast(CHAT_PAGE_SIZE)

        getChatsMessages.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(@NonNull dataSnapshot: DataSnapshot) {
                listener.onLoadMoreFinished()
                try {
                    if (dataSnapshot.exists()) {
                        val newMessages = ArrayList<HashMap<String, Object>>()
                        for (_data in dataSnapshot.children) {
                            val messageData =
                                _data.getValue(object :
                                    GenericTypeIndicator<HashMap<String, Object>>() {})
                            if (messageData != null && messageData.containsKey(KEY_KEY) && messageData[KEY_KEY] != null) {
                                if (messageKeys.add(messageData[KEY_KEY].toString())) {
                                    newMessages.add(messageData)
                                }
                            }
                        }

                        if (newMessages.isNotEmpty()) {
                            newMessages.sortBy { getMessageTimestamp(it) }
                            oldestMessageKey = newMessages.firstOrNull()?.get(KEY_KEY)?.toString()
                            listener.onOldMessagesLoaded(newMessages)
                            fetchRepliedMessages(newMessages)
                        } else {
                            oldestMessageKey = null // No more messages
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatFirebaseManager", "Error processing old messages: ${e.message}")
                } finally {
                    isLoading = false
                }
            }

            override fun onCancelled(@NonNull databaseError: DatabaseError) {
                listener.onLoadMoreFinished()
                isLoading = false
                Log.e("ChatFirebaseManager", "Error loading old messages: ${databaseError.message}")
            }
        })
    }

    fun attachMessagesListener() {
        if (chatChildEventListener != null) {
            detachMessagesListener()
        }

        chatChildEventListener = object : ChildEventListener {
            override fun onChildAdded(
                dataSnapshot: DataSnapshot,
                previousChildName: String?
            ) {
                val newMessage = dataSnapshot.getValue(object :
                    GenericTypeIndicator<HashMap<String, Object>>() {})
                if (newMessage != null && newMessage[KEY_KEY] != null) {
                    val messageKey = newMessage[KEY_KEY].toString()
                    if (messageKeys.add(messageKey)) {
                        listener.onNewMessageAdded(newMessage)
                        if (newMessage.containsKey("replied_message_id")) {
                            fetchRepliedMessages(arrayListOf(newMessage))
                        }
                    } else {
                        // Message already exists, likely a local one confirmed by server
                        listener.onMessageChanged(newMessage)
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val updatedMessage = snapshot.getValue(object :
                    GenericTypeIndicator<HashMap<String, Object>>() {})
                if (updatedMessage != null && updatedMessage[KEY_KEY] != null) {
                    listener.onMessageChanged(updatedMessage)
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val removedKey = snapshot.key
                if (removedKey != null) {
                    messageKeys.remove(removedKey)
                    listener.onMessageRemoved(removedKey)
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatFirebaseManager", "Chat listener cancelled: ${error.message}")
            }
        }
        chatMessagesRef.addChildEventListener(chatChildEventListener!!)
    }

    fun detachMessagesListener() {
        chatChildEventListener?.let {
            chatMessagesRef.removeEventListener(it)
        }
        chatChildEventListener = null
    }

    private fun fetchRepliedMessages(messages: List<HashMap<String, Object>>) {
        val repliedIdsToFetch = messages
            .mapNotNull { it["replied_message_id"]?.toString() }
            .filter { it != "null" }
            .distinct()

        if (repliedIdsToFetch.isEmpty()) return

        for (messageKey in repliedIdsToFetch) {
            chatMessagesRef.child(messageKey)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val repliedMessage = snapshot.getValue(object :
                                GenericTypeIndicator<HashMap<String, Object>>() {})
                            if (repliedMessage != null) {
                                listener.onRepliedMessageFetched(messageKey, repliedMessage)
                            }
                        }
                    }

                    override fun onCancelled(@NonNull error: DatabaseError) {}
                })
        }
    }

    private fun getMessageTimestamp(message: HashMap<String, Object>): Long {
        return when (val pushDateObj = message["push_date"]) {
            is Long -> pushDateObj
            is Double -> pushDateObj.toLong()
            is String -> pushDateObj.toLongOrNull() ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }
    }

    interface ChatFirebaseListener {
        fun onInitialMessagesLoaded(messages: List<HashMap<String, Object>>)
        fun onOldMessagesLoaded(messages: List<HashMap<String, Object>>)
        fun onNewMessageAdded(message: HashMap<String, Object>)
        fun onMessageChanged(message: HashMap<String, Object>)
        fun onMessageRemoved(messageKey: String)
        fun onRepliedMessageFetched(
            repliedToKey: String,
            message: HashMap<String, Object>
        )

        fun onNoMessagesFound()
        fun onLoadMoreStarted()
        fun onLoadMoreFinished()
    }
}