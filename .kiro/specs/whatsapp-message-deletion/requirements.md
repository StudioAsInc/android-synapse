# Requirements Document

## Introduction

This feature implements a WhatsApp-like message deletion system for the chat functionality. Users can delete messages either for themselves only or for everyone in the chat. The feature includes multi-select capability with a Material 3 top app bar (toolbar) that displays action options when messages are selected. The current implementation fails because it attempts to update a non-existent `deleted_at` column in the database.

## Glossary

- **ChatApp**: The Android application that provides messaging functionality
- **MessageDeletionSystem**: The subsystem responsible for handling message deletion operations
- **MultiSelectMode**: The UI state where users can select multiple messages for batch operations
- **ActionToolbar**: The Material 3 top app bar that appears during multi-select mode
- **SupabaseBackend**: The backend database system storing message data
- **DeleteForMe**: A deletion operation that marks messages as deleted only for the current user
- **DeleteForEveryone**: A deletion operation that marks messages as deleted for all chat participants
- **MessageTable**: The database table storing message records

## Requirements

### Requirement 1

**User Story:** As a chat user, I want to delete messages for myself only, so that I can remove messages from my view without affecting other participants

#### Acceptance Criteria

1. WHEN the user selects "Delete for me" on a message, THE ChatApp SHALL update the message record to mark it as deleted for the current user only
2. WHILE a message is marked as deleted for the current user, THE ChatApp SHALL display a placeholder text "You deleted this message" in place of the message content
3. WHEN a message is deleted for the current user, THE ChatApp SHALL preserve the message content for other chat participants
4. THE ChatApp SHALL store user-specific deletion information in a way that does not require a `deleted_at` column

### Requirement 2

**User Story:** As a chat user, I want to delete messages for everyone in the chat, so that I can remove inappropriate or incorrect messages from all participants' views

#### Acceptance Criteria

1. WHEN the user selects "Delete for everyone" on their own message, THE ChatApp SHALL update the message record to mark it as deleted for all participants
2. WHILE a message is deleted for everyone, THE ChatApp SHALL display a placeholder text "This message was deleted" for all chat participants
3. IF the user attempts to delete another user's message for everyone, THEN THE ChatApp SHALL prevent the operation and display an error message
4. WHEN a message is deleted for everyone, THE ChatApp SHALL update the `is_deleted` and `delete_for_everyone` fields in the MessageTable
5. THE ChatApp SHALL complete the deletion operation without attempting to update a `deleted_at` column

### Requirement 3

**User Story:** As a chat user, I want to select multiple messages at once, so that I can efficiently delete several messages in a single operation

#### Acceptance Criteria

1. WHEN the user long-presses on a message, THE ChatApp SHALL enter MultiSelectMode and select the pressed message
2. WHILE in MultiSelectMode, THE ChatApp SHALL display checkboxes or selection indicators on all messages
3. WHEN the user taps on additional messages in MultiSelectMode, THE ChatApp SHALL toggle the selection state of those messages
4. THE ChatApp SHALL maintain a list of selected message IDs during MultiSelectMode
5. WHEN the user exits MultiSelectMode, THE ChatApp SHALL clear all selections and hide selection indicators

### Requirement 4

**User Story:** As a chat user, I want to see a Material 3 action toolbar when I select messages, so that I can easily access deletion and other message actions

#### Acceptance Criteria

1. WHEN the ChatApp enters MultiSelectMode, THE ActionToolbar SHALL replace the standard top app bar
2. THE ActionToolbar SHALL display the count of selected messages in the title area
3. THE ActionToolbar SHALL include a close/cancel button that exits MultiSelectMode
4. THE ActionToolbar SHALL include a delete button that triggers the deletion confirmation dialog
5. WHEN the user exits MultiSelectMode, THE ChatApp SHALL restore the standard top app bar
6. THE ActionToolbar SHALL follow Material 3 design guidelines for styling and behavior

### Requirement 5

**User Story:** As a chat user, I want to see a confirmation dialog before deleting messages, so that I can choose between deletion options and avoid accidental deletions

#### Acceptance Criteria

1. WHEN the user taps the delete button in the ActionToolbar, THE ChatApp SHALL display a deletion confirmation dialog
2. THE confirmation dialog SHALL include the title "Delete message?" or "Delete messages?" based on selection count
3. IF the user owns all selected messages, THEN THE confirmation dialog SHALL display both "Delete for everyone" and "Delete for me" options
4. IF any selected message is not owned by the user, THEN THE confirmation dialog SHALL display only the "Delete for me" option
5. THE confirmation dialog SHALL include a "Cancel" button that dismisses the dialog without deleting messages
6. WHEN the user selects a deletion option, THE ChatApp SHALL execute the corresponding deletion operation for all selected messages

### Requirement 6

**User Story:** As a developer, I want the deletion system to work with the existing database schema, so that I don't need to modify the database structure

#### Acceptance Criteria

1. THE MessageDeletionSystem SHALL use the existing `is_deleted` and `delete_for_everyone` boolean fields in the MessageTable
2. THE MessageDeletionSystem SHALL NOT attempt to update or reference a `deleted_at` timestamp column
3. WHERE user-specific deletion tracking is needed, THE MessageDeletionSystem SHALL use a separate table or JSONB field to store deleted message IDs per user
4. THE MessageDeletionSystem SHALL handle all database operations through the SupabaseBackend singleton client
5. WHEN a database operation fails, THE MessageDeletionSystem SHALL log the error and display a user-friendly error message

### Requirement 7

**User Story:** As a chat user, I want deleted messages to update in real-time across all devices, so that deletion changes are immediately visible to all participants

#### Acceptance Criteria

1. WHEN a message is deleted for everyone, THE ChatApp SHALL use Supabase Realtime to broadcast the deletion event
2. WHEN the ChatApp receives a deletion event via Realtime, THE ChatApp SHALL update the message display immediately
3. THE ChatApp SHALL subscribe to message update events for the current chat when the chat screen is active
4. WHEN the chat screen is closed, THE ChatApp SHALL unsubscribe from Realtime events to conserve resources

### Requirement 8

**User Story:** As a chat user, I want the message list to remain stable during multi-select operations, so that I can accurately select the messages I intend to delete

#### Acceptance Criteria

1. WHILE in MultiSelectMode, THE ChatApp SHALL prevent automatic scrolling or list updates that would disrupt user selection
2. WHILE in MultiSelectMode, THE ChatApp SHALL disable message animations that could cause position changes
3. WHEN new messages arrive during MultiSelectMode, THE ChatApp SHALL queue them for display after exiting MultiSelectMode
4. THE ChatApp SHALL maintain scroll position when entering and exiting MultiSelectMode
