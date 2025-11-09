# Design Document: Material Design 3 Post Cards with Animations

## Overview

This design transforms the existing post card UI into a fully Material Design 3 compliant component system with rich animations. The implementation will enhance the current `PostsAdapter` and `item_post.xml` layout to incorporate MD3 design tokens, elevation system, dynamic theming, and sophisticated motion design patterns.

The design leverages the existing Material Components library (version 1.14.0-alpha06) already included in the project, which provides comprehensive MD3 support including dynamic color, elevation overlays, and motion specifications.

## Architecture

### Component Structure

```
PostsAdapter (Enhanced)
├── PostViewHolder
│   ├── MD3CardAnimator (new)
│   ├── InteractionAnimator (new)
│   └── ContentAnimator (new)
└── PostDiffCallback (existing)

UI Components
├── item_post_md3.xml (new MD3 layout)
├── PostCardAnimations.kt (new animation utilities)
└── MD3PostCardStyle.xml (new theme attributes)
```

### Key Design Decisions

1. **Non-Breaking Enhancement**: Create new MD3 components alongside existing ones to allow gradual migration
2. **Animation Performance**: Use hardware-accelerated animations with proper layer types
3. **Theme Integration**: Leverage Material3 dynamic color system for automatic theme adaptation
4. **Accessibility**: Ensure all animations respect user's motion preferences via `Settings.Global.ANIMATOR_DURATION_SCALE`

## Components and Interfaces

### 1. Enhanced PostsAdapter

The adapter will be extended to support MD3 animations while maintaining backward compatibility.

**New Constructor Parameters:**
```kotlin
class PostsAdapter(
    // ... existing parameters ...
    private val enableMD3Animations: Boolean = true,
    private val animationConfig: AnimationConfig = AnimationConfig.DEFAULT
)
```

**Animation Configuration:**
```kotlin
data class AnimationConfig(
    val entranceDuration: Long = 300L,
    val interactionDuration: Long = 100L,
    val exitDuration: Long = 250L,
    val staggerDelay: Long = 50L,
    val enableEntranceAnimations: Boolean = true,
    val enableInteractionAnimations: Boolean = true,
    val respectMotionPreferences: Boolean = true
)
```

### 2. PostCardAnimations Utility

A dedicated animation utility class that encapsulates all animation logic.

**Key Methods:**
```kotlin
object PostCardAnimations {
    fun animateEntrance(view: View, position: Int, config: AnimationConfig)
    fun animatePress(view: View, config: AnimationConfig)
    fun animateRelease(view: View, config: AnimationConfig)
    fun animateButtonClick(view: View, config: AnimationConfig)
    fun animateExit(view: View, config: AnimationConfig, onComplete: () -> Unit)
    fun animateContentUpdate(view: View, config: AnimationConfig)
    fun animateImageLoad(imageView: ImageView, config: AnimationConfig)
    fun createShimmerAnimation(view: View): ValueAnimator
}
```

**Animation Specifications:**
- Entrance: Alpha 0→1 + Scale 0.95→1.0 with FastOutSlowIn interpolator
- Press: Scale 1.0→0.98 with LinearOutSlowIn interpolator
- Release: Scale 0.98→1.0 with FastOutLinearIn interpolator
- Button Click: Scale 1.0→1.2→1.0 with OvershootInterpolator
- Exit: Alpha 1→0 + Scale 1.0→0.9 with FastOutLinearIn interpolator

### 3. MD3 Layout Design (item_post_md3.xml)

**Material Design 3 Specifications:**
- Card Style: Filled card (elevated surface)
- Elevation: 1dp at rest, 2dp when pressed
- Corner Radius: 12dp
- Container Color: `?attr/colorSurfaceContainerLow`
- Content Padding: 16dp
- Typography: Material3 type scale

**Layout Structure:**
```xml
<com.google.android.material.card.MaterialCardView
    style="@style/Widget.Material3.CardView.Filled"
    android:elevation="1dp"
    app:cardCornerRadius="12dp"
    app:cardBackgroundColor="?attr/colorSurfaceContainerLow">
    
    <!-- Header with avatar, name, timestamp -->
    <!-- Content area with markdown support -->
    <!-- Media container with aspect ratio -->
    <!-- Action bar with MD3 icon buttons -->
    
</com.google.android.material.card.MaterialCardView>
```

### 4. Interaction States

**State Management:**
- Default: Elevation 1dp, alpha 1.0, scale 1.0
- Hovered (for large screens): Elevation 2dp
- Pressed: Elevation 2dp, scale 0.98
- Focused: Outline with `?attr/colorPrimary`
- Disabled: Alpha 0.38

### 5. Image Loading with Shimmer

**Shimmer Placeholder:**
```kotlin
class ShimmerDrawable : Drawable() {
    private val shimmerPaint = Paint()
    private val shimmerAnimator: ValueAnimator
    
    // Gradient shimmer effect during image load
    // Colors: colorSurfaceVariant → colorSurface → colorSurfaceVariant
}
```

## Data Models

No changes to existing `Post` model required. The design works with the current data structure.

**Optional Enhancement:**
```kotlin
data class Post(
    // ... existing fields ...
    @kotlinx.serialization.Transient
    var isLikedByCurrentUser: Boolean = false, // For animation state
    @kotlinx.serialization.Transient
    var hasBeenViewed: Boolean = false // For entrance animation tracking
)
```

## Error Handling

### Animation Failures

1. **Motion Disabled**: Check `Settings.Global.ANIMATOR_DURATION_SCALE` and skip animations if 0
2. **Low Memory**: Catch `OutOfMemoryError` during bitmap operations and fallback to simple transitions
3. **View Detached**: Check `view.isAttachedToWindow` before starting animations
4. **Animation Interruption**: Cancel existing animations before starting new ones

**Error Recovery:**
```kotlin
fun safeAnimate(view: View, animation: () -> Unit) {
    try {
        if (view.isAttachedToWindow && shouldAnimate(view.context)) {
            animation()
        }
    } catch (e: Exception) {
        Log.w("PostCardAnimations", "Animation failed", e)
        // Fallback to final state without animation
    }
}
```

### Image Loading Failures

1. **Network Error**: Show placeholder with retry option
2. **Invalid URL**: Show error icon with fade-in
3. **Memory Pressure**: Use Glide's automatic downsampling

## Testing Strategy

### Unit Tests

**PostCardAnimations Tests:**
```kotlin
class PostCardAnimationsTest {
    @Test fun `entrance animation applies correct alpha and scale`()
    @Test fun `press animation scales to 0_98`()
    @Test fun `animations respect motion preferences`()
    @Test fun `stagger delay increases with position`()
    @Test fun `exit animation completes before callback`()
}
```

### UI Tests

**Animation Integration Tests:**
```kotlin
@Test fun `post cards animate on scroll into view`()
@Test fun `like button bounces on click`()
@Test fun `card scales on press and release`()
@Test fun `shimmer displays during image load`()
@Test fun `animations disabled when motion preference is off`()
```

### Visual Regression Tests

- Capture screenshots of cards in different states
- Verify MD3 elevation and color tokens
- Test dark/light theme consistency
- Validate dynamic color application

## Performance Considerations

### Optimization Strategies

1. **Hardware Acceleration**: Enable hardware layers during animations
   ```kotlin
   view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
   // ... animate ...
   view.setLayerType(View.LAYER_TYPE_NONE, null)
   ```

2. **Animation Pooling**: Reuse animator instances for repeated animations
3. **Stagger Limiting**: Only stagger first 10 visible items to prevent lag
4. **Bitmap Caching**: Use Glide's memory and disk cache for images
5. **RecyclerView Optimization**: Disable animations during fast scroll

**Performance Metrics:**
- Target: 60fps (16.67ms per frame) during animations
- Maximum entrance animation time: 300ms + (position * 50ms), capped at 800ms
- Memory overhead: < 2MB for animation objects

### Memory Management

```kotlin
override fun onViewRecycled(holder: PostViewHolder) {
    super.onViewRecycled(holder)
    holder.cancelAnimations() // Cancel any running animations
    holder.clearImageCache() // Clear Glide requests
}
```

## Accessibility

### Motion Preferences

```kotlin
fun shouldAnimate(context: Context): Boolean {
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    )
    return scale > 0f
}
```

### Content Descriptions

All interactive elements will have proper content descriptions:
- Like button: "Like post by [username], currently [liked/not liked], [count] likes"
- Comment button: "Comment on post, [count] comments"
- Share button: "Share post"
- More options: "More options for post"

### Touch Target Sizes

All interactive elements meet minimum 48dp touch target size per Material Design guidelines.

## Theme Integration

### MD3 Color Tokens

```xml
<!-- Post Card Colors -->
<attr name="postCardBackgroundColor" format="color" />
<attr name="postCardContentColor" format="color" />
<attr name="postCardActionColor" format="color" />

<!-- Default values using MD3 tokens -->
<item name="postCardBackgroundColor">?attr/colorSurfaceContainerLow</item>
<item name="postCardContentColor">?attr/colorOnSurface</item>
<item name="postCardActionColor">?attr/colorOnSurfaceVariant</item>
```

### Dynamic Color Support

The design automatically adapts to:
- System dynamic colors (Android 12+)
- Light/Dark theme
- Custom app themes
- High contrast mode

## Migration Path

### Phase 1: Parallel Implementation
- Create new MD3 components without modifying existing code
- Add feature flag to toggle between old and new UI

### Phase 2: Gradual Rollout
- Enable MD3 cards for new users
- A/B test with existing users
- Monitor performance metrics

### Phase 3: Full Migration
- Replace old layouts with MD3 versions
- Remove legacy code
- Update documentation

## Dependencies

All required dependencies are already included in the project:
- `com.google.android.material:material:1.14.0-alpha06` (MD3 components)
- `com.github.bumptech.glide:glide:5.0.0-rc01` (Image loading)
- `androidx.recyclerview:recyclerview:1.4.0` (List display)
- `io.noties.markwon:core:4.6.2` (Markdown rendering)

No additional dependencies required.

## Implementation Notes

### Existing Code Reuse

The design maximizes reuse of existing components:
- `PostsAdapter` structure remains intact
- `Post` model unchanged
- Glide integration preserved
- Markwon markdown rendering maintained

### New Files Required

1. `app/src/main/java/com/synapse/social/studioasinc/animations/PostCardAnimations.kt`
2. `app/src/main/res/layout/item_post_md3.xml`
3. `app/src/main/res/values/styles_post_card.xml`
4. `app/src/main/res/animator/post_card_entrance.xml` (optional, for XML-based animations)

### Code Organization

Following the existing project structure:
- Animations in `animations/` package
- Adapter remains in root package
- Layouts in `res/layout/`
- Styles in `res/values/`
