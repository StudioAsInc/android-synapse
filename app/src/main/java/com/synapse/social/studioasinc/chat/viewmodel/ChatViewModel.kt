package com.synapse.social.studioasinc.chat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.synapse.social.studioasinc.chat.model.ChatMessage
import com.synapse.social.studioasinc.chat.model.ChatUser
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val firebaseDatabase = FirebaseDatabase.getInstance()
    private val usersCache = mutableMapOf<String, ChatUser>()

    private val _chatMessages = MutableLiveData<List<ChatMessage>>()
    val chatMessages: LiveData<List<ChatMessage>> = _chatMessages

    private val _groupName = MutableLiveData<String>()
    val groupName: LiveData<String> = _groupName

    private val _groupIcon = MutableLiveData<String>()
    val groupIcon: LiveData<String> = _groupIcon

    fun loadChatMessages(groupId: String) {
        viewModelScope.launch {
            val messagesRef = firebaseDatabase.getReference("groups").child(groupId).child("messages")
            messagesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
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

    private fun fetchUsers(userIds: List<String>, onComplete: () -> Unit) {
        val usersToFetch = userIds.filterNot { usersCache.containsKey(it) }
        if (usersToFetch.isEmpty()) {
            onComplete()
            return
        }

        val usersRef = firebaseDatabase.getReference("skyline/users")
        usersToFetch.forEach { userId ->
            usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nickname = snapshot.child("nickname").getValue(String::class.java) ?: ""
                    val username = snapshot.child("username").getValue(String::class.java) ?: ""
                    usersCache[userId] = ChatUser(nickname, username)
                    if (usersCache.keys.containsAll(userIds)) {
                        onComplete()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    fun loadGroupDetails(groupId: String) {
        val groupRef = firebaseDatabase.getReference("groups").child(groupId)
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
}
