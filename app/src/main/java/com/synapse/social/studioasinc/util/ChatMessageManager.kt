package com.synapse.social.studioasinc.util

import android.util.Log
import io.github.jan_tennert.supabase.SupabaseClient
import io.github.jan_tennert.supabase.postgrest.Postgrest
import io.github.jan_tennert.supabase.postgrest.query.Columns
import io.github.jan_tennert.supabase.gotrue.Auth
import io.github.jan_tennert.supabase.realtime.Realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.HashMap

class ChatMessageManager(private val supabase: SupabaseClient) { // Changed from object to class

    private val postgrest = supabase.postgrest
    private val auth = supabase.auth

    private const val CHATS_TABLE = "chats" // Supabase table for messages
    private const val USER_CHATS_TABLE = "user_chats" // Supabase table for user-chat associations
    private const val GROUPS_TABLE = "groups" // Supabase table for groups
    private const val INBOX_TABLE = "inbox" // Supabase table for inbox entries

    private const val CHAT_ID_KEY = "chat_id"
    private const val UID_KEY = "uid"
    private const val LAST_MESSAGE_UID_KEY = "last_message_uid"
    private const val LAST_MESSAGE_TEXT_KEY = "last_message_text"
    private const val LAST_MESSAGE_STATE_KEY = "last_message_state"
    private const val PUSH_DATE_KEY = "push_date"

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

    fun sendMessageToDb(
        messageMap: HashMap<String, Any>,
        senderUid: String,
        recipientUid: String,
        isGroup: Boolean
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isGroup) {
                    // For groups, insert directly into the chat messages table
                    val groupMessageMap = HashMap(messageMap)
                    groupMessageMap[CHAT_ID_KEY] = recipientUid // Group ID acts as chat_id
                    postgrest.from(CHATS_TABLE).insert(groupMessageMap)
                } else {
                    val chatId = getChatId(senderUid, recipientUid)
                    val directMessageMap = HashMap(messageMap)
                    directMessageMap[CHAT_ID_KEY] = chatId

                    // Insert message into the main chats table
                    postgrest.from(CHATS_TABLE).insert(directMessageMap)

                    // Update user-chat associations (upsert to handle existing entries)
                    postgrest.from(USER_CHATS_TABLE).upsert(
                        mapOf("user_id" to senderUid, CHAT_ID_KEY to chatId),
                        onConflict = "user_id, chat_id" // Define conflict target for upsert
                    )
                    postgrest.from(USER_CHATS_TABLE).upsert(
                        mapOf("user_id" to recipientUid, CHAT_ID_KEY to chatId),
                        onConflict = "user_id, chat_id"
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatMessageManager", "Error sending message: ${e.message}", e)
            }
        }
    }

    fun updateInbox(lastMessage: String, recipientUid: String, isGroup: Boolean) {
        val senderUid = auth.currentUserOrNull()?.id ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isGroup) {
                    // Fetch group members to update their inboxes
                    val response = postgrest.from(GROUPS_TABLE)
                        .select("members")
                        .eq("id", recipientUid)
                        .limit(1)
                        .execute()

                    val groupData = response.decodeSingleOrNull<Map<String, List<String>>>() // Assuming members is a list of UIDs
                    val members = groupData?.get("members")

                    members?.forEach { memberUid ->
                        val inboxUpdate = createInboxUpdate(
                            chatId = recipientUid,
                            conversationPartnerUid = recipientUid, // For group, partner is the group itself
                            lastMessage = lastMessage,
                            isGroup = true
                        )
                        // Upsert into inbox table
                        postgrest.from(INBOX_TABLE).upsert(inboxUpdate, onConflict = "user_id, chat_id")
                    }
                } else {
                    val chatId = getChatId(senderUid, recipientUid)

                    // Update inbox for the current user
                    val senderInboxUpdate = createInboxUpdate(
                        chatId = chatId,
                        conversationPartnerUid = recipientUid,
                        lastMessage = lastMessage,
                        isGroup = false
                    )
                    postgrest.from(INBOX_TABLE).upsert(senderInboxUpdate, onConflict = "user_id, chat_id")

                    // Update inbox for the other user
                    val recipientInboxUpdate = createInboxUpdate(
                        chatId = chatId,
                        conversationPartnerUid = senderUid,
                        lastMessage = lastMessage,
                        isGroup = false
                    )
                    postgrest.from(INBOX_TABLE).upsert(recipientInboxUpdate, onConflict = "user_id, chat_id")
                }
            } catch (e: Exception) {
                Log.e("ChatMessageManager", "Error updating inbox: ${e.message}", e)
            }
        }
    }

    private fun createInboxUpdate(
        chatId: String,
        conversationPartnerUid: String,
        lastMessage: String,
        isGroup: Boolean
    ): HashMap<String, Any> {
        val currentUserId = auth.currentUserOrNull()?.id ?: ""
        return hashMapOf(
            "user_id" to currentUserId, // This identifies whose inbox it is
            CHAT_ID_KEY to chatId,
            UID_KEY to conversationPartnerUid,
            LAST_MESSAGE_UID_KEY to currentUserId,
            LAST_MESSAGE_TEXT_KEY to lastMessage,
            LAST_MESSAGE_STATE_KEY to "sended",
            PUSH_DATE_KEY to System.currentTimeMillis().toString(), // Use current timestamp
            "chat_type" to if (isGroup) "group" else "single",
            "is_group" to isGroup
        )
    }
}
