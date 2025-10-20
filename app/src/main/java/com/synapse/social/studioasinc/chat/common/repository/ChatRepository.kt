package com.synapse.social.studioasinc.chat.common.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.synapse.social.studioasinc.model.ChatMessage
import com.synapse.social.studioasinc.model.UserStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ChatRepository {

    private val database = FirebaseDatabase.getInstance().reference
    private val chatsRef = database.child("chats")
    private val usersRef = database.child("users")

    fun getChatMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull {
                    it.getValue(ChatMessage::class.java)
                }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        chatsRef.child(chatId).addValueEventListener(listener)
        awaitClose { chatsRef.child(chatId).removeEventListener(listener) }
    }

    fun getUserStatus(userId: String): Flow<UserStatus> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userStatus = snapshot.getValue(UserStatus::class.java) ?: UserStatus()
                trySend(userStatus)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        usersRef.child(userId).addValueEventListener(listener)
        awaitClose { usersRef.child(userId).removeEventListener(listener) }
    }

    suspend fun sendMessage(chatId: String, message: ChatMessage): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        chatsRef.child(chatId).child(message.key).setValue(message)
            .addOnSuccessListener { deferred.complete(true) }
            .addOnFailureListener { deferred.complete(false) }
        return deferred.await()
    }

    suspend fun deleteMessage(chatId: String, message: ChatMessage) {
        chatsRef.child(chatId).child(message.key).removeValue()
    }

    suspend fun blockUser(userId: String, targetId: String) {
        database.child("blocklist").child(userId).child(targetId).setValue(true)
    }

    suspend fun unblockUser(userId: String, targetId: String) {
        database.child("blocklist").child(userId).child(targetId).removeValue()
    }

    fun getBlockedUsers(userId: String): Flow<List<String>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val blockedUsers = snapshot.children.mapNotNull { it.key }
                trySend(blockedUsers)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        database.child("blocklist").child(userId).addValueEventListener(listener)
        awaitClose { database.child("blocklist").child(userId).removeEventListener(listener) }
    }

    fun getMoreMessages(chatId: String, lastMessageKey: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull {
                    it.getValue(ChatMessage::class.java)
                }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        chatsRef.child(chatId).orderByKey().endAt(lastMessageKey).limitToLast(20).addValueEventListener(listener)
        awaitClose { chatsRef.child(chatId).removeEventListener(listener) }
    }
}
