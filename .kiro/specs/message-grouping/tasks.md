# Implementation Plan: Message Grouping

- [x] 1. Create message position enum and grouping calculation logic








  - Add MessagePosition enum to ChatAdapter with SINGLE, FIRST, MIDDLE, LAST values
  - Implement isGroupableMessageType() method to filter message types that support grouping
  - Implement shouldGroupWithPrevious() method to check if current message groups with previous
  - Implement shouldGroupWithNext() method to check if current message groups with next
  - Implement calculateMessagePosition() method that returns MessagePosition based on adjacent messages
  - Add bounds checking and null safety for all array access operations
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 6.5_

- [x] 2. Implement corner radius application logic




  - Create applyMessageBubbleBackground() method that accepts MessagePosition and isMyMessage parameters
  - Map MessagePosition.SINGLE to shape_outgoing_message_single.xml / shape_incoming_message_single.xml
  - Map MessagePosition.FIRST to shape_outgoing_message_first.xml / shape_incoming_message_first.xml
  - Map MessagePosition.MIDDLE to shape_outgoing_message_middle.xml / shape_incoming_message_middle.xml
  - Map MessagePosition.LAST to shape_outgoing_message_last.xml / shape_incoming_message_last.xml
  - Apply appropriate drawable resource to holder.messageBubble using setBackgroundResource()
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 6.2, 6.3_
-

- [x] 3. Implement timestamp visibility logic




  - Create getTimeDifference() method to calculate milliseconds between two message timestamps
  - Implement shouldShowTimestamp() method with 60-second threshold logic
  - Return true for SINGLE and LAST message positions
  - Return true for FIRST and MIDDLE positions if time difference with next message > 60000ms
  - Handle null timestamp values with fallback to current time
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_


- [x] 4. Implement username visibility logic for group chats




  - Create shouldShowUsername() method accepting position, messagePosition, isGroupChat, and isMyMessage
  - Return true only for SINGLE or FIRST message positions in group chats
  - Return false for MIDDLE and LAST message positions
  - Return false when isMyMessage is true (current user's messages)
  - Return false when isGroupChat is false (1-on-1 chats)
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 5. Integrate grouping logic into bindCommonMessageProperties




  - Call calculateMessagePosition() at the start of bindCommonMessageProperties()
  - Replace existing bubble background logic with applyMessageBubbleBackground() call
  - Modify timestamp visibility logic to use shouldShowTimestamp() method
  - Modify username visibility logic to use shouldShowUsername() method
  - Ensure grouping logic executes before other message styling
  - Verify deleted message handling still works correctly with grouping
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 5.1, 5.2, 6.1_

- [x] 6. Verify grouping works across all message types





  - Test text messages (VIEW_TYPE_TEXT) display with correct grouping
  - Test media messages (VIEW_TYPE_MEDIA_GRID) display with correct grouping
  - Test video messages (VIEW_TYPE_VIDEO) display with correct grouping
  - Test voice messages (VIEW_TYPE_VOICE_MESSAGE) display with correct grouping
  - Test link preview messages (VIEW_TYPE_LINK_PREVIEW) display with correct grouping
  - Verify typing, error, and loading indicators don't participate in grouping
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_
-

- [x] 7. Handle edge cases and deleted messages




  - Verify single message in conversation displays as SINGLE position
  - Verify deleted messages are excluded from grouping calculations
  - Test that non-deleted messages group correctly across deleted messages
  - Verify grouping works correctly at start and end of conversation
  - Test rapid message sending (< 60 seconds) hides intermediate timestamps
  - Test delayed message sending (> 60 seconds) shows all timestamps
  - _Requirements: 1.4, 3.1, 3.2, 6.5_


- [x] 8. Performance testing and optimization




  - Measure message binding time with grouping enabled using Android Profiler
  - Verify binding time is under 5ms per message
  - Test scrolling performance with 1000+ messages
  - Verify 60 FPS maintained during fast scrolling
  - Check memory usage remains stable (no drawable resource leaks)
  - Profile calculateMessagePosition() execution time
  - _Requirements: 6.1, 6.4_


- [x] 9. Manual testing across scenarios


  - Test basic grouping with 3 consecutive messages from same sender
  - Test timestamp threshold with messages sent 30 seconds apart vs 70 seconds apart
  - Test group chat username display with multiple senders
  - Test mixed message types (text, image, video) grouping together
  - Test deleted message handling within groups
  - Test incoming vs outgoing message grouping
  - Test edge cases (empty list, single message, alternating senders)
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 5.1, 5.2_
