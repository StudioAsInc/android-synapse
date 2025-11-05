# Implementation Plan

- [x] 1. Set up database schema and Supabase Realtime infrastructure






  - Add `delivered_at` and `read_at` columns to messages table
  - Create database indexes for efficient queries on message_state and chat_id
  - Configure Supabase Realtime channels for chat rooms
  - Update RLS policies for typing events and read receipts
  - _Requirements: 6.2, 6.3_
-

- [x] 2. Implement SupabaseRealtimeService for WebSocket management




  - [x] 2.1 Create SupabaseRealtimeService class with channel lifecycle management


    - Implement `subscribeToChat()` to create and manage Realtime channels
    - Implement `broadcastTyping()` to send typing events
    - Implement `broadcastReadReceipt()` to send read receipt events
    - Implement `unsubscribeFromChat()` for cleanup
    - Add thread-safe channel map for concurrent access
    - _Requirements: 6.2, 6.3_

  - [x] 2.2 Add reconnection logic and error handling


    - Implement exponential backoff for reconnection attempts
    - Add connection state monitoring (Connected, Connecting, Disconnected, Error)
    - Implement graceful degradation with polling fallback
    - Add connection status callbacks for UI updates
    - _Requirements: 6.2_
-

- [x] 3. Implement TypingIndicatorManager for typing event handling



  - [x] 3.1 Create TypingIndicatorManager with debouncing logic


    - Implement `onUserTyping()` with 500ms debounce
    - Implement `onUserStoppedTyping()` with cleanup
    - Add auto-stop timer (3 seconds of inactivity)
    - Manage coroutine jobs per chat room
    - _Requirements: 1.1, 1.3, 1.4, 6.1_

  - [x] 3.2 Add typing event subscription handling


    - Implement `subscribeToTypingEvents()` with callback
    - Parse incoming typing events from Realtime
    - Update typing user list in real-time
    - Implement `unsubscribe()` for cleanup
    - _Requirements: 1.2, 6.4_

- [x] 4. Implement ReadReceiptManager for read receipt tracking











  - [x] 4.1 Create ReadReceiptManager with batching logic


    - Implement `markMessagesAsRead()` with 1-second batching
    - Implement `updateMessageState()` for state transitions
    - Add pending read receipts queue per chat
    - Integrate with PreferencesManager for privacy settings
    - _Requirements: 3.1, 3.2, 3.3, 4.4, 6.5_



  - [x] 4.2 Add read receipt broadcasting and subscription





    - Implement `subscribeToReadReceipts()` with callback
    - Broadcast read events via Supabase Realtime
    - Handle incoming read receipt events
    - Update local message states immediately for optimistic UI


    - _Requirements: 4.1, 4.2, 4.3_
-

  - [x] 4.3 Implement privacy controls for read receipts




    - Add `isReadReceiptsEnabled()` check before broadcasting


    - Respect user preference in all read receipt operations
    - Still receive and display read receipts from others when disabled
    - _Requirements: 5.1, 5.2, 5.3_
 



- [x] 5. Update data models and interfaces







  - [x] 5.1 Enhance ChatMessage interface with new fields


    - Add `deliveredAt: Long?` field
    - Add `readAt: Long?` field


    - Update ChatMessageImpl data class

    - _Requirements: 3.1, 3.2, 3.3_

  - [x] 5.2 Create ReadReceiptEvent data class

    - Define fields: chatId, userId, messageIds, timestamp
    - Add serialization annotations for Supabase
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 5.3 Create ChatPreferences data class

    - Define fields: sendReadReceipts, showTypingIndicators
    - Add default values (both true)
    - _Requirements: 5.1, 5.4_

- [x] 6. Update SupabaseChatService with new operations


  - [x] 6.1 Update sendMessage() to set initial message state


    - Set message_state to MessageState.SENT on successful send
    - Add delivered_at and read_at as null initially
    - _Requirements: 3.1_

  - [x] 6.2 Enhance markMessagesAsRead() for batching


    - Update message_state to MessageState.READ
    - Set read_at timestamp
    - Batch multiple message updates into single operation
    - Broadcast read receipt event via Realtime
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [x] 6.3 Add updateMessageDeliveryState() method


    - Update message_state to MessageState.DELIVERED
    - Set delivered_at timestamp
    - Broadcast delivery event via Realtime
    - _Requirements: 3.2_

- [x] 7. Create TypingAnimationView custom widget






  - [x] 7.1 Implement animated three-dot view


    - Create custom View class extending View
    - Implement dot drawing with Paint
    - Add wave animation with 1200ms duration
    - Implement `startAnimation()` and `stopAnimation()` methods
    - _Requirements: 2.1_

  - [x] 7.2 Create typing indicator layout


    - Create `view_typing_indicator.xml` layout
    - Add TextView for "User is typing..." text
    - Add TypingAnimationView for animated dots
    - Apply Material Design styling with 8dp spacing
    - _Requirements: 2.2, 2.5_

- [x] 8. Update message bubble layouts for read receipts





  - [x] 8.1 Add read receipt icon to outgoing message layouts


    - Add ImageView for read receipt icon (12dp size)
    - Position next to timestamp, aligned right
    - Add to chat_bubble_outgoing.xml and related layouts
    - _Requirements: 3.4_

  - [x] 8.2 Create extension function for message state icons


    - Create `MessageStateExtensions.kt` file
    - Implement `ImageView.setMessageState(state: String)` extension
    - Map states to icons: sent (single check), delivered (double check), read (blue double check), failed (error)
    - Apply color filters for read state (primary color)
    - _Requirements: 3.1, 3.2, 3.3, 3.5_


- [x] 9. Update ChatViewModel with typing and read receipt state




  - [x] 9.1 Add typing indicator state management


    - Add `_typingUsers` MutableStateFlow<List<String>>
    - Expose `typingUsers` StateFlow for UI observation
    - Implement `onUserTyping(text: String)` to trigger typing events
    - Implement `handleTypingUpdate()` to update typing user list
    - _Requirements: 1.1, 1.2, 1.4, 1.5_

  - [x] 9.2 Add read receipt state management


    - Implement `markVisibleMessagesAsRead()` for visible messages
    - Implement `handleReadReceiptUpdate()` to update message states
    - Update `_messages` StateFlow when read receipts arrive
    - Add lifecycle-aware read receipt marking (only when chat visible)
    - _Requirements: 4.1, 4.2, 4.3, 4.5_

  - [x] 9.3 Integrate managers with ViewModel


    - Initialize TypingIndicatorManager in ViewModel
    - Initialize ReadReceiptManager in ViewModel
    - Subscribe to typing events in `onChatOpened()`
    - Subscribe to read receipt events in `onChatOpened()`
    - Unsubscribe in `onChatClosed()` or `onCleared()`
    - _Requirements: 6.4_
-

- [x] 10. Update chat UI to display typing indicators




  - [x] 10.1 Add typing indicator to chat RecyclerView



    - Inflate typing indicator view at bottom of message list
    - Observe `typingUsers` StateFlow from ViewModel
    - Show/hide typing indicator based on typing user list
    - Update text: "User is typing..." or "User1, User2 are typing..."
    - Limit to 2 names maximum, show "3 people are typing..." for more
    - _Requirements: 1.2, 2.2, 2.3_

  - [x] 10.2 Add fade-in/fade-out animations


    - Apply 200ms fade-in animation when showing typing indicator
    - Apply 200ms fade-out animation when hiding typing indicator
    - Auto-hide after 5 seconds if no update received
    - _Requirements: 2.1, 2.4_
-


- [x] 11. Update chat adapter to show read receipt icons





  - [x] 11.1 Bind read receipt icons in message ViewHolders


    - Update outgoing message ViewHolder to bind read receipt icon
    - Call `setMessageState()` extension with message.messageState
    - Update icon when message state changes
    - Hide icon for incoming messages
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [x] 11.2 Handle real-time message state updates


    - Observe message state changes from ViewModel
    - Use DiffUtil to efficiently update only changed messages
    - Animate icon changes with subtle fade transition
    - _Requirements: 4.3_
-

- [x] 12. Implement user preferences for privacy controls




  - [x] 12.1 Create PreferencesManager for chat settings


    - Create `PreferencesManager.kt` using DataStore
    - Add `sendReadReceipts: Boolean` preference (default true)
    - Add `showTypingIndicators: Boolean` preference (default true)
    - Implement suspend functions to get/set preferences
    - _Requirements: 5.1, 5.4_

  - [x] 12.2 Create settings UI for read receipts and typing


    - Add "Chat Privacy" section to settings screen
    - Add "Send Read Receipts" toggle switch
    - Add "Send Typing Indicators" toggle switch
    - Add explanatory text for each setting
    - _Requirements: 5.1, 5.5_

  - [x] 12.3 Integrate preferences with managers


    - Check `sendReadReceipts` before broadcasting read events
    - Check `showTypingIndicators` before broadcasting typing events
    - Update managers when preferences change
    - _Requirements: 5.2, 5.3, 5.5_

- [x] 13. Add lifecycle management for Realtime subscriptions





  - [x] 13.1 Subscribe to events when chat screen opens


    - Call `subscribeToTypingEvents()` in Fragment/Activity onResume
    - Call `subscribeToReadReceipts()` in Fragment/Activity onResume
    - Mark visible messages as read when chat opens
    - _Requirements: 4.1, 6.4_

  - [x] 13.2 Unsubscribe when chat screen closes


    - Call `unsubscribe()` in Fragment/Activity onPause
    - Clean up coroutine jobs
    - Stop typing indicator animations
    - _Requirements: 6.4_

  - [x] 13.3 Handle app backgrounding


    - Defer read receipt updates when app is backgrounded
    - Stop sending typing events when app is backgrounded
    - Resume operations when app returns to foreground
    - _Requirements: 4.5_

- [x] 14. Add error handling and connection monitoring





  - [x] 14.1 Implement connection state UI indicator


    - Add connection status banner at top of chat screen
    - Show "Connecting..." when establishing connection
    - Show "Connection lost" when disconnected
    - Hide banner when connected
    - _Requirements: 6.2_

  - [x] 14.2 Implement graceful degradation


    - Fall back to polling every 5 seconds if Realtime fails
    - Queue typing events and send when connection restored
    - Batch and sync read receipts when back online
    - _Requirements: 6.2_
- [x] 15. Add performance monitoring and optimization




- [ ] 15. Add performance monitoring and optimization




  - [x] 15.1 Implement RealtimeMetrics tracking


    - Track typing events sent count
    - Track read receipts sent count
    - Track reconnection count
    - Measure average event latency
    - _Requirements: 6.1, 6.5_

  - [x] 15.2 Optimize database queries


    - Add index on (chat_id, message_state) for read receipt queries
    - Use batch updates for multiple message state changes
    - Limit typing status table size with TTL or cleanup job


   - _Requirements: 6.5_

- [x] 16. Integration and end-to-end testing









  - [x] 16.1 Test typing indicators across two devices


    - Verify typing indicator appears within 200ms
    - Verify auto-stop after 3 seconds
    - Verify immediate stop when message sent
    - Test with multiple users typing simultaneously
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 16.2 Test read receipts across two devices


    - Verify message state transitions: sent → delivered → read
    - Verify read receipt icons update in real-time
    - Verify batching works correctly
    - Test privacy settings (disabled read receipts)
    - _Requirements: 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3_

  - [x] 16.3 Test error scenarios




    - Test behavior with poor network connection
    - Test reconnection after connection loss
    - Test graceful degradation to polling
    - Test app backgrounding and foregrounding
    - _Requirements: 4.5, 6.2_
