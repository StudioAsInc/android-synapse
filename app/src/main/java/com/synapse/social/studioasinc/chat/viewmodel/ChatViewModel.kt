package com.synapse.social.studioasinc.chat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.synapse.social.studioasinc.chat.model.ChatMessage
import com.synapse.social.studioasinc.chat.model.ChatUser
import com.synapse.social.studioasinc.util.ChatMessageManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class ChatViewModel : ViewModel() {

    private val firebaseDatabase = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCache = mutableMapOf<String, ChatUser>()
    private var oldestMessageKey: String? = null
    private val pageSize = 20

    private val activeListeners = mutableMapOf<DatabaseReference, ValueEventListener>()

    private val _chatMessages = MutableLiveData<List<ChatMessage>>()
    val chatMessages: LiveData<List<ChatMessage>> = _chatMessages

    private val _groupName = MutableLiveData<String>()
    val groupName: LiveData<String> = _groupName

    private val _groupIcon = MutableLiveData<String>()
    val groupIcon: LiveData<String> = _groupIcon

    private val _userDetails = MutableLiveData<ChatUser>()
    val userDetails: LiveData<ChatUser> = _userDetails

    private val _userStatus = MutableLiveData<String>()
    val userStatus: LiveData<String> = _userStatus

    private val _userAvatar = MutableLiveData<String>()
    val userAvatar: LiveData<String> = _userAvatar

    private val _repliedMessages = MutableLiveData<HashMap<String, ChatMessage>>()
    val repliedMessages: LiveData<HashMap<String, ChatMessage>> = _repliedMessages

    fun fetchRepliedMessage(messageId: String, otherId: String, isGroup: Boolean) {
        viewModelScope.launch {
            val messagesRef = if (isGroup) {
                firebaseDatabase.getReference("skyline/group-chats").child(otherId).child("messages")
            } else {
                val myUid = auth.currentUser!!.uid
                val chatId = ChatMessageManager.getChatId(myUid, otherId)
                firebaseDatabase.getReference("chats").child(chatId)
            }
            messagesRef.child(messageId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val message = snapshot.getValue(ChatMessage::class.java)
                    if (message != null) {
                        val currentMap = _repliedMessages.value ?: hashMapOf()
                        currentMap[messageId] = message
                        _repliedMessages.postValue(currentMap)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("ChatViewModel", "Failed to fetch replied message: ${error.message}")
                }
            })
        }
    }

    fun loadChatMessages(otherId: String, isGroup: Boolean) {
        viewModelScope.launch {
            val messagesRef = if (isGroup) {
                firebaseDatabase.getReference("skyline/group-chats").child(otherId).child("messages")
            } else {
                val myUid = auth.currentUser!!.uid
                val chatId = ChatMessageManager.getChatId(myUid, otherId)
                firebaseDatabase.getReference("chats").child(chatId)
            }

            val listener = messagesRef.limitToLast(pageSize).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                    if (messages.isNotEmpty()) {
                        oldestMessageKey = messages.first().timestamp.toString()
                    }
                    val userIds = messages.map { it.uid }.distinct()
                    viewModelScope.launch {
                        val usersToFetch = userIds.filterNot { usersCache.containsKey(it) }
                        if (usersToFetch.isNotEmpty()) {
                            val usersRef = firebaseDatabase.getReference("skyline/users")
                            val fetchedUsers = usersToFetch.map { userId ->
                                async { userId to fetchUser(usersRef.child(userId)) }
                            }.awaitAll().filterNot { it.second == null }.associate { it.first to it.second!! }
                            usersCache.putAll(fetchedUsers)
                        }
                        val messagesWithUsers = messages.map { it.copy(user = usersCache[it.uid]) }
                        _chatMessages.postValue(messagesWithUsers)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("ChatViewModel", "Failed to load chat messages: ${error.message}")
                }
            })
            activeListeners[messagesRef] = listener
        }
    }

    fun loadMoreMessages(otherId: String, isGroup: Boolean) {
        if (oldestMessageKey == null) return

        viewModelScope.launch {
            val messagesRef = if (isGroup) {
                firebaseDatabase.getReference("skyline/group-chats").child(otherId).child("messages")
            } else {
                val myUid = auth.currentUser!!.uid
                val chatId = ChatMessageManager.getChatId(myUid, otherId)
                firebaseDatabase.getReference("chats").child(chatId)
            }

            messagesRef.orderByKey().endAt(oldestMessageKey).limitToLast(pageSize + 1)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val newMessages = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }.dropLast(1)
                        if (newMessages.isNotEmpty()) {
                            oldestMessageKey = newMessages.first().timestamp.toString()
                            val userIds = newMessages.map { it.uid }.distinct()
                            fetchUsers(userIds) {
                                val messagesWithUsers = newMessages.map { it.copy(user = usersCache[it.uid]) }
                                val currentMessages = _chatMessages.value ?: emptyList()
                                _chatMessages.postValue(messagesWithUsers + currentMessages)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        android.util.Log.e("ChatViewModel", "Failed to load more chat messages: ${error.message}")
                    }
                })
        }
    }

    private suspend fun fetchUser(userRef: DatabaseReference): ChatUser? = suspendCancellableCoroutine { continuation ->
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nickname = snapshot.child("nickname").getValue(String::class.java) ?: ""
                val username = snapshot.child("username").getValue(String::class.java) ?: ""
                if (continuation.isActive) {
                    continuation.resume(ChatUser(nickname, username))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("ChatViewModel", "Failed to fetch user: ${error.message}")
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
        userRef.addListenerForSingleValueEvent(listener)

        continuation.invokeOnCancellation {
            userRef.removeEventListener(listener)
        }
    }

    fun loadGroupDetails(groupId: String) {
        val groupRef = firebaseDatabase.getReference("skyline/group-chats").child(groupId)
        val listener = groupRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    _groupName.postValue(dataSnapshot.child("name").getValue(String::class.java))
                    _groupIcon.postValue(dataSnapshot.child("icon").getValue(String::class.java))
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                android.util.Log.e("ChatViewModel", "Failed to load group details: ${databaseError.message}")
            }
        })
        activeListeners[groupRef] = listener
    }

    fun loadUserDetails(userId: String) {
        val userRef = firebaseDatabase.getReference("skyline/users").child(userId)
        val listener = userRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val nickname = snapshot.child("nickname").getValue(String::class.java) ?: ""
                    val username = snapshot.child("username").getValue(String::class.java) ?: ""
                    _userDetails.postValue(ChatUser(nickname, username))

                    val status = snapshot.child("status").getValue(String::class.java) ?: "Offline"
                    _userStatus.postValue(status)

                    val avatar = snapshot.child("avatar_url").getValue(String::class.java)
                    if (avatar != null) {
                        _userAvatar.postValue(avatar)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("ChatViewModel", "Failed to load user details: ${error.message}")
            }
        })
        activeListeners[userRef] = listener
    }

    override fun onCleared() {
        super.onCleared()
        activeListeners.forEach { (ref, listener) ->
            ref.removeEventListener(listener)
        }
    }
}