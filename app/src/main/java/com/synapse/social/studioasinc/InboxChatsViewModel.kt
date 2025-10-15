package com.synapse.social.studioasinc

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.synapse.social.studioasinc.backend.SupabaseClient
import com.synapse.social.studioasinc.model.InboxChatItem
import com.synapse.social.studioasinc.model.UnreadMessage
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InboxChatsViewModel : ViewModel() {

    private val _inboxChats = MutableLiveData<List<InboxChatItem>>()
    val inboxChats: LiveData<List<InboxChatItem>> = _inboxChats

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun fetchInboxChats(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = SupabaseClient.client.postgrest["inbox"].select(
                    Columns.raw("*, users(*), groups(*)")
                ) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                val gson = Gson()
                val listType = object : TypeToken<List<InboxChatItem>>() {}.type
                val chats: List<InboxChatItem> = gson.fromJson(response.data, listType)

                val unreadCounts = fetchUnreadCounts(userId, chats.map { it.uid })
                chats.forEach { chat ->
                    chat.unreadCount = unreadCounts[chat.uid] ?: 0
                }

                _inboxChats.postValue(chats)
            } catch (e: Exception) {
                Log.e("InboxChatsViewModel", "Error fetching inbox chats", e)
                _error.postValue("Failed to load chats. Please try again later.")
            }
        }
    }

    private suspend fun fetchUnreadCounts(userId: String, chatIds: List<String>): Map<String, Long> {
        return try {
            val response = SupabaseClient.client.postgrest["chats"].select {
                filter {
                    eq("user_id", userId)
                    isIn("chat_id", chatIds)
                    eq("message_state", "sended")
                }
            }
            val gson = Gson()
            val listType = object : TypeToken<List<UnreadMessage>>() {}.type
            val unreadMessages: List<UnreadMessage> = gson.fromJson(response.data, listType)
            unreadMessages.groupingBy { it.chatId }.eachCount().mapValues { it.value.toLong() }
        } catch (e: Exception) {
            Log.e("InboxChatsViewModel", "Error fetching unread counts", e)
            emptyMap()
        }
    }
}
