# Implementation Plan

- [ ] 1. Fix message alignment in ChatAdapter
  - Modify the `bindCommonMessageProperties()` method to correctly set FrameLayout.LayoutParams gravity
  - Add `requestLayout()` call to ensure layout updates are applied
  - Verify the inner LinearLayout is correctly accessed as the first child of message_layout FrameLayout
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 2. Remove hardcoded layout gravity from chat bubble layouts
  - Remove `android:layout_gravity="start"` from inner LinearLayout in chat_bubble_text.xml
  - Check and fix other chat bubble layout files (media, video, link_preview, voice, error) if they have the same issue
  - Ensure all inner LinearLayouts have `android:layout_width="wrap_content"` to respect width constraints
  - _Requirements: 2.1, 2.2, 2.5, 3.1, 3.2_

- [ ] 3. Verify fix across all message types
  - Build the project and install on device/emulator
  - Test sent messages appear on right side with proper width
  - Test received messages appear on left side with proper width
  - Test different message types (text, media, links, errors) for consistent behavior
  - Test edge cases (short messages, long messages, deleted messages)
  - _Requirements: 1.1, 1.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5_
