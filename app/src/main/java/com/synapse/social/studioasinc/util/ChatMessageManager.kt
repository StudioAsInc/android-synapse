package com.synapse.social.studioasinc.util

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.auth.FirebaseAuth

class ChatMessageManager(
    private val chatMessagesRef: DatabaseReference,
    private val listener: ChatMessageListener
) {

    interface ChatMessageListener {
        fun onInitialMessagesLoaded(messages: List<HashMap<String, Any>>, oldestMessageKey: String?)
        fun onOlderMessagesLoaded(messages: List<HashMap<String, Any>>, oldestMessageKey: String?)
        fun onMessageAdded(message: HashMap<String, Any>)
        fun onMessageChanged(message: HashMap<String, Any>)
        fun onMessageRemoved(messageKey: String)
        fun onNoMessages()
        fun onError(error: String)
        fun onRepliedMessageFetched(repliedMessage: HashMap<String, Any>, originalMessageKey: String)
    }

    private var chatListener: ChildEventListener? = null
    private var isLoading = false
    private var oldestMessageKey: String? = null

    companion object {
        private val firebaseDatabase = FirebaseDatabase.getInstance()
        private val auth = FirebaseAuth.getInstance()

        private const val SKYLINE_REF = "skyline"
        private const val CHATS_REF = "chats"
        private const val USER_CHATS_REF = "user-chats"
        private const val GROUP_CHATS_REF = "group-chats"
        private const val INBOX_REF = "inbox"

        private const val CHAT_ID_KEY = "chatID"
        private const val UID_KEY = "uid"
        private const val LAST_MESSAGE_UID_KEY = "last_message_uid"
        private const val LAST_MESSAGE_TEXT_KEY = "last_message_text"
        private const val LAST_MESSAGE_STATE_KEY = "last_message_state"
        private const val PUSH_DATE_KEY = "push_date"
        private const val KEY_KEY = "key"

        fun getChatId(uid1: String?, uid2: String?): String {
            if (uid1 == null || uid2 == null) {
                return ""
            }
            return if (uid1.compareTo(uid2) > 0) {
                uid1 + uid2
            } else {
                uid2 + uid1
            }
        }
    }

    fun loadInitialMessages() {
        val getChatsMessages = chatMessagesRef.limitToLast(CHAT_PAGE_SIZE)
        getChatsMessages.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val initialMessages = mutableListOf<HashMap<String, Any>>()
                    for (data in dataSnapshot.children) {
                        try {
                            val messageData = data.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})
                            if (messageData != null && messageData[KEY_KEY] != null) {
                                initialMessages.add(messageData)
                            }
                        } catch (e: Exception) {
                            listener.onError(e.message ?: "Error processing initial message data.")
                        }
                    }

                    if (initialMessages.isNotEmpty()) {
                        initialMessages.sortBy { getMessageTimestamp(it) }
                        oldestMessageKey = initialMessages.firstOrNull()?.get(KEY_KEY)?.toString()
                        listener.onInitialMessagesLoaded(initialMessages, oldestMessageKey)
                    } else {
                        listener.onNoMessages()
                    }
                } else {
                    listener.onNoMessages()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                listener.onError(databaseError.message)
            }
        })
    }

    fun loadOlderMessages() {
        if (isLoading || oldestMessageKey == null) {
            return
        }
        isLoading = true

        val getChatsMessages = chatMessagesRef.orderByKey().endBefore(oldestMessageKey).limitToLast(CHAT_PAGE_SIZE)
        getChatsMessages.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val newMessages = mutableListOf<HashMap<String, Any>>()
                    for (data in dataSnapshot.children) {
                        val messageData = data.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})
                        if (messageData != null && messageData[KEY_KEY] != null) {
                            newMessages.add(messageData)
                        }
                    }

                    if (newMessages.isNotEmpty()) {
                        newMessages.sortBy { getMessageTimestamp(it) }
                        oldestMessageKey = newMessages.firstOrNull()?.get(KEY_KEY)?.toString()
                        listener.onOlderMessagesLoaded(newMessages, oldestMessageKey)
                    }
                }
                isLoading = false
            }

            override fun onCancelled(databaseError: DatabaseError) {
                isLoading = false
                listener.onError(databaseError.message)
            }
        })
    }

    fun attachMessageListener() {
        if (chatListener != null) return

        chatListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val newMessage = snapshot.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})
                if (newMessage != null && newMessage[KEY_KEY] != null) {
                    listener.onMessageAdded(newMessage)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val updatedMessage = snapshot.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})
                if (updatedMessage != null && updatedMessage[KEY_KEY] != null) {
                    listener.onMessageChanged(updatedMessage)
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val removedKey = snapshot.key
                if (removedKey != null) {
                    listener.onMessageRemoved(removedKey)
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                listener.onError(error.message)
            }
        }
        chatMessagesRef.addChildEventListener(chatListener!!)
    }

    fun detachMessageListener() {
        chatListener?.let {
            chatMessagesRef.removeEventListener(it)
            chatListener = null
        }
    }

    fun fetchRepliedMessages(messages: List<HashMap<String, Any>>, repliedMessagesCache: HashMap<String, HashMap<String, Any>>) {
        val repliedIdsToFetch = messages
            .mapNotNull { it["replied_message_id"]?.toString() }
            .filter { it != "null" && !repliedMessagesCache.containsKey(it) }
            .toSet()

        if (repliedIdsToFetch.isEmpty()) return

        for (messageKey in repliedIdsToFetch) {
            repliedMessagesCache[messageKey] = hashMapOf()

            chatMessagesRef.child(messageKey).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val repliedMessage = snapshot.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})
                        if (repliedMessage != null) {
                            listener.onRepliedMessageFetched(repliedMessage, messageKey)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    repliedMessagesCache.remove(messageKey)
                }
            })
        }
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
private const val CHAT_PAGE_SIZE = 80