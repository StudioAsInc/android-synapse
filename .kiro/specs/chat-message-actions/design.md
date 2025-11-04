# Design Document: Chat Message Actions

## Overview

This design document outlines the implementation of a comprehensive message actions system for the Synapse chat application. When users long-press on message bubbles, a Material Design bottom sheet will appear with fully functional actions: Reply, Forward, Delete, Edit, and AI Summary (powered by Gemini AI).

All features will be production-ready with complete backend integration via Supabase, real-time synchronization, and proper error handling. This is not a mock or placeholder implementation.

## Architecture

### System Components

The message actions system consists of five main layers:

1. **UI Layer**: Bottom sheet dialog, action buttons, confirmation dialogs
2. **Adapter Layer**: Long-press gesture detection in ChatAdapter
3. **ViewModel Layer**: Business logic coordination and state management
4. **Service Layer**: Backend operations via SupabaseChatService and new GeminiAIService
5. **Database Layer**: Supabase Postgrest with Row Level Security policies

### Integration Points

**Existing Components**:
- `ChatAdapter.kt`: Add long-press detection and action callbacks
- `ChatAdapterListener` interface: Extend with new action methods
- `SupabaseChatService.kt`: Already has `editMessage()` and `deleteMessage()` methods
- `chat_bubble_*.xml` layouts: Enable long-press gestures

**New Components**:
- `MessageActionsBottomSheet.kt`: Bottom sheet dialog for action menu
- `MessageActionsViewModel.kt`: Coordinates all message actions
- `GeminiAIService.kt`: Integrates with Gemini API for AI summaries
- `ForwardMessageDialog.kt`: Conversation selection for forwarding
- `EditMessageDialog.kt`: Message editing interface
- `MessageActionRepository.kt`: Data layer for message operations

## Components and Interfaces

### 1. MessageActionsBottomSheet

**File**: `app/src/main/java/com/synapse/social/studioasinc/chat/MessageActionsBottomSheet.kt`

Material Design bottom sheet that displays available actions for a message.

**Key Features**:
- Extends `BottomSheetDialogFragment` from Material Components
- Dynamic action list based on message ownership and type
- Icon + label for each action
- Smooth animations and haptic feedback
- Dismisses on action selection or outside tap

**Layout**: `res/layout/bottom_sheet_message_actions.xml`

```xml
<LinearLayout>
    <TextView id="@+id/message_preview" /> <!-- Shows message snippet -->
    <RecyclerView id="@+id/actions_list" /> <!-- Action items -->
</LinearLayout>
```

**Action Item Layout**: `res/layout/item_message_action.xml`

```xml
<LinearLayout orientation="horizontal">
    <ImageView id="@+id/action_icon" />
    <TextView id="@+id/action_label" />
</LinearLayout>
```

**Actions Data Class**:
```kotlin
data class MessageAction(
    val id: String, // "reply", "forward", "edit", "delete", "ai_summary"
    val label: String,
    val icon: Int, // Drawable resource ID
    val isDestructive: Boolean = false
)
```

**Methods**:
- `show(fragmentManager, messageData, listener)`: Display bottom sheet
- `getAvailableActions(messageData, currentUserId)`: Filter actions based on context
- `onActionSelected(action)`: Handle action selection

### 2. Extended ChatAdapterListener Interface

**File**: `app/src/main/java/com/synapse/social/studioasinc/chat/interfaces/ChatInterfaces.kt`

Add new methods to existing `ChatAdapterListener`:

```kotlin
interface ChatAdapterListener {
    // Existing methods
    fun onMessageClick(messageId: String, position: Int)
    fun onMessageLongClick(messageId: String, position: Int): Boolean
    fun onReplyClick(messageId: String, messageText: String, senderName: String)
    fun onAttachmentClick(attachmentUrl: String, attachmentType: String)
    fun onUserProfileClick(userId: String)
    
    // New methods for message actions
    fun onReplyAction(messageId: String, messageText: String, senderName: String)
    fun onForwardAction(messageId: String, messageData: Map<String, Any?>)
    fun onEditAction(messageId: String, currentText: String)
    fun onDeleteAction(messageId: String, deleteForEveryone: Boolean)
    fun onAISummaryAction(messageId: String, messageText: String)
}
```

### 3. MessageActionsViewModel

**File**: `app/src/main/java/com/synapse/social/studioasinc/chat/presentation/MessageActionsViewModel.kt`

Coordinates all message action operations with proper state management.

**Dependencies**:
- `MessageActionRepository`: Data operations
- `GeminiAIService`: AI summary generation
- `SupabaseChatService`: Backend operations

**State Classes**:
```kotlin
sealed class MessageActionState {
    object Idle : MessageActionState()
    object Loading : MessageActionState()
    data class Success(val message: String) : MessageActionState()
    data class Error(val error: String) : MessageActionState()
}

data class ForwardState(
    val selectedChats: List<String> = emptyList(),
    val isForwarding: Boolean = false,
    val forwardedCount: Int = 0
)

data class AISummaryState(
    val isGenerating: Boolean = false,
    val summary: String? = null,
    val error: String? = null,
    val characterCount: Int = 0,
    val estimatedReadTime: Int = 0
)
```

**Methods**:
```kotlin
// Reply
fun prepareReply(messageId: String, messageText: String, senderName: String)

// Forward
fun forwardMessage(messageId: String, targetChatIds: List<String>): Flow<ForwardState>

// Edit
fun editMessage(messageId: String, newContent: String): Flow<MessageActionState>

// Delete
fun deleteMessage(messageId: String, deleteForEveryone: Boolean): Flow<MessageActionState>

// AI Summary
fun generateAISummary(messageText: String): Flow<AISummaryState>
fun getCachedSummary(messageId: String): String?
fun cacheSummary(messageId: String, summary: String)
```

### 4. GeminiAIService

**File**: `app/src/main/java/com/synapse/social/studioasinc/backend/GeminiAIService.kt`

Integrates with Google Gemini API for AI-powered message summarization.

**Configuration**:
- API Key stored in `gradle.properties`: `GEMINI_API_KEY`
- Endpoint: `https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent`
- Uses OkHttp for HTTP requests
- Implements retry logic and rate limiting

**Key Methods**:
```kotlin
suspend fun generateSummary(text: String, maxLength: Int = 100): Result<String>
suspend fun generateKeyPoints(text: String): Result<List<String>>
fun isRateLimited(): Boolean
fun getRateLimitResetTime(): Long
```

**Summarization Prompt**:
```
Summarize the following message in 2-3 concise sentences, capturing the key points:

[MESSAGE TEXT]

Summary:
```

**Error Handling**:
- Network failures: Retry with exponential backoff
- Rate limiting: Cache and display reset time
- Invalid API key: Log error and show user-friendly message
- Timeout: 10-second timeout with cancellation support

### 5. ForwardMessageDialog

**File**: `app/src/main/java/com/synapse/social/studioasinc/chat/ForwardMessageDialog.kt`

Dialog for selecting conversation(s) to forward messages to.

**Layout**: `res/layout/dialog_forward_message.xml`

```xml
<LinearLayout>
    <EditText id="@+id/search_conversations" hint="Search conversations" />
    <RecyclerView id="@+id/conversations_list" /> <!-- Checkboxes -->
    <LinearLayout orientation="horizontal">
        <Button id="@+id/btn_cancel" />
        <Button id="@+id/btn_forward" />
    </LinearLayout>
</LinearLayout>
```

**Features**:
- Loads user's conversations from Supabase
- Search/filter by conversation name
- Multi-select with checkboxes
- Shows participant avatars and names
- Displays selected count
- Confirms before forwarding

**Data Class**:
```kotlin
data class ForwardableConversation(
    val chatId: String,
    val displayName: String,
    val avatarUrl: String?,
    val isGroup: Boolean,
    val participantCount: Int,
    var isSelected: Boolean = false
)
```

### 6. EditMessageDialog

**File**: `app/src/main/java/com/synapse/social/studioasinc/chat/EditMessageDialog.kt`

Dialog for editing message content with edit history display.

**Layout**: `res/layout/dialog_edit_message.xml`

```xml
<LinearLayout>
    <TextView text="Edit Message" />
    <EditText id="@+id/edit_message_input" />
    <TextView id="@+id/character_count" />
    <LinearLayout orientation="horizontal">
        <Button id="@+id/btn_cancel" />
        <Button id="@+id/btn_save" />
    </LinearLayout>
</LinearLayout>
```

**Features**:
- Pre-populates with current message text
- Character counter
- Validates non-empty content
- Shows loading state while saving
- Prevents editing messages older than 48 hours

### 7. MessageActionRepository

**File**: `app/src/main/java/com/synapse/social/studioasinc/chat/data/MessageActionRepository.kt`

Data layer that coordinates between local cache and Supabase backend.

**Dependencies**:
- `SupabaseChatService`: Backend operations
- `SharedPreferences`: Local caching for AI summaries
- `SupabaseClient`: Direct database access for complex queries

**Methods**:
```kotlin
// Forward operations
suspend fun forwardMessageToChat(messageData: Map<String, Any?>, targetChatId: String): Result<String>
suspend fun forwardMessageToMultipleChats(messageData: Map<String, Any?>, targetChatIds: List<String>): Result<Int>

// Edit operations
suspend fun editMessage(messageId: String, newContent: String): Result<Unit>
suspend fun getEditHistory(messageId: String): Result<List<MessageEdit>>

// Delete operations
suspend fun deleteMessageLocally(messageId: String): Result<Unit>
suspend fun deleteMessageForEveryone(messageId: String): Result<Unit>

// AI Summary caching
fun getCachedSummary(messageId: String): String?
fun cacheSummary(messageId: String, summary: String)
fun clearSummaryCache()
```

**Data Classes**:
```kotlin
data class MessageEdit(
    val editedAt: Long,
    val previousContent: String,
    val editedBy: String
)
```

## Data Models

### Database Schema Extensions

**messages table** (existing, add new columns):
```sql
ALTER TABLE messages ADD COLUMN IF NOT EXISTS edit_history JSONB DEFAULT '[]';
ALTER TABLE messages ADD COLUMN IF NOT EXISTS forwarded_from_message_id TEXT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS forwarded_from_chat_id TEXT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS delete_for_everyone BOOLEAN DEFAULT FALSE;
```

**message_forwards table** (new):
```sql
CREATE TABLE message_forwards (
    id TEXT PRIMARY KEY,
    original_message_id TEXT NOT NULL REFERENCES messages(id),
    original_chat_id TEXT NOT NULL REFERENCES chats(chat_id),
    forwarded_message_id TEXT NOT NULL REFERENCES messages(id),
    forwarded_chat_id TEXT NOT NULL REFERENCES chats(chat_id),
    forwarded_by TEXT NOT NULL REFERENCES users(uid),
    forwarded_at BIGINT NOT NULL,
    UNIQUE(original_message_id, forwarded_chat_id)
);
```

**ai_summaries table** (new):
```sql
CREATE TABLE ai_summaries (
    id TEXT PRIMARY KEY,
    message_id TEXT NOT NULL REFERENCES messages(id),
    summary_text TEXT NOT NULL,
    generated_at BIGINT NOT NULL,
    generated_by TEXT NOT NULL REFERENCES users(uid),
    model_version TEXT DEFAULT 'gemini-pro',
    character_count INT NOT NULL,
    UNIQUE(message_id)
);
```

### Row Level Security Policies

**messages table policies**:
```sql
-- Allow users to edit their own messages
CREATE POLICY "Users can edit own messages"
ON messages FOR UPDATE
USING (sender_id = auth.uid() AND created_at > (EXTRACT(EPOCH FROM NOW()) * 1000 - 172800000)); -- 48 hours

-- Allow users to delete their own messages
CREATE POLICY "Users can delete own messages"
ON messages FOR UPDATE
USING (sender_id = auth.uid());

-- Allow users to view messages in their chats
CREATE POLICY "Users can view messages in their chats"
ON messages FOR SELECT
USING (
    chat_id IN (
        SELECT chat_id FROM chat_participants WHERE user_id = auth.uid()
    )
);
```

**message_forwards table policies**:
```sql
-- Users can create forwards for messages in their chats
CREATE POLICY "Users can forward messages"
ON message_forwards FOR INSERT
WITH CHECK (
    forwarded_by = auth.uid() AND
    original_chat_id IN (SELECT chat_id FROM chat_participants WHERE user_id = auth.uid()) AND
    forwarded_chat_id IN (SELECT chat_id FROM chat_participants WHERE user_id = auth.uid())
);

-- Users can view forward history for their messages
CREATE POLICY "Users can view forward history"
ON message_forwards FOR SELECT
USING (
    forwarded_by = auth.uid() OR
    original_chat_id IN (SELECT chat_id FROM chat_participants WHERE user_id = auth.uid())
);
```

## Error Handling

### User-Facing Error Messages

All errors must be user-friendly and actionable:

**Reply Errors**:
- "Unable to reply to this message" (generic)
- "This message has been deleted" (specific)

**Forward Errors**:
- "Failed to forward message. Please try again."
- "You don't have permission to send messages in [Chat Name]"
- "Forwarded to X of Y conversations" (partial success)

**Edit Errors**:
- "Unable to edit message. Please try again."
- "This message is too old to edit" (>48 hours)
- "Message cannot be empty"

**Delete Errors**:
- "Failed to delete message. Please try again."
- "Unable to delete for everyone. Message may have already been deleted."

**AI Summary Errors**:
- "Unable to generate summary. Please try again."
- "AI service is temporarily unavailable"
- "Rate limit reached. Try again in X minutes"
- "Message is too short to summarize" (<100 characters)

### Technical Error Logging

All errors must be logged with full context:

```kotlin
Log.e(TAG, "Failed to forward message", exception)
Log.e(TAG, "Gemini API error: ${response.code} - ${response.message}")
Log.e(TAG, "Edit message failed for messageId=$messageId, userId=$userId", exception)
```

### Retry Logic

**Network Failures**:
- Automatic retry with exponential backoff (1s, 2s, 4s)
- Maximum 3 retry attempts
- Show retry button for user-initiated retry

**Rate Limiting**:
- Detect 429 status codes from Gemini API
- Cache rate limit reset time
- Display countdown timer to user
- Disable AI Summary action until reset

**Offline Handling**:
- Queue actions locally when offline
- Sync when connection restored
- Show "Pending" indicator for queued actions

## Testing Strategy

### Unit Tests

**MessageActionsViewModel Tests**:
- Test reply preparation with valid message data
- Test forward to single and multiple chats
- Test edit message validation (empty, too old)
- Test delete for me vs delete for everyone
- Test AI summary generation and caching
- Test error handling for all operations

**GeminiAIService Tests**:
- Mock API responses for successful summarization
- Test rate limiting detection and handling
- Test network error retry logic
- Test timeout handling
- Test invalid API key handling

**MessageActionRepository Tests**:
- Test forward message creation with proper metadata
- Test edit history tracking
- Test delete operations (local vs server)
- Test summary caching and retrieval

### Integration Tests

**End-to-End Action Flows**:
1. Long-press message → Bottom sheet appears
2. Select Reply → Reply preview shows in input
3. Send reply → Message linked correctly in database
4. Select Forward → Dialog shows conversations
5. Forward to multiple chats → Messages created in all chats
6. Select Edit → Dialog pre-populated with text
7. Save edit → Message updated with "(edited)" label
8. Select Delete for everyone → Message deleted for all users
9. Select AI Summary → Summary generated and displayed

**Real-time Synchronization Tests**:
- Edit message on Device A → Update appears on Device B
- Delete message on Device A → Deletion syncs to Device B
- Forward message → Recipient sees forwarded message immediately

### Manual Testing Checklist

**Bottom Sheet**:
- [ ] Long-press triggers bottom sheet after 500ms
- [ ] Haptic feedback on long-press
- [ ] Background dims when sheet appears
- [ ] Sheet dismisses on outside tap
- [ ] Sheet dismisses on back button
- [ ] Actions filtered based on message ownership
- [ ] Icons and labels display correctly

**Reply Action**:
- [ ] Reply preview shows above input
- [ ] Preview shows sender name and message snippet
- [ ] Preview limited to 3 lines
- [ ] Close button cancels reply
- [ ] Sent message includes reply reference
- [ ] Tapping reply indicator scrolls to original message

**Forward Action**:
- [ ] Dialog shows all user conversations
- [ ] Search filters conversations correctly
- [ ] Multi-select works with checkboxes
- [ ] Selected count updates
- [ ] Forward button disabled when no selection
- [ ] Success message shows forwarded count
- [ ] Forwarded messages have "Forwarded" label

**Edit Action**:
- [ ] Dialog pre-populated with current text
- [ ] Character counter updates
- [ ] Save button disabled for empty text
- [ ] Edit action hidden for messages >48 hours old
- [ ] Edited message shows "(edited)" label
- [ ] Tapping "(edited)" shows edit history

**Delete Action**:
- [ ] Confirmation dialog shows two options
- [ ] "Delete for me" removes locally only
- [ ] "Delete for everyone" syncs to all users
- [ ] Deleted messages show placeholder text
- [ ] Media attachments removed from deleted messages

**AI Summary Action**:
- [ ] Loading indicator shows during generation
- [ ] Summary displays in dialog with original message
- [ ] Character count and read time shown
- [ ] Copy button copies summary to clipboard
- [ ] Cached summaries load instantly
- [ ] Rate limit message shows when limit reached
- [ ] Action hidden for messages <100 characters

## Implementation Notes

### Phased Approach

**Phase 1: Bottom Sheet Infrastructure** (Foundation)
- Create MessageActionsBottomSheet component
- Integrate long-press detection in ChatAdapter
- Implement action filtering logic
- Test bottom sheet display and dismissal

**Phase 2: Reply and Forward** (Core Actions)
- Implement reply preview UI
- Add reply reference to message sending
- Create ForwardMessageDialog
- Implement forward message logic with database operations
- Test reply and forward end-to-end

**Phase 3: Edit and Delete** (Ownership Actions)
- Create EditMessageDialog
- Implement edit message with history tracking
- Add delete confirmation dialog
- Implement delete for me vs delete for everyone
- Test edit and delete with real-time sync

**Phase 4: AI Summary** (Advanced Feature)
- Create GeminiAIService with API integration
- Implement summary generation and caching
- Add rate limiting and error handling
- Create AI summary display dialog
- Test with various message lengths and content

**Phase 5: Polish and Optimization** (Final)
- Add animations and transitions
- Optimize database queries
- Implement offline queueing
- Add comprehensive error handling
- Performance testing and optimization

### Gemini API Integration

**Setup**:
1. Obtain API key from Google AI Studio
2. Add to `gradle.properties`: `GEMINI_API_KEY=your_key_here`
3. Add to `BuildConfig` in `build.gradle.kts`

**Request Format**:
```json
{
  "contents": [{
    "parts": [{
      "text": "Summarize the following message in 2-3 concise sentences: [MESSAGE]"
    }]
  }],
  "generationConfig": {
    "temperature": 0.4,
    "maxOutputTokens": 150
  }
}
```

**Response Parsing**:
```kotlin
val summary = response
    .getJSONArray("candidates")
    .getJSONObject(0)
    .getJSONObject("content")
    .getJSONArray("parts")
    .getJSONObject(0)
    .getString("text")
```

### Real-time Synchronization

**Supabase Realtime Channels**:
```kotlin
// Subscribe to message updates
val channel = client.realtime.createChannel("messages:$chatId")
channel.on<PostgresAction.Update>("messages") { update ->
    val messageId = update.record["id"] as String
    val isEdited = update.record["is_edited"] as Boolean
    val isDeleted = update.record["is_deleted"] as Boolean
    
    // Update UI accordingly
    if (isEdited) updateMessageInUI(messageId, update.record)
    if (isDeleted) markMessageAsDeleted(messageId)
}
channel.subscribe()
```

### Performance Considerations

**Caching Strategy**:
- AI summaries cached in SharedPreferences (key: `ai_summary_$messageId`)
- Cache expiration: 7 days
- Cache size limit: 100 summaries (LRU eviction)

**Database Optimization**:
- Index on `messages.chat_id` for fast message queries
- Index on `message_forwards.original_message_id` for forward tracking
- Index on `ai_summaries.message_id` for summary lookups

**Network Optimization**:
- Batch forward operations when possible
- Use Supabase RPC functions for complex operations
- Implement request debouncing for typing indicators

### Accessibility

**Screen Reader Support**:
- Content descriptions for all action icons
- Announce bottom sheet appearance
- Announce action results ("Message forwarded to 3 conversations")

**Touch Targets**:
- Minimum 48dp touch targets for all actions
- Adequate spacing between action items (16dp)

**Visual Feedback**:
- Ripple effects on action selection
- Loading indicators for async operations
- Success/error snackbars with appropriate colors

## Design Decisions and Rationales

### 1. Bottom Sheet vs Context Menu

**Decision**: Use Material Design Bottom Sheet instead of traditional context menu

**Rationale**:
- Better mobile UX with thumb-friendly positioning
- More space for action labels and icons
- Supports dynamic action lists
- Consistent with modern messaging apps (WhatsApp, Telegram)
- Easier to add future actions without cluttering UI

### 2. Gemini API vs Local Summarization

**Decision**: Use Gemini API for AI summarization instead of local ML models

**Rationale**:
- Higher quality summaries with state-of-the-art LLM
- No app size increase from bundled models
- Supports multiple languages automatically
- Easier to update and improve over time
- Cost-effective with free tier (60 requests/minute)

### 3. Edit Time Limit (48 Hours)

**Decision**: Restrict message editing to 48 hours after sending

**Rationale**:
- Prevents abuse and conversation manipulation
- Maintains conversation integrity and trust
- Aligns with industry standards (Telegram: 48h, WhatsApp: unlimited but shows edit history)
- Balances user flexibility with conversation authenticity

### 4. Forward with "Forwarded" Label

**Decision**: Mark forwarded messages with visible "Forwarded" label

**Rationale**:
- Transparency about message origin
- Prevents misinformation and context loss
- Industry standard (WhatsApp, Telegram)
- Helps users understand message provenance

### 5. Delete for Me vs Delete for Everyone

**Decision**: Offer both local and server-side deletion options

**Rationale**:
- User control over their own message history
- Respects privacy (delete for me)
- Allows mistake correction (delete for everyone)
- Standard feature in modern messaging apps
- Clear labeling prevents confusion

### 6. AI Summary Caching

**Decision**: Cache AI summaries locally with 7-day expiration

**Rationale**:
- Reduces API costs and rate limiting
- Instant summary display for repeated requests
- Message content rarely changes after 7 days
- Balances freshness with performance

### 7. Multi-Select Forward

**Decision**: Allow forwarding to multiple conversations simultaneously

**Rationale**:
- Efficient for sharing with multiple people
- Common use case (sharing news, links, media)
- Reduces repetitive actions
- Standard feature in messaging apps

### 8. Edit History Tracking

**Decision**: Store complete edit history in database

**Rationale**:
- Transparency and trust
- Prevents malicious editing
- Allows users to see what changed
- Supports moderation and dispute resolution
- Industry best practice (Telegram, Discord)