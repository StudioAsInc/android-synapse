# Design Document: Inline Reply UI Enhancement

## Overview

This design transforms the reply preview from a separate component into an integrated element within the message input container. The new design creates a cohesive visual experience by embedding the reply preview directly above the text input field, using Material Design 3 principles with rounded corners, appropriate elevation, and smooth animations.

The implementation will modify the existing `chat_reply_layout.xml` and integrate it within the `message_input_layout.xml` container, ensuring the reply preview appears as part of the input area rather than a separate floating element.

## Architecture

### Component Structure

```
message_input_overall_container (LinearLayout - Horizontal)
├── btn_attachments (MaterialButton)
├── message_input_container (LinearLayout - Vertical) [NEW]
│   ├── reply_preview_container (MaterialCardView) [MODIFIED]
│   │   ├── reply_content_layout (LinearLayout - Horizontal)
│   │   │   ├── reply_accent_border (View)
│   │   │   ├── reply_text_container (LinearLayout - Vertical)
│   │   │   │   ├── reply_username (TextView)
│   │   │   │   ├── reply_message (TextView)
│   │   │   │   └── reply_media_preview (ImageView)
│   │   │   └── reply_close_button (ImageView)
│   │   └── [positioned in top-right corner]
│   └── message_input_outlined_round (LinearLayout)
│       └── message_et (FadeEditText)
└── btn_sendMessage (MaterialButton)
```

### Key Design Changes

1. **Container Restructuring**: Wrap the message input field and reply preview in a new vertical LinearLayout
2. **Reply Preview Integration**: Position reply preview inside the message input container
3. **Visual Styling**: Apply rounded corners, elevation, and Material Design 3 colors
4. **Close Button Positioning**: Place close icon in top-right corner with small size
5. **Animation Support**: Enable smooth show/hide transitions

## Components and Interfaces

### 1. Layout Components

#### New Message Input Container
- **Type**: LinearLayout (Vertical orientation)
- **Purpose**: Contains both reply preview and text input field
- **Styling**:
  - Background: `@drawable/bg_message_input_container` (rounded corners)
  - Corner radius: `20dp`
  - Padding: `8dp`
  - Elevation: `2dp`

#### Modified Reply Preview Container
- **Type**: MaterialCardView
- **Purpose**: Displays reply information with modern styling
- **Styling**:
  - Background: `@color/md_theme_surfaceContainerLow`
  - Corner radius: `16dp`
  - Elevation: `1dp`
  - Margin: `8dp` (all sides)
  - Padding: `12dp`
  - Visibility: `gone` (default)

#### Close Button
- **Type**: ImageView
- **Purpose**: Dismiss reply preview
- **Styling**:
  - Size: `24dp x 24dp`
  - Icon: `@drawable/ic_close_48px`
  - Tint: `@color/md_theme_onSurfaceVariant`
  - Background: `?attr/selectableItemBackgroundBorderless`
  - Padding: `4dp`
  - Position: Top-right corner of reply preview

### 2. Kotlin Components

#### ChatActivity Modifications

**New Properties**:
```kotlin
private var replyPreviewContainer: MaterialCardView? = null
private var messageInputContainer: LinearLayout? = null
```

**Modified Methods**:

1. **`initialize()`**
   - Initialize new container references
   - Setup reply preview visibility observers
   - Configure animation properties

2. **`prepareReply(messageId: String, messageText: String, senderName: String)`**
   - Set reply data
   - Show reply preview with animation
   - Focus text input field
   - Update container layout

3. **`cancelReply()`**
   - Clear reply state
   - Hide reply preview with animation
   - Reset container layout

4. **`setupReplyPreview()`**
   - Configure close button click listener
   - Setup animation transitions
   - Handle keyboard interactions

### 3. Animation Specifications

#### Show Animation
```kotlin
replyPreviewContainer?.apply {
    alpha = 0f
    translationY = -20f
    visibility = View.VISIBLE
    animate()
        .alpha(1f)
        .translationY(0f)
        .setDuration(200)
        .setInterpolator(DecelerateInterpolator())
        .start()
}
```

#### Hide Animation
```kotlin
replyPreviewContainer?.apply {
    animate()
        .alpha(0f)
        .translationY(-20f)
        .setDuration(150)
        .setInterpolator(AccelerateInterpolator())
        .withEndAction {
            visibility = View.GONE
            translationY = 0f
        }
        .start()
}
```

## Data Models

### Reply State
```kotlin
data class ReplyState(
    val messageId: String?,
    val messageText: String,
    val senderName: String,
    val mediaUrl: String? = null,
    val mediaType: String? = null
)
```

No database changes required - this is purely a UI enhancement.

## Error Handling

### Edge Cases

1. **Long Message Text**
   - Apply `maxLines="2"` with `ellipsize="end"`
   - Ensure text doesn't overflow container

2. **Long Username**
   - Apply `singleLine="true"` with `ellipsize="end"`
   - Maximum width constraint

3. **Media Preview Loading**
   - Show placeholder while loading
   - Handle load failures gracefully
   - Use Glide for efficient image loading

4. **Keyboard Interactions**
   - Maintain keyboard focus on text input
   - Adjust layout when keyboard appears
   - Smooth transitions during keyboard show/hide

5. **Rapid Reply/Cancel Actions**
   - Cancel ongoing animations before starting new ones
   - Prevent animation conflicts
   - Debounce rapid clicks

## Testing Strategy

### Unit Tests
Not required for this UI-only feature (following project guidelines).

### Manual Testing Checklist

1. **Visual Appearance**
   - [ ] Reply preview appears inside message input container
   - [ ] Rounded corners applied correctly
   - [ ] Close button positioned in top-right corner
   - [ ] Colors match Material Design 3 theme
   - [ ] Elevation and shadows render properly

2. **Functionality**
   - [ ] Selecting a message shows reply preview
   - [ ] Close button dismisses reply preview
   - [ ] Reply data displays correctly (username, message)
   - [ ] Media preview shows when applicable
   - [ ] Text input maintains focus

3. **Animations**
   - [ ] Show animation is smooth (200ms)
   - [ ] Hide animation is smooth (150ms)
   - [ ] No animation jank or stuttering
   - [ ] Container height adjusts smoothly

4. **Edge Cases**
   - [ ] Long messages truncate properly
   - [ ] Long usernames truncate properly
   - [ ] Media loading handles errors
   - [ ] Rapid clicks don't cause issues
   - [ ] Keyboard interactions work correctly

5. **Accessibility**
   - [ ] Close button has minimum touch target (48dp)
   - [ ] Content descriptions set for screen readers
   - [ ] Color contrast meets WCAG standards
   - [ ] Focus order is logical

6. **Device Testing**
   - [ ] Test on small screens (< 5 inches)
   - [ ] Test on large screens (> 6 inches)
   - [ ] Test in portrait and landscape
   - [ ] Test with different font sizes

## Design Decisions and Rationales

### 1. Container Integration
**Decision**: Wrap reply preview and text input in a single container

**Rationale**: Creates visual cohesion and makes it clear that the reply preview is part of the message composition area, not a separate floating element.

### 2. MaterialCardView for Reply Preview
**Decision**: Use MaterialCardView instead of LinearLayout

**Rationale**: Provides built-in elevation, rounded corners, and Material Design 3 styling with minimal code. Ensures consistency with other card-based UI elements in the app.

### 3. Close Button Size (24dp)
**Decision**: Use 24dp icon size with 4dp padding (32dp total touch target)

**Rationale**: Balances visibility with space efficiency. The 32dp touch target is slightly below the 48dp recommendation but acceptable for secondary actions in constrained spaces.

### 4. Animation Duration (200ms show, 150ms hide)
**Decision**: Asymmetric animation timing

**Rationale**: Slightly longer show animation feels more deliberate and draws attention. Faster hide animation feels more responsive and doesn't delay user actions.

### 5. Corner Radius (16dp)
**Decision**: Use 16dp corner radius for reply preview

**Rationale**: Matches the message bubble corner radius (`message_bubble_corner_radius`) for visual consistency. Slightly smaller than the outer container (20dp) to create visual hierarchy.

### 6. Background Color
**Decision**: Use `md_theme_surfaceContainerLow`

**Rationale**: Provides subtle elevation above the input field while maintaining readability. Follows Material Design 3 surface hierarchy guidelines.

### 7. No Separate Research Files
**Decision**: Integrate design research directly into this document

**Rationale**: Following spec workflow guidelines - research context is maintained in the conversation thread rather than separate files.

## Implementation Notes

### Resource Files to Create/Modify

1. **`bg_message_input_container.xml`** (NEW)
   - Drawable for outer container background
   - Rounded corners with elevation

2. **`chat_reply_layout.xml`** (MODIFY)
   - Restructure to use MaterialCardView
   - Reposition close button
   - Update styling

3. **`message_input_layout.xml`** (MODIFY)
   - Add new vertical container
   - Integrate reply preview
   - Adjust spacing

4. **`strings.xml`** (ADD)
   - Content descriptions for accessibility

### Kotlin Files to Modify

1. **`ChatActivity.kt`**
   - Update `initialize()` method
   - Modify `prepareReply()` method
   - Implement `cancelReply()` method
   - Add animation logic

### Dependencies

No new dependencies required. Uses existing:
- Material Design 3 components
- Android View animations
- Glide (already in project)

## Accessibility Considerations

1. **Content Descriptions**
   - Close button: "Cancel reply"
   - Reply preview: "Replying to [username]: [message preview]"

2. **Touch Targets**
   - Close button: 32dp (acceptable for secondary action)
   - All interactive elements meet minimum size

3. **Color Contrast**
   - Username text: Primary color on surface (passes WCAG AA)
   - Message text: OnSurfaceVariant (passes WCAG AA)
   - Close icon: OnSurfaceVariant (passes WCAG AA)

4. **Screen Reader Support**
   - Announce when reply mode is activated
   - Announce when reply is cancelled
   - Logical focus order maintained

## Performance Considerations

1. **Animation Performance**
   - Use hardware acceleration for animations
   - Keep animation duration short (< 300ms)
   - Use simple property animations (alpha, translationY)

2. **Layout Efficiency**
   - Minimize layout nesting depth
   - Use `View.GONE` instead of removing/adding views
   - Avoid unnecessary layout passes

3. **Image Loading**
   - Use Glide's thumbnail loading for media previews
   - Apply appropriate image size constraints
   - Cache loaded images

## Future Enhancements

1. **Swipe to Dismiss**: Allow swiping reply preview to cancel
2. **Reply Chain Visualization**: Show reply thread context
3. **Quick Reply Actions**: Add quick action buttons (edit, forward)
4. **Voice Reply**: Integrate voice input for replies
5. **Reply Templates**: Suggest quick reply templates
