package com.synapse.social.studioasinc

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.synapse.social.studioasinc.ChatConstants.KEY_KEY
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ChatDataHandler(
    private val listener: ChatDataListener,
    private val firebaseDB: FirebaseDatabase,
    private val auth: FirebaseAuth,
    var chatMessagesRef: DatabaseReference,
    var userRef: DatabaseReference?,
    private val blocklistRef: DatabaseReference,
    private val isGroup: Boolean,
    private val recipientUid: String
) {
    private var chatChildListener: ChildEventListener? = null
    private var userStatusListener: ValueEventListener? = null
    private var blocklistListener: ChildEventListener? = null

    private var oldestMessageKey: String? = null
    var isLoading = false
        private set
    private val CHAT_PAGE_SIZE = 80

    fun attachListeners() {
        attachBlocklistListener()
        attachChatListener()
        if (!isGroup) {
            attachUserStatusListener()
        }
    }

    fun detachListeners() {
        detachBlocklistListener()
        detachChatListener()
        detachUserStatusListener()
    }

    fun loadInitialData() {
        resolveFirstUserName()
        if (isGroup) {
            getGroupReference()
        } else {
            getChatMessagesRef()
        }
    }

    private fun resolveFirstUserName() {
        auth.currentUser?.uid?.let { uid ->
            val userRef = firebaseDB.getReference(ChatConstants.SKYLINE_REF)
                .child(ChatConstants.USERS_REF)
                .child(uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val nickname = dataSnapshot.child("nickname").getValue(String::class.java)
                    val username = dataSnapshot.child("username").getValue(String::class.java)
                    val name = if (nickname != null && nickname != "null") nickname
                               else if (username != null && username != "null") "@$username"
                               else "Unknown User"
                    listener.onFirstUserNameResolved(name)
                }
                override fun onCancelled(error: DatabaseError) {
                    listener.onFirstUserNameResolved("Unknown User")
                }
            })
        }
    }

    private fun getChatMessagesRef() {
        isLoading = true
        val getChatsMessages = chatMessagesRef.limitToLast(CHAT_PAGE_SIZE)
        getChatsMessages.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val initialMessages = ArrayList<HashMap<String, Any>>()
                if (dataSnapshot.exists()) {
                    for (_data in dataSnapshot.children) {
                        val messageData = _data.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})
                        messageData?.let { initialMessages.add(it) }
                    }
                    initialMessages.sortBy { getMessageTimestamp(it) }
                    if (initialMessages.isNotEmpty()) {
                        oldestMessageKey = initialMessages.first()[KEY_KEY]?.toString()
                    }
                }
                listener.onInitialMessagesLoaded(initialMessages, oldestMessageKey)
                isLoading = false
            }
            override fun onCancelled(databaseError: DatabaseError) {
                listener.onDataLoadError("Initial message load failed: " + databaseError.message)
                isLoading = false
            }
        })
    }

    fun getOldChatMessagesRef() {
        if (isLoading || oldestMessageKey == null) return
        isLoading = true
        listener.showLoadMoreIndicator()

        val getChatsMessages = chatMessagesRef.orderByKey().endBefore(oldestMessageKey).limitToLast(CHAT_PAGE_SIZE)
        getChatsMessages.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val newMessages = ArrayList<HashMap<String, Any>>()
                    for (_data in dataSnapshot.children) {
                        val messageData = _data.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})
                        messageData?.let { newMessages.add(it) }
                    }
                    newMessages.sortBy { getMessageTimestamp(it) }
                    val newOldestKey = if (newMessages.isNotEmpty()) newMessages.first()[KEY_KEY]?.toString() else null
                    oldestMessageKey = newOldestKey
                    listener.onMoreMessagesLoaded(newMessages, oldestMessageKey)
                } else {
                    oldestMessageKey = null
                    listener.onNoMoreMessages()
                }
                isLoading = false
                listener.hideLoadMoreIndicator()
            }
            override fun onCancelled(databaseError: DatabaseError) {
                listener.onDataLoadError("Error processing old messages: " + databaseError.message)
                isLoading = false
                listener.hideLoadMoreIndicator()
            }
        })
    }

    private fun getGroupReference() {
        val groupRef = firebaseDB.getReference("groups").child(recipientUid)
        groupRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    listener.onGroupProfileUpdated(dataSnapshot)
                }
                getChatMessagesRef()
            }
            override fun onCancelled(databaseError: DatabaseError) {
                getChatMessagesRef()
            }
        })
    }

    private fun attachChatListener() {
        if (chatChildListener != null) return
        chatChildListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val newMessage = dataSnapshot.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})
                newMessage?.let { listener.onNewMessageAdded(it) }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val updatedMessage = snapshot.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})
                updatedMessage?.let { listener.onMessageChanged(it) }
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                snapshot.key?.let { listener.onMessageRemoved(it) }
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatActivity", "Chat listener cancelled: " + error.message)
            }
        }
        chatChildListener?.let { chatMessagesRef.addChildEventListener(it) }
    }

    private fun attachUserStatusListener() {
        if (userStatusListener != null) return
        userStatusListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    listener.onUserProfileUpdated(dataSnapshot)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        }
        userRef?.addValueEventListener(userStatusListener!!)
    }

    private fun attachBlocklistListener() {
        if (blocklistListener != null) return
        blocklistListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) = handleBlocklistUpdate(snapshot)
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) = handleBlocklistUpdate(snapshot)
            override fun onChildRemoved(snapshot: DataSnapshot) = handleBlocklistUpdate(snapshot) // Needed to detect unblocking
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        blocklistRef.addChildEventListener(blocklistListener!!)
    }

    private fun handleBlocklistUpdate(dataSnapshot: DataSnapshot) {
        val myUid = auth.currentUser?.uid ?: return
        val blockedUsers = dataSnapshot.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {}) ?: mapOf()
        val iAmBlocked = (dataSnapshot.key == recipientUid) && blockedUsers.containsKey(myUid)
        val iBlockedThem = (dataSnapshot.key == myUid) && blockedUsers.containsKey(recipientUid)
        listener.onBlockStatusChanged(iAmBlocked, iBlockedThem)
    }

    private fun detachChatListener() = chatChildListener?.let { chatMessagesRef.removeEventListener(it) }
    private fun detachUserStatusListener() = userStatusListener?.let { userRef?.removeEventListener(it) }
    private fun detachBlocklistListener() = blocklistListener?.let { blocklistRef.removeEventListener(it) }

    private fun getMessageTimestamp(message: HashMap<String, Any>): Long {
        return when (val pushDateObj = message["push_date"]) {
            is Long -> pushDateObj
            is Double -> pushDateObj.toLong()
            is String -> pushDateObj.toLong()
            else -> System.currentTimeMillis()
        }
    }
}