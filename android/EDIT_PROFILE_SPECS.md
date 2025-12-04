# Edit Profile Activity - Jetpack Compose Redesign Specifications

## 1. Overview

### 1.1 Purpose
Redesign the Edit Profile Activity using Jetpack Compose with Material 3 Expressive components, maintaining design consistency with the Settings feature while following MVVM architecture and modern Android development standards.

### 1.2 Target
- **Activity**: `ProfileEditActivity.kt`
- **New Compose Implementation**: `EditProfileScreen.kt`
- **ViewModel**: `EditProfileViewModel.kt`
- **UI State**: `EditProfileUiState.kt`

### 1.3 Design Philosophy
- Material 3 Expressive design language
- Consistent with Settings UI patterns
- Focus on accessibility and usability
- Smooth animations and transitions
- Proper error handling and validation feedback

---

## 2. Architecture (MVVM)

### 2.1 Structure
```
presentation/
├── editprofile/
│   ├── EditProfileScreen.kt          // Main Compose UI
│   ├── EditProfileViewModel.kt       // Business logic & state
│   ├── EditProfileUiState.kt         // UI state models
│   ├── components/
│   │   ├── ProfileImageSection.kt    // Cover & avatar images
│   │   ├── ProfileFormFields.kt      // Input fields
│   │   ├── GenderSelector.kt         // Gender selection UI
│   │   └── ValidationIndicator.kt    // Real-time validation
```

### 2.2 ViewModel Responsibilities
- Manage UI state with StateFlow
- Handle form validation (username, nickname, biography)
- Coordinate image uploads with repository
- Interact with Supabase services via repository pattern
- Emit UI events (success, error, navigation)

### 2.3 Repository Pattern
```kotlin
EditProfileRepository:
- getUserProfile(userId: String): Flow<Result<UserProfile>>
- updateProfile(profile: UserProfile): Result<Unit>
- uploadAvatar(userId: String, imagePath: String): Result<String>
- uploadCover(userId: String, imagePath: String): Result<String>
- checkUsernameAvailability(username: String): Result<Boolean>
```

---

## 3. UI Design Specifications

### 3.1 Layout Structure

#### Top App Bar
- **Type**: `MediumTopAppBar` with scroll behavior
- **Title**: "Edit Profile"
- **Navigation Icon**: Back arrow (24dp, onSurface color)
- **Actions**: Save icon button (24dp, primary color when enabled, disabled when invalid)
- **Scroll Behavior**: `exitUntilCollapsedScrollBehavior()`
- **Colors**: 
  - Container: `surface`
  - Scrolled Container: `surfaceContainer`
  - Content: `onSurface`

#### Content Layout
- **Container**: `LazyColumn` with `nestedScroll` modifier
- **Padding**: 16dp horizontal, 8dp vertical content padding
- **Spacing**: 24dp between sections

### 3.2 Profile Image Section

#### Cover Image
- **Dimensions**: Full width, 200dp height
- **Shape**: `RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)`
- **Placeholder**: `R.drawable.user_null_cover_photo`
- **Overlay**: Semi-transparent scrim with edit icon (48dp touch target)
- **Click Action**: Open image picker for cover photo
- **Loading State**: Shimmer effect during upload

#### Avatar Image
- **Dimensions**: 96dp diameter (larger than Settings' 64dp for editing context)
- **Position**: Centered, overlapping cover by 48dp
- **Shape**: `CircleShape`
- **Border**: 4dp white/surface border
- **Placeholder**: `R.drawable.avatar` or initial letter in `primaryContainer`
- **Overlay**: Edit icon badge (32dp) at bottom-right
- **Click Action**: Open image picker for avatar
- **Loading State**: Circular progress indicator overlay

#### Container Card
- **Background**: `surfaceContainerHigh`
- **Shape**: `RoundedCornerShape(24.dp)`
- **Elevation**: 2.dp tonal elevation
- **Padding**: 24dp bottom padding for avatar overlap

### 3.3 Form Fields Section

#### Username Field
- **Component**: `OutlinedTextField` with Material 3 styling
- **Label**: "Username"
- **Placeholder**: "Enter username"
- **Leading Icon**: `ic_person` (24dp, `onSurfaceVariant`)
- **Trailing Icon**: 
  - Check icon (success, `primary`)
  - Error icon (error, `error`)
  - Loading indicator (checking availability)
- **Shape**: `RoundedCornerShape(12.dp)`
- **Max Length**: 25 characters
- **Keyboard Type**: `KeyboardType.Ascii`
- **Capitalization**: `KeyboardCapitalization.None`
- **Validation**: Real-time with debounce (500ms)
- **Error Display**: Below field with `bodySmall` typography in `error` color
- **Helper Text**: "@username" format hint in `onSurfaceVariant`

**Validation Rules**:
- Required (not empty)
- 3-25 characters
- Lowercase letters, numbers, underscore, period only
- Must contain at least one letter
- Unique (check against database)

#### Nickname Field
- **Component**: `OutlinedTextField`
- **Label**: "Display Name"
- **Placeholder**: "Enter display name (optional)"
- **Leading Icon**: `ic_badge` (24dp)
- **Shape**: `RoundedCornerShape(12.dp)`
- **Max Length**: 30 characters
- **Keyboard Type**: `KeyboardType.Text`
- **Capitalization**: `KeyboardCapitalization.Words`
- **Helper Text**: "Optional - shown instead of username"

#### Biography Field
- **Component**: `OutlinedTextField` with `minLines = 3`, `maxLines = 5`
- **Label**: "Bio"
- **Placeholder**: "Tell us about yourself (optional)"
- **Leading Icon**: `ic_description` (24dp, aligned to top)
- **Shape**: `RoundedCornerShape(12.dp)`
- **Max Length**: 250 characters
- **Counter**: "X/250" in `onSurfaceVariant` at bottom-right
- **Keyboard Type**: `KeyboardType.Text`
- **Capitalization**: `KeyboardCapitalization.Sentences`

#### Container
- **Background**: `surfaceContainer`
- **Shape**: `RoundedCornerShape(24.dp)`
- **Padding**: 16dp internal padding
- **Spacing**: 16dp between fields

### 3.4 Gender Selection Section

#### Layout
- **Container**: `surfaceContainer` card with `RoundedCornerShape(24.dp)`
- **Header**: "Gender" in `titleMedium`, `primary` color
- **Subtitle**: "Choose how you'd like to be identified" in `bodyMedium`, `onSurfaceVariant`
- **Padding**: 16dp

#### Gender Options
Three options in vertical arrangement with 8dp spacing:

**Option Card** (repeated for Male, Female, Hidden):
- **Component**: `Surface` with `clickable` modifier
- **Shape**: `RoundedCornerShape(16.dp)`
- **Background**: 
  - Selected: `primaryContainer`
  - Unselected: `surfaceContainerHighest`
- **Border**: 
  - Selected: 2.dp, `primary`
  - Unselected: 1.dp, `outlineVariant`
- **Padding**: 16dp
- **Min Height**: 56dp (touch target)

**Option Content**:
- **Layout**: Row with icon, text, spacer, radio button
- **Icon**: 24dp, tinted `primary` (selected) or `onSurfaceVariant` (unselected)
  - Male: `ic_male`
  - Female: `ic_female`
  - Hidden: `ic_visibility_off`
- **Text**: `bodyLarge` with `fontWeight.Medium`
- **Radio Button**: Material 3 `RadioButton` at trailing edge

**Animation**: 
- Scale animation (0.95 → 1.0) on selection
- Color transition (300ms)

### 3.5 Region Selection Section

#### Layout
- **Container**: `surfaceContainer` card with `RoundedCornerShape(24.dp)`
- **Component**: `SettingsNavigationItem` (reused from Settings)
- **Title**: "Region"
- **Subtitle**: Current region or "Not set"
- **Icon**: `ic_location` (24dp)
- **Trailing**: Chevron icon
- **Click Action**: Navigate to `SelectRegionActivity`

### 3.6 History Sections

#### Profile Photo History
- **Container**: `surfaceContainer` card
- **Component**: `SettingsNavigationItem`
- **Title**: "Profile Photo History"
- **Subtitle**: "View and restore previous photos"
- **Icon**: `ic_history` (24dp)
- **Click Action**: Navigate to `ProfilePhotoHistoryActivity`

#### Cover Photo History
- **Container**: `surfaceContainer` card
- **Component**: `SettingsNavigationItem`
- **Title**: "Cover Photo History"
- **Subtitle**: "View and restore previous covers"
- **Icon**: `ic_history` (24dp)
- **Click Action**: Navigate to `ProfileCoverPhotoHistoryActivity`

---

## 4. Spacing & Dimensions

### 4.1 Consistent Spacing (from Settings)
```kotlin
object EditProfileSpacing {
    val screenPadding = 16.dp
    val sectionSpacing = 24.dp
    val itemSpacing = 16.dp
    val cardPadding = 16.dp
    val iconSize = 24.dp
    val minTouchTarget = 48.dp
    
    // Edit Profile specific
    val coverHeight = 200.dp
    val avatarSize = 96.dp
    val avatarOverlap = 48.dp
    val imageSectionPadding = 24.dp
}
```

### 4.2 Corner Radii (from Settings)
```kotlin
object EditProfileShapes {
    val cardShape = RoundedCornerShape(24.dp)
    val itemShape = RoundedCornerShape(16.dp)
    val inputShape = RoundedCornerShape(12.dp)
    val coverShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}
```

---

## 5. Colors & Theme

### 5.1 Color Mappings (Material 3)
```kotlin
object EditProfileColors {
    val cardBackground = MaterialTheme.colorScheme.surfaceContainer
    val cardBackgroundElevated = MaterialTheme.colorScheme.surfaceContainerHigh
    val sectionTitle = MaterialTheme.colorScheme.primary
    val inputBorder = MaterialTheme.colorScheme.outline
    val inputBorderFocused = MaterialTheme.colorScheme.primary
    val inputBorderError = MaterialTheme.colorScheme.error
    val successIndicator = MaterialTheme.colorScheme.primary
    val errorIndicator = MaterialTheme.colorScheme.error
    val helperText = MaterialTheme.colorScheme.onSurfaceVariant
    val counterText = MaterialTheme.colorScheme.onSurfaceVariant
    val imageOverlay = Color.Black.copy(alpha = 0.4f)
}
```

### 5.2 Dynamic Color Support
- Support Android 12+ dynamic colors via `SynapseTheme`
- Respect user's theme preference (Light/Dark/System)
- All colors derived from `MaterialTheme.colorScheme`

---

## 6. Typography

### 6.1 Text Styles (Material 3 Type Scale)
```kotlin
object EditProfileTypography {
    val screenTitle = MaterialTheme.typography.headlineMedium
    val sectionHeader = MaterialTheme.typography.titleMedium
    val fieldLabel = MaterialTheme.typography.bodyLarge
    val fieldInput = MaterialTheme.typography.bodyLarge
    val helperText = MaterialTheme.typography.bodySmall
    val errorText = MaterialTheme.typography.bodySmall
    val counterText = MaterialTheme.typography.bodySmall
}
```

---

## 7. Validation & Error Handling

### 7.1 Real-Time Validation

#### Username Validation
```kotlin
sealed class UsernameValidation {
    object Valid : UsernameValidation()
    object Checking : UsernameValidation()
    data class Error(val message: String) : UsernameValidation()
}
```

**Validation Flow**:
1. User types → Debounce 500ms
2. Check format rules locally
3. If format valid → Check availability via API
4. Update UI with result (icon + message)

**Error Messages**:
- "Username is required"
- "Username must be 3-25 characters"
- "Only lowercase letters, numbers, _ and . allowed"
- "Username must contain at least one letter"
- "Username is already taken"

#### Nickname Validation
- Max 30 characters
- Error: "Nickname must be 30 characters or less"

#### Biography Validation
- Max 250 characters
- Show counter: "X/250"
- Error: "Bio must be 250 characters or less"

### 7.2 Save Button State
```kotlin
sealed class SaveButtonState {
    object Enabled : SaveButtonState()
    object Disabled : SaveButtonState()
    object Saving : SaveButtonState()
}
```

**Enabled When**:
- All validations pass
- At least one field changed
- Not currently saving

**Disabled When**:
- Any validation error exists
- No changes made
- Currently saving

### 7.3 Error Display
- **Field Errors**: Below field with error icon and message
- **Save Errors**: Snackbar at bottom with retry action
- **Network Errors**: Snackbar with "Retry" action
- **Image Upload Errors**: Dialog with error details

---

## 8. Loading States

### 8.1 Initial Load
- **Skeleton Screen**: Shimmer effect for all sections
- **Duration**: Until profile data loaded
- **Fallback**: Show error state if load fails

### 8.2 Image Upload
- **Avatar**: Circular progress overlay (48dp)
- **Cover**: Linear progress bar at top
- **Feedback**: Haptic feedback on success

### 8.3 Save Operation
- **Button**: Show `CircularProgressIndicator` (18dp)
- **Disable**: All inputs during save
- **Success**: Haptic feedback + navigate back
- **Error**: Show snackbar with message

---

## 9. Animations & Transitions

### 9.1 Screen Entry
- **Type**: Slide in from right with fade
- **Duration**: 300ms
- **Easing**: `FastOutSlowInEasing`

### 9.2 Field Focus
- **Border**: Animate color change (150ms)
- **Label**: Animate position and scale (150ms)

### 9.3 Validation Feedback
- **Success Icon**: Scale in with bounce (200ms)
- **Error Icon**: Shake animation (300ms)
- **Error Text**: Fade in from top (150ms)

### 9.4 Gender Selection
- **Selection**: Scale animation (0.95 → 1.0, 200ms)
- **Background**: Color transition (300ms)
- **Border**: Width and color transition (200ms)

### 9.5 Image Upload
- **Progress**: Circular reveal for avatar, linear for cover
- **Success**: Scale bounce (300ms)
- **Error**: Shake animation (300ms)

---

## 10. Accessibility

### 10.1 Content Descriptions
- All icons have meaningful descriptions
- Images include user name in description
- Form fields have proper labels
- Error messages announced by TalkBack

### 10.2 Touch Targets
- Minimum 48dp for all interactive elements
- Adequate spacing between clickable items
- Large tap areas for image selection

### 10.3 Keyboard Navigation
- Proper focus order (top to bottom)
- IME actions: "Next" for fields, "Done" for last field
- Submit on "Done" action if valid

### 10.4 Screen Reader Support
```kotlin
// Example semantic properties
modifier = Modifier.semantics {
    contentDescription = "Username field, required"
    stateDescription = when (validationState) {
        is Valid -> "Valid username"
        is Checking -> "Checking availability"
        is Error -> validationState.message
    }
}
```

---

## 11. State Management

### 11.1 UI State Model
```kotlin
data class EditProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val profile: UserProfile? = null,
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val username: String = "",
    val usernameValidation: UsernameValidation = UsernameValidation.Valid,
    val nickname: String = "",
    val nicknameError: String? = null,
    val biography: String = "",
    val biographyError: String? = null,
    val selectedGender: Gender = Gender.Hidden,
    val selectedRegion: String? = null,
    val hasChanges: Boolean = false,
    val error: String? = null
)

enum class Gender {
    Male, Female, Hidden
}
```

### 11.2 ViewModel Events
```kotlin
sealed class EditProfileEvent {
    data class UsernameChanged(val username: String) : EditProfileEvent()
    data class NicknameChanged(val nickname: String) : EditProfileEvent()
    data class BiographyChanged(val biography: String) : EditProfileEvent()
    data class GenderSelected(val gender: Gender) : EditProfileEvent()
    data class RegionSelected(val region: String) : EditProfileEvent()
    data class AvatarSelected(val uri: Uri) : EditProfileEvent()
    data class CoverSelected(val uri: Uri) : EditProfileEvent()
    object SaveClicked : EditProfileEvent()
    object BackClicked : EditProfileEvent()
}
```

### 11.3 Navigation Events
```kotlin
sealed class EditProfileNavigation {
    object NavigateBack : EditProfileNavigation()
    object NavigateToRegionSelection : EditProfileNavigation()
    object NavigateToProfileHistory : EditProfileNavigation()
    object NavigateToCoverHistory : EditProfileNavigation()
}
```

---

## 12. Image Handling

### 12.1 Image Selection
- **Picker**: Use `ActivityResultContracts.PickVisualMedia`
- **Type**: Images only
- **Compression**: Resize to max 1024x1024 before upload
- **Format**: JPEG with 85% quality

### 12.2 Image Upload Flow
```
1. User selects image
2. Show preview immediately (local URI)
3. Compress image in background
4. Upload to Supabase Storage
5. Get public URL
6. Update UI state with URL
7. Save to profile history
```

### 12.3 Image Display
- **Library**: Glide Compose (`GlideImage`)
- **Placeholder**: Show default avatar/cover
- **Error**: Show error icon with retry option
- **Loading**: Shimmer effect or progress indicator

---

## 13. Integration Points

### 13.1 Existing Activities
- **SelectRegionActivity**: Launch for result, receive selected region
- **ProfilePhotoHistoryActivity**: Launch to view/restore photos
- **ProfileCoverPhotoHistoryActivity**: Launch to view/restore covers

### 13.2 Supabase Services
- **AuthenticationService**: Get current user ID
- **DatabaseService**: CRUD operations on `users` table
- **StorageService**: Upload avatar and cover images

### 13.3 Navigation
- **Entry**: From Settings → Edit Profile
- **Exit**: Back to Settings or Profile screen
- **Result**: Broadcast profile updated event

---

## 14. Testing Considerations

### 14.1 Unit Tests
- ViewModel validation logic
- Username availability checking
- Form state management
- Image upload handling

### 14.2 UI Tests
- Field input and validation
- Gender selection
- Image picker integration
- Save button states
- Error display

### 14.3 Integration Tests
- Profile load and save flow
- Image upload and storage
- Navigation between screens

---

## 15. Performance Optimization

### 15.1 Debouncing
- Username validation: 500ms debounce
- Biography counter: No debounce (immediate)

### 15.2 Image Optimization
- Compress before upload
- Cache loaded images
- Use appropriate image sizes

### 15.3 State Management
- Use `StateFlow` for reactive updates
- Avoid unnecessary recompositions
- Use `remember` and `derivedStateOf` appropriately

---

## 16. Migration Strategy

### 16.1 Phase 1: Create Compose UI
- Build `EditProfileScreen.kt` with all components
- Create `EditProfileViewModel.kt` with business logic
- Implement validation and state management

### 16.2 Phase 2: Integrate with Activity
- Update `ProfileEditActivity` to use `setContent`
- Apply `SynapseTheme` with dynamic color support
- Handle activity results (region, images)

### 16.3 Phase 3: Testing & Refinement
- Test all user flows
- Verify accessibility
- Performance profiling
- Bug fixes and polish

### 16.4 Phase 4: Deprecate Old UI
- Remove XML layouts
- Clean up old view binding code
- Update documentation

---

## 17. Code Structure Example

### 17.1 File Organization
```
presentation/editprofile/
├── EditProfileScreen.kt              // Main screen composable
├── EditProfileViewModel.kt           // ViewModel with business logic
├── EditProfileUiState.kt             // State models and events
├── EditProfileRepository.kt          // Data layer abstraction
├── components/
│   ├── ProfileImageSection.kt        // Cover + Avatar component
│   ├── ProfileFormFields.kt          // Username, nickname, bio fields
│   ├── GenderSelector.kt             // Gender selection component
│   ├── RegionSelector.kt             // Region navigation item
│   ├── HistoryNavigationItems.kt    // Photo history links
│   └── ValidationIndicator.kt        // Validation feedback UI
```

### 17.2 Reusable Components (from Settings)
- `SettingsNavigationItem` for region and history
- `SettingsCard` for section containers
- `SettingsSpacing`, `SettingsShapes`, `SettingsColors` for consistency
- `SynapseTheme` for theming

---

## 18. Dependencies Required

### 18.1 Compose Dependencies (Already in project)
```gradle
// Compose BOM
implementation platform('androidx.compose:compose-bom:2024.02.00')
implementation 'androidx.compose.ui:ui'
implementation 'androidx.compose.material3:material3'
implementation 'androidx.compose.ui:ui-tooling-preview'
implementation 'androidx.activity:activity-compose'
implementation 'androidx.lifecycle:lifecycle-viewmodel-compose'

// Glide for Compose
implementation 'com.github.bumptech.glide:compose:1.0.0-beta01'
```

### 18.2 Additional Libraries
```gradle
// Coil for image loading (alternative to Glide)
implementation 'io.coil-kt:coil-compose:2.5.0'

// Accompanist for permissions
implementation 'com.google.accompanist:accompanist-permissions:0.32.0'
```

---

## 19. Success Metrics

### 19.1 User Experience
- Profile update completion rate > 95%
- Average time to update profile < 60 seconds
- Error rate < 5%

### 19.2 Performance
- Screen load time < 500ms
- Image upload time < 3 seconds (on average network)
- Smooth 60fps animations

### 19.3 Accessibility
- TalkBack navigation success rate 100%
- All touch targets meet 48dp minimum
- Color contrast ratios meet WCAG AA standards

---

## 20. Future Enhancements

### 20.1 Potential Features
- Crop and rotate images before upload
- Multiple avatar/cover selection
- Profile preview before save
- Social media link fields
- Profile badges and verification

### 20.2 Advanced Validation
- Username suggestions when taken
- Real-time character count with color coding
- Biography formatting (bold, italic)

### 20.3 Enhanced UX
- Drag-and-drop image upload
- Undo/redo for changes
- Auto-save drafts
- Profile completion percentage

---

## Summary

This specification provides a comprehensive blueprint for redesigning the Edit Profile Activity using Jetpack Compose with Material 3 Expressive components. The design maintains consistency with the Settings feature while following MVVM architecture and modern Android development best practices.

**Key Principles**:
1. **Consistency**: Reuse Settings design patterns and components
2. **Accessibility**: Meet WCAG standards and support assistive technologies
3. **Performance**: Optimize for smooth animations and fast load times
4. **Maintainability**: Clean architecture with separation of concerns
5. **User Experience**: Clear feedback, intuitive interactions, and error handling

**Implementation Priority**:
1. Core UI structure and layout
2. Form validation and state management
3. Image upload functionality
4. Animations and transitions
5. Accessibility enhancements
6. Testing and refinement
