// To-do: Migrate Firebase to Supabase
// 1. **No direct Firebase dependencies in this file.** This is a state management data class.
// 2. **Review Usage**: While this class is backend-agnostic, the logic that creates and updates this state will need to be refactored.
// 3. **Data Model Alignment**: The `replyMessageId` property holds the ID of a message. Ensure that this ID corresponds to the primary key of the `messages` table in Supabase after the migration.

package com.synapse.social.studioasinc

data class ChatState(
    val isRecording: Boolean = false,
    val isLoading: Boolean = false,
    val replyMessageId: String? = null
)