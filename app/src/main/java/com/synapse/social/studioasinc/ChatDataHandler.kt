package com.synapse.social.studioasinc

import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.synapse.social.studioasinc.ChatConstants.KEY_KEY
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

interface ChatDataListener {
    fun onFirstUserNameChanged(name: String)
    fun onSecondUserNameChanged(name: String)
    fun onSecondUserAvatarChanged(avatarUrl: String?)
    fun scrollToBottom()
    fun runOnUiThread(action: Runnable)
}

class ChatDataHandler(
    private val listener: ChatDataListener,
    private val firebaseDB: FirebaseDatabase,
    private val auth: FirebaseAuth,
    var chatMessagesRef: DatabaseReference,
    var userRef: DatabaseReference?,
    private val blocklistRef: DatabaseReference,
    private val chatMessagesList: MutableList<HashMap<String, Any>>,
    private val messageKeys: MutableSet<String>,
    private val locallyDeletedMessages: MutableSet<String>,
    private val repliedMessagesCache: MutableMap<String, HashMap<String, Any>>,
    private val chatAdapter: ChatAdapter,
    private val chatUIUpdater: ChatUIUpdater,
    private val chatMessagesListRecycler: androidx.recyclerview.widget.RecyclerView,
    private val isGroup: Boolean,
    private val recipientUid: String,
    private val messageInputContainer: LinearLayout,
    private val blockedTextView: TextView
) {
    private var chatChildListener: ChildEventListener? = null
    private var userStatusListener: ValueEventListener? = null
    private var blocklistChildListener: ChildEventListener? = null

    var firstUserName: String = "Unknown User"
        set(value) {
            if (field != value) {
                field = value
                listener.onFirstUserNameChanged(value)
            }
        }

    var secondUserName: String = "Unknown User"
        set(value) {
            if (field != value) {
                field = value
                listener.onSecondUserNameChanged(value)
            }
        }
    var secondUserAvatar: String? = null
        set(value) {
            if (field != value) {
                field = value
                listener.onSecondUserAvatarChanged(value)
            }
        }

    var oldestMessageKey: String? = null
    var isLoading: Boolean = false
    private val CHAT_PAGE_SIZE = 80

    fun attachListeners() {
        attachBlocklistListener()
        attachChatListener()
        attachUserStatusListener()
    }

    fun detachListeners() {
        detachBlocklistListener()
        detachChatListener()
        detachUserStatusListener()
    }

    fun getUserReference() {
        auth.currentUser?.uid?.let { uid ->
            val getFirstUserName = firebaseDB.getReference(ChatConstants.SKYLINE_REF)
                .child(ChatConstants.USERS_REF)
                .child(uid)

            getFirstUserName.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    try {
                        if (dataSnapshot.exists()) {
                            val nickname = dataSnapshot.child("nickname").getValue(String::class.java)
                            val username = dataSnapshot.child("username").getValue(String::class.java)
                            firstUserName = if (nickname != null && nickname != "null") {
                                nickname
                            } else if (username != null && username != "null") {
                                "@$username"
                            } else {
                                "Unknown User".also { Log.w("ChatActivity", "Both nickname and username are null or 'null'") }
                            }
                        } else {
                            Log.w("ChatActivity", "User data snapshot doesn't exist")
                            firstUserName = "Unknown User"
                        }
                    } catch (e: Exception) {
                        Log.e("ChatActivity", "Error processing user data: " + e.message)
                        firstUserName = "Unknown User"
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("ChatActivity", "Failed to get first user name: " + databaseError.message)
                    firstUserName = "Unknown User"
                }
            })
            getChatMessagesRef()
        }
    }

    fun getChatMessagesRef() {
        val getChatsMessages = chatMessagesRef.limitToLast(CHAT_PAGE_SIZE)
        getChatsMessages.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                try {
                    if (dataSnapshot.exists()) {
                        chatUIUpdater.updateNoChatVisibility(false)
                        chatMessagesList.clear()
                        messageKeys.clear()
                        val initialMessages = ArrayList<HashMap<String, Any>>()
                        for (_data in dataSnapshot.children) {
                            try {
                                val messageData =
                                    _data.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})
                                if (messageData != null && messageData.containsKey(KEY_KEY) && messageData[KEY_KEY] != null) {
                                    initialMessages.add(messageData)
                                    messageKeys.add(messageData[KEY_KEY].toString())
                                } else {
                                    Log.w("ChatActivity", "Skipping initial message without valid key: " + _data.key)
                                }
                            } catch (e: Exception) {
                                Log.e("ChatActivity", "Error processing initial message data: " + e.message)
                            }
                        }
                        if (initialMessages.isNotEmpty()) {
                            initialMessages.sortBy { getMessageTimestamp(it) }
                            val oldestMessage = initialMessages[0]
                            if (oldestMessage.containsKey(KEY_KEY) && oldestMessage[KEY_KEY] != null) {
                                oldestMessageKey = oldestMessage[KEY_KEY].toString()
                            }
                            chatMessagesList.addAll(initialMessages)
                            chatAdapter.notifyDataSetChanged()
                            chatMessagesListRecycler.scrollToPosition(chatMessagesList.size - 1)
                            fetchRepliedMessages(initialMessages)
                        }
                    } else {
                        chatUIUpdater.updateNoChatVisibility(true)
                    }
                } catch (e: Exception) {
                    Log.e("ChatActivity", "Error processing initial chat messages: " + e.message)
                    chatUIUpdater.updateNoChatVisibility(true)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("ChatActivity", "Initial message load failed: " + databaseError.message)
                chatUIUpdater.updateNoChatVisibility(true)
            }
        })
    }

    private fun attachChatListener() {
        if (chatChildListener != null) {
            try {
                chatMessagesRef.removeEventListener(chatChildListener!!)
            } catch (e: Exception) {
                Log.w("ChatActivity", "Error removing existing chat listener: " + e.message)
            }
            chatChildListener = null
        }
        chatChildListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                if (dataSnapshot.exists()) {
                    val newMessage = dataSnapshot.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})
                    if (newMessage != null && newMessage[KEY_KEY] != null) {
                        val messageKey = newMessage[KEY_KEY].toString()
                        if (!messageKeys.contains(messageKey)) {
                            messageKeys.add(messageKey)
                            safeUpdateRecyclerView()
                            val insertPosition = findCorrectInsertPosition(newMessage)
                            chatMessagesList.add(insertPosition, newMessage)
                            chatAdapter.notifyItemInserted(insertPosition)
                            if (insertPosition > 0) chatAdapter.notifyItemChanged(insertPosition - 1)
                            if (insertPosition < chatMessagesList.size - 1) chatAdapter.notifyItemChanged(insertPosition + 1)
                            if (insertPosition == chatMessagesList.size - 1) {
                                chatMessagesListRecycler.post { listener.scrollToBottom() }
                            }
                            if (newMessage.containsKey("replied_message_id")) {
                                fetchRepliedMessages(arrayListOf(newMessage))
                            }
                        } else {
                            val oldPosition = chatMessagesList.indexOfFirst { it[KEY_KEY] == messageKey }
                            if (oldPosition != -1) {
                                newMessage.remove("isLocalMessage")
                                val newPosition = findCorrectInsertPosition(newMessage)
                                if (oldPosition == newPosition) {
                                    chatMessagesList[oldPosition] = newMessage
                                    chatAdapter.notifyItemChanged(oldPosition)
                                } else {
                                    chatMessagesList.removeAt(oldPosition)
                                    chatMessagesList.add(newPosition, newMessage)
                                    chatAdapter.notifyItemMoved(oldPosition, newPosition)
                                    chatAdapter.notifyItemChanged(newPosition)
                                }
                            }
                        }
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.exists()) {
                    val updatedMessage = snapshot.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})
                    if (updatedMessage != null && updatedMessage[KEY_KEY] != null) {
                        val key = updatedMessage[KEY_KEY].toString()
                        val index = chatMessagesList.indexOfFirst { it[KEY_KEY]?.toString() == key }
                        if (index != -1) {
                            chatMessagesList[index] = updatedMessage
                            chatAdapter.notifyItemChanged(index)
                        }
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val removedKey = snapshot.key
                    if (removedKey != null) {
                        if (locallyDeletedMessages.contains(removedKey)) {
                            locallyDeletedMessages.remove(removedKey)
                            return
                        }
                        val index = chatMessagesList.indexOfFirst { it[KEY_KEY]?.toString() == removedKey }
                        if (index != -1) {
                            chatMessagesList.removeAt(index)
                            messageKeys.remove(removedKey)
                            chatAdapter.notifyItemRemoved(index)
                            if (chatMessagesList.isNotEmpty() && index < chatMessagesList.size) {
                                chatAdapter.notifyItemChanged(index.coerceAtMost(chatMessagesList.size - 1))
                            }
                            if (chatMessagesList.isEmpty()) {
                                safeUpdateRecyclerView()
                            }
                        }
                    }
                }
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatActivity", "Chat listener cancelled: " + error.message)
            }
        }
        chatChildListener?.let { chatMessagesRef.addChildEventListener(it) }
    }

    private fun detachChatListener() {
        chatChildListener?.let { chatMessagesRef.removeEventListener(it) }
        chatChildListener = null
    }

    private fun attachUserStatusListener() {
        userRef?.let { ref ->
            if (userStatusListener != null) {
                try {
                    ref.removeEventListener(userStatusListener!!)
                } catch (e: Exception) {
                    Log.w("ChatActivity", "Error removing existing user status listener: " + e.message)
                }
                userStatusListener = null
            }
            userStatusListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        chatUIUpdater.updateUserProfile(dataSnapshot)
                        val nickname = dataSnapshot.child("nickname").getValue(String::class.java)
                        val username = dataSnapshot.child("username").getValue(String::class.java)
                        secondUserName = if (nickname != null && nickname != "null") nickname else if (username != null && username != "null") "@$username" else "Unknown User"
                        secondUserAvatar = dataSnapshot.child("avatar_url").getValue(String::class.java)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("ChatActivity", "Failed to get user reference: " + databaseError.message)
                }
            }
            userStatusListener?.let { ref.addValueEventListener(it) }
        }
    }

    private fun detachUserStatusListener() {
        userStatusListener?.let { userRef?.removeEventListener(it) }
        userStatusListener = null
    }

    private fun attachBlocklistListener() {
        if (blocklistChildListener != null) return
        blocklistChildListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                handleBlocklistUpdate(snapshot)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                handleBlocklistUpdate(snapshot)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                handleBlocklistUpdate(snapshot)
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        blocklistChildListener?.let { blocklistRef.addChildEventListener(it) }
    }

    private fun detachBlocklistListener() {
        blocklistChildListener?.let { blocklistRef.removeEventListener(it) }
        blocklistChildListener = null
    }

    private fun findCorrectInsertPosition(newMessage: HashMap<String, Any>): Int {
        if (chatMessagesList.isEmpty()) return 0
        val newMessageTime = getMessageTimestamp(newMessage)
        for (i in chatMessagesList.indices) {
            if (newMessageTime <= getMessageTimestamp(chatMessagesList[i])) {
                return i
            }
        }
        return chatMessagesList.size
    }

    private fun getMessageTimestamp(message: HashMap<String, Any>): Long {
        return when (val pushDateObj = message["push_date"]) {
            is Long -> pushDateObj
            is Double -> pushDateObj.toLong()
            is String -> pushDateObj.toLong()
            else -> System.currentTimeMillis()
        }
    }

    private fun safeUpdateRecyclerView() {
        try {
            chatMessagesListRecycler.post {
                try {
                    chatUIUpdater.updateNoChatVisibility(chatMessagesList.isEmpty())
                } catch (e: Exception) {
                    Log.e("ChatActivity", "Error updating RecyclerView visibility: " + e.message)
                }
            }
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error in safe update: " + e.message)
        }
    }

    private fun fetchRepliedMessages(messages: List<HashMap<String, Any>>) {
        val repliedIdsToFetch = HashSet<String>()
        for (message in messages) {
            if (message.containsKey("replied_message_id")) {
                val repliedId = message["replied_message_id"].toString()
                if (repliedId != "null" && !repliedMessagesCache.containsKey(repliedId)) {
                    repliedIdsToFetch.add(repliedId)
                }
            }
        }
        if (repliedIdsToFetch.isEmpty()) return

        for (messageKey in repliedIdsToFetch) {
            repliedMessagesCache[messageKey] = HashMap()
            chatMessagesRef.child(messageKey).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val repliedMessage = snapshot.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})
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
        listener.runOnUiThread {
            for (i in chatMessagesList.indices) {
                val message = chatMessagesList[i]
                if (message.containsKey("replied_message_id") && repliedMessageKey == message["replied_message_id"].toString()) {
                    if (i < chatAdapter.itemCount) {
                        chatAdapter.notifyItemChanged(i)
                    }
                }
            }
        }
    }

    fun getOldChatMessagesRef() {
        if (isLoading || oldestMessageKey.isNullOrEmpty() || oldestMessageKey == "null") {
            return
        }
        isLoading = true
        listener.runOnUiThread { activity._showLoadMoreIndicator() }

        val getChatsMessages = chatMessagesRef
            .orderByKey()
            .endBefore(oldestMessageKey)
            .limitToLast(CHAT_PAGE_SIZE)

        getChatsMessages.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                listener.runOnUiThread { activity._hideLoadMoreIndicator() }
                try {
                    if (dataSnapshot.exists()) {
                        val newMessages = ArrayList<HashMap<String, Any>>()
                        for (_data in dataSnapshot.children) {
                            try {
                                val messageData = _data.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})
                                if (messageData != null && messageData.containsKey(KEY_KEY) && messageData[KEY_KEY] != null) {
                                    if (!messageKeys.contains(messageData[KEY_KEY].toString())) {
                                        newMessages.add(messageData)
                                        messageKeys.add(messageData[KEY_KEY].toString())
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("ChatActivity", "Error processing message data: " + e.message)
                            }
                        }

                        if (newMessages.isNotEmpty()) {
                            newMessages.sortBy { getMessageTimestamp(it) }
                            oldestMessageKey = newMessages[0][KEY_KEY]?.toString()

                            val layoutManager = chatMessagesListRecycler.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
                            val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                            val firstVisibleView = layoutManager.findViewByPosition(firstVisiblePosition)
                            val topOffset = firstVisibleView?.top ?: 0

                            chatMessagesList.addAll(0, newMessages)
                            chatAdapter.notifyItemRangeInserted(0, newMessages.size)

                            layoutManager.scrollToPositionWithOffset(firstVisiblePosition + newMessages.size, topOffset)
                            fetchRepliedMessages(newMessages)
                        } else {
                            oldestMessageKey = null
                            Log.d("ChatActivity", "No more messages to load, pagination complete")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatActivity", "Error processing old messages: " + e.message)
                } finally {
                    isLoading = false
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                listener.runOnUiThread { activity._hideLoadMoreIndicator() }
                isLoading = false
                Log.e("ChatActivity", "Error processing old messages: " + databaseError.message)
            }
        })
    }

    fun getGroupReference() {
        val groupRef = firebaseDB.getReference("groups").child(recipientUid)
        groupRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    chatUIUpdater.updateGroupProfile(dataSnapshot)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
        getChatMessagesRef()
    }

    private fun handleBlocklistUpdate(dataSnapshot: DataSnapshot) {
        val childKey = dataSnapshot.key ?: return
        val childValue = dataSnapshot.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})

        auth.currentUser?.uid?.let { myUid ->
            val otherUid = recipientUid

            if (childKey == otherUid) {
                if (childValue?.containsKey(myUid) == true) {
                    messageInputContainer.visibility = View.GONE
                    blockedTextView.visibility = View.VISIBLE
                } else {
                    messageInputContainer.visibility = View.VISIBLE
                    blockedTextView.visibility = View.GONE
                }
            }

            if (childKey == myUid) {
                if (childValue?.containsKey(otherUid) == true) {
                    messageInputContainer.visibility = View.GONE
                } else {
                    messageInputContainer.visibility = View.VISIBLE
                }
            }
        }
    }
}