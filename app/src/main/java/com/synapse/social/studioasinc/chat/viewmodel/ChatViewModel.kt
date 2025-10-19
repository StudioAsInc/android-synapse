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

class ChatViewModel : ViewModel() {

    private val firebaseDatabase = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCache = mutableMapOf<String, ChatUser>()
    private var oldestMessageKey: String? = null
    private val pageSize = 20

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

    fun loadChatMessages(otherId: String, isGroup: Boolean) {
        viewModelScope.launch {
            val messagesRef = if (isGroup) {
                firebaseDatabase.getReference("skyline/group-chats").child(otherId).child("messages")
            } else {
                val myUid = auth.currentUser!!.uid
                val chatId = ChatMessageManager.getChatId(myUid, otherId)
                firebaseDatabase.getReference("chats").child(chatId)
            }

            messagesRef.limitToLast(pageSize).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                    if (messages.isNotEmpty()) {
                        oldestMessageKey = messages.first().timestamp.toString()
                    }
                    val userIds = messages.map { it.uid }.distinct()
                    fetchUsers(userIds) {
                        val messagesWithUsers = messages.map { it.copy(user = usersCache[it.uid]) }
                        _chatMessages.postValue(messagesWithUsers)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
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
                        // Handle error
                    }
                })
        }
    }

    private fun fetchUsers(userIds: List<String>, onComplete: () -> Unit) {
        val usersToFetch = userIds.filterNot { usersCache.containsKey(it) }
        if (usersToFetch.isEmpty()) {
            onComplete()
            return
        }

        val usersRef = firebaseDatabase.getReference("skyline/users")
        var fetchedCount = 0
        usersToFetch.forEach { userId ->
            usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nickname = snapshot.child("nickname").getValue(String::class.java) ?: ""
                    val username = snapshot.child("username").getValue(String::class.java) ?: ""
                    usersCache[userId] = ChatUser(nickname, username)
                    fetchedCount++
                    if (fetchedCount == usersToFetch.size) {
                        onComplete()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                    fetchedCount++
                    if (fetchedCount == usersToFetch.size) {
                        onComplete()
                    }
                }
            })
        }
    }

    fun loadGroupDetails(groupId: String) {
        val groupRef = firebaseDatabase.getReference("skyline/group-chats").child(groupId)
        groupRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    _groupName.postValue(dataSnapshot.child("name").getValue(String::class.java))
                    _groupIcon.postValue(dataSnapshot.child("icon").getValue(String::class.java))
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }

    fun loadUserDetails(userId: String) {
        val userRef = firebaseDatabase.getReference("skyline/users").child(userId)
        userRef.addValueEventListener(object: ValueEventListener {
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
                // handle error
            }
        })
    }
}