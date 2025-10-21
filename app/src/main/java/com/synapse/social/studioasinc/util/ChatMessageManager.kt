package com.synapse.social.studioasinc.util

import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.HashMap

object ChatMessageManager {

    private val dbService = SupabaseDatabaseService()
    private val authService = SupabaseAuthenticationService()

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

    suspend fun sendMessageToDb(
        messageMap: HashMap<String, Any>,
        senderUid: String,
        recipientUid: String,
        uniqueMessageKey: String,
        isGroup: Boolean
    ) {
        try {
            if (isGroup) {
                // Insert group message
                dbService.insert("group_messages", messageMap)
            } else {
                // Insert direct message
                dbService.insert("messages", messageMap)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    suspend fun updateInbox(
        senderUid: String,
        recipientUid: String,
        lastMessage: String,
        isGroup: Boolean = false
    ) {
        try {
            if (isGroup) {
                // Update group inbox for all members
                val groupMembers = dbService.selectWithFilter<Map<String, Any?>>(
                    table = "group_members",
                    columns = "user_id"
                ) { query ->
                    query.eq("group_id", recipientUid)
                }
                
                groupMembers.forEach { member ->
                    val userId = member["user_id"] as? String
                    if (userId != null) {
                        updateUserInbox(userId, recipientUid, lastMessage, true)
                    }
                }
            } else {
                // Update inbox for both users
                updateUserInbox(senderUid, recipientUid, lastMessage, false)
                updateUserInbox(recipientUid, senderUid, lastMessage, false)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    private suspend fun updateUserInbox(
        userId: String,
        partnerId: String,
        lastMessage: String,
        isGroup: Boolean
    ) {
        try {
            val inboxData = mapOf(
                "user_id" to userId,
                "chat_partner_id" to if (!isGroup) partnerId else null,
                "group_id" to if (isGroup) partnerId else null,
                "unread_count" to 1,
                "updated_at" to java.time.Instant.now().toString()
            )
            
            dbService.upsert("inbox", inboxData)
        } catch (e: Exception) {
            // Handle error
        }
    }
}
