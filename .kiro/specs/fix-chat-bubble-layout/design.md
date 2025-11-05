# Design Document

## Overview

This design addresses the chat bubble layout issues where messages appear on the wrong side and span full width. The root cause is in the `bindCommonMessageProperties()` method in ChatAdapter.kt, which incorrectly attempts to set layout parameters on the inner LinearLayout within the message_layout FrameLayout.

## Architecture

### Current Implementation Issues

1. **Incorrect Child Access**: The code attempts to get the first child of `message_layout` FrameLayout, but the layout structure has the LinearLayout as the direct child
2. **Wrong Layout Params Cast**: The code casts to `LinearLayout.LayoutParams` when it should cast to `FrameLayout.LayoutParams`
3. **Layout Not Applied**: Even when parameters are set, they may not be properly applied to the view

### Proposed Solution

Fix the `bindCommonMessageProperties()` method in ChatAdapter.kt to:
1. Correctly access the inner LinearLayout (direct child of FrameLayout)
2. Cast layout parameters to `FrameLayout.LayoutParams`
3. Set the gravity property based on message sender
4. Apply the updated layout parameters back to the view

## Components and Interfaces

### ChatAdapter.kt

**Method: `bindCommonMessageProperties(holder: BaseMessageViewHolder, position: Int)`**

Current problematic code (lines ~485-495):
```kotlin
// Set message layout alignment - message_layout is now a FrameLayout
holder.messageLayout?.let { layout ->
    // Find the inner LinearLayout that contains the message bubble
    val innerLayout = layout.getChildAt(0) as? LinearLayout
    innerLayout?.let { inner ->
        val layoutParams = inner.layoutParams as? android.widget.FrameLayout.LayoutParams
        layoutParams?.let { params ->
            params.gravity = if (isMyMessage) Gravity.END else Gravity.START
            inner.layoutParams = params
        }
    }
}
```

**Issue Analysis:**
- The code correctly identifies that `message_layout` is a FrameLayout
- It attempts to get the first child as LinearLayout
- However, the layout parameters assignment may not be triggering a layout update

**Proposed Fix:**
```kotlin
// Set message layout alignment
holder.messageLayout?.let { layout ->
    val innerLayout = layout.getChildAt(0) as? LinearLayout
    innerLayout?.let { inner ->
        val params = inner.layoutParams as? FrameLayout.LayoutParams
        if (params != null) {
            params.gravity = if (isMyMessage) Gravity.END else Gravity.START
            inner.layoutParams = params
            inner.requestLayout()
        }
    }
}
```

### Layout File: chat_bubble_text.xml

**Current Structure:**
```xml
<FrameLayout
    android:id="@+id/message_layout"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content">
    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="start"
        android:orientation="vertical">
        <!-- Message content -->
    </LinearLayout>
</FrameLayout>
```

**Issue:** The inner LinearLayout has a hardcoded `android:layout_gravity="start"` which may be overriding the programmatic gravity setting.

**Proposed Fix:**
Remove the hardcoded `android:layout_gravity="start"` attribute from the inner LinearLayout, allowing the adapter to control alignment dynamically.

## Data Models

No data model changes required. The fix operates on existing message data structure:
- `sender_id` or `uid`: Identifies the message sender
- Current user ID from `authService.getCurrentUser()?.id`

## Error Handling

### Null Safety
- All layout access uses safe calls (`?.`)
- Type casting uses safe cast operator (`as?`)
- Null checks before applying layout parameters

### Fallback Behavior
- If layout parameters cannot be cast correctly, the message will render with default alignment
- No crashes or exceptions should occur from layout parameter issues

## Testing Strategy

### Manual Testing
1. **Sent Message Alignment**: Send a message and verify it appears on the right side
2. **Received Message Alignment**: Receive a message and verify it appears on the left side
3. **Short Message Width**: Send/receive short messages and verify bubbles wrap content
4. **Long Message Width**: Send/receive long messages and verify bubbles respect max width
5. **Different Message Types**: Test text, media, link preview, and error messages for consistent alignment

### Visual Verification
- Compare message alignment before and after fix
- Verify bubble widths are appropriate for content length
- Check that deleted messages maintain proper alignment
- Ensure group chat messages with usernames display correctly

### Edge Cases
- Empty messages
- Very long single-word messages
- Messages with special characters
- Rapid message sending/receiving
- Screen rotation (if applicable)

## Implementation Notes

### Files to Modify
1. **app/src/main/java/com/synapse/social/studioasinc/ChatAdapter.kt**
   - Fix `bindCommonMessageProperties()` method
   - Ensure proper layout parameter handling

2. **app/src/main/res/layout/chat_bubble_text.xml**
   - Remove hardcoded `android:layout_gravity="start"` from inner LinearLayout

3. **Other chat bubble layouts** (if they exist and have the same issue):
   - chat_bubble_media.xml
   - chat_bubble_video.xml
   - chat_bubble_link_preview.xml
   - chat_bubble_voice.xml
   - chat_bubble_error.xml

### Verification Steps
1. Build the project: `./gradlew assembleDebug`
2. Install on device/emulator
3. Open an existing chat or create a new one
4. Send messages and verify right alignment
5. Receive messages (or view existing received messages) and verify left alignment
6. Check that message bubbles wrap content appropriately

## Performance Considerations

- The fix involves minimal overhead (simple layout parameter updates)
- No additional memory allocation required
- Layout updates are already part of the RecyclerView binding process
- No impact on scrolling performance

## Accessibility

- Message alignment helps users with visual impairments distinguish message direction
- Proper width constraints improve readability
- No negative accessibility impact from this fix
