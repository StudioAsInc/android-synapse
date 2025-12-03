# Implementation Plan

- [x] 1. Set up Material 3 Expressive design system for settings


  - [x] 1.1 Create SettingsTheme.kt with design tokens

    - Define `SettingsColors` object with semantic color mappings (categoryIconTint, sectionTitle, cardBackground, destructiveButton)
    - Define `SettingsShapes` object with corner radii (28dp cards, 24dp sections, 16dp items, 12dp inputs)
    - Define `SettingsSpacing` object with consistent padding and sizing values
    - Define `SettingsTypography` mappings for screen titles, section headers, item titles/subtitles
    - _Requirements: 1.4, 4.1_

  - [x] 1.2 Create SettingsAnimations.kt with motion specs

    - Define screen enter/exit transitions (fadeIn + slideInHorizontally)
    - Define content expand/collapse animations
    - Define press feedback scale animation
    - _Requirements: 1.2_

- [x] 2. Set up project structure and core interfaces





  - [x] 2.1 Create settings navigation sealed class and routes


    - Create `SettingsDestination.kt` with all navigation routes
    - Define route constants for Hub, Account, Privacy, Appearance, Notifications, Chat, Storage, Language, About
    - _Requirements: 1.1, 1.2_
  - [x] 2.2 Create settings data models and enums


    - Create `ThemeMode`, `FontScale`, `ProfileVisibility`, `ContentVisibility`, `MediaAutoDownload` enums
    - Create `AppearanceSettings`, `PrivacySettings`, `NotificationPreferences`, `ChatSettings` data classes
    - Create `SettingsCategory` and `UserProfileSummary` models
    - _Requirements: 3.2, 3.8, 4.1, 4.5, 5.3, 6.4_
  - [x] 2.3 Create SettingsRepository interface


    - Define Flow properties for all settings categories
    - Define suspend functions for updating settings
    - Define cache management methods
    - _Requirements: 10.1, 10.2, 10.5_
  - [ ]* 2.4 Write property test for enum options completeness
    - **Property 2: Enum Options Completeness**
    - **Validates: Requirements 3.2, 3.8, 4.5, 5.3, 6.4**

- [x] 3. Implement settings persistence layer






  - [x] 3.1 Create SettingsDataStore implementation

    - Implement DataStore preferences for all settings
    - Create serializers for complex data types
    - Implement error handling with default fallbacks
    - _Requirements: 10.1, 10.2, 10.4_

  - [x] 3.2 Implement SettingsRepositoryImpl



    - Implement all repository interface methods
    - Wire up DataStore flows to repository flows
    - Implement cache size calculation and clearing
    - _Requirements: 7.2, 10.1, 10.3_
  - [ ]* 3.3 Write property test for settings persistence round trip
    - **Property 1: Settings Persistence Round Trip**
    - **Validates: Requirements 4.2, 4.3, 4.7, 5.2, 5.6, 6.2, 6.3, 7.3, 10.1**
  - [ ]* 3.4 Write property test for settings restoration on launch
    - **Property 4: Settings Restoration on Launch**
    - **Validates: Requirements 10.2, 10.4**

- [x] 4. Checkpoint - Make sure all tests are passing





  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Create reusable Material 3 Expressive settings UI components





  - [x] 5.1 Create SettingsItem composables


    - Implement `SettingsToggleItem` with Material 3 Switch (primary thumb, primaryContainer track)
    - Implement `SettingsNavigationItem` with chevron icon (onSurfaceVariant at 0.5 alpha)
    - Implement `SettingsSelectionItem` with ExposedDropdownMenuBox
    - Implement `SettingsSliderItem` with Material 3 Slider and preview text
    - Implement `SettingsButtonItem` with FilledTonalButton (16dp corners)
    - Implement `SettingsHeaderItem` with titleMedium typography in primary color
    - Use consistent 16dp horizontal padding, 16dp vertical padding per item
    - _Requirements: 1.4, 2.1, 3.1, 4.1, 5.1, 6.1_
  - [x] 5.2 Create SettingsSection and SettingsCard containers

    - Implement `SettingsCard` with surfaceContainer background, 24dp corner radius
    - Implement `SettingsSection` with titleMedium header in primary color
    - Add HorizontalDivider between items (outlineVariant at 0.5 alpha)
    - Use 24dp spacing between sections
    - _Requirements: 1.1, 1.4_
  - [x] 5.3 Create ProfileHeaderCard component

    - Display 64dp circular avatar with border
    - Display name in titleLarge, email in bodyMedium
    - Use surfaceContainerHigh background with 28dp corners
    - Add Edit Profile FilledTonalButton
    - _Requirements: 1.5_

- [x] 6. Implement Settings Hub screen





  - [x] 6.1 Create SettingsHubViewModel


    - Expose user profile summary state
    - Expose settings categories list
    - Handle navigation events
    - _Requirements: 1.5_
  - [x] 6.2 Create SettingsHubScreen composable


    - Use LargeTopAppBar with exitUntilCollapsedScrollBehavior
    - Display ProfileHeaderCard at top with user info
    - Display categorized settings groups in SettingsCard containers
    - Each category shows icon (onSurfaceVariant), title, subtitle, and chevron
    - Use LazyColumn with 16dp horizontal padding, 24dp section spacing
    - _Requirements: 1.1, 1.4, 1.5_
  - [ ]* 6.3 Write property test for category display completeness
    - **Property 3: Settings Category Display Completeness**
    - **Validates: Requirements 1.1, 1.4**

- [x] 7. Implement Account Settings screen




  - [x] 7.1 Create AccountSettingsViewModel


    - Expose linked accounts state
    - Handle email change, password change flows
    - Handle delete account confirmation
    - _Requirements: 2.1, 2.3, 2.4, 2.5, 2.6_
  - [x] 7.2 Create AccountSettingsScreen composable


    - Use MediumTopAppBar with back navigation
    - Display Edit Profile, Change Email, Change Password as SettingsNavigationItems
    - Display Linked Accounts section with social provider icons and connect/disconnect buttons
    - Display Delete Account as destructive SettingsButtonItem (errorContainer background)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_
  - [x] 7.3 Create account-related dialogs


    - Implement ChangeEmailDialog with OutlinedTextField (12dp corners), validation
    - Implement ChangePasswordDialog with secure text fields, strength indicator
    - Implement DeleteAccountDialog with warning icon, confirmation text input
    - Use AlertDialog with 28dp corner radius
    - _Requirements: 2.3, 2.4, 2.6_

- [x] 8. Implement Privacy and Security Settings screen




  - [x] 8.1 Create PrivacySecurityViewModel


    - Expose privacy settings state (profile visibility, content visibility)
    - Expose security settings state (2FA, biometric lock)
    - Handle blocked/muted users navigation
    - _Requirements: 3.1, 3.2, 3.8_
  - [x] 8.2 Create PrivacySecurityScreen composable


    - Use MediumTopAppBar with back navigation
    - Display Profile Privacy section with SettingsSelectionItem for visibility
    - Display Security section with SettingsToggleItems for 2FA and biometric
    - Display Blocking section with SettingsNavigationItems for blocked/muted users
    - Display Active Sessions as SettingsNavigationItem
    - Group related items in SettingsCard containers
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

- [x] 9. Implement Appearance Settings screen





  - [x] 9.1 Create AppearanceViewModel


    - Expose theme mode, dynamic color, font scale states
    - Handle theme changes with immediate preview
    - Check Android SDK for dynamic color support
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  - [x] 9.2 Create AppearanceScreen composable


    - Use MediumTopAppBar with back navigation
    - Display Theme Mode as SettingsSelectionItem with Light/Dark/System options
    - Display Dynamic Color as SettingsToggleItem (conditionally visible based on SDK >= 31)
    - Display Font Size as SettingsSliderItem with live preview text below
    - Display Chat Customization as SettingsNavigationItem (placeholder)
    - Apply theme changes immediately using SynapseTheme
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_
  - [ ]* 9.3 Write property test for dynamic color visibility by SDK
    - **Property 9: Dynamic Color Visibility by SDK**
    - **Validates: Requirements 4.4**

- [x] 10. Checkpoint - Make sure all tests are passing




  - Ensure all tests pass, ask the user if questions arise.
-

- [x] 11. Implement Notification Settings screen




  - [x] 11.1 Create NotificationSettingsViewModel


    - Expose notification preferences state for all categories
    - Handle individual category toggles
    - Handle in-app notification toggle
    - _Requirements: 5.1, 5.2, 5.3, 5.6_
  - [x] 11.2 Create NotificationSettingsScreen composable


    - Use MediumTopAppBar with back navigation
    - Display Push Notifications section with SettingsToggleItems for each category
    - Display Notification Sound as SettingsNavigationItem (placeholder)
    - Display Do Not Disturb as SettingsNavigationItem (placeholder)
    - Display In-App Notifications as SettingsToggleItem
    - Group by "Activity" and "Preferences" sections
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

- [x] 12. Implement Chat Settings screen




  - [x] 12.1 Create ChatSettingsViewModel


    - Expose chat settings state (read receipts, typing indicators, auto-download)
    - Handle settings toggles
    - _Requirements: 6.1, 6.2, 6.3, 6.4_
  - [x] 12.2 Create ChatSettingsScreen composable


    - Use MediumTopAppBar with back navigation
    - Display Read Receipts as SettingsToggleItem
    - Display Typing Indicators as SettingsToggleItem
    - Display Media Auto-Download as SettingsSelectionItem (WiFi Only, Always, Never)
    - Display Message Requests as SettingsNavigationItem
    - Display Chat Privacy as SettingsNavigationItem (links to existing ChatPrivacySettingsActivity)
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [x] 13. Implement Storage and Data Settings screen




  - [x] 13.1 Create StorageDataViewModel


    - Expose cache size and storage statistics
    - Handle cache clearing with size calculation
    - Expose data saver mode state
    - _Requirements: 7.1, 7.2, 7.3_
  - [x] 13.2 Create StorageDataScreen composable


    - Use MediumTopAppBar with back navigation
    - Display Storage Usage card with visual progress indicator
    - Display cache size with Clear Cache SettingsButtonItem
    - Display Data Saver as SettingsToggleItem
    - Display Storage Provider Configuration as SettingsNavigationItem
    - Display AI Configuration as SettingsNavigationItem
    - Show confirmation snackbar after cache clear with freed space
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_
  - [ ]* 13.3 Write property test for cache clear size reduction
    - **Property 6: Cache Clear Size Reduction**
    - **Validates: Requirements 7.2**

- [x] 14. Implement Language and Region Settings screen



  - [x] 14.1 Create LanguageRegionViewModel


    - Expose available languages list with native names
    - Expose current language selection
    - Handle language change (placeholder)
    - _Requirements: 8.1, 8.4_
  - [x] 14.2 Create LanguageRegionScreen composable


    - Use MediumTopAppBar with back navigation
    - Display current language with checkmark indicator
    - Display available languages list with native script names (e.g., "日本語", "Español")
    - Display Region preferences as SettingsNavigationItem (placeholder)
    - Use RadioButton selection for language items
    - _Requirements: 8.1, 8.2, 8.3, 8.4_
  - [ ]* 14.3 Write property test for language native script display
    - **Property 10: Language Native Script Display**
    - **Validates: Requirements 8.4**

- [x] 15. Implement About and Support Settings screen




  - [x] 15.1 Create AboutSupportViewModel


    - Expose app version and build info
    - Handle external link navigation
    - Handle feedback submission
    - _Requirements: 9.1, 9.5_
  - [x] 15.2 Create AboutSupportScreen composable


    - Use MediumTopAppBar with back navigation
    - Display app logo and version info in centered header card
    - Display Terms of Service as SettingsNavigationItem (opens browser)
    - Display Privacy Policy as SettingsNavigationItem (opens browser)
    - Display Help Center as SettingsNavigationItem (placeholder)
    - Display Report a Problem as SettingsNavigationItem (opens feedback dialog)
    - Display Check for Updates as SettingsNavigationItem (placeholder)
    - Display Open Source Licenses as SettingsNavigationItem
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7_
  - [x] 15.3 Create FeedbackDialog composable


    - Display category dropdown (Bug, Feature Request, Other)
    - Display multiline description TextField
    - Display Submit button with loading state
    - Use AlertDialog with 28dp corner radius
    - _Requirements: 9.5_

- [x] 16. Checkpoint - Make sure all tests are passing




  - Ensure all tests pass, ask the user if questions arise.

- [x] 17. Wire up navigation and integrate screens





  - [x] 17.1 Create SettingsNavHost


    - Set up NavHost with all settings destinations
    - Configure enter/exit transitions (fadeIn + slideInHorizontally)
    - Handle back navigation with popBackStack
    - Use rememberNavController for state preservation
    - _Requirements: 1.2, 1.3_

  - [x] 17.2 Update SettingsActivity

    - Replace current SettingsScreen with SettingsNavHost
    - Wire up all navigation callbacks
    - Integrate with existing activities (ProfileEditActivity, ChatPrivacySettingsActivity)
    - Apply SynapseTheme with dynamic color support
    - _Requirements: 1.2, 2.2, 6.6_
  - [ ]* 17.3 Write property test for navigation route consistency
    - **Property 5: Navigation Route Consistency**
    - **Validates: Requirements 1.2**

- [x] 18. Implement logout and settings cleanup




  - [x] 18.1 Add logout settings segregation logic


    - Implement clearUserSettings in repository
    - Preserve device-level settings on logout (theme, font scale, dynamic color)
    - Clear user-specific settings on logout (notification prefs, privacy settings)
    - _Requirements: 10.3_
  - [ ]* 18.2 Write property test for logout settings segregation
    - **Property 7: Logout Settings Segregation**
    - **Validates: Requirements 10.3**
  - [ ]* 18.3 Write property test for StateFlow emission on change
    - **Property 8: StateFlow Emission on Change**
    - **Validates: Requirements 10.5**

- [x] 19. Add string resources and polish





  - [x] 19.1 Add string resources for all settings screens


    - Add strings for all setting titles, subtitles, and descriptions
    - Add strings for dialogs and confirmations
    - Add strings for error messages
    - _Requirements: 1.4, 2.1, 3.1, 4.1, 5.1, 6.1, 7.1, 8.1, 9.1_
  - [x] 19.2 Add icons and visual polish


    - Ensure all settings categories have appropriate Material icons
    - Verify consistent use of SettingsColors, SettingsShapes, SettingsSpacing
    - Add ripple effects on clickable items
    - Verify dark/light theme consistency
    - Test dynamic color on Android 12+ devices
    - _Requirements: 1.4_
  - [x] 19.3 Add accessibility support


    - Add contentDescription to all icons
    - Ensure proper focus order for screen readers
    - Verify touch targets are at least 48dp
    - _Requirements: 1.4_

- [x] 20. Final Checkpoint - Make sure all tests are passing





  - Ensure all tests pass, ask the user if questions arise.
