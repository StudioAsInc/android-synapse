# Implementation Plan

- [x] 1. Update dimension resources for message spacing




  - Add `message_spacing_grouped` (2dp) dimension to `dimens.xml`
  - Add `message_spacing_ungrouped` (12dp) dimension to `dimens.xml`
  - Remove fixed `layout_marginTop` from chat bubble layout files
  - _Requirements: 1.1, 4.1, 4.2, 4.5_







- [ ] 2. Implement message grouping logic in ChatAdapter



  - [ ] 2.1 Add MessagePosition enum to ChatAdapter
    - Define SINGLE, FIRST, MIDDLE, LAST positions
    - _Requirements: 1.2, 1.3, 1.4, 1.5_
  
  - [ ] 2.2 Implement grouping calculation methods
    - Write `shouldGroupWithPrevious()` method with bounds checking


    - Write `shouldGroupWithNext()` method with bounds checking
    - Write `calculateMessagePosition()` method

    - Handle deleted messages as group breakers



    - Handle different message types appropriately
    - _Requirements: 1.1, 2.1, 4.4_
  
  - [-] 2.3 Apply dynamic spacing in onBindViewHolder

    - Get MessagePosition for current message
    - Apply appropriate spacing based on position
    - Use dimension resources for spacing values
    - _Requirements: 1.1, 4.1, 4.2, 4.3_

- [x] 3. Implement corner radius adjustment and dynamic width for message bubbles



  - [x] 3.1 Create helper methods in ViewHolder base class

    - Write `getMaxBubbleWidth()` method to calculate 75% of screen width
    - Write method to generate GradientDrawable with custom corners
    - Support different corner configurations (top, bottom, all, none)
    - Apply appropriate colors for sent vs received messages
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 5.2, 5.5_
  
  - [x] 3.2 Apply corner radii and dynamic width in ViewHolder bind methods

    - Update TextViewHolder to apply corner radii and max width
    - Update MediaViewHolder to apply corner radii and max width
    - Update VideoViewHolder to apply corner radii and max width
    - Update AudioViewHolder to apply corner radii and max width
    - Update DocumentViewHolder to apply corner radii and max width
    - Set maxWidth programmatically using `getMaxBubbleWidth()`
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 5.2, 5.5_

- [x] 4. Implement optimistic UI updates for message sending


  - [x] 4.1 Modify sendMessage() in ChatActivity


    - Generate temporary message ID using timestamp
    - Create optimistic message map with temp ID
    - Add optimistic message to messagesList immediately
    - Clear input field immediately after adding to list
    - Notify adapter of new message insertion
    - Scroll to bottom to show new message
    - _Requirements: 3.1, 3.2, 3.5_
  

  - [x] 4.2 Update message with real ID after server response

    - Find message by temp ID in messagesList
    - Update message with real ID from server
    - Update delivery status to "sent"
    - Remove optimistic flag
    - Notify adapter of item change
    - _Requirements: 3.3_

  
  - [x] 4.3 Handle message send failures

    - Update message delivery status to "failed"
    - Show error indicator on message
    - Provide retry functionality
    - _Requirements: 3.4_

- [x] 5. Implement realtime message deduplication





  - [x] 5.1 Add deduplication logic to realtime handler


    - Check if incoming message ID already exists in messagesList
    - If exists, update existing message instead of adding new one
    - If new, add message to list
    - Calculate affected range for grouping updates
    - _Requirements: 2.1, 2.3_
  
  - [x] 5.2 Implement grouping recalculation for affected messages


    - When new message added, recalculate grouping for previous message
    - When new message added, calculate grouping for new message
    - Notify adapter of changes to affected positions only
    - Use notifyItemChanged() instead of notifyDataSetChanged()
    - _Requirements: 2.2, 2.3, 2.4_

- [x] 6. Update layout XML files to support dynamic spacing and proper bubble width




  - [x] 6.1 Fix message bubble width in chat_bubble_text.xml


    - Change inner LinearLayout width from `match_parent` to `wrap_content`
    - Remove static `android:maxWidth` from messageBG (will be set programmatically)
    - Verify `layout_gravity` is set correctly (start for received, end for sent)
    - Remove `android:layout_marginTop="@dimen/message_vertical_spacing"`
    - _Requirements: 1.1, 4.1, 4.2, 5.1, 5.2, 5.5, 5.6_
  

  - [x] 6.2 Fix message bubble width in chat_bubble_image.xml

    - Change inner LinearLayout width from `match_parent` to `wrap_content`
    - Remove static `android:maxWidth` from messageBG (will be set programmatically)
    - Verify `layout_gravity` is set correctly
    - Remove `android:layout_marginTop="@dimen/message_vertical_spacing"`
    - _Requirements: 1.1, 4.1, 4.2, 5.1, 5.2, 5.5, 5.6_
  
  - [x] 6.3 Fix message bubble width in chat_bubble_video.xml


    - Change inner LinearLayout width from `match_parent` to `wrap_content`
    - Remove static `android:maxWidth` from messageBG (will be set programmatically)
    - Verify `layout_gravity` is set correctly
    - Remove `android:layout_marginTop="@dimen/message_vertical_spacing"`
    - _Requirements: 1.1, 4.1, 4.2, 5.1, 5.2, 5.5, 5.6_
  

  - [x] 6.4 Fix message bubble width in chat_bubble_audio.xml

    - Change inner LinearLayout width from `match_parent` to `wrap_content`
    - Remove static `android:maxWidth` from messageBG (will be set programmatically)
    - Verify `layout_gravity` is set correctly
    - Remove `android:layout_marginTop="@dimen/message_vertical_spacing"`
    - _Requirements: 1.1, 4.1, 4.2, 5.1, 5.2, 5.5, 5.6_
  
  - [x] 6.5 Fix message bubble width in chat_bubble_document.xml


    - Change inner LinearLayout width from `match_parent` to `wrap_content`
    - Remove static `android:maxWidth` from messageBG (will be set programmatically)
    - Verify `layout_gravity` is set correctly
    - Remove `android:layout_marginTop="@dimen/message_vertical_spacing"`
    - _Requirements: 1.1, 4.1, 4.2, 5.1, 5.2, 5.5, 5.6_

- [x] 7. Test and validate the implementation



  - [ ] 7.1 Test message grouping visually
    - Send multiple consecutive messages from same sender
    - Verify 2dp spacing between grouped messages
    - Verify correct corner radii for FIRST, MIDDLE, LAST positions
    - Send message from different sender
    - Verify 12dp spacing between different senders
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 4.1, 4.2_
  
  - [ ] 7.2 Test realtime sync and deduplication
    - Send message and verify no duplicate appears
    - Send multiple messages rapidly
    - Verify grouping updates correctly with each new message
    - Close and reopen chat to verify grouping persists
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_
  
  - [ ] 7.3 Test optimistic UI performance
    - Send message and measure time to appear in UI
    - Verify input clears immediately
    - Test with slow network connection
    - Verify failed messages show error state
    - _Requirements: 3.1, 3.2, 3.3, 3.4_
  
  - [ ] 7.4 Test message bubble width behavior across different screen sizes
    - Send short message (e.g., "Hi") and verify bubble wraps to content
    - Send long message and verify bubble stops at 75% of screen width
    - Verify sent messages align to right edge
    - Verify received messages align to left edge
    - Test with different message types (text, image, video)
    - Test on phone screen (verify reasonable width)
    - Test on tablet or large screen (verify doesn't span excessively)
    - Rotate device and verify width adapts to new orientation
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_
