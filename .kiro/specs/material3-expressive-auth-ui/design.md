# Design Document: Material 3 Expressive Authentication UI

## Overview

This design document outlines the comprehensive redesign of the authentication screens (Login, Create Account, and Forgot Password) using Material 3 Expressive design principles. The redesign will transform the existing authentication UI to match the modern, clean aesthetic shown in the reference design, featuring rounded corners, elevated cards, smooth animations, and a cohesive Material 3 Expressive visual language.

The design maintains the existing Supabase authentication backend integration while completely overhauling the visual presentation and user experience to align with Material 3 Expressive guidelines.

## Architecture

### Design System Foundation

The authentication UI will be built on Material 3 Expressive design tokens already established in the codebase:

- **Shape System**: Expressive rounded corners (8dp small, 12dp medium, 16dp large, 24dp extra-large)
- **Color System**: Material 3 dynamic color with green primary (#4C662B) and surface containers
- **Typography**: Product Sans font family with expressive scale (36sp titles, 18sp subtitles, 16sp body)
- **Elevation**: Subtle shadows (1-8dp) for depth and hierarchy
- **Motion**: Smooth transitions (150-300ms) with overshoot interpolators for expressiveness

### Screen Structure

The authentication flow consists of three primary screens, all sharing a common visual language:

1. **Login Screen**: Email + Password with social auth options
2. **Create Account Screen**: Email + Password + Username with social auth options
3. **Forgot Password Screen**: Email input with reset functionality

All screens will use a consistent layout pattern:
- Centered logo with app branding at top
- White/surface-colored card container with 24dp corner radius
- Input fields with 16dp corner radius and Material 3 outlined style
- Primary action button with 16dp corner radius and 64dp height
- Social authentication buttons below primary form
- Smooth transitions between modes

## Components and Interfaces

### 1. Layout Structure

#### Main Container
```xml
CoordinatorLayout (root)
├── ScrollView (enables keyboard scrolling)
│   └── ConstraintLayout (content container)
│       ├── Logo Card (96dp × 96dp, 24dp radius)
│       ├── App Name TextView (36sp, Product Sans Bold)
│       ├── Welcome Message TextView (18sp, Product Sans)
│       ├── Form Card Container (24dp radius, 8dp elevation)
│       │   ├── Email Input (outlined, 16dp radius)
│       │   ├── Password Input (outlined, 16dp radius, toggle icon)
│       │   ├── Password Strength Indicator (sign-up only)
│       │   ├── Username Input (sign-up only, 16dp radius)
│       │   ├── Error Card (16dp radius, error container color)
│       │   ├── Primary Action Button (64dp height, 16dp radius)
│       │   └── Mode Toggle Text (centered, 48dp min touch)
│       ├── Social Auth Divider ("Or Continue With Account")
│       ├── Social Auth Buttons Container
│       │   ├── Google Button (48dp circle)
│       │   ├── Facebook Button (48dp circle)
│       │   └── Apple Button (48dp circle)
│       └── Email Verification Layout (hidden by default)
└── Loading Overlay (blur effect on API 31+)
```

### 2. Input Fields Design

#### Email Input Field
- **Style**: Material 3 Outlined TextInputLayout
- **Corner Radius**: 16dp on all corners
- **Start Icon**: Email icon with primary color tint
- **End Icon**: Dynamic (checkmark for valid, alert for invalid)
- **Stroke Width**: 2dp
- **Stroke Color**: Primary color when focused
- **Height**: 56dp (standard Material 3)
- **Padding**: 16dp vertical
- **Font**: Product Sans, 16sp
- **Real-time Validation**: 300ms debounce with visual feedback

#### Password Input Field
- **Style**: Material 3 Outlined TextInputLayout
- **Corner Radius**: 16dp on all corners
- **Start Icon**: Lock icon with primary color tint
- **End Icon**: Password toggle (eye icon)
- **Stroke Width**: 2dp
- **Stroke Color**: Primary color when focused
- **Height**: 56dp
- **Padding**: 16dp vertical
- **Font**: Product Sans, 16sp
- **Input Type**: textPassword with toggle visibility

#### Username Input Field (Sign-up only)
- **Style**: Material 3 Outlined TextInputLayout
- **Corner Radius**: 16dp on all corners
- **Start Icon**: Person icon with primary color tint
- **Stroke Width**: 2dp
- **Height**: 56dp
- **Padding**: 16dp vertical
- **Font**: Product Sans, 16sp
- **Animation**: Slide down (300ms) when entering sign-up mode

### 3. Password Strength Indicator

Displayed only in sign-up mode below the password field:

```kotlin
// Strength levels with colors
sealed class PasswordStrength(val color: Int, val message: String, val progress: Int) {
    object Weak : PasswordStrength(R.color.password_weak, "Weak password", 33)
    object Fair : PasswordStrength(R.color.password_fair, "Fair password", 66)
    object Strong : PasswordStrength(R.color.password_strong, "Strong password", 100)
}
```

- **Visual**: Linear progress indicator (4dp height, 2dp corner radius)
- **Colors**: Red (#F44336) weak, Orange (#FF9800) fair, Green (#4CAF50) strong
- **Text**: 12sp Product Sans below progress bar
- **Animation**: Fade in (200ms) when password field has content
- **Evaluation**: Based on length (6/8/10+ chars) and complexity (uppercase, lowercase, digits, special chars)

### 4. Primary Action Button

The main CTA button for sign-in/sign-up actions:

- **Dimensions**: Full width, 64dp height
- **Corner Radius**: 16dp
- **Background**: Primary color (#4C662B)
- **Text**: 18sp Product Sans Bold, white color
- **Icon**: Arrow forward (24dp) at end with 8dp padding
- **Ripple**: Primary container color
- **States**:
  - **Normal**: Full opacity, enabled
  - **Loading**: 60% opacity, disabled, no text/icon
  - **Success**: Checkmark icon at start, "Success!" text, green tint, pulse animation
  - **Pressed**: Scale down to 0.95x (100ms)
- **Haptic Feedback**: CONTEXT_CLICK on press, CONFIRM on success

### 5. Social Authentication Buttons

Three circular buttons for third-party authentication:

- **Layout**: Horizontal LinearLayout, center-aligned
- **Spacing**: 16dp between buttons
- **Button Dimensions**: 48dp × 48dp circular
- **Background**: White with 1dp outline
- **Icons**: Brand icons (24dp) centered
- **Elevation**: 2dp
- **Ripple**: Light gray
- **Providers**: Google, Facebook, Apple (in that order)

#### Social Auth Divider
- **Text**: "Or Continue With Account"
- **Style**: 14sp Product Sans, onSurfaceVariant color
- **Visual**: Horizontal lines on both sides of text
- **Spacing**: 24dp margin top/bottom

### 6. Error Display System

Enhanced error handling with field-specific highlighting:

#### Error Card
- **Background**: colorErrorContainer (#FFDAD6)
- **Corner Radius**: 16dp
- **Elevation**: 2dp
- **Padding**: 16dp
- **Layout**: Horizontal with alert icon (24dp) + text
- **Icon Tint**: colorError (#BA1A1A)
- **Text**: 14sp Product Sans, colorOnErrorContainer
- **Animation**: Fade in (200ms) + shake effect
- **Haptic**: REJECT feedback

#### Field-Specific Highlighting
```kotlin
enum class ErrorField {
    EMAIL, PASSWORD, USERNAME, GENERAL
}
```

- **Email Error**: Red alert icon in end position, shake animation
- **Password Error**: Error state on field, shake animation
- **Username Error**: Red alert icon in end position, shake animation
- **General Error**: Only error card shown, no field highlighting

### 7. Forgot Password Screen

A simplified screen for password recovery:

```xml
ConstraintLayout
├── Back Button (top-left, 48dp touch target)
├── Logo Card (96dp × 96dp, 24dp radius)
├── Title TextView ("Forgot Password", 36sp)
├── Subtitle TextView ("Enter your email...", 16sp)
├── Form Card Container (24dp radius)
│   ├── Email Input (outlined, 16dp radius)
│   └── Continue Button (64dp height, 16dp radius)
└── Loading Overlay
```

**Behavior**:
1. User enters email address
2. Real-time validation with visual feedback
3. Tap "Continue" to send reset link
4. Show success message with email confirmation
5. Supabase sends password reset email
6. User redirected back to login screen

### 8. Email Verification Screen

Enhanced verification UI (existing, to be styled consistently):

- **Icon**: Animated email icon (120dp × 120dp)
- **Title**: "Verify Your Email" (22sp Product Sans Bold)
- **Message**: Descriptive text (14sp)
- **Email Display**: Highlighted in primary container card
- **Resend Button**: Tonal button (56dp height, 12dp radius)
- **Cooldown Timer**: Circular progress with countdown (60 seconds)
- **Auto-refresh Indicator**: Small circular progress with status text
- **Back Button**: Text button to return to sign-in

## Data Models

### UI State Management

```kotlin
sealed class AuthUIState {
    object Loading : AuthUIState()
    object SignInForm : AuthUIState()
    object SignUpForm : AuthUIState()
    object ForgotPasswordForm : AuthUIState()
    data class EmailVerificationPending(val email: String) : AuthUIState()
    data class Authenticated(val user: UserInfo) : AuthUIState()
    data class Error(val message: String) : AuthUIState()
}
```

### Form Data

```kotlin
data class AuthFormData(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val isSignUpMode: Boolean = false
)
```

### Validation State

```kotlin
data class ValidationState(
    val isEmailValid: Boolean = false,
    val isPasswordValid: Boolean = false,
    val isUsernameValid: Boolean = false,
    val passwordStrength: PasswordStrength = PasswordStrength.Weak
)
```

## Animations and Transitions

### 1. Screen Entry Animations

**Logo Entrance** (600ms):
- Scale from 0 to 1 with OvershootInterpolator(1.5f)
- Fade from 0 to 1 alpha
- Delay: 100ms

**App Name Fade-in** (400ms):
- Translate Y from -20dp to 0
- Fade from 0 to 1 alpha
- Delay: 400ms

**Welcome Message Fade-in** (400ms):
- Translate Y from -20dp to 0
- Fade from 0 to 1 alpha
- Delay: 600ms

### 2. Mode Toggle Animations

**Sign-in to Sign-up**:
1. Username field slides down (300ms) with fade-in
2. Welcome text crossfades (150ms) to "Create your account"
3. Toggle text crossfades (150ms) to "Already have an account? Sign In"
4. Button text changes to "Create Account"
5. Password strength indicator fades in if password exists

**Sign-up to Sign-in**:
1. Username field slides up (200ms) with fade-out
2. Welcome text crossfades (150ms) to "Welcome back"
3. Toggle text crossfades (150ms) to "Don't have an account? Sign Up"
4. Button text changes to "Sign In"
5. Password strength indicator fades out

### 3. Input Field Interactions

**Focus Animation** (200ms):
- Scale to 1.02x when focused
- Scale back to 1.0x when unfocused
- Stroke color changes to primary

**Validation Success** (100ms):
- Subtle scale pulse (1.01x → 1.0x)
- Checkmark icon appears in end position
- Green tint on icon

**Validation Error** (50ms × 5):
- Shake animation: -5dp → 5dp → -5dp → 5dp → 0dp
- Red alert icon appears
- Error text displays below field

### 4. Button Interactions

**Press Animation** (100ms):
- Scale down to 0.95x on ACTION_DOWN
- Scale back to 1.0x on ACTION_UP
- Haptic feedback (CONTEXT_CLICK)

**Success Animation** (500ms):
- Checkmark icon appears at start
- Text changes to "Success!"
- Green tint applied
- Pulse animation (1.05x → 1.0x)
- Haptic feedback (CONFIRM)
- Screen fades out (300ms) before navigation

### 5. Loading States

**Loading Overlay** (200ms):
- Fade in with 0.8 alpha black background
- Blur effect (25px radius) on API 31+
- Circular progress indicator (64dp)
- Loading message with fade transitions

**Button Loading**:
- Text and icon removed
- Opacity reduced to 60%
- Disabled state

### 6. Error Animations

**Error Card Entrance** (200ms):
- Fade in from 0 to 1 alpha
- Shake animation (loaded from R.anim.shake)
- Haptic feedback (REJECT)

**Field Error Highlight** (50ms × 5):
- Shake animation on specific field
- Error icon appears
- Error text displays

### 7. Verification Screen Transitions

**Main Form to Verification** (300ms):
- Main form fades out and slides up (-50dp)
- Verification layout fades in and slides down (50dp → 0)
- Stagger delay: 100ms
- Email icon animation starts after transition

**Verification to Main Form** (300ms):
- Email icon scales down (0.8x) and fades out
- Verification layout fades out and slides down (50dp)
- Main form fades in and slides up (-50dp → 0)
- Stagger delay: 100ms

## Accessibility Features

### 1. Content Descriptions

All interactive elements have descriptive labels:
- Input fields: "Email address input field", "Password input field", "Username input field"
- Buttons: "Sign in button. Double tap to sign in with your credentials"
- Toggle: "Switch authentication mode. Double tap to toggle between sign in and sign up"
- Social buttons: "Sign in with Google", "Sign in with Facebook", "Sign in with Apple"

### 2. Touch Targets

All interactive elements meet 48dp minimum:
- Buttons: 64dp height (exceeds minimum)
- Social auth buttons: 48dp × 48dp
- Toggle text: 48dp min height with padding
- Input fields: 56dp height (exceeds minimum)

### 3. Reduced Motion Support

```kotlin
private fun shouldReduceMotion(): Boolean {
    return try {
        val animationScale = Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        animationScale == 0f
    } catch (e: Exception) {
        false
    }
}
```

When reduced motion is enabled:
- All animations skip or use 0ms duration
- Transitions are instant
- Focus changes are immediate
- No scale or translation effects

### 4. Screen Reader Support

- State changes announced: "Switched to sign up mode"
- Validation results announced: "Email address is valid", "Password error: too short"
- Error messages announced: "Error: Invalid email or password"
- Loading states announced: "Loading. Please wait..."

### 5. Keyboard Navigation

- IME actions configured: NEXT for email, DONE for password (sign-in), DONE for username (sign-up)
- Auto-focus on first invalid field
- Keyboard shows automatically on error
- Smooth scrolling to focused field with 16dp padding

## Responsive Design

### 1. Form Width Constraints

```xml
app:layout_constraintWidth_max="@dimen/auth_form_max_width"
```

- Mobile: Full width with 24dp horizontal margins
- Tablet: Maximum 480dp width, centered
- Large screens: Maximum 480dp width, centered

### 2. Landscape Layout

Separate layout resource: `layout-land/activity_auth.xml`
- Horizontal arrangement for better space utilization
- Logo and form side-by-side
- Reduced vertical spacing
- Same component styling

### 3. Tablet Layout

Separate layout resource: `layout-sw600dp/activity_auth.xml`
- Increased horizontal margins (48dp)
- Larger logo (120dp × 120dp)
- Wider form container (max 600dp)
- Increased text sizes (40sp title, 20sp subtitle)

### 4. Keyboard Handling

```kotlin
private fun scrollToView(view: View) {
    val scrollView = findScrollViewParent(view)
    scrollView?.let {
        val keyboardPadding = resources.getDimensionPixelSize(R.dimen.auth_keyboard_padding)
        val scrollY = rect.top - keyboardPadding
        scrollView.smoothScrollTo(0, scrollY)
    }
}
```

- ScrollView container ensures all content accessible
- Auto-scroll focused field into view
- 16dp padding above keyboard
- Smooth scrolling animation

## Error Handling

### 1. User-Friendly Error Messages

```kotlin
private fun getUserFriendlyErrorMessage(exception: Exception, context: String): Pair<String, ErrorField>
```

Error mapping:
- Network errors → "Unable to connect. Check your internet connection."
- Invalid credentials → "Email or password incorrect. Please try again."
- Email not verified → "Please verify your email address first."
- Account exists → "Account already exists. Please sign in instead."
- Weak password → "Password too weak. Use at least 6 characters."
- Rate limiting → "Too many attempts. Please wait a few minutes."

### 2. Field-Specific Errors

- Email errors highlight email field with red icon
- Password errors highlight password field
- Username errors highlight username field
- General errors show only error card

### 3. Auto-Focus on Error

First invalid field receives focus automatically:
- Keyboard shows
- Field scrolls into view
- Error message announced to screen readers

## Testing Strategy

### 1. Visual Regression Testing

- Screenshot tests for all authentication screens
- Compare against reference design
- Test light and dark themes
- Test different screen sizes (phone, tablet, landscape)

### 2. Interaction Testing

- Button press animations
- Input field focus states
- Mode toggle transitions
- Error display and dismissal
- Loading states
- Success states

### 3. Accessibility Testing

- TalkBack navigation
- Touch target sizes
- Color contrast ratios
- Reduced motion support
- Keyboard navigation

### 4. Responsive Testing

- Phone portrait (360dp × 640dp)
- Phone landscape (640dp × 360dp)
- Tablet portrait (768dp × 1024dp)
- Tablet landscape (1024dp × 768dp)
- Large screen (1280dp × 800dp)

### 5. Integration Testing

- Supabase authentication flow
- Email verification flow
- Password reset flow
- Social authentication (when implemented)
- Error handling for network failures
- Session management

### 6. Performance Testing

- Animation frame rates (60fps target)
- Layout inflation time
- Blur effect performance (API 31+)
- Memory usage during animations
- Keyboard show/hide performance

## Implementation Notes

### 1. Existing Code Preservation

The current AuthActivity.kt has extensive functionality that must be preserved:
- Supabase authentication integration
- Email verification flow
- Real-time validation with debouncing
- Password strength evaluation
- Error handling with user-friendly messages
- State management with sealed classes
- Accessibility features
- Reduced motion support
- Keyboard handling
- Session cleanup

### 2. Layout Modifications Only

The implementation will focus on:
- Updating XML layouts for Material 3 Expressive styling
- Adding social authentication button UI (OAuth logic separate task)
- Creating forgot password screen layout
- Refining animations and transitions
- Enhancing visual feedback

### 3. New Resources Required

**Drawables**:
- `ic_arrow_forward.xml` - Arrow icon for buttons
- `ic_check_circle.xml` - Success checkmark icon
- `ic_email_animated.xml` - Animated email icon for verification
- `bg_social_button.xml` - Social button background
- `ripple_rounded.xml` - Rounded ripple effect

**Animations**:
- `anim/shake.xml` - Shake animation for errors
- `anim/pulse.xml` - Pulse animation for attention

**Dimensions**:
- Already defined in dimens.xml (auth_form_max_width, auth_horizontal_margin, etc.)

**Colors**:
- Already defined in colors.xml (password strength colors, error colors, etc.)

### 4. Social Authentication Integration

Social auth buttons will be added to the UI, but OAuth implementation is a separate concern:
- UI components created and styled
- Click listeners attached
- OAuth flow implementation deferred to separate task
- Placeholder toast messages for now

### 5. Forgot Password Flow

New screen to be created:
- Separate activity or fragment (to be determined)
- Email input with validation
- Supabase password reset integration
- Success confirmation
- Navigation back to login

## Design Decisions and Rationales

### 1. Why Material 3 Expressive?

- **Modern Aesthetic**: Aligns with latest Android design guidelines
- **Brand Consistency**: Matches the green nature-themed brand identity
- **User Familiarity**: Users expect Material Design on Android
- **Accessibility**: Built-in accessibility features and guidelines
- **Expressiveness**: Rounded corners and bold shapes create friendly, approachable feel

### 2. Why Preserve Existing Logic?

- **Proven Functionality**: Current authentication flow works reliably
- **Comprehensive Features**: Extensive error handling, validation, and accessibility
- **Risk Mitigation**: Avoid introducing bugs in critical authentication flow
- **Efficiency**: Focus effort on visual improvements, not logic rewrites

### 3. Why Separate Forgot Password Screen?

- **Clarity**: Dedicated screen reduces cognitive load
- **Focus**: Single task per screen improves completion rates
- **Flexibility**: Easier to add features like security questions later
- **Best Practice**: Industry standard pattern for password recovery

### 4. Why Social Authentication Buttons?

- **Convenience**: Reduces friction for new users
- **Industry Standard**: Expected feature in modern apps
- **Conversion**: Increases sign-up completion rates
- **User Preference**: Many users prefer social login

### 5. Why Real-time Validation?

- **Immediate Feedback**: Users know instantly if input is valid
- **Error Prevention**: Catches mistakes before submission
- **User Confidence**: Visual confirmation builds trust
- **Reduced Errors**: Fewer failed authentication attempts

### 6. Why Password Strength Indicator?

- **Security**: Encourages stronger passwords
- **Guidance**: Shows users what makes a strong password
- **Transparency**: Users understand password requirements
- **Best Practice**: Industry standard for sign-up flows

## Conclusion

This design provides a comprehensive blueprint for transforming the authentication UI into a modern, Material 3 Expressive experience. The design maintains all existing functionality while dramatically improving visual appeal, user experience, and accessibility. The implementation will be incremental, focusing on layout and styling changes while preserving the robust authentication logic already in place.
