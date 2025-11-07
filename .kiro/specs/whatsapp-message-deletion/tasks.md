# Implementation Plan

- [x] 1. Set up database schema and migration





  - Create the `user_deleted_messages` table in Supabase with proper columns (id, user_id, message_id, deleted_at)
  - Set up RLS policies for user-specific access control
  - Create indexes for efficient querying
  - Test the table creation and RLS policies with sample data
  - _Requirements: 6.3, 6.4_



- [x] 2. Implement MessageDeletionRepository



- [ ] 2.1 Create repository class and basic structure
  - Create `MessageDeletionRepository.kt` in the `data/repository` package
  - Add Supabase client initialization


  - Define suspend functions for delete operations
  - _Requirements: 6.4, 6.5_

- [x] 2.2 Implement delete for me operation


  - Write `deleteForMe()` function that inserts records into `user_deleted_messages` table
  - Add batch insert support for multiple message IDs
  - Implement error handling with proper Result types
  - _Requirements: 1.1, 1.4, 6.5_



- [ ] 2.3 Implement delete for everyone operation
  - Write `deleteForEveryone()` function that updates `is_deleted` and `delete_for_everyone` fields



  - Add ownership validation to ensure only message owners can delete for everyone


  - Implement batch update support
  - Remove any references to `deleted_at` column to fix the current bug
  - _Requirements: 2.1, 2.4, 2.5, 6.2_

- [x] 2.4 Add helper functions for message queries

  - Implement `getMessagesBySenderId()` to validate ownership
  - Add function to fetch user-deleted message IDs for current user
  - Implement retry logic with exponential backoff for network failures
  - _Requirements: 2.3, 6.5_




- [ ] 3. Create MessageDeletionViewModel

- [ ] 3.1 Set up ViewModel with state management
  - Create `MessageDeletionViewModel.kt` in the `presentation/viewmodel` package
  - Define `DeletionState` sealed class (Idle, Deleting, Success, Error)


  - Set up StateFlow for deletion state and SharedFlow for errors
  - Inject MessageDeletionRepository
  - _Requirements: 6.5_

- [ ] 3.2 Implement deletion business logic
  - Write `deleteMessagesForMe()` function with proper state updates
  - Write `deleteMessagesForEveryone()` function with ownership validation


  - Add `validateMessageOwnership()` helper function
  - Implement error handling and user-friendly error messages
  - _Requirements: 1.1, 2.1, 2.3, 5.4, 6.5_

- [ ] 4. Implement MultiSelectManager

- [ ] 4.1 Create MultiSelectManager class
  - Create `MultiSelectManager.kt` in the `chat` package
  - Add properties for selection state (selectedMessageIds, isMultiSelectMode)
  - Implement `enterMultiSelectMode()` and `exitMultiSelectMode()` functions
  - Add `toggleMessageSelection()` and selection query functions
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_



- [ ] 4.2 Implement action toolbar management
  - Add references to action toolbar and standard toolbar
  - Implement `showActionToolbar()` to display Material 3 action bar
  - Implement `hideActionToolbar()` to restore standard toolbar
  - Add `updateActionToolbarTitle()` to show selection count
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

- [ ] 5. Create action toolbar layout and menu

- [ ] 5.1 Design action toolbar XML layout
  - Create `action_toolbar.xml` with MaterialToolbar
  - Add close/cancel navigation icon
  - Add TextView for selection count display
  - Apply Material 3 styling and theming
  - _Requirements: 4.1, 4.2, 4.6_


- [ ] 5.2 Create message action menu
  - Create `message_action_menu.xml` with delete action item
  - Add delete icon with proper content description
  - Configure action item to always show in toolbar
  - _Requirements: 4.4_

- [ ] 6. Update ChatAdapter for multi-select support

- [ ] 6.1 Add selection UI components to message layouts
  - Add CheckBox for selection indicator to message item layouts
  - Add selection overlay View with Material 3 styling
  - Set initial visibility to GONE
  - Update all message layout variants (sent, received, media, etc.)
  - _Requirements: 3.2, 8.1_


- [ ] 6.2 Implement selection logic in adapter
  - Add `isMultiSelectMode` property to ChatAdapter
  - Pass MultiSelectManager reference to adapter
  - Update `onBindViewHolder()` to show/hide selection indicators based on mode
  - Implement click handler to toggle selection in multi-select mode
  - Implement long-click handler to enter multi-select mode
  - Add visual feedback for selected messages (checkbox checked, overlay visible)
  - _Requirements: 3.1, 3.2, 3.3, 8.2_

- [ ] 7. Create MessageDeletionCoordinator

- [ ] 7.1 Implement coordinator class
  - Create `MessageDeletionCoordinator.kt` in the `chat` package
  - Add constructor parameters for activity, viewModel, and currentUserId

  - Implement `initiateDelete()` function to start deletion flow
  - Add `validateOwnership()` helper to check if user owns all selected messages
  - _Requirements: 2.3, 5.4_

- [ ] 7.2 Implement deletion dialog flow
  - Update `showDeletionDialog()` to handle single and multiple messages
  - Pass ownership validation result to determine available options
  - Wire up dialog callbacks to execute deletion operations
  - Implement `executeDelete()` to call ViewModel functions
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

- [ ] 8. Update DeleteConfirmationDialog

- [ ] 8.1 Modify dialog to support multi-select
  - Update `newInstance()` to accept List<String> of message IDs
  - Add parameter for `canDeleteForEveryone` boolean
  - Update dialog title to show "Delete message?" or "Delete messages?" based on count
  - _Requirements: 5.1, 5.2_

- [ ] 8.2 Implement conditional option display
  - Show both "Delete for everyone" and "Delete for me" when user owns all messages
  - Show only "Delete for me" when any message is not owned by user
  - Add "Cancel" button to dismiss dialog
  - Wire up button clicks to callback interface
  - _Requirements: 5.3, 5.4, 5.5_

- [ ] 9. Integrate components in ChatActivity


- [ ] 9.1 Initialize multi-select components
  - Instantiate MultiSelectManager with activity and adapter references
  - Instantiate MessageDeletionViewModel using ViewModelProvider
  - Instantiate MessageDeletionCoordinator with required dependencies
  - Add action toolbar layout to activity_chat.xml
  - _Requirements: 3.1, 4.1_

- [ ] 9.2 Set up action toolbar click handlers
  - Implement navigation icon click to exit multi-select mode
  - Implement delete menu item click to show deletion dialog
  - Wire up MultiSelectManager to handle toolbar interactions
  - _Requirements: 4.3, 4.4, 4.5_

- [ ] 9.3 Observe ViewModel state changes
  - Collect deletionState flow in lifecycleScope
  - Show loading indicator during Deleting state
  - Exit multi-select mode and refresh messages on Success state
  - Show error toast on Error state
  - Collect errorState flow and display error messages
  - _Requirements: 6.5_


- [ ] 9.4 Update adapter initialization
  - Pass MultiSelectManager reference to ChatAdapter constructor
  - Set up adapter callbacks for long-click to enter multi-select mode
  - Ensure adapter notifies MultiSelectManager of selection changes
  - _Requirements: 3.1, 3.2_

- [ ] 10. Implement Realtime sync for deletions

- [ ] 10.1 Subscribe to message update events
  - Update existing Realtime subscription to listen for message updates
  - Filter for updates where `is_deleted` or `delete_for_everyone` changed
  - Handle deletion events in `handleMessageUpdate()` callback
  - _Requirements: 7.1, 7.2_


- [ ] 10.2 Update UI on deletion events
  - Refresh message display when deletion event received
  - Update message content to show "This message was deleted" placeholder
  - Ensure smooth UI updates without disrupting scroll position
  - Handle edge case where deleted message is currently selected
  - _Requirements: 1.2, 2.2, 7.2, 7.3_

- [ ] 10.3 Manage Realtime subscription lifecycle
  - Ensure subscription is active when chat screen is visible
  - Unsubscribe when chat screen is closed to conserve resources
  - Handle reconnection scenarios gracefully
  - _Requirements: 7.4_


- [ ] 11. Implement message display logic for deleted messages

- [ ] 11.1 Update message rendering in adapter
  - Check if message is deleted for everyone (use `isDeleted` && `deleteForEveryone`)
  - Check if message is deleted for current user (query `user_deleted_messages` table)
  - Display "This message was deleted" for messages deleted for everyone
  - Display "You deleted this message" for messages deleted for current user only
  - Hide media content and attachments for deleted messages
  - _Requirements: 1.2, 2.2_

- [ ] 11.2 Load user-deleted messages on chat open
  - Query `user_deleted_messages` table for current user and chat
  - Store deleted message IDs in memory for quick lookup
  - Use cached IDs to determine message visibility during rendering
  - _Requirements: 1.3, 1.4_


- [ ] 12. Handle multi-select mode stability

- [ ] 12.1 Prevent disruptions during selection
  - Disable automatic scrolling when in multi-select mode
  - Disable message animations when in multi-select mode
  - Queue incoming messages for display after exiting multi-select mode
  - _Requirements: 8.1, 8.2, 8.3_

- [ ] 12.2 Maintain scroll position
  - Save scroll position when entering multi-select mode
  - Restore scroll position when exiting multi-select mode
  - Ensure selection indicators don't cause layout shifts
  - _Requirements: 8.4_

- [ ] 13. Add error handling and user feedback

- [ ] 13.1 Implement error messages
  - Create string resources for all error scenarios
  - Display appropriate error message for network failures
  - Display permission error when trying to delete others' messages for everyone
  - Show generic error message for unknown failures
  - _Requirements: 6.5_

- [ ] 13.2 Add loading and success feedback
  - Show progress indicator during deletion operation
  - Display success toast after successful deletion
  - Provide haptic feedback on selection changes
  - Add visual confirmation when exiting multi-select mode
  - _Requirements: 5.6_

- [ ] 14. Optimize performance

- [ ] 14.1 Implement batch operations
  - Ensure repository performs batch inserts/updates in single transaction
  - Optimize database queries to minimize round trips
  - Use prepared statements where applicable
  - _Requirements: 1.1, 2.1_

- [ ] 14.2 Add caching and optimistic updates
  - Cache user-deleted message IDs in memory during chat session
  - Implement optimistic UI updates (update UI before server confirms)
  - Revert optimistic updates if server operation fails
  - Debounce selection changes to reduce UI update frequency
  - _Requirements: 7.2_

- [ ] 15. Final testing and bug fixes

- [ ] 15.1 Test single message deletion
  - Test delete for me on own message
  - Test delete for me on others' message
  - Test delete for everyone on own message
  - Verify error when trying to delete others' message for everyone
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3_

- [ ] 15.2 Test multi-message deletion
  - Test selecting multiple messages
  - Test deleting multiple own messages for everyone
  - Test deleting mix of own and others' messages (should only allow delete for me)
  - Verify selection count updates correctly
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 5.2, 5.3, 5.4_

- [ ] 15.3 Test UI interactions
  - Test entering multi-select mode via long-press
  - Test exiting multi-select mode via close button
  - Test action toolbar appearance and disappearance
  - Test selection indicators visibility
  - Verify Material 3 styling is applied correctly
  - _Requirements: 3.1, 3.5, 4.1, 4.2, 4.3, 4.5, 4.6_

- [ ] 15.4 Test Realtime sync
  - Test deletion updates across multiple devices
  - Verify deleted messages appear correctly on all devices
  - Test reconnection scenarios
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [ ] 15.5 Test edge cases and error scenarios
  - Test deletion with poor network connection
  - Test deletion when offline (should show error)
  - Test rapid selection/deselection
  - Test deleting messages while new messages arrive
  - Verify scroll position stability during multi-select
  - _Requirements: 6.5, 8.1, 8.2, 8.3, 8.4_
