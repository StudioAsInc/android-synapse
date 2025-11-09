# Implementation Plan: Inline Reply UI Enhancement

- [x] 1. Create background drawable for message input container





  - Create `bg_message_input_container.xml` in `res/drawable`
  - Define shape with 20dp corner radius
  - Apply appropriate background color and elevation
  - _Requirements: 1.1, 4.1_


- [x] 2. Restructure message input layout to support inline reply



  - [x] 2.1 Modify `message_input_layout.xml` structure


    - Wrap existing input field in new vertical LinearLayout container
    - Set container ID as `message_input_container`
    - Apply proper spacing and padding
    - _Requirements: 1.2, 1.4_
  
  - [x] 2.2 Update layout constraints and styling


    - Apply background drawable to container
    - Set elevation and corner radius
    - Ensure proper alignment with attachment and send buttons
    - _Requirements: 4.1, 4.3_
- [x] 3. Redesign reply preview layout with inline styling




- [ ] 3. Redesign reply preview layout with inline styling

  - [x] 3.1 Convert reply layout to MaterialCardView


    - Replace root LinearLayout with MaterialCardView in `chat_reply_layout.xml`
    - Apply 16dp corner radius
    - Set background color to `md_theme_surfaceContainerLow`
    - Add 1dp elevation
    - _Requirements: 4.1, 4.2, 4.4_
  
  - [x] 3.2 Reposition close button to top-right corner


    - Change close button size to 24dp x 24dp
    - Add 4dp padding for touch target
    - Position in top-right corner using ConstraintLayout or RelativeLayout
    - Update icon tint to `md_theme_onSurfaceVariant`
    - _Requirements: 2.1, 2.3_
  
  - [x] 3.3 Update reply content styling


    - Adjust username TextView styling (primary color, bold, 13sp)
    - Update message TextView styling (onSurfaceVariant, 14sp, maxLines=2)
    - Configure media preview ImageView (60dp, rounded corners)
    - Ensure proper text truncation with ellipsize
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 4. Implement reply preview show/hide animations








  - [x] 4.1 Create show animation in ChatActivity


    - Implement fade-in animation (alpha 0 to 1)
    - Add slide-up animation (translationY -20dp to 0)
    - Set duration to 200ms with DecelerateInterpolator
    - _Requirements: 5.1, 5.3_
  
  - [x] 4.2 Create hide animation in ChatActivity




    - Implement fade-out animation (alpha 1 to 0)
    - Add slide-down animation (translationY 0 to -20dp)
    - Set duration to 150ms with AccelerateInterpolator
    - Set visibility to GONE after animation completes
    - _Requirements: 5.2, 5.3_


- [x] 5. Update ChatActivity to support inline reply preview





  - [x] 5.1 Add new view references in initialize() method

    - Add `messageInputContainer` LinearLayout reference
    - Add `replyPreviewContainer` MaterialCardView reference
    - Initialize close button reference
    - _Requirements: 1.1, 1.2_
  
  - [x] 5.2 Implement prepareReply() method


    - Set reply username and message text
    - Handle media preview visibility and loading
    - Show reply preview with animation
    - Maintain keyboard focus on text input
    - _Requirements: 3.1, 3.2, 3.3, 5.1, 5.4_
  
  - [x] 5.3 Implement cancelReply() method


    - Clear reply state (replyMessageId = null)
    - Hide reply preview with animation
    - Clear reply text fields
    - Hide media preview
    - _Requirements: 2.2, 5.2_
  
  - [x] 5.4 Setup close button click listener


    - Wire close button to cancelReply() method
    - Add haptic feedback on click
    - Ensure proper touch feedback
    - _Requirements: 2.2, 2.4_

- [x] 6. Add accessibility support




  - [x] 6.1 Add content descriptions to strings.xml


    - Add "Cancel reply" for close button
    - Add "Replying to %s" for reply preview
    - _Requirements: 2.1, 3.1_
  
  - [x] 6.2 Apply content descriptions to views


    - Set contentDescription on close button
    - Set contentDescription on reply preview container
    - Ensure screen reader announces reply state changes
    - _Requirements: 2.1, 3.1_



- [x] 7. Handle edge cases and error scenarios



  - [x] 7.1 Implement text truncation handling


    - Verify long usernames truncate with ellipsis
    - Verify long messages truncate to 2 lines
    - Test with various text lengths
    - _Requirements: 3.2_
  
  - [x] 7.2 Implement media preview error handling


    - Add placeholder for loading state
    - Handle image load failures gracefully
    - Use Glide error handling
    - _Requirements: 3.3_
  
  - [x] 7.3 Prevent animation conflicts

    - Cancel ongoing animations before starting new ones
    - Add animation state tracking
    - Debounce rapid close button clicks
    - _Requirements: 5.1, 5.2_
- [x] 8. Test and validate implementation




- [ ] 8. Test and validate implementation


  - [x] 8.1 Verify visual appearance

    - Check reply preview appears inside message input container
    - Verify rounded corners and elevation
    - Confirm close button positioning
    - Test color scheme matches Material Design 3
    - _Requirements: 1.1, 4.1, 4.2, 4.3, 4.4_
  
  - [x] 8.2 Test functionality

    - Test selecting message shows reply preview
    - Test close button dismisses preview
    - Verify reply data displays correctly
    - Test media preview when applicable
    - Confirm text input maintains focus
    - _Requirements: 2.1, 2.2, 3.1, 3.2, 3.3, 5.4_
  

  - [ ] 8.3 Validate animations
    - Test show animation smoothness
    - Test hide animation smoothness
    - Verify container height adjusts properly
    - Check for animation jank
    - _Requirements: 5.1, 5.2, 5.3_
  
  - [x] 8.4 Test edge cases

    - Test with long messages and usernames
    - Test media loading and errors
    - Test rapid reply/cancel actions
    - Test keyboard interactions
    - _Requirements: 3.2, 3.3_
  
  - [x] 8.5 Verify accessibility

    - Test with TalkBack screen reader
    - Verify content descriptions
    - Check touch target sizes
    - Test focus order
    - _Requirements: 2.1, 2.3_
  

  - [ ] 8.6 Test on different devices
    - Test on small screens (< 5 inches)
    - Test on large screens (> 6 inches)
    - Test in portrait and landscape orientations
    - Test with different system font sizes
    - _Requirements: 1.1, 1.2, 1.4_
