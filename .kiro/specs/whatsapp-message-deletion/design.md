# Design Document

## Overview

This design implements a WhatsApp-like message deletion system with multi-select capability and a Material 3 contextual action toolbar. The system addresses the current bug where the app attempts to update a non-existent `deleted_at` column in the database. The solution uses existing database fields (`is_deleted`, `delete_for_everyone`) and introduces a new table for tracking user-specific deletions.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      ChatActivity                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         MultiSelectManager                            │  │
│  │  - Selection state tracking                           │  │
│  │  - Action toolbar lifecycle                           │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         MessageDeletionCoordinator                    │  │
│  │  - Orchestrates deletion flow                         │  │
│  │  - Validates permissions                              │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  MessageDeletionViewModel                    │
│  - Manages deletion state                                    │
│  - Coordinates repository operations                         │
│  - Exposes UI state flows                                    │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│               MessageDeletionRepository                      │
│  - Database operations                                       │
│  - Supabase API calls                                        │
│  - Error handling and retry logic                           │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Supabase Backend                          │
│  ┌──────────────────┐  ┌──────────────────────────────┐    │
│  │  messages table  │  │  user_deleted_messages table │    │
│  └──────────────────┘  └──────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

1. **MultiSelectManager**: Manages multi-select mode state, selection tracking, and action toolbar visibility
2. **MessageDeletionCoordinator**: Coordinates the deletion flow, including dialog display and permission validation
3. **MessageDeletionViewModel**: Provides reactive state management and business logic
4. **MessageDeletionRepository**: Handles all database operations and Supabase interactions
5. **ChatAdapter**: Updated to support selection indicators and multi-select interactions

## Components and Interfaces

### 1. MultiSelectManager

```kotlin
class MultiSelectManager(
    private val activity: ChatActivity,
    private val adapter: ChatAdapter
) {
    private val selectedMessageIds = mutableSetOf<String>()
    private var isMultiSelectMode = false
    private var actionToolbar: MaterialToolbar? = null
    private var standardToolbar: MaterialToolbar? = null
    
    fun enterMultiSelectMode(initialMessageId: String)
    fun exitMultiSelectMode()
    fun toggleMessageSelection(messageId: String)
    fun isMessageSelected(messageId: String): Boolean
    fun getSelectedMessages(): List<String>
    fun getSelectionCount(): Int
    private fun showActionToolbar()
    private fun hideActionToolbar()
    private fun updateActionToolbarTitle()
}
```

### 2. MessageDeletionCoordinator

```kotlin
class MessageDeletionCoordinator(
    private val activity: ChatActivity,
    private val viewModel: MessageDeletionViewModel,
    private val currentUserId: String
) {
    fun initiateDelete(messageIds: List<String>)
    private fun showDeletionDialog(messageIds: List<String>, canDeleteForEveryone: Boolean)
    private fun executeDelete(messageIds: List<String>, deleteForEveryone: Boolean)
    private fun validateOwnership(messageIds: List<String>): Boolean
}
```

### 3. MessageDeletionViewModel

```kotlin
class MessageDeletionViewModel(
    private val repository: MessageDeletionRepository
) : ViewModel() {
    
    private val _deletionState = MutableStateFlow<DeletionState>(DeletionState.Idle)
    val deletionState: StateFlow<DeletionState> = _deletionState.asStateFlow()
    
    private val _errorState = MutableSharedFlow<String>()
    val errorState: SharedFlow<String> = _errorState.asSharedFlow()
    
    suspend fun deleteMessagesForMe(messageIds: List<String>, userId: String)
    suspend fun deleteMessagesForEveryone(messageIds: List<String>, userId: String)
    private fun validateMessageOwnership(messageIds: List<String>, userId: String): Boolean
}

sealed class DeletionState {
    object Idle : DeletionState()
    object Deleting : DeletionState()
    data class Success(val deletedCount: Int) : DeletionState()
    data class Error(val message: String) : DeletionState()
}
```

### 4. MessageDeletionRepository

```kotlin
class MessageDeletionRepository {
    private val supabaseClient = SupabaseClient.client
    
    suspend fun deleteForMe(messageIds: List<String>, userId: String): Result<Unit>
    suspend fun deleteForEveryone(messageIds: List<String>, userId: String): Result<Unit>
    suspend fun getMessagesBySenderId(messageIds: List<String>, userId: String): List<String>
    private suspend fun insertUserDeletedMessages(messageIds: List<String>, userId: String): Result<Unit>
    private suspend fun updateMessagesAsDeleted(messageIds: List<String>, deleteForEveryone: Boolean): Result<Unit>
}
```

### 5. Updated ChatAdapter

```kotlin
class ChatAdapter(
    private val context: Context,
    private val messagesList: List<HashMap<String, Any?>>,
    private val currentUserId: String,
    private val multiSelectManager: MultiSelectManager?
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
    
    var isMultiSelectMode: Boolean = false
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        // Existing binding logic
        // Add selection indicator visibility
        // Add click/long-click handlers for multi-select
    }
    
    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val selectionCheckbox: CheckBox = view.findViewById(R.id.selection_checkbox)
        val selectionOverlay: View = view.findViewById(R.id.selection_overlay)
        // Existing view references
    }
}
```

## Data Models

### Database Schema Changes

#### New Table: user_deleted_messages

```sql
CREATE TABLE user_deleted_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    deleted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, message_id)
);

-- Index for efficient queries
CREATE INDEX idx_user_deleted_messages_user_id ON user_deleted_messages(user_id);
CREATE INDEX idx_user_deleted_messages_message_id ON user_deleted_messages(message_id);

-- RLS Policies
ALTER TABLE user_deleted_messages ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own deleted messages"
    ON user_deleted_messages FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own deleted messages"
    ON user_deleted_messages FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own deleted message records"
    ON user_deleted_messages FOR DELETE
    USING (auth.uid() = user_id);
```

#### Existing messages Table (No Changes Required)

The existing `messages` table already has the necessary fields:
- `is_deleted` (BOOLEAN): Marks if message is deleted
- `delete_for_everyone` (BOOLEAN): Indicates if deleted for all users
- `sender_id` (UUID): Message owner
- `updated_at` (TIMESTAMPTZ): Last update timestamp

### Kotlin Data Models

#### MessageDeletionRequest

```kotlin
data class MessageDeletionRequest(
    val messageIds: List<String>,
    val deleteForEveryone: Boolean,
    val userId: String
)
```

#### UserDeletedMessage

```kotlin
@Serializable
data class UserDeletedMessage(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id")
    val userId: String,
    @SerialName("message_id")
    val messageId: String,
    @SerialName("deleted_at")
    val deletedAt: Long = System.currentTimeMillis()
)
```

#### Updated Message Model

The existing `Message` model already supports deletion fields. We'll add a computed property:

```kotlin
// Extension function to check if message is deleted for specific user
fun Message.isDeletedForUser(userId: String, userDeletedMessageIds: Set<String>): Boolean {
    return when {
        deleteForEveryone && isDeleted -> true
        userDeletedMessageIds.contains(id) -> true
        else -> false
    }
}
```

## UI Components

### 1. Action Toolbar Layout (action_toolbar.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.appbar.MaterialToolbar
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/action_toolbar"
    android:layout_width="match_parent"
    android:layout_height="?attr/actionBarSize"
    android:background="?attr/colorSurfaceContainer"
    android:elevation="4dp"
    app:navigationIcon="@drawable/ic_close"
    app:menu="@menu/message_action_menu"
    style="@style/Widget.Material3.Toolbar">
    
    <TextView
        android:id="@+id/selection_count_text"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textAppearance="?attr/textAppearanceTitleLarge"
        android:textColor="?attr/colorOnSurface"
        android:text="1" />
        
</com.google.android.material.appbar.MaterialToolbar>
```

### 2. Message Action Menu (message_action_menu.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    
    <item
        android:id="@+id/action_delete"
        android:icon="@drawable/ic_delete"
        android:title="@string/delete"
        app:showAsAction="always" />
        
    <item
        android:id="@+id/action_forward"
        android:icon="@drawable/ic_forward"
        android:title="@string/forward"
        app:showAsAction="ifRoom" />
        
</menu>
```

### 3. Updated Message Item Layout

Add selection indicator to existing message layouts:

```xml
<!-- Add to chat_msg_cv_synapse.xml and other message layouts -->
<CheckBox
    android:id="@+id/selection_checkbox"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginStart="8dp"
    android:visibility="gone"
    style="@style/Widget.Material3.CompoundButton.CheckBox" />

<View
    android:id="@+id/selection_overlay"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?attr/colorPrimaryContainer"
    android:alpha="0.1"
    android:visibility="gone" />
```

### 4. Deletion Confirmation Dialog

Update existing `DeleteConfirmationDialog` to support multi-select:

```kotlin
class DeleteConfirmationDialog : DialogFragment() {
    
    companion object {
        fun newInstance(
            messageIds: List<String>,
            canDeleteForEveryone: Boolean
        ): DeleteConfirmationDialog {
            return DeleteConfirmationDialog().apply {
                arguments = Bundle().apply {
                    putStringArrayList("message_ids", ArrayList(messageIds))
                    putBoolean("can_delete_for_everyone", canDeleteForEveryone)
                }
            }
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val messageIds = arguments?.getStringArrayList("message_ids") ?: emptyList()
        val canDeleteForEveryone = arguments?.getBoolean("can_delete_for_everyone") ?: false
        val messageCount = messageIds.size
        
        val title = if (messageCount == 1) {
            getString(R.string.delete_message)
        } else {
            getString(R.string.delete_messages, messageCount)
        }
        
        // Build dialog with appropriate options
    }
}
```

## Error Handling

### Error Scenarios and Handling

1. **Database Column Not Found Error** (Current Bug)
   - Root Cause: Code attempts to update `deleted_at` column that doesn't exist
   - Solution: Remove all references to `deleted_at`, use `is_deleted` and `delete_for_everyone` instead
   - Fallback: If update fails, log error and show user-friendly message

2. **Network Failures**
   - Implement retry logic with exponential backoff (max 3 attempts)
   - Show offline indicator in UI
   - Queue deletions for retry when connection restored

3. **Permission Errors**
   - Validate message ownership before attempting "delete for everyone"
   - Show appropriate error message if user tries to delete others' messages for everyone
   - Gracefully degrade to "delete for me" option

4. **Realtime Sync Failures**
   - Implement local state updates immediately for optimistic UI
   - Revert changes if server operation fails
   - Show sync status indicator

### Error Messages

```kotlin
object DeletionErrorMessages {
    const val NETWORK_ERROR = "Unable to delete messages. Please check your connection."
    const val PERMISSION_ERROR = "You can only delete your own messages for everyone."
    const val UNKNOWN_ERROR = "Failed to delete messages. Please try again."
    const val PARTIAL_FAILURE = "Some messages could not be deleted."
}
```

## Testing Strategy

### Unit Tests

1. **MultiSelectManager Tests**
   - Test selection state management
   - Test toolbar visibility transitions
   - Test selection count updates

2. **MessageDeletionViewModel Tests**
   - Test deletion state flows
   - Test ownership validation
   - Test error handling

3. **MessageDeletionRepository Tests**
   - Mock Supabase client
   - Test delete for me operation
   - Test delete for everyone operation
   - Test error scenarios

### Integration Tests

1. **End-to-End Deletion Flow**
   - Test single message deletion
   - Test multi-message deletion
   - Test delete for me vs delete for everyone

2. **Realtime Sync Tests**
   - Test deletion event broadcasting
   - Test UI updates on receiving deletion events
   - Test conflict resolution

### UI Tests

1. **Multi-Select Interaction Tests**
   - Test long-press to enter multi-select mode
   - Test message selection/deselection
   - Test action toolbar appearance

2. **Dialog Tests**
   - Test dialog options based on ownership
   - Test dialog dismissal
   - Test deletion confirmation

## Implementation Phases

### Phase 1: Database Setup
- Create `user_deleted_messages` table
- Set up RLS policies
- Create database migration

### Phase 2: Repository Layer
- Implement `MessageDeletionRepository`
- Add delete for me operation
- Add delete for everyone operation
- Implement error handling

### Phase 3: ViewModel Layer
- Create `MessageDeletionViewModel`
- Implement state management
- Add ownership validation
- Wire up repository calls

### Phase 4: Multi-Select UI
- Create `MultiSelectManager`
- Update `ChatAdapter` for selection indicators
- Implement action toolbar
- Add selection state tracking

### Phase 5: Deletion Flow
- Create `MessageDeletionCoordinator`
- Update `DeleteConfirmationDialog`
- Wire up deletion operations
- Add success/error feedback

### Phase 6: Realtime Integration
- Subscribe to message update events
- Handle deletion broadcasts
- Update UI on realtime events
- Test sync across devices

### Phase 7: Testing & Polish
- Write unit tests
- Perform integration testing
- UI/UX polish
- Performance optimization

## Performance Considerations

1. **Batch Operations**: Delete multiple messages in a single database transaction
2. **Optimistic Updates**: Update UI immediately, revert on failure
3. **Lazy Loading**: Load user-deleted messages only when needed
4. **Caching**: Cache user-deleted message IDs in memory during chat session
5. **Debouncing**: Debounce selection changes to avoid excessive UI updates

## Security Considerations

1. **RLS Policies**: Ensure users can only mark their own messages as deleted
2. **Ownership Validation**: Server-side validation for "delete for everyone" operations
3. **Input Validation**: Validate message IDs before database operations
4. **Rate Limiting**: Implement rate limiting for deletion operations to prevent abuse

## Accessibility

1. **Screen Reader Support**: Add content descriptions for selection checkboxes
2. **Touch Targets**: Ensure minimum 48dp touch targets for selection
3. **Visual Feedback**: Provide clear visual feedback for selected state
4. **Keyboard Navigation**: Support keyboard navigation in multi-select mode (for Android TV/tablets)

## Migration Strategy

### Handling Existing Code

1. **Remove `deleted_at` References**: Search and remove all code attempting to update `deleted_at` column
2. **Update Existing Deletion Logic**: Replace with new repository-based approach
3. **Backward Compatibility**: Ensure existing messages with `is_deleted=true` display correctly
4. **Data Migration**: No data migration needed as we're using existing fields

### Rollout Plan

1. Deploy database changes (new table)
2. Deploy backend validation (if any server-side logic)
3. Deploy app update with new deletion system
4. Monitor error logs for any issues
5. Gradual rollout to users (staged release)
