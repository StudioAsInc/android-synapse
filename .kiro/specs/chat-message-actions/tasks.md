# Implementation Plan

- [x] 1. Set up database schema and RLS policies





  - [x] 1.1 Create database migration for message schema extensions


    - Add `edit_history` JSONB column to messages table
    - Add `forwarded_from_message_id` TEXT column to messages table
    - Add `forwarded_from_chat_id` TEXT column to messages table
    - Add `delete_for_everyone` BOOLEAN column to messages table with default FALSE
    - _Requirements: 5.5, 3.5, 4.4_

  - [x] 1.2 Create message_forwards table

    - Create table with columns: id, original_message_id, original_chat_id, forwarded_message_id, forwarded_chat_id, forwarded_by, forwarded_at
    - Add foreign key constraints to messages, chats, and users tables
    - Add unique constraint on (original_message_id, forwarded_chat_id)
    - _Requirements: 3.5, 8.1_

  - [x] 1.3 Create ai_summaries table

    - Create table with columns: id, message_id, summary_text, generated_at, generated_by, model_version, character_count
    - Add foreign key constraints to messages and users tables
    - Add unique constraint on message_id
    - _Requirements: 6.5, 8.1_

  - [x] 1.4 Implement Row Level Security policies

    - Create policy "Users can edit own messages" with 48-hour time limit check
    - Create policy "Users can delete own messages" for message owners
    - Create policy "Users can forward messages" for chat participants
    - Create policy "Users can view forward history" for authorized users
    - Test policies with different user contexts
    - _Requirements: 5.1, 4.1, 8.2, 8.3_

- [x] 2. Create GeminiAIService for AI summarization




  - [x] 2.1 Set up Gemini API configuration


    - Add GEMINI_API_KEY to gradle.properties
    - Add BuildConfig field for API key in build.gradle.kts
    - Create GeminiAIService.kt with API endpoint constants
    - _Requirements: 6.3, 6.6_

  - [x] 2.2 Implement summary generation method

    - Create `generateSummary(text: String, maxLength: Int)` suspend function
    - Build JSON request with summarization prompt
    - Use OkHttp to make POST request to Gemini API
    - Parse JSON response to extract summary text
    - Return Result<String> with success or error
    - _Requirements: 6.3, 6.4_

  - [x] 2.3 Implement rate limiting and error handling

    - Detect 429 status codes for rate limiting
    - Store rate limit reset time in SharedPreferences
    - Implement `isRateLimited()` and `getRateLimitResetTime()` methods
    - Add retry logic with exponential backoff for network failures
    - Set 10-second timeout for API requests
    - Log all errors with full context using Log.e
    - _Requirements: 6.6, 6.9_

  - [x] 2.4 Add character count and reading time calculation

    - Calculate character count from message text
    - Estimate reading time (average 200 words per minute)
    - Include in summary response data
    - _Requirements: 6.7_

- [x] 3. Create MessageActionRepository data layer





  - [x] 3.1 Implement forward message operations


    - Create `forwardMessageToChat(messageData, targetChatId)` method
    - Copy message content, attachments, and formatting to target chat
    - Store forward relationship in message_forwards table
    - Add "Forwarded" label to forwarded message metadata
    - Create `forwardMessageToMultipleChats(messageData, targetChatIds)` for batch forwarding
    - Return success count for partial failures
    - _Requirements: 3.4, 3.5, 3.6, 3.7_

  - [x] 3.2 Implement edit message operations


    - Create `editMessage(messageId, newContent)` method using existing SupabaseChatService.editMessage()
    - Validate message age (must be <48 hours old)
    - Validate non-empty content
    - Store previous content in edit_history JSONB array
    - Update is_edited flag and edited_at timestamp
    - Create `getEditHistory(messageId)` method to retrieve edit history
    - _Requirements: 5.2, 5.3, 5.5, 5.8_

  - [x] 3.3 Implement delete message operations


    - Create `deleteMessageLocally(messageId)` method for local-only deletion
    - Mark message as deleted in local cache without database update
    - Create `deleteMessageForEveryone(messageId)` method using SupabaseChatService.deleteMessage()
    - Update delete_for_everyone flag in database
    - Remove media attachment URLs from deleted messages
    - _Requirements: 4.3, 4.4, 4.5, 4.6_

  - [x] 3.4 Implement AI summary caching


    - Create `getCachedSummary(messageId)` method using SharedPreferences
    - Create `cacheSummary(messageId, summary)` method with 7-day expiration
    - Implement LRU cache eviction (max 100 summaries)
    - Create `clearSummaryCache()` method for cache management
    - _Requirements: 6.5_

- [x] 4. Create MessageActionsViewModel





  - [x] 4.1 Set up ViewModel with dependencies


    - Create MessageActionsViewModel extending ViewModel
    - Inject MessageActionRepository, GeminiAIService, SupabaseChatService
    - Define state classes: MessageActionState, ForwardState, AISummaryState
    - Initialize StateFlow for each action type
    - _Requirements: 8.1_

  - [x] 4.2 Implement reply preparation method

    - Create `prepareReply(messageId, messageText, senderName)` method
    - Emit state with reply preview data
    - Truncate message text to 3 lines for preview
    - _Requirements: 2.1, 2.2_

  - [x] 4.3 Implement forward message method

    - Create `forwardMessage(messageId, targetChatIds)` method returning Flow<ForwardState>
    - Emit Loading state at start
    - Call repository.forwardMessageToMultipleChats()
    - Emit Success state with forwarded count
    - Handle partial failures (some chats succeed, others fail)
    - Emit Error state for complete failures
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.7_


  - [ ] 4.4 Implement edit message method
    - Create `editMessage(messageId, newContent)` method returning Flow<MessageActionState>
    - Validate message age (<48 hours)
    - Validate non-empty content
    - Emit Loading state during save
    - Call repository.editMessage()
    - Emit Success state with confirmation message
    - Emit Error state for validation or save failures
    - _Requirements: 5.2, 5.3, 5.8_


  - [ ] 4.5 Implement delete message method
    - Create `deleteMessage(messageId, deleteForEveryone)` method returning Flow<MessageActionState>
    - Emit Loading state during deletion
    - Call appropriate repository method based on deleteForEveryone flag
    - Emit Success state with confirmation
    - Emit Error state for failures with retry option

    - _Requirements: 4.2, 4.3, 4.4, 4.7_

  - [ ] 4.6 Implement AI summary generation method
    - Create `generateAISummary(messageText)` method returning Flow<AISummaryState>
    - Check if summary is cached first
    - Emit Loading state during generation
    - Call GeminiAIService.generateSummary()
    - Cache successful summary
    - Emit Success state with summary, character count, and read time
    - Handle rate limiting with reset time
    - Emit Error state for failures
    - _Requirements: 6.2, 6.3, 6.4, 6.5, 6.7, 6.9_
-

- [-] 5. Create MessageActionsBottomSheet UI component



  - [x] 5.1 Create bottom sheet layout XML

    - Create res/layout/bottom_sheet_message_actions.xml
    - Add TextView for message preview (max 2 lines with ellipsis)
    - Add RecyclerView for action items list
    - Add divider between preview and actions
    - Apply Material Design styling with rounded top corners
    - _Requirements: 1.1, 1.2_



  - [ ] 5.2 Create action item layout XML
    - Create res/layout/item_message_action.xml
    - Add horizontal LinearLayout with icon and label
    - Add ImageView for action icon (24dp)
    - Add TextView for action label
    - Add ripple effect for touch feedback
    - Apply proper spacing (16dp padding)
    - _Requirements: 7.6_

  - [ ] 5.3 Implement MessageActionsBottomSheet class


    - Create MessageActionsBottomSheet.kt extending BottomSheetDialogFragment
    - Define MessageAction data class with id, label, icon, isDestructive
    - Implement show() method accepting messageData and listener
    - Set up RecyclerView with action items adapter
    - Implement dismiss on outside tap and back button
    - Add haptic feedback on sheet appearance
    - _Requirements: 1.1, 1.2, 1.3, 1.5_

  - [ ] 5.4 Implement action filtering logic
    - Create `getAvailableActions(messageData, currentUserId)` method
    - Filter out Edit and Delete for Everyone for messages from other users
    - Filter out Edit for media-only messages
    - Filter out all actions for system messages
    - Filter out all actions for already deleted messages
    - Filter out AI Summary for messages <100 characters
    - Return actions in order: Reply, Forward, Edit, Delete, AI Summary
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 6.1_

  - [ ] 5.5 Implement action selection handling
    - Create `onActionSelected(action)` method
    - Dismiss bottom sheet on action selection
    - Call appropriate listener callback based on action ID
    - Pass necessary data (messageId, messageText, etc.) to callbacks
    - _Requirements: 1.5_



- [ ] 6. Integrate long-press detection in ChatAdapter


  - [ ] 6.1 Add long-press listener to message bubbles
    - Update bindCommonMessageProperties() in ChatAdapter.kt
    - Add OnLongClickListener to itemView
    - Trigger haptic feedback on long-press
    - Call listener.onMessageLongClick(messageId, position)
    - Return true to consume the event
    - _Requirements: 1.1, 1.3_

  - [ ] 6.2 Extend ChatAdapterListener interface
    - Add onReplyAction(messageId, messageText, senderName) method
    - Add onForwardAction(messageId, messageData) method
    - Add onEditAction(messageId, currentText) method
    - Add onDeleteAction(messageId, deleteForEveryone) method
    - Add onAISummaryAction(messageId, messageText) method
    - Update ChatAdapter to use new interface methods
    - _Requirements: 2.1, 3.1, 5.1, 4.1, 6.1_

- [ ] 7. Create ForwardMessageDialog



  - [ ] 7.1 Create forward dialog layout XML
    - Create res/layout/dialog_forward_message.xml
    - Add EditText for conversation search with search icon
    - Add RecyclerView for conversations list
    - Add TextView for selected count display
    - Add Cancel and Forward buttons in horizontal layout
    - Apply Material Design dialog styling
    - _Requirements: 3.1, 3.3_

  - [ ] 7.2 Create conversation item layout XML
    - Create res/layout/item_forward_conversation.xml
    - Add CheckBox for selection
    - Add CircleImageView for avatar
    - Add TextView for conversation name
    - Add TextView for participant count (groups only)
    - Add proper spacing and alignment
    - _Requirements: 3.1_

  - [ ] 7.3 Implement ForwardMessageDialog class
    - Create ForwardMessageDialog.kt extending DialogFragment
    - Define ForwardableConversation data class
    - Load user's conversations from SupabaseChatService.getUserChats()
    - Set up RecyclerView with conversations adapter
    - Implement multi-select with checkbox state management
    - Update selected count TextView on selection changes
    - _Requirements: 3.1, 3.2_

  - [ ] 7.4 Implement search/filter functionality
    - Add TextWatcher to search EditText
    - Filter conversations by display name
    - Update RecyclerView with filtered results
    - Show "No conversations found" message when empty
    - _Requirements: 3.3_

  - [ ] 7.5 Implement forward confirmation
    - Disable Forward button when no conversations selected
    - Show loading indicator during forward operation
    - Call viewModel.forwardMessage() with selected chat IDs
    - Display success message with forwarded count
    - Handle partial failures with appropriate messaging
    - Dismiss dialog on success
    - _Requirements: 3.4, 3.7_

- [ ] 8. Create EditMessageDialog
  - [ ] 8.1 Create edit dialog layout XML
    - Create res/layout/dialog_edit_message.xml
    - Add TextView for "Edit Message" title
    - Add EditText for message content (multiline, max 5 lines)
    - Add TextView for character counter
    - Add Cancel and Save buttons in horizontal layout
    - Apply Material Design dialog styling
    - _Requirements: 5.2_

  - [ ] 8.2 Implement EditMessageDialog class
    - Create EditMessageDialog.kt extending DialogFragment
    - Pre-populate EditText with current message text
    - Set cursor to end of text
    - Focus EditText and show keyboard on dialog open
    - _Requirements: 5.2_

  - [ ] 8.3 Implement character counter
    - Add TextWatcher to EditText
    - Update character count TextView on text change
    - Show count as "X / 5000" format
    - Change color to warning when approaching limit
    - _Requirements: 5.2_

  - [ ] 8.4 Implement save validation and submission
    - Disable Save button for empty text
    - Validate message age (<48 hours) before allowing edit
    - Show loading indicator during save
    - Call viewModel.editMessage() with new content
    - Display success or error message
    - Dismiss dialog on success
    - _Requirements: 5.2, 5.3, 5.8_

- [ ] 9. Create EditHistoryDialog
  - [ ] 9.1 Create edit history layout XML
    - Create res/layout/dialog_edit_history.xml
    - Add TextView for "Edit History" title
    - Add RecyclerView for edit history items
    - Add Close button
    - Apply Material Design dialog styling
    - _Requirements: 5.6_

  - [ ] 9.2 Create edit history item layout XML
    - Create res/layout/item_edit_history.xml
    - Add TextView for timestamp
    - Add TextView for previous content (max 3 lines)
    - Add divider between items
    - Apply proper spacing
    - _Requirements: 5.6_

  - [ ] 9.3 Implement EditHistoryDialog class
    - Create EditHistoryDialog.kt extending DialogFragment
    - Load edit history from repository.getEditHistory()
    - Display history items in chronological order (newest first)
    - Format timestamps as relative time (e.g., "2 hours ago")
    - Show "No edit history" message if empty
    - _Requirements: 5.6_

- [ ] 10. Create AISummaryDialog
  - [ ] 10.1 Create AI summary layout XML
    - Create res/layout/dialog_ai_summary.xml
    - Add TextView for "AI Summary" title
    - Add ProgressBar for loading state
    - Add TextView for summary text
    - Add TextView for character count and read time
    - Add horizontal divider
    - Add TextView for original message (collapsible)
    - Add Copy and Close buttons
    - Apply Material Design dialog styling
    - _Requirements: 6.4, 6.7_

  - [ ] 10.2 Implement AISummaryDialog class
    - Create AISummaryDialog.kt extending DialogFragment
    - Show loading indicator while generating summary
    - Display summary text when ready
    - Show character count as "X characters"
    - Show estimated read time as "~X min read"
    - Implement collapsible original message section
    - _Requirements: 6.2, 6.4, 6.7_

  - [ ] 10.3 Implement copy to clipboard functionality
    - Add click listener to Copy button
    - Copy summary text to ClipboardManager
    - Show toast confirmation "Summary copied"
    - _Requirements: 6.8_

  - [ ] 10.4 Implement error state display
    - Show error message when summary generation fails
    - Display rate limit message with countdown timer
    - Show "Message too short" for messages <100 characters
    - Provide retry button for transient errors
    - _Requirements: 6.6, 6.9_

- [ ] 11. Implement reply functionality in chat UI
  - [ ] 11.1 Create reply preview layout
    - Create res/layout/layout_reply_preview.xml (or update existing chat_reply_layout.xml)
    - Add horizontal layout with close button and reply content
    - Add TextView for sender name
    - Add TextView for message preview (max 3 lines)
    - Add ImageView for media preview (if applicable)
    - Apply Material Design styling with accent color
    - _Requirements: 2.1, 2.2_

  - [ ] 11.2 Implement reply preview display logic
    - Show reply preview above message input when reply action triggered
    - Populate preview with sender name and message text
    - Truncate message text to 3 lines with ellipsis
    - Show media thumbnail if original message has attachments
    - _Requirements: 2.1, 2.2_

  - [ ] 11.3 Implement reply cancellation
    - Add click listener to close button in reply preview
    - Hide reply preview on close button click
    - Clear reply state in ViewModel
    - _Requirements: 2.6_

  - [ ] 11.4 Implement reply message sending
    - Include replied_message_id when sending message with active reply
    - Store reply reference in messages table
    - Clear reply preview after message sent
    - _Requirements: 2.3_

  - [ ] 11.5 Implement reply indicator in message bubbles
    - Update chat bubble layouts to show reply indicator
    - Display quoted message content in compact format
    - Show sender name of quoted message
    - Limit quoted text to 2 lines with ellipsis
    - Apply distinct styling (border, background color)
    - _Requirements: 2.4_

  - [ ] 11.6 Implement scroll to original message
    - Add click listener to reply indicator in message bubbles
    - Find original message position in RecyclerView
    - Scroll to original message with smooth animation
    - Highlight original message briefly (flash animation)
    - Handle case where original message is not loaded (load more messages)
    - _Requirements: 2.5_

- [ ] 12. Implement delete confirmation dialog
  - [ ] 12.1 Create delete confirmation layout XML
    - Create res/layout/dialog_delete_message.xml
    - Add TextView for confirmation message
    - Add RadioGroup with two options: "Delete for me" and "Delete for everyone"
    - Add explanation text for each option
    - Add Cancel and Delete buttons
    - Apply Material Design dialog styling with warning colors
    - _Requirements: 4.2_

  - [ ] 12.2 Implement DeleteConfirmationDialog class
    - Create DeleteConfirmationDialog.kt extending DialogFragment
    - Set default selection to "Delete for me"
    - Disable "Delete for everyone" option for messages from other users
    - Show warning icon for destructive action
    - _Requirements: 4.1, 4.2_

  - [ ] 12.3 Implement delete confirmation handling
    - Get selected option from RadioGroup
    - Call viewModel.deleteMessage() with appropriate flag
    - Show loading indicator during deletion
    - Display success or error message
    - Dismiss dialog on success
    - _Requirements: 4.3, 4.4, 4.7_

- [ ] 13. Implement deleted message display
  - [ ] 13.1 Update message bubble layouts for deleted state
    - Add deleted message placeholder layout to chat bubble XMLs
    - Show "This message was deleted" text in italic
    - Apply distinct styling (gray background, lighter text)
    - Hide message content and attachments
    - Keep timestamp and sender info visible
    - _Requirements: 4.5_

  - [ ] 13.2 Update ChatAdapter to handle deleted messages
    - Check is_deleted flag in bindCommonMessageProperties()
    - Show deleted placeholder instead of message content
    - Hide media attachments for deleted messages
    - Prevent long-press actions on deleted messages
    - _Requirements: 4.5, 4.6, 7.4_

- [ ] 14. Implement forwarded message display
  - [ ] 14.1 Update message bubble layouts for forwarded indicator
    - Add "Forwarded" label to chat bubble layouts
    - Position label above message content
    - Apply distinct styling (icon + text, muted color)
    - _Requirements: 3.6_

  - [ ] 14.2 Update ChatAdapter to show forwarded indicator
    - Check forwarded_from_message_id field in message data
    - Show "Forwarded" label when field is present
    - Preserve all message content and formatting
    - _Requirements: 3.5, 3.6_

- [ ] 15. Implement edited message display
  - [ ] 15.1 Update message bubble layouts for edited indicator
    - Add "(edited)" label to message timestamp area
    - Apply distinct styling (smaller font, muted color)
    - Make label clickable for edit history
    - _Requirements: 5.4_

  - [ ] 15.2 Update ChatAdapter to show edited indicator
    - Check is_edited flag in message data
    - Append "(edited)" to timestamp when flag is true
    - Add click listener to show edit history dialog
    - _Requirements: 5.4, 5.6_

- [ ] 16. Implement real-time synchronization
  - [ ] 16.1 Set up Supabase Realtime channel for messages
    - Create Realtime channel subscription in chat Activity/Fragment
    - Subscribe to UPDATE events on messages table filtered by chat_id
    - Handle connection and disconnection events
    - _Requirements: 8.3, 8.4_

  - [ ] 16.2 Handle real-time message edits
    - Listen for UPDATE events with is_edited flag change
    - Update message in RecyclerView adapter data
    - Refresh affected message view
    - Show brief animation to indicate update
    - _Requirements: 8.4_

  - [ ] 16.3 Handle real-time message deletions
    - Listen for UPDATE events with is_deleted flag change
    - Update message in RecyclerView adapter data
    - Replace message content with deleted placeholder
    - Show brief animation to indicate deletion
    - _Requirements: 8.4_

  - [ ] 16.4 Handle real-time forwarded messages
    - Listen for INSERT events on messages table
    - Check if new message is in current chat
    - Add message to RecyclerView adapter data
    - Scroll to new message if user is at bottom
    - Show notification if user is scrolled up
    - _Requirements: 8.4_

- [ ] 17. Implement offline queueing for message actions
  - [ ] 17.1 Create action queue data structure
    - Define PendingAction data class with action type, messageId, and parameters
    - Create ActionQueue class using Room database or SharedPreferences
    - Implement add, remove, and getAll methods
    - _Requirements: 8.5_

  - [ ] 17.2 Queue actions when offline
    - Detect network connectivity state
    - Queue edit, delete, and forward actions when offline
    - Show "Pending" indicator on affected messages
    - Store action data for later execution
    - _Requirements: 8.5_

  - [ ] 17.3 Process queued actions when online
    - Listen for network connectivity changes
    - Process queued actions in order when connection restored
    - Update UI to reflect action completion
    - Remove successfully processed actions from queue
    - Retry failed actions with exponential backoff
    - _Requirements: 8.5_

- [ ] 18. Add comprehensive error handling
  - [ ] 18.1 Implement user-friendly error messages
    - Create string resources for all error scenarios
    - Map technical errors to user-friendly messages
    - Show errors in Snackbar with appropriate duration
    - Include retry action for transient errors
    - _Requirements: 4.7, 6.6_

  - [ ] 18.2 Implement technical error logging
    - Add Log.e() calls for all error scenarios
    - Include full exception stack traces
    - Log message IDs and user IDs for debugging
    - Add error context (action type, parameters)
    - _Requirements: 6.6_

  - [ ] 18.3 Implement retry logic for network failures
    - Detect network errors (timeout, connection refused, etc.)
    - Implement exponential backoff (1s, 2s, 4s)
    - Maximum 3 retry attempts
    - Show retry button for user-initiated retry
    - _Requirements: 8.5_

- [ ] 19. Add animations and transitions
  - [ ] 19.1 Implement bottom sheet animations
    - Add slide-up animation for bottom sheet appearance
    - Add slide-down animation for bottom sheet dismissal
    - Add fade animation for background dim
    - Set animation duration to 200ms
    - _Requirements: 1.2, 1.4_

  - [ ] 19.2 Implement message update animations
    - Add fade animation for edited message updates
    - Add slide-out animation for deleted messages
    - Add highlight flash for scrolled-to messages
    - Add ripple effect for action selections
    - _Requirements: 8.4_

  - [ ] 19.3 Implement loading state animations
    - Add progress indicator for async operations
    - Add shimmer effect for loading summaries
    - Add pulse animation for pending actions
    - Ensure smooth transitions between states
    - _Requirements: 6.2_

- [ ] 20. Testing and validation
  - [ ] 20.1 Test bottom sheet functionality
    - Verify long-press triggers bottom sheet after 500ms
    - Verify haptic feedback on long-press
    - Verify background dims when sheet appears
    - Verify sheet dismisses on outside tap and back button
    - Verify actions filtered correctly based on message context
    - Test on different screen sizes and orientations
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 7.1, 7.2, 7.3, 7.4, 7.5_

  - [ ] 20.2 Test reply functionality end-to-end
    - Verify reply preview shows correctly
    - Verify reply preview cancellation works
    - Verify reply message includes correct reference
    - Verify reply indicator displays in message bubble
    - Verify scroll to original message works
    - Test with text messages, media messages, and deleted messages
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [ ] 20.3 Test forward functionality end-to-end
    - Verify conversation selection dialog displays all chats
    - Verify search filters conversations correctly
    - Verify multi-select works with checkboxes
    - Verify forward to single and multiple chats
    - Verify forwarded messages have "Forwarded" label
    - Verify success message shows correct count
    - Test with text, media, and link preview messages
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

  - [ ] 20.4 Test edit functionality end-to-end
    - Verify edit dialog pre-populates with current text
    - Verify character counter updates correctly
    - Verify save validation (empty text, message age)
    - Verify edited message shows "(edited)" label
    - Verify edit history dialog displays correctly
    - Verify edit action hidden for messages >48 hours old
    - Test real-time sync of edits across devices
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.8_

  - [ ] 20.5 Test delete functionality end-to-end
    - Verify delete confirmation dialog shows two options
    - Verify "Delete for me" removes locally only
    - Verify "Delete for everyone" syncs to all users
    - Verify deleted messages show placeholder text
    - Verify media attachments removed from deleted messages
    - Verify delete action hidden for other users' messages
    - Test real-time sync of deletions across devices
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

  - [ ] 20.6 Test AI summary functionality end-to-end
    - Verify loading indicator shows during generation
    - Verify summary displays correctly in dialog
    - Verify character count and read time shown
    - Verify copy to clipboard works
    - Verify cached summaries load instantly
    - Verify rate limit handling with countdown
    - Verify action hidden for messages <100 characters
    - Test with various message lengths and content types
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9_

  - [ ] 20.7 Test real-time synchronization
    - Test edit sync between two devices
    - Test delete sync between two devices
    - Test forward message delivery in real-time
    - Verify updates appear within 2 seconds
    - Test with poor network conditions
    - _Requirements: 8.3, 8.4_

  - [ ] 20.8 Test offline queueing
    - Disable network and perform actions
    - Verify "Pending" indicators show
    - Re-enable network and verify actions process
    - Test queue persistence across app restarts
    - Test error handling for failed queued actions
    - _Requirements: 8.5_

  - [ ] 20.9 Test error scenarios
    - Test network timeout errors
    - Test Gemini API rate limiting
    - Test invalid message ID errors
    - Test permission denied errors
    - Verify user-friendly error messages display
    - Verify retry functionality works
    - Verify all errors logged to Logcat
    - _Requirements: 4.7, 6.6, 8.5, 8.6_

  - [ ] 20.10 Test accessibility
    - Verify screen reader announces all actions
    - Verify content descriptions on all icons
    - Verify touch targets are minimum 48dp
    - Verify keyboard navigation works
    - Test with TalkBack enabled
    - _Requirements: 7.6_
