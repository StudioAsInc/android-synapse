package com.synapse.social.studioasinc

import com.google.firebase.database.DataSnapshot
import java.util.ArrayList
import java.util.HashMap

interface ChatDataListener {
    fun onInitialMessagesLoaded(initialMessages: ArrayList<HashMap<String, Any>>, oldestMessageKey: String?)
    fun onNewMessageAdded(newMessage: HashMap<String, Any>)
    fun onMessageChanged(updatedMessage: HashMap<String, Any>)
    fun onMessageRemoved(removedKey: String)
    fun onUserProfileUpdated(dataSnapshot: DataSnapshot)
    fun onGroupProfileUpdated(dataSnapshot: DataSnapshot)
    fun onFirstUserNameResolved(name: String)
    fun onMoreMessagesLoaded(newMessages: ArrayList<HashMap<String, Any>>, oldestKey: String?)
    fun onNoMoreMessages()
    fun onDataLoadError(error: String)
    fun onBlockStatusChanged(isBlocked: Boolean, isBlockedByMe: Boolean)
    fun showLoadMoreIndicator()
    fun hideLoadMoreIndicator()
}