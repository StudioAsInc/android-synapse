# Implementation Plan

- [ ] 1. Create XML drawable resources for profile edit styling
  - Create drawable XML files in res/drawable/ directory for all visual styling components
  - Use theme attributes (?attr/colorSurface, ?attr/colorOutline, ?attr/colorError) for all colors
  - Define shapes with appropriate corner radii and stroke widths
  - _Requirements: 1.3, 2.1, 2.2, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

- [ ] 1.1 Create input field drawable resources
  - Create bg_profile_edit_input_normal.xml with 28dp corner radius, 3dp stroke using ?attr/colorOutline
  - Create bg_profile_edit_input_error.xml with 28dp corner radius, 3dp stroke using ?attr/colorError
  - Create bg_profile_edit_input_selector.xml as state list drawable with activated state for error
  - _Requirements: 1.3, 2.1, 2.2, 3.1, 3.2, 3.3, 4.1_

- [ ] 1.2 Create container drawable resources
  - Create bg_profile_edit_container.xml with 28dp corner radius, 3dp stroke using ?attr/colorOutline
  - Use ?attr/colorSurface for fill color to support theme switching
  - _Requirements: 1.3, 2.1, 2.2, 4.2, 4.3, 4.4, 4.5_

- [ ] 1.3 Create profile image drawable resources
  - Create bg_profile_edit_profile_card.xml with 28dp corner radius and transparent fill
  - Create bg_profile_edit_profile_image.xml with 300dp corner radius (circular) and transparent fill
  - _Requirements: 1.3, 2.1, 2.2, 4.6, 4.7_

- [ ] 2. Update activity_profile_edit.xml layout file
  - Update all input fields (mUsernameInput, mNicknameInput, mBiographyInput) to use @drawable/bg_profile_edit_input_selector
  - Update all containers (gender, region, profile_image_history_stage, cover_image_history_stage) to use @drawable/bg_profile_edit_container
  - Update profile cards (profileRelativeCard, stage1RelativeUpProfileCard) to use appropriate drawable resources
  - Remove any hardcoded color values from layout attributes
  - _Requirements: 1.3, 2.5, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 5.5_

- [ ] 3. Refactor ProfileEditActivity error handling methods
  - Update setUsernameError() to use isActivated state instead of programmatic background
  - Update clearUsernameError() to use isActivated state instead of programmatic background
  - Update setNicknameError() to use isActivated state instead of programmatic background
  - Update clearNicknameError() to use isActivated state instead of programmatic background
  - Update setBiographyError() to use isActivated state instead of programmatic background
  - Update clearBiographyError() to use isActivated state instead of programmatic background
  - _Requirements: 1.1, 1.2, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5, 5.4_

- [ ] 4. Remove programmatic styling from ProfileEditActivity
  - Remove all drawable creation code from initializeLogic() method
  - Remove utility methods: stateColor(), imageColor(), viewGraphics(), createGradientDrawable(), createStrokeDrawable()
  - Remove all hardcoded color values from the Kotlin code
  - Clean up unused imports (GradientDrawable, Color, PorterDuff)
  - _Requirements: 1.1, 1.2, 1.5, 2.4, 5.1, 5.2, 5.3, 5.4_

- [ ] 5. Verify and test the refactored implementation
  - Build the project and verify no compilation errors
  - Manually test all input field error states (username, nickname, biography)
  - Verify all visual styling appears correctly (rounded corners, strokes, colors)
  - Test theme switching between light and dark modes
  - Verify all existing functionality works (validation, saving, image upload, navigation)
  - Confirm at least 50 lines of code removed from ProfileEditActivity
  - _Requirements: 2.3, 5.3_
