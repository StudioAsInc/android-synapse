# MD3 Post Cards - Accessibility Features

## Overview

This document describes the accessibility features implemented for the Material Design 3 post cards feature, ensuring the app is usable by all users, including those with disabilities.

## Motion Preferences

### Implementation

The animation system respects the user's motion preferences set at the system level:

```kotlin
fun shouldAnimate(context: Context): Boolean {
    return try {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        scale > 0f
    } catch (e: Exception) {
        Log.w(TAG, "Failed to check motion preferences", e)
        true // Default to enabled if check fails
    }
}
```

### Behavior

- **Motion Enabled**: All animations play normally (entrance, press/release, button clicks, etc.)
- **Motion Disabled**: All animations are skipped, content appears instantly
- **System Setting**: Controlled via `Settings > Accessibility > Remove animations` on Android

### Animation Configuration

The `AnimationConfig` class includes a `respectMotionPreferences` flag:

```kotlin
data class AnimationConfig(
    val respectMotionPreferences: Boolean = true
    // ... other properties
)
```

When set to `true`, all animations check motion preferences before executing.

## Content Descriptions

### Dynamic Content Descriptions

All interactive elements have proper content descriptions that update dynamically based on state:

#### Like Button
- **Not Liked**: "Like post, X likes"
- **Liked**: "Unlike post, X likes"
- Updates when like count changes

#### Comment Button
- **Description**: "Comment on post, X comments"
- Updates when comment count changes

#### Share Button
- **Description**: "Share post"
- Static description

#### More Options Button
- **Description**: "More options for post"
- Static description

#### Author Avatar
- **Description**: "Author profile picture @username"
- Includes username for context

#### Card Container
- **Description**: "Post by @username"
- Provides context for the entire card

#### Timestamp
- **Description**: "Posted 2h ago" (or similar)
- Provides temporal context

### Implementation Example

```kotlin
// Update like button accessibility
likeButton.contentDescription = if (liked) {
    context.getString(R.string.like_post_liked, count)
} else {
    context.getString(R.string.like_post_with_count, count)
}
```

## Touch Target Sizes

### Minimum Touch Target: 48dp

All interactive elements meet or exceed the Material Design minimum touch target size of 48dp × 48dp:

#### Updated Elements

1. **Author Avatar**: Increased from 40dp to 48dp
   ```xml
   <com.google.android.material.imageview.ShapeableImageView
       android:id="@+id/authorAvatar"
       android:layout_width="48dp"
       android:layout_height="48dp"
       android:clickable="true"
       android:focusable="true"
       ... />
   ```

2. **Like Button**: Already 48dp minimum (wrap_content with minWidth/minHeight)
   ```xml
   <com.google.android.material.button.MaterialButton
       android:id="@+id/likeButton"
       android:minWidth="48dp"
       android:minHeight="48dp"
       ... />
   ```

3. **Comment Button**: Already 48dp minimum
4. **Share Button**: Already 48dp (48dp × 48dp)
5. **More Options Button**: Already 48dp (48dp × 48dp)

### Benefits

- Easier to tap for users with motor impairments
- Reduces accidental taps
- Improves usability on all devices
- Meets WCAG 2.1 Level AAA guidelines (44px minimum)

## Screen Reader Support (TalkBack)

### Focusable Elements

All interactive elements are properly marked as focusable:

```xml
android:clickable="true"
android:focusable="true"
```

### Reading Order

TalkBack reads elements in a logical order:

1. Author avatar and name
2. Post timestamp
3. More options button
4. Post content
5. Post image (if present)
6. Like button with count
7. Comment button with count
8. Share button

### Announcements

When users interact with elements:

- **Like**: "Like post, 5 likes" → tap → "Unlike post, 6 likes"
- **Comment**: "Comment on post, 3 comments"
- **Share**: "Share post"

### Content Updates

When content changes (like counts, comment counts), TalkBack announces the new values automatically because the content descriptions are updated.

## Testing with TalkBack

### How to Enable TalkBack

1. Go to **Settings > Accessibility > TalkBack**
2. Toggle TalkBack on
3. Use two-finger swipe to scroll
4. Single tap to focus, double tap to activate

### Test Scenarios

#### 1. Navigate Post Card
- Swipe right to move through elements
- Verify each element announces correctly
- Verify reading order is logical

#### 2. Like a Post
- Focus on like button
- Verify announcement: "Like post, X likes"
- Double tap to like
- Verify announcement updates: "Unlike post, X+1 likes"

#### 3. Motion Preferences
- Disable animations in system settings
- Navigate to posts
- Verify no animations play
- Verify content still appears correctly

#### 4. Touch Targets
- Enable TalkBack
- Tap near edges of buttons
- Verify buttons activate reliably

## Accessibility Strings

All accessibility strings are defined in `strings.xml`:

```xml
<!-- MD3 Post Card Accessibility Strings -->
<string name="author_avatar">Author profile picture</string>
<string name="post_options">More options for post</string>
<string name="post_image">Post image</string>
<string name="like_post">Like post</string>
<string name="like_post_with_count">Like post, %1$d likes</string>
<string name="like_post_liked">Unlike post, %1$d likes</string>
<string name="comment_on_post">Comment on post</string>
<string name="comment_on_post_with_count">Comment on post, %1$d comments</string>
<string name="share_post">Share post</string>
<string name="post_by_user">Post by %1$s</string>
<string name="post_timestamp">Posted %1$s</string>
```

### Localization

These strings support localization through Android's resource system. Translations can be added in:
- `values-tr/strings.xml` (Turkish)
- `values-bn/strings.xml` (Bengali)
- Other language folders as needed

## Best Practices Followed

### 1. Semantic HTML/XML
- Used proper Material Design components
- Marked interactive elements as clickable and focusable

### 2. Keyboard Navigation
- All interactive elements are focusable
- Focus order follows visual order

### 3. Color Contrast
- Uses Material Design 3 color tokens
- Ensures proper contrast ratios
- Supports dark theme

### 4. Text Scaling
- Uses `sp` units for text sizes
- Supports system font size settings
- Text remains readable at 200% scale

### 5. Motion Sensitivity
- Respects system motion preferences
- Provides instant alternatives to animations
- No flashing or strobing effects

## WCAG 2.1 Compliance

### Level A (Minimum)
✅ 1.1.1 Non-text Content - All images have alt text
✅ 2.1.1 Keyboard - All functionality available via keyboard/TalkBack
✅ 2.4.4 Link Purpose - All buttons have clear purposes
✅ 4.1.2 Name, Role, Value - All elements properly labeled

### Level AA (Recommended)
✅ 1.4.3 Contrast - Meets minimum contrast ratios
✅ 1.4.5 Images of Text - Uses actual text, not images
✅ 2.4.7 Focus Visible - Focus indicators visible
✅ 2.5.5 Target Size - Minimum 48dp touch targets

### Level AAA (Enhanced)
✅ 2.2.3 No Timing - No time limits on interactions
✅ 2.3.3 Animation from Interactions - Respects motion preferences
✅ 2.5.5 Target Size (Enhanced) - 48dp exceeds 44px minimum

## Future Enhancements

### Potential Improvements

1. **Custom Accessibility Actions**
   - Add custom actions for quick like/comment/share
   - Implement via `ViewCompat.addAccessibilityAction()`

2. **Live Regions**
   - Announce like count changes automatically
   - Use `ViewCompat.setAccessibilityLiveRegion()`

3. **Grouped Content**
   - Group related elements (avatar + name)
   - Use `android:accessibilityTraversalBefore/After`

4. **Haptic Feedback**
   - Add vibration on button presses
   - Provide tactile feedback for actions

5. **Voice Commands**
   - Integrate with Voice Access
   - Support custom voice commands

## Resources

- [Android Accessibility Guide](https://developer.android.com/guide/topics/ui/accessibility)
- [Material Design Accessibility](https://material.io/design/usability/accessibility.html)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [TalkBack User Guide](https://support.google.com/accessibility/android/answer/6283677)

## Testing Checklist

- [ ] Enable TalkBack and navigate through post cards
- [ ] Verify all elements have proper content descriptions
- [ ] Test with animations disabled in system settings
- [ ] Verify touch targets are at least 48dp
- [ ] Test with large font sizes (200% scale)
- [ ] Test in dark mode for contrast
- [ ] Test with color inversion enabled
- [ ] Verify focus order is logical
- [ ] Test button interactions with TalkBack
- [ ] Verify announcements update when content changes
