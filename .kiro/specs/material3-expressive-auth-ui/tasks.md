# Implementation Plan

- [x] 1. Create drawable resources for Material 3 Expressive styling








  - Create arrow forward icon drawable for button end icon
  - Create checkmark circle icon drawable for success state
  - Create social authentication button backgrounds (Google, Facebook, Apple)
  - Create rounded ripple effect drawable for interactive elements
  - Update existing gradient background for auth screens
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 2. Update main authentication layout (activity_auth.xml) with Material 3 Expressive design






  - [x] 2.1 Restructure logo and branding section

    - Update logo card to 96dp × 96dp with 24dp corner radius
    - Style app name TextView with 36sp Product Sans Bold
    - Style welcome message TextView with 18sp Product Sans
    - Apply entrance animations (logo scale, text fade-in)
    - _Requirements: 1.1, 1.4, 2.1, 2.2, 2.3_
  

  - [x] 2.2 Redesign input fields with Material 3 outlined style

    - Update email TextInputLayout with 16dp corner radius and 2dp stroke
    - Update password TextInputLayout with 16dp corner radius and password toggle
    - Update username TextInputLayout with 16dp corner radius (sign-up only)
    - Configure start icons with primary color tint
    - Set proper padding (16dp vertical) and height (56dp)
    - _Requirements: 1.3, 1.4, 4.4, 6.2_
  
  - [x] 2.3 Enhance password strength indicator styling

    - Update LinearProgressIndicator with 4dp height and 2dp corner radius
    - Apply color-coded progress (red/orange/green) based on strength
    - Style strength text with 12sp Product Sans
    - Configure fade-in animation for sign-up mode
    - _Requirements: 1.4, 4.4_
  

  - [x] 2.4 Redesign primary action button

    - Update button to 64dp height with 16dp corner radius
    - Configure arrow forward icon at end with 8dp padding
    - Style text with 18sp Product Sans Bold
    - Apply ripple effect with primary container color
    - Configure press animation (scale to 0.95x)
    - _Requirements: 1.5, 2.5, 4.1_
  
  - [x] 2.5 Enhance error card design

    - Update card with 16dp corner radius and error container background
    - Add alert icon (24dp) with error color tint
    - Style error text with 14sp Product Sans
    - Configure fade-in and shake animations
    - _Requirements: 1.4, 4.3_
  

  - [x] 2.6 Add social authentication section

    - Create divider with "Or Continue With Account" text
    - Add horizontal LinearLayout for social buttons
    - Create three circular buttons (48dp × 48dp) for Google, Facebook, Apple
    - Apply brand icons (24dp) centered in each button
    - Configure 16dp spacing between buttons
    - Add ripple effects and elevation (2dp)
    - _Requirements: 3.1, 3.3, 3.4_
-

- [x] 3. Create forgot password screen layout



  - [x] 3.1 Design forgot password activity layout


    - Create new layout file (activity_forgot_password.xml)
    - Add back button at top-left with 48dp touch target
    - Add logo card (96dp × 96dp, 24dp radius)
    - Add title TextView ("Forgot Password", 36sp Product Sans Bold)
    - Add subtitle TextView with instructions (16sp Product Sans)
    - _Requirements: 5.1, 5.2, 6.2_
  
  - [x] 3.2 Add forgot password form components

    - Create form card container with 24dp corner radius
    - Add email TextInputLayout with 16dp corner radius
    - Add "Continue" button (64dp height, 16dp radius)
    - Configure loading overlay for submission state
    - _Requirements: 5.2, 5.3, 5.4_

- [x] 4. Create ForgotPasswordActivity Kotlin implementation





  - [x] 4.1 Implement ForgotPasswordActivity class

    - Create activity class extending AppCompatActivity
    - Setup View Binding for type-safe view access
    - Initialize Supabase client reference
    - Configure back button navigation
    - _Requirements: 5.1, 5.2_
  
  - [x] 4.2 Implement email validation and submission

    - Add real-time email validation with 300ms debounce
    - Implement password reset request via Supabase auth
    - Show loading state during submission
    - Display success message with email confirmation
    - Handle errors with user-friendly messages
    - _Requirements: 5.4, 5.5, 4.1, 4.2_
  

  - [x] 4.3 Add animations and accessibility

    - Implement entrance animations for logo and text
    - Add focus animations for input field
    - Configure button press animations
    - Add content descriptions for all interactive elements
    - Support reduced motion preferences
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 6.1, 6.3_

- [x] 5. Update AuthActivity for social authentication UI





  - [x] 5.1 Add social button click listeners


    - Attach click listeners to Google, Facebook, Apple buttons
    - Add haptic feedback (CONTEXT_CLICK) on button press
    - Show placeholder toast messages for OAuth flow
    - Configure button press animations (scale to 0.95x)
    - _Requirements: 3.2, 2.5_
  
  - [x] 5.2 Add "Forgot Password" navigation


    - Add "Forgot Password" link to sign-in form
    - Style link with primary color and 48dp min touch target
    - Implement navigation to ForgotPasswordActivity
    - Add haptic feedback on tap
    - _Requirements: 5.1_

- [x] 6. Update landscape layout (layout-land/activity_auth.xml)




  - Restructure layout for horizontal arrangement
  - Position logo and form side-by-side
  - Reduce vertical spacing for better fit
  - Maintain all component styling from portrait layout
  - Test keyboard handling in landscape mode
  - _Requirements: 7.2, 7.4_




- [ ] 7. Update tablet layout (layout-sw600dp/activity_auth.xml)

  - Increase horizontal margins to 48dp
  - Update logo size to 120dp × 120dp
  - Increase form container max width to 600dp





  - Update text sizes (40sp title, 20sp subtitle)
  - Test on tablet emulator or device
  - _Requirements: 7.1, 7.3_

- [x] 8. Enhance existing animations in AuthActivity



  - [ ] 8.1 Refine mode toggle animations
    - Smooth username field slide down/up transitions (300ms)
    - Crossfade welcome text and toggle text (150ms)
    - Update button text and icon smoothly


    - Trigger password strength indicator visibility
    - _Requirements: 2.1, 2.2, 2.3, 2.4_
  
  - [x] 8.2 Enhance input field interaction animations




    - Apply 1.02x scale on focus (200ms)
    - Add validation success pulse animation (1.01x → 1.0x)
    - Refine validation error shake animation
    - Update end icon transitions (checkmark/alert)
    - _Requirements: 2.4, 4.5_
  




  - [ ] 8.3 Improve button state animations
    - Refine press animation (0.95x scale, 100ms)
    - Enhance success state with pulse (1.05x → 1.0x)
    - Add screen fade-out before navigation (300ms)


    - Update loading state transitions
    - _Requirements: 2.5, 4.1_

- [ ] 9. Update email verification layout styling

  - Update verification card with 24dp corner radius
  - Style verification title with 22sp Product Sans Bold
  - Update resend button to tonal style (56dp height, 12dp radius)




  - Enhance cooldown timer circular progress styling
  - Update auto-refresh indicator styling
  - Style back button consistently
  - _Requirements: 1.1, 1.4, 6.1_

- [ ] 10. Add animation resource files



  - Create shake animation XML (anim/shake.xml) for error feedback
  - Create pulse animation XML (anim/pulse.xml) for attention
  - Configure animation durations and interpolators
  - Test animations on different API levels
  - _Requirements: 2.1, 2.2, 2.3, 4.1_

- [x] 11. Verify accessibility compliance



  - Test with TalkBack screen reader enabled
  - Verify all touch targets meet 48dp minimum
  - Test reduced motion support (disable animations)
  - Verify keyboard navigation with IME actions
  - Test color contrast ratios for WCAG compliance
  - Verify content descriptions for all interactive elements
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 12. Test responsive design across screen sizes

  - Test on phone portrait (360dp × 640dp)
  - Test on phone landscape (640dp × 360dp)
  - Test on tablet portrait (768dp × 1024dp)
  - Test on tablet landscape (1024dp × 768dp)
  - Test on large screen (1280dp × 800dp)
  - Verify form width constraints and centering
  - Test keyboard handling on all sizes
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_



- [ ] 13. Perform visual regression testing

  - Capture screenshots of all authentication screens
  - Compare against reference design image
  - Verify Material 3 Expressive styling (rounded corners, elevation, colors)
  - Test light theme appearance
  - Verify animations run at 60fps
  - Check blur effect on API 31+ devices
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 4.2_
