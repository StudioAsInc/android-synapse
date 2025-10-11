package com.synapse.social.studioasinc.util

import java.util.Calendar
import java.util.HashMap

object ChatMessageManager {

    // private val firebaseDatabase = Supabase.client
    // private val auth = Supabase.auth

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

    // Supabase: fun sendMessageToDb(
    // Supabase:     messageMap: HashMap<String, Any>,
    // Supabase:     senderUid: String,
    // Supabase:     recipientUid: String,
    // Supabase:     uniqueMessageKey: String,
    // Supabase:     isGroup: Boolean
    // Supabase: ) {
    // Supabase:     // Supabase: Implement with Supabase
    // Supabase: }

    // Supabase: fun updateInbox(lastMessage: String, recipientUid: String, isGroup: Boolean, groupName: String? = null) {
    // Supabase:     // Supabase: Implement with Supabase
    // Supabase: }

    // Supabase: private fun createInboxUpdate(
    // Supabase:     chatId: String,
    // Supabase:     conversationPartnerUid: String,
    // Supabase:     lastMessage: String,
    // Supabase:     isGroup: Boolean
    // Supabase: ): HashMap<String, Any> {
    // Supabase:     // Supabase: Implement with Supabase
    // Supabase:     return hashMapOf()
    // Supabase: }
}
