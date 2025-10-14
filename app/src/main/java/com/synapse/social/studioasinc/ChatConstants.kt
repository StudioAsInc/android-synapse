// To-do: Migrate Firebase to Supabase
// 1. **Review and Refactor Constants**: This object contains constants that are tightly coupled to the Firebase Realtime Database schema.
//    - The `*_REF` constants (e.g., `SKYLINE_REF`, `USERS_REF`, `CHATS_REF`) represent paths in the Firebase JSON tree. These will need to be replaced with constants representing table names in the Supabase PostgreSQL database (e.g., `TABLE_USERS`, `TABLE_MESSAGES`).
//    - The `*_KEY` constants (e.g., `UID_KEY`, `MESSAGE_TEXT_KEY`) represent keys in the Firebase HashMaps. These should be replaced with constants representing column names in the Supabase tables (e.g., `COLUMN_USER_ID`, `COLUMN_CONTENT`).
// 2. **Data Model Alignment**: Ensure the new constants align with the new data classes (e.g., `Message.kt`, `User.kt`) that will be created to represent the data from Supabase.

package com.synapse.social.studioasinc

object ChatConstants {
    const val SKYLINE_REF = "skyline"
    const val USERS_REF = "users"
    const val CHATS_REF = "chats"
    const val USER_CHATS_REF = "user-chats"
    const val INBOX_REF = "inbox"
    const val BLOCKLIST_REF = "blocklist"
    const val TYPING_MESSAGE_REF = "typing-message"
    const val USERNAME_REF = "username"

    const val UID_KEY = "uid"
    const val ORIGIN_KEY = "origin"
    const val KEY_KEY = "key"
    const val MESSAGE_TEXT_KEY = "message_text"
    const val TYPE_KEY = "TYPE"
    const val MESSAGE_STATE_KEY = "message_state"
    const val PUSH_DATE_KEY = "push_date"
    const val REPLIED_MESSAGE_ID_KEY = "replied_message_id"
    const val ATTACHMENTS_KEY = "attachments"
    const val LAST_MESSAGE_UID_KEY = "last_message_uid"
    const val LAST_MESSAGE_TEXT_KEY = "last_message_text"
    const val LAST_MESSAGE_STATE_KEY = "last_message_state"
    const val CHAT_ID_KEY = "chatID"

    const val MESSAGE_TYPE = "MESSAGE"
    const val ATTACHMENT_MESSAGE_TYPE = "ATTACHMENT_MESSAGE"
    const val VOICE_MESSAGE_TYPE = "VOICE_MESSAGE"

    const val GEMINI_MODEL = "gemini-2.5-flash-lite"
    const val GEMINI_EXPLANATION_MODEL = "gemini-2.5-flash"
    const val EXPLAIN_CONTEXT_MESSAGES_BEFORE = 5
    const val EXPLAIN_CONTEXT_MESSAGES_AFTER = 2
    const val TAG = "ChatActivity"
}