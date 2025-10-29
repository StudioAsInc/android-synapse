# Design Document: Profile Edit XML Styling Refactoring

## Overview

This design document outlines the approach for refactoring the ProfileEditActivity from programmatic styling to XML-based styling. The refactoring will eliminate all GradientDrawable creation in Kotlin code and replace it with reusable XML drawable resources that follow Material Design 3 guidelines and properly support theming.

The current implementation violates Android best practices by:
- Creating drawables programmatically in the Activity code
- Using hardcoded color values (0xFFFFFFFF, 0xFFEEEEEE, etc.)
- Mixing UI styling concerns with business logic
- Making the code harder to maintain and test

The refactored implementation will:
- Define all visual styling in XML drawable resources
- Use theme attributes (?attr/colorSurface, ?attr/colorOutline) for colors
- Support automatic light/dark theme switching
- Reduce code complexity and improve maintainability
- Follow existing project conventions for drawable naming

## Architecture

### Component Structure

```
ProfileEditActivity (Kotlin)
├── Business Logic (validation, data loading, saving)
├── View References (ViewBinding)
└── Event Handlers (click listeners, text watchers)

XML Resources
├── Layout (activity_profile_edit.xml)
│   └── References drawable resources via @drawable/
├── Drawables (res/drawable/)
│   ├── Input field backgrounds (normal and error states)
│   ├── Container backgrounds (rounded with stroke)
│   ├── Profile image backgrounds (circular)
│   └── State list selectors
└── Values (res/values/)
    ├── colors.xml (theme-aware color definitions)
    └── dimens.xml (dimension values for consistency)
```

### Separation of Concerns

**ProfileEditActivity Responsibilities:**
- Initialize views using ViewBinding
- Handle user input validation
- Manage data loading and saving
- Coordinate with Supabase services
- Handle activity lifecycle

**XML Resources Responsibilities:**
- Define all visual appearance (colors, shapes, strokes)
- Handle state changes (normal, error, focused)
- Support theme switching (light/dark mode)
- Provide reusable styling components

## Components and Interfaces

### 1. XML Drawable Resources

#### Input Field Drawables

**bg_profile_edit_input_normal.xml**
- Purpose: Normal state background for input fields
- Shape: Rounded rectangle with 28dp corner radius
- Fill: ?attr/colorSurface (white in light mode, dark in dark mode)
- Stroke: 3dp width, ?attr/colorOutline (light grey)

**bg_profile_edit_input_error.xml**
- Purpose: Error state background for input fields
- Shape: Rounded rectangle with 28dp corner radius
- Fill: ?attr/colorSurface
- Stroke: 3dp width, ?attr/colorError (red)

**bg_profile_edit_input_selector.xml**
- Purpose: State list drawable that switches between normal and error states
- States:
  - `android:state_activated="true"` → error background
  - Default → normal background

#### Container Drawables

**bg_profile_edit_container.xml**
- Purpose: Background for gender, region, and history containers
- Shape: Rounded rectangle with 28dp corner radius
- Fill: ?attr/colorSurface
- Stroke: 3dp width, ?attr/colorOutline

#### Profile Image Drawables

**bg_profile_edit_profile_card.xml**
- Purpose: Background for profile image card
- Shape: Rounded rectangle with 28dp corner radius
- Fill: Transparent
- No stroke

**bg_profile_edit_profile_image.xml**
- Purpose: Background for profile image (circular)
- Shape: Rounded rectangle with 300dp corner radius (creates circle effect)
- Fill: Transparent
- No stroke

### 2. ProfileEditActivity Refactoring

#### Methods to Remove

The following utility methods will be completely removed:
- `stateColor(statusColor: Int, navigationColor: Int)` - Status bar styling should be in theme
- `imageColor(image: ImageView, color: Int)` - Icon tinting should be in XML
- `viewGraphics(view: View, onFocus: Int, onRipple: Int, radius: Double, stroke: Double, strokeColor: Int)` - All styling in XML
- `createGradientDrawable(radius: Int, color: Int): GradientDrawable` - No programmatic drawables
- `createStrokeDrawable(radius: Int, stroke: Int, strokeColor: Int, fillColor: Int): GradientDrawable` - No programmatic drawables

#### Methods to Modify

**initializeLogic()**
- Remove all drawable creation code
- Remove all background assignment code
- Keep only: elevation setting, typeface setting, getUserReference() call
- Estimated reduction: ~15 lines of code

**Error Handling Methods**
- `setUsernameError(error: String)` - Remove background assignment, keep error text
- `clearUsernameError()` - Remove background assignment, keep error clearing
- `setNicknameError(error: String)` - Remove background assignment, keep error text
- `clearNicknameError()` - Remove background assignment, keep error clearing
- `setBiographyError(error: String)` - Remove background assignment, keep error text
- `clearBiographyError()` - Remove background assignment, keep error clearing

**Error State Management Strategy:**
Instead of programmatically changing backgrounds, we'll use the `activated` state:
```kotlin
// Set error state
mUsernameInput.isActivated = true
mUsernameInput.error = errorMessage

// Clear error state
mUsernameInput.isActivated = false
mUsernameInput.error = null
```

The state list drawable will automatically switch between normal and error backgrounds based on the `activated` state.

### 3. Layout File Updates

**activity_profile_edit.xml**

Update the following views to reference new drawable resources:

```xml
<!-- Input fields -->
<com.synapse.social.studioasinc.FadeEditText
    android:id="@+id/mUsernameInput"
    android:background="@drawable/bg_profile_edit_input_selector"
    ... />

<com.synapse.social.studioasinc.FadeEditText
    android:id="@+id/mNicknameInput"
    android:background="@drawable/bg_profile_edit_input_selector"
    ... />

<com.synapse.social.studioasinc.FadeEditText
    android:id="@+id/mBiographyInput"
    android:background="@drawable/bg_profile_edit_input_selector"
    ... />

<!-- Containers -->
<LinearLayout
    android:id="@+id/gender"
    android:background="@drawable/bg_profile_edit_container"
    ... />

<LinearLayout
    android:id="@+id/region"
    android:background="@drawable/bg_profile_edit_container"
    ... />

<LinearLayout
    android:id="@+id/profile_image_history_stage"
    android:background="@drawable/bg_profile_edit_container"
    ... />

<LinearLayout
    android:id="@+id/cover_image_history_stage"
    android:background="@drawable/bg_profile_edit_container"
    ... />

<!-- Profile cards -->
<androidx.cardview.widget.CardView
    android:id="@+id/profileRelativeCard"
    android:background="@drawable/bg_profile_edit_profile_card"
    ... />

<androidx.cardview.widget.CardView
    android:id="@+id/stage1RelativeUpProfileCard"
    android:background="@drawable/bg_profile_edit_profile_image"
    ... />
```

## Data Models

No data model changes are required. This refactoring only affects the presentation layer.

## Error Handling

### Current Approach (Programmatic)
```kotlin
private fun setUsernameError(error: String) {
    mUsernameInput.background = createStrokeDrawable(28, 3, 0xFFF44336.toInt(), 0xFFFFFFFF.toInt())
    mUsernameInput.error = error
}
```

### New Approach (XML-based)
```kotlin
private fun setUsernameError(error: String) {
    mUsernameInput.isActivated = true
    mUsernameInput.error = error
}
```

The state list drawable automatically handles the visual change based on the `activated` state.

### Benefits
- Cleaner code with single responsibility
- Automatic theme support
- Consistent styling across all input fields
- Easier to test and maintain

## Testing Strategy

### Manual Testing Checklist

1. **Visual Appearance**
   - [ ] Input fields display with correct rounded corners (28dp)
   - [ ] Input fields have correct stroke width (3dp) and color
   - [ ] Gender container displays with correct styling
   - [ ] Region container displays with correct styling
   - [ ] Profile image history container displays with correct styling
   - [ ] Cover image history container displays with correct styling
   - [ ] Profile image card is circular
   - [ ] Profile relative card has rounded corners

2. **Error States**
   - [ ] Username field shows red stroke when error occurs
   - [ ] Username field returns to normal stroke when error clears
   - [ ] Nickname field shows red stroke when error occurs
   - [ ] Nickname field returns to normal stroke when error clears
   - [ ] Biography field shows red stroke when error occurs
   - [ ] Biography field returns to normal stroke when error clears

3. **Theme Support**
   - [ ] All colors adapt correctly in light mode
   - [ ] All colors adapt correctly in dark mode
   - [ ] No hardcoded colors are visible
   - [ ] Theme switching works without restart

4. **Functionality**
   - [ ] All existing functionality works as before
   - [ ] Profile saving works correctly
   - [ ] Image upload works correctly
   - [ ] Validation works correctly
   - [ ] Navigation works correctly

### Code Quality Checks

1. **Code Reduction**
   - Verify at least 50 lines of code removed from ProfileEditActivity
   - Confirm no GradientDrawable references remain in the activity

2. **XML Validation**
   - All drawable XML files are well-formed
   - All color references use theme attributes
   - No hardcoded color values in XML

3. **Build Verification**
   - Project builds without errors
   - No lint warnings related to hardcoded colors
   - No deprecated API usage

## Implementation Notes

### Dimension Values

Create or update `res/values/dimens.xml` to include:
```xml
<dimen name="profile_edit_corner_radius">28dp</dimen>
<dimen name="profile_edit_stroke_width">3dp</dimen>
<dimen name="profile_edit_image_corner_radius">300dp</dimen>
```

### Color References

Use existing theme attributes:
- `?attr/colorSurface` - Background fill color
- `?attr/colorOutline` - Normal stroke color
- `?attr/colorError` - Error stroke color
- `?attr/colorOnSurface` - Text color
- `?attr/colorOnSurfaceVariant` - Secondary text color

### Backward Compatibility

This refactoring maintains 100% backward compatibility:
- No API changes
- No behavior changes
- No data model changes
- Only internal implementation changes

### Performance Considerations

**Benefits:**
- XML drawables are cached by the system
- No runtime drawable creation overhead
- Reduced memory allocations
- Faster view inflation

**No Negative Impact:**
- XML parsing happens once during inflation
- Drawable caching prevents repeated parsing
- State changes are handled efficiently by the framework

## Migration Path

1. Create all new XML drawable resources
2. Update layout file to reference new drawables
3. Refactor error handling methods to use `activated` state
4. Remove programmatic styling from `initializeLogic()`
5. Remove utility methods for drawable creation
6. Test all functionality and visual appearance
7. Verify theme switching works correctly
8. Clean up any unused code or imports

## Design Decisions and Rationales

### Decision 1: Use `activated` State for Error Handling
**Rationale:** The `activated` state is semantically appropriate for error states and is well-supported by state list drawables. It's cleaner than using custom view properties or tags.

### Decision 2: Create Specific Drawable Resources Instead of Generic Ones
**Rationale:** While we could reuse existing drawables like `shape_rounded_12dp_stroke.xml`, creating specific `bg_profile_edit_*` drawables provides:
- Clear naming that indicates usage
- Flexibility to adjust styling without affecting other screens
- Better documentation of design intent
- Easier to locate and modify in the future

### Decision 3: Keep CardView Components
**Rationale:** The existing CardView components provide elevation and shadow effects that are part of Material Design. We'll update their backgrounds but keep the CardView wrapper for these visual effects.

### Decision 4: Remove Status Bar Styling from Activity
**Rationale:** Status bar colors should be defined in the app theme, not in individual activities. This ensures consistency across the app and follows Android best practices.

### Decision 5: Use Theme Attributes Instead of Direct Color References
**Rationale:** Theme attributes (?attr/colorSurface) automatically adapt to light/dark themes, while direct color references (@color/white) do not. This ensures proper theme support without additional code.
