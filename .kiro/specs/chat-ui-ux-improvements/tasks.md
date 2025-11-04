# Implementation Plan

- [x] 1. Create dimension and drawable resources





  - Create `message_bubble_max_width` and spacing dimensions in `values/dimens.xml`
  - Update `shape_outgoing_message_single.xml` with consistent 16dp border-radius
  - Update `shape_incoming_message_single.xml` with consistent 16dp border-radius
  - Create `shape_error_message.xml` drawable for error state styling
  - _Requirements: 2.1, 2.2, 5.3, 5.4_

- [x] 2. Refactor chat_bubble_text.xml layout





  - [x] 2.1 Implement max-width constraint on message bubbles


    - Change `message_layout` from `layout_width="0dp"` to `layout_width="wrap_content"`
    - Remove `layout_weight="1"` from `message_layout`
    - Add `android:maxWidth="@dimen/message_bubble_max_width"` to `messageBG`
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 2.2 Reposition metadata inside message bubble


    - Move `my_message_info` LinearLayout inside `messageBG` container
    - Set `android:layout_gravity="end"` on `my_message_info` for bottom-right positioning
    - Add `android:layout_marginTop="4dp"` to `my_message_info` for spacing from message text
    - Reduce font size of timestamp to 11sp for visual hierarchy
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [x] 2.3 Improve vertical spacing between messages


    - Add `android:layout_marginTop="@dimen/message_vertical_spacing"` to `body` LinearLayout
    - Ensure consistent padding within `messageBG` (12dp all sides)
    - _Requirements: 5.1, 5.2_

- [x] 3. Apply layout changes to other message bubble types





  - [x] 3.1 Update chat_bubble_media.xml


    - Apply max-width constraint to media bubble container
    - Move metadata inside bubble
    - Add consistent spacing
    - _Requirements: 2.1, 2.2, 3.1, 3.2, 5.1_


  - [x] 3.2 Update chat_bubble_video.xml

    - Apply max-width constraint to video bubble container
    - Move metadata inside bubble
    - Add consistent spacing
    - _Requirements: 2.1, 2.2, 3.1, 3.2, 5.1_


  - [x] 3.3 Update chat_bubble_link_preview.xml

    - Apply max-width constraint to link preview bubble container
    - Move metadata inside bubble
    - Add consistent spacing
    - _Requirements: 2.1, 2.2, 3.1, 3.2, 5.1_

  - [x] 3.4 Update chat_bubble_voice.xml


    - Apply max-width constraint to voice bubble container
    - Move metadata inside bubble
    - Add consistent spacing
    - _Requirements: 2.1, 2.2, 3.1, 3.2, 5.1_

- [x] 4. Implement error state handling in ChatAdapter




  - [x] 4.1 Add error view type and ViewHolder


    - Add `VIEW_TYPE_ERROR` constant to ChatAdapter companion object
    - Create `ErrorViewHolder` class with error message TextView and retry action
    - Create `chat_bubble_error.xml` layout with user-friendly error display
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 4.2 Implement error detection logic


    - Update `getItemViewType()` to detect failed messages by checking `delivery_status == "failed"` or `message_state == "failed"`
    - Return `VIEW_TYPE_ERROR` for failed messages
    - _Requirements: 4.1, 4.5_

  - [x] 4.3 Implement error message binding


    - Create `bindErrorViewHolder()` method that displays "Failed to send" text
    - Log full error details using `Log.e(TAG, "Message send failed", exception)`
    - Implement retry click listener that calls `listener.onMessageRetry(messageId, position)`
    - Ensure no stack traces are displayed in UI
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [x] 4.4 Add retry callback to ChatAdapterListener interface


    - Add `onMessageRetry(messageId: String, position: Int)` method to ChatAdapterListener interface
    - _Requirements: 4.6_


- [x] 5. Update ChatAdapter message alignment logic




  - [x] 5.1 Verify and fix message alignment in bindCommonMessageProperties


    - Ensure `isMyMessage == true` results in `Gravity.END` alignment on `message_layout`
    - Ensure `isMyMessage == false` results in `Gravity.START` alignment on `message_layout`
    - Verify alignment is applied correctly to LayoutParams
    - _Requirements: 1.1, 1.2, 1.4_

  - [x] 5.2 Implement dynamic vertical spacing based on sender changes


    - Track previous message sender in adapter
    - Apply `message_vertical_spacing_sender_change` (12dp) when sender changes
    - Apply `message_vertical_spacing` (8dp) for consecutive messages from same sender
    - _Requirements: 5.1, 5.2_

  - [x] 5.3 Ensure distinct visual styling for user vs recipient messages


    - Verify `shape_outgoing_message_single` is applied to user messages
    - Verify `shape_incoming_message_single` is applied to recipient messages
    - Ensure text colors match theme (onPrimaryContainer for user, onSurfaceVariant for recipient)
    - _Requirements: 1.3_


- [x] 6. Manual testing and validation


  - [ ] 6.1 Test message alignment
    - Verify sent messages appear on right side
    - Verify received messages appear on left side
    - Test alignment consistency across all message types
    - Test group chat message alignment with usernames
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [ ] 6.2 Test bubble width constraints
    - Verify short messages have compact bubbles
    - Verify long messages wrap within max-width
    - Verify bubbles never span full screen width
    - Test on different screen sizes
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [ ] 6.3 Test metadata positioning
    - Verify timestamp appears inside bubble
    - Verify status icons appear inside bubble next to timestamp
    - Verify metadata is in bottom-right corner
    - Verify metadata doesn't overlap message text
    - Verify font size hierarchy
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [ ] 6.4 Test error handling
    - Simulate message send failure
    - Verify "Failed to send" message appears
    - Verify no stack traces in UI
    - Verify error styling is distinct
    - Verify retry action works
    - Verify stack traces appear in Logcat
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

  - [ ] 6.5 Test visual spacing and polish
    - Verify 8dp spacing between same-sender messages
    - Verify 12dp spacing when sender changes
    - Verify consistent 16dp border-radius on all bubbles
    - Verify no tail effects or inconsistent corners
    - Verify adequate padding inside bubbles
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
