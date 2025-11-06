# Implementation Plan

- [x] 1. Create animation utilities and configuration









  - Create `PostCardAnimations.kt` utility class with entrance, press, release, and exit animation methods
  - Implement `AnimationConfig` data class for configurable animation parameters
  - Add motion preference detection using `Settings.Global.ANIMATOR_DURATION_SCALE`
  - Implement safe animation wrapper with error handling and view attachment checks
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3, 5.4_

- [x] 2. Create MD3 post card layout





  - Create `item_post_md3.xml` using MaterialCardView with MD3 filled card style
  - Apply 12dp corner radius and proper elevation (1dp at rest)
  - Use MD3 color tokens (`colorSurfaceContainerLow`, `colorOnSurface`, etc.)
  - Implement header section with circular avatar using ShapeableImageView
  - Add content area with proper typography scale and markdown support
  - Create action bar with MD3 icon buttons for like, comment, and share
  - Add image container with proper aspect ratio constraints
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 3. Implement shimmer loading animation





  - Create `ShimmerDrawable` class for image placeholder animation
  - Implement gradient shimmer effect using ValueAnimator
  - Add shimmer to image views during Glide loading
  - Handle image load success with fade-in animation
  - Handle image load failure with error placeholder and fade-in
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [x] 4. Enhance PostsAdapter with MD3 animations





  - Add `enableMD3Animations` and `animationConfig` parameters to PostsAdapter constructor
  - Implement entrance animations in `onBindViewHolder` with position-based stagger
  - Add touch listener for press/release animations on card container
  - Implement button click animations for like, comment, and share buttons
  - Add animation cancellation in `onViewRecycled` to prevent memory leaks
  - Enable hardware layer during animations for performance
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4_
- [x] 5. Create MD3 theme styles and attributes




- [ ] 5. Create MD3 theme styles and attributes

  - Create `styles_post_card.xml` with MD3 card style definitions
  - Define custom theme attributes for post card colors
  - Add elevation and state list animators for card interactions
  - Ensure dynamic color support for Android 12+
  - Add dark theme variants
  - _Requirements: 1.1, 1.2, 1.4_
-

- [x] 6. Implement content update animations




  - Add cross-fade animation for post content updates
  - Implement smooth transitions when like/comment counts change
  - Add animation for post state changes (liked/unliked)
  - _Requirements: 5.3, 5.4_
-

- [x] 7. Add accessibility support




  - Implement motion preference checking before animations
  - Add proper content descriptions to all interactive elements
  - Ensure minimum 48dp touch targets for all buttons
  - Test with TalkBack screen reader
  - _Requirements: 2.4, 3.4, 4.4_

- [x] 8. Integrate with existing PostsAdapter



  - Update PostsAdapter to use new MD3 layout
  - Maintain backward compatibility with existing callback interfaces
  - Preserve Glide image loading integration
  - Maintain Markwon markdown rendering support
  - Test with existing HomeFragment and other consumers
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 9. Write unit tests for animations





  - Test entrance animation applies correct alpha and scale values
  - Test press animation scales to 0.98
  - Test release animation restores to 1.0 scale
  - Test button click bounce animation
  - Test animations respect motion preferences
  - Test stagger delay calculation
  - Test animation cancellation on view recycling
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4_

- [x] 10. Write UI tests for card interactions





  - Test cards animate on scroll into view
  - Test like button bounces on click
  - Test card scales on press and release
  - Test shimmer displays during image load
  - Test animations disabled when motion preference is off
  - Test dark/light theme rendering
  - _Requirements: 1.1, 1.2, 2.1, 3.1, 4.1, 6.1, 6.2_
