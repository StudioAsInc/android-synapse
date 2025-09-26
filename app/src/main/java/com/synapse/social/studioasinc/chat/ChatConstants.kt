package com.synapse.social.studioasinc.chat

object ChatConstants {
    // Firebase Database References
    const val SKYLINE_REF = "skyline"
    const val USERS_REF = "users"
    const val CHATS_REF = "chats"
    const val GROUP_CHATS_REF = "skyline/group-chats"
    const val GROUPS_REF = "groups"
    const val BLOCKLIST_REF = "blocklist"
    const val INBOX_REF = "inbox"
    const val USER_CHATS_REF = "user_chats"
    const val TYPING_MESSAGE_REF = "typing"

    // Message Data Keys
    const val UID_KEY = "uid"
    const val KEY_KEY = "key"
    const val MESSAGE_TEXT_KEY = "message_text"
    const val MESSAGE_STATE_KEY = "message_state"
    const val REPLIED_MESSAGE_ID_KEY = "replied_message_id"
    const val PUSH_DATE_KEY = "push_date"
    const val TYPE_KEY = "type"
    const val ATTACHMENTS_KEY = "attachments"
    const val AUDIO_URL_KEY = "audio_url"
    const val AUDIO_DURATION_KEY = "audio_duration"

    // Message Types
    const val MESSAGE_TYPE = "message"
    const val ATTACHMENT_MESSAGE_TYPE = "attachment"
    const val VOICE_MESSAGE_TYPE = "voice"

    // User Data Keys
    const val USER_NICKNAME_KEY = "nickname"
    const val USER_USERNAME_KEY = "username"
    const val ONE_SIGNAL_PLAYER_ID_KEY = "oneSignalPlayerId"

    // Intent Extras & Prefs
    const val ORIGIN_KEY = "origin"
    const val IS_GROUP_KEY = "isGroup"
    const val CHAT_ID_KEY = "chat_id"
    const val THEME_PREFS = "theme"
    const val CHAT_BACKGROUND_URL_KEY = "chat_background_url"

    // Group Data Keys
    const val GROUP_NAME_KEY = "name"
    const val GROUP_ICON_KEY = "icon"

    // Presence & Status
    const val PRESENCE_IDLE = "Idle"
    const val USER_STATUS_GROUP = "Group"
    const val TYPING_STATUS_KEY = "typingMessageStatus"
    const val TYPING_STATUS_TRUE = "true"
    const val MESSAGE_STATE_SENDED = "sended"

    // UI State
    const val IS_LOADING_MORE_KEY = "isLoadingMore"

    // Gemini AI Models
    const val GEMINI_MODEL_FLASH = "gemini-1.5-flash"
    const val GEMINI_MODEL_FLASH_LITE = "gemini-1.5-flash-lite"
}