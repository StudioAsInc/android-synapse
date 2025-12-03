# Design Document

## Overview

This design document outlines the architecture and implementation approach for redesigning the authentication UI using Jetpack Compose and Material 3 expressive components. The redesign will replace the existing XML-based `AuthActivity` with a modern, fully Compose-based implementation that provides superior user experience, accessibility, and maintainability.

The authentication system will maintain compatibility with the existing Supabase authentication backend while introducing a new UI layer built entirely with Compose. The design follows MVVM architecture with clear separation between UI, business logic, and data layers.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│  ┌────────────────┐  ┌────────────────┐  ┌───────────────┐ │
│  │ AuthScreen     │  │ SignInScreen   │  │ SignUpScreen  │ │
│  │ (Compose)      │  │ (Compose)      │  │ (Compose)     │ │
│  └────────┬───────┘  └────────┬───────┘  └───────┬───────┘ │
│           │                   │                   │          │
│           └───────────────────┴───────────────────┘          │
│                              │                               │
│                   ┌──────────▼──────────┐                    │
│                   │   AuthViewModel     │                    │
│                   │   (StateFlow)       │                    │
│                   └──────────┬──────────┘                    │
└──────────────────────────────┼───────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────┐
│                      Domain Layer                             │
│                   ┌──────────────────┐                        │
│                   │  AuthRepository  │                        │
│                   └────────┬─────────┘                        │
└────────────────────────────┼──────────────────────────────────┘
                             │
┌────────────────────────────▼──────────────────────────────────┐
│                       Data Layer                              │
│  ┌──────────────────────┐  ┌──────────────────────────────┐  │
│  │ SupabaseClient       │  │ SharedPreferences            │  │
│  │ (Singleton)          │  │ (Local Storage)              │  │
│  └──────────────────────┘  └──────────────────────────────┘  │
└───────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

**Presentation Layer (Compose UI)**
- `AuthScreen`: Main composable that hosts the authentication flow with navigation
- `SignInScreen`: Sign-in form with email/password inputs and OAuth options
- `SignUpScreen`: Sign-up form with email/password/username inputs
- `EmailVerificationScreen`: Email verification UI with resend functionality
- `ForgotPasswordScreen`: Password reset request form
- `ResetPasswordScreen`: New password entry form
- `AuthComponents`: Reusable UI components (buttons, text fields, etc.)

**ViewModel Layer**
- `AuthViewModel`: Manages authentication state, handles user actions, coordinates with repository
- State management using `StateFlow` for reactive UI updates
- Input validation logic with debouncing
- Navigation events emission

**Domain Layer**
- `AuthRepository`: Existing repository for authentication operations
- Business logic for sign-in, sign-up, email verification, password reset
- Integration with Supabase Auth

**Data Layer**
- `SupabaseClient`: Existing singleton for Supabase operations
- `SharedPreferences`: Local storage for user preferences and temporary data

## Components and Interfaces

### UI State Models

```kotlin
// Main authentication UI state
sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class SignIn(
        val email: String = "",
        val password: String = "",
        val emailError: String? = null,
        val passwordError: String? = null,
        val generalError: String? = null,
        val isEmailValid: Boolean = false
    ) : AuthUiState()
    
    data class SignUp(
        val email: String = "",
        val password: String = "",
        val username: String = "",
        val emailError: String? = null,
        val passwordError: String? = null,
        val usernameError: String? = null,
        val generalError: String? = null,
        val isEmailValid: Boolean = false,
        val passwordStrength: PasswordStrength = PasswordStrength.Weak
    ) : AuthUiState()
    
    data class EmailVerification(
        val email: String,
        val canResend: Boolean = true,
        val resendCooldownSeconds: Int = 0
    ) : AuthUiState()
    
    data class ForgotPassword(
        val email: String = "",
        val emailError: String? = null,
        val isEmailValid: Boolean = false,
        val emailSent: Boolean = false
    ) : AuthUiState()
    
    data class ResetPassword(
        val password: String = "",
        val confirmPassword: String = "",
        val passwordError: String? = null,
        val confirmPasswordError: String? = null,
        val passwordStrength: PasswordStrength = PasswordStrength.Weak
    ) : AuthUiState()
    
    data class Success(val message: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

// Password strength levels
sealed class PasswordStrength(val progress: Float, val label: String, val color: Color) {
    object Weak : PasswordStrength(0.33f, "Weak", Color.Red)
    object Fair : PasswordStrength(0.66f, "Fair", Color(0xFFFFA500))
    object Strong : PasswordStrength(1.0f, "Strong", Color.Green)
}

// Navigation events
sealed class AuthNavigationEvent {
    object NavigateToMain : AuthNavigationEvent()
    object NavigateToSignIn : AuthNavigationEvent()
    object NavigateToSignUp : AuthNavigationEvent()
    object NavigateToEmailVerification : AuthNavigationEvent()
    object NavigateToForgotPassword : AuthNavigationEvent()
    data class NavigateToResetPassword(val token: String) : AuthNavigationEvent()
    object NavigateBack : AuthNavigationEvent()
}
```

### ViewModel Interface

```kotlin
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    // Navigation events
    private val _navigationEvent = MutableSharedFlow<AuthNavigationEvent>()
    val navigationEvent: SharedFlow<AuthNavigationEvent> = _navigationEvent.asSharedFlow()
    
    // User actions
    fun onSignInClick(email: String, password: String)
    fun onSignUpClick(email: String, password: String, username: String)
    fun onForgotPasswordClick()
    fun onResetPasswordClick(password: String, confirmPassword: String, token: String)
    fun onResendVerificationClick()
    fun onBackToSignInClick()
    fun onToggleModeClick()
    fun onOAuthClick(provider: String)
    
    // Input validation
    fun onEmailChanged(email: String)
    fun onPasswordChanged(password: String)
    fun onUsernameChanged(username: String)
    
    // Internal methods
    private fun validateEmail(email: String): Boolean
    private fun validatePassword(password: String): Boolean
    private fun validateUsername(username: String): Boolean
    private fun calculatePasswordStrength(password: String): PasswordStrength
    private fun startResendCooldown()
    private fun checkEmailVerification(email: String)
}
```

### Composable Components

```kotlin
// Main authentication screen with navigation
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onNavigateToMain: () -> Unit
)

// Sign-in screen
@Composable
fun SignInScreen(
    state: AuthUiState.SignIn,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onToggleModeClick: () -> Unit,
    onOAuthClick: (String) -> Unit
)

// Sign-up screen
@Composable
fun SignUpScreen(
    state: AuthUiState.SignUp,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onToggleModeClick: () -> Unit,
    onOAuthClick: (String) -> Unit
)

// Email verification screen
@Composable
fun EmailVerificationScreen(
    state: AuthUiState.EmailVerification,
    onResendClick: () -> Unit,
    onBackToSignInClick: () -> Unit
)

// Reusable components
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    isValid: Boolean,
    isPassword: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
)

@Composable
fun AuthButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    modifier: Modifier = Modifier
)

@Composable
fun OAuthButton(
    provider: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)

@Composable
fun PasswordStrengthIndicator(
    strength: PasswordStrength,
    modifier: Modifier = Modifier
)
```

## Data Models

### Existing Models (Reused)

The design will reuse existing data models from the current authentication system:

- `UserInfo`: Supabase user information model
- `AuthResult`: Authentication result wrapper
- `AuthConfig`: Authentication configuration

### New Models

```kotlin
// Email validation result
data class EmailValidationResult(
    val isValid: Boolean,
    val error: String? = null
)

// Password validation result
data class PasswordValidationResult(
    val isValid: Boolean,
    val strength: PasswordStrength,
    val error: String? = null
)

// Username validation result
data class UsernameValidationResult(
    val isValid: Boolean,
    val error: String? = null
)

// Form validation result
data class FormValidationResult(
    val isValid: Boolean,
    val errors: Map<String, String> = emptyMap()
)
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Authentication Flow Properties

**Property 1: Valid credentials authenticate successfully**
*For any* valid email and password combination, when authentication is attempted, the system should successfully authenticate and navigate to the main screen
**Validates: Requirements 1.1**

**Property 2: Invalid credentials show error**
*For any* invalid email or password combination, when authentication is attempted, the system should display an error message without navigating
**Validates: Requirements 1.2**

**Property 3: Loading state disables inputs**
*For any* authentication operation in progress, all input controls should be disabled and a loading indicator should be visible
**Validates: Requirements 1.3**

**Property 4: Sign-up creates account**
*For any* valid email, password, and username combination, when sign-up is attempted, the system should create an account and navigate to email verification
**Validates: Requirements 2.1, 2.4**

**Property 5: Password strength is calculated correctly**
*For any* password string, the system should calculate and display password strength feedback with appropriate visual indicators
**Validates: Requirements 2.2**

**Property 6: Form validation shows field-specific errors**
*For any* form submission with invalid fields, the system should display error messages specific to each invalid field
**Validates: Requirements 2.3**

**Property 7: Username validation enforces minimum length**
*For any* username string, the system should validate that it meets the minimum length requirement of 3 characters
**Validates: Requirements 2.5**

### Email Verification Properties

**Property 8: Verification screen shows correct email**
*For any* completed sign-up, the email verification screen should display the registered email address
**Validates: Requirements 3.1**

**Property 9: Resend enforces cooldown**
*For any* resend verification request, the system should send the email and enforce a 60-second cooldown before allowing another resend
**Validates: Requirements 3.2**

**Property 10: Cooldown displays countdown**
*For any* active cooldown period, the system should display a countdown timer with circular progress indicator
**Validates: Requirements 3.3**

**Property 11: Verification triggers auto sign-in**
*For any* email verification completion, the system should automatically detect verification and sign in the user
**Validates: Requirements 3.4**

### Password Reset Properties

**Property 12: Reset email is sent**
*For any* valid email address in password reset flow, the system should send a password reset email
**Validates: Requirements 4.2**

**Property 13: Reset confirmation is displayed**
*For any* sent password reset email, the system should display a confirmation message with instructions
**Validates: Requirements 4.3**

**Property 14: New password triggers navigation**
*For any* successfully set new password, the system should navigate to sign-in screen with success message
**Validates: Requirements 4.5**

### Validation Properties

**Property 15: Email validation provides feedback**
*For any* email input, the system should validate format and display visual feedback (checkmark for valid, error for invalid)
**Validates: Requirements 5.1, 5.2, 5.3**

**Property 16: Password strength indicator is shown**
*For any* password input during sign-up, the system should display a password strength indicator with color-coded progress bar
**Validates: Requirements 5.4**

**Property 17: Validation announces to screen readers**
*For any* validation state change, the system should announce the change to screen readers for accessibility
**Validates: Requirements 5.5**

### Responsive Layout Properties

**Property 18: Screen density scales elements**
*For any* screen density configuration, all UI elements should scale proportionally
**Validates: Requirements 6.4**

**Property 19: Keyboard scrolls focused field**
*For any* keyboard appearance, the focused input field should scroll into view with appropriate padding
**Validates: Requirements 6.5**

### Animation Properties

**Property 20: Animations use correct easing**
*For any* triggered animation, the system should use Material 3 motion specifications with appropriate easing curves
**Validates: Requirements 7.5**

### Accessibility Properties

**Property 21: Interactive elements have content descriptions**
*For any* interactive element, the system should provide descriptive content descriptions for screen readers
**Validates: Requirements 8.1**

**Property 22: Errors announce to screen readers**
*For any* validation error, the system should announce the error message to screen readers
**Validates: Requirements 8.2**

**Property 23: Reduced motion disables animations**
*For any* user with reduced motion enabled, the system should disable or reduce all decorative animations
**Validates: Requirements 8.3, 9.5**

**Property 24: Touch targets meet minimum size**
*For any* interactive element, the system should ensure minimum touch target size of 48dp
**Validates: Requirements 8.4**

**Property 25: Text meets contrast requirements**
*For any* displayed text, the system should ensure minimum contrast ratio of 4.5:1 for normal text and 3:1 for large text
**Validates: Requirements 8.5**

### UI State Properties

**Property 26: Loading shows blur effect**
*For any* active loading state, the system should display a blur effect on background content
**Validates: Requirements 9.2**

**Property 27: Success shows animation**
*For any* successful authentication, the system should display a success animation with scale pulse effect
**Validates: Requirements 9.4**

### OAuth Properties

**Property 28: OAuth buttons trigger placeholder**
*For any* OAuth button tap, the system should provide haptic feedback and display a placeholder message
**Validates: Requirements 11.2**

### Input Interaction Properties

**Property 29: Focus highlights field**
*For any* focused input field, the system should apply visual highlighting to the field border
**Validates: Requirements 12.1**

**Property 30: Typing clears errors**
*For any* input field with an error, typing should clear the error message
**Validates: Requirements 12.2**

**Property 31: Blur triggers validation**
*For any* input field losing focus, the system should validate the input and display appropriate indicators
**Validates: Requirements 12.3**

**Property 32: Validation shows indicators**
*For any* completed validation, the system should display appropriate success or error indicators
**Validates: Requirements 12.4**

## Error Handling

### Error Categories

1. **Network Errors**: Connection failures, timeouts, server errors
2. **Validation Errors**: Invalid email format, weak password, short username
3. **Authentication Errors**: Invalid credentials, account not found, email not verified
4. **Rate Limiting**: Too many attempts, cooldown active
5. **System Errors**: Unexpected exceptions, configuration issues

### Error Display Strategy

- **Field-level errors**: Display inline below the relevant input field
- **Form-level errors**: Display in a card at the top of the form
- **Toast messages**: Use for transient feedback (success, info)
- **Dialog messages**: Use for critical errors requiring acknowledgment

### Error Recovery

- Provide clear actionable messages
- Offer retry mechanisms for network errors
- Suggest corrections for validation errors
- Link to help/support for persistent issues

## Testing Strategy

### Unit Testing

Unit tests will verify specific behaviors and edge cases:

- Email validation logic with various formats
- Password strength calculation with different inputs
- Username validation with boundary cases
- Form validation with mixed valid/invalid inputs
- State transitions in ViewModel
- Error handling for different failure scenarios

### Property-Based Testing

Property-based tests will verify universal properties across many inputs using **Kotest Property Testing** library:

- Generate random valid/invalid credentials and verify authentication behavior
- Generate random passwords and verify strength calculation consistency
- Generate random form inputs and verify validation rules
- Generate random email addresses and verify format validation
- Test state management with random action sequences

**Configuration**: Each property-based test will run a minimum of 100 iterations to ensure thorough coverage.

### UI Testing

Compose UI tests will verify:

- Screen composition and layout
- User interactions (clicks, text input)
- Navigation flows
- Accessibility features (content descriptions, semantics)
- Responsive layouts for different screen sizes

### Integration Testing

Integration tests will verify:

- End-to-end authentication flows
- Supabase integration
- SharedPreferences persistence
- Navigation between screens

### Accessibility Testing

- TalkBack compatibility testing
- Contrast ratio verification
- Touch target size verification
- Reduced motion behavior
- Keyboard navigation

## Implementation Notes

### Material 3 Components

The implementation will use the following Material 3 expressive components:

- `TextField` with `OutlinedTextFieldDefaults` for input fields
- `Button` with `FilledTonalButton` style for primary actions
- `OutlinedButton` for secondary actions
- `LinearProgressIndicator` for password strength
- `CircularProgressIndicator` for loading states
- `Card` with elevated style for error messages
- `Surface` for background containers
- `Icon` with Material Icons for visual feedback

### Dynamic Theming

```kotlin
@Composable
fun AuthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### Animations

All animations will respect the system's reduced motion setting:

```kotlin
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        val animationScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        animationScale == 0f
    }
}

@Composable
fun animatedScale(enabled: Boolean): Float {
    val reducedMotion = rememberReducedMotion()
    return if (reducedMotion) 1f else {
        animateFloatAsState(
            targetValue = if (enabled) 1f else 0.95f,
            animationSpec = tween(durationMillis = 100)
        ).value
    }
}
```

### Adaptive Layouts

```kotlin
@Composable
fun AuthScreen() {
    val windowSizeClass = calculateWindowSizeClass()
    
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            // Phone portrait: Single column
            SingleColumnLayout()
        }
        WindowWidthSizeClass.Medium,
        WindowWidthSizeClass.Expanded -> {
            // Tablet or landscape: Two column
            TwoColumnLayout()
        }
    }
}
```

### Accessibility

All composables will include proper semantics:

```kotlin
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    isValid: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        modifier = Modifier
            .semantics {
                contentDescription = "$label input field"
                if (error != null) {
                    error(error)
                }
                if (isValid) {
                    stateDescription = "Valid"
                }
            }
    )
}
```

## Migration Strategy

### Phase 1: Create Compose UI (Parallel)
- Build new Compose screens alongside existing XML
- Implement ViewModels with StateFlow
- Create reusable Compose components
- No changes to existing AuthActivity

### Phase 2: Integration
- Create new `AuthComposeActivity` using Compose
- Wire up navigation and ViewModel
- Test all flows in new activity
- Keep old AuthActivity as fallback

### Phase 3: Gradual Rollout
- Feature flag to switch between old and new UI
- Monitor crash reports and user feedback
- Gradually increase rollout percentage

### Phase 4: Cleanup
- Remove old AuthActivity and XML layouts
- Remove feature flag
- Update documentation

## Dependencies

### New Dependencies Required

```gradle
// Jetpack Compose
implementation "androidx.compose.ui:ui:1.5.4"
implementation "androidx.compose.material3:material3:1.1.2"
implementation "androidx.compose.ui:ui-tooling-preview:1.5.4"
implementation "androidx.activity:activity-compose:1.8.1"
implementation "androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2"
implementation "androidx.navigation:navigation-compose:2.7.5"

// Accompanist for system UI
implementation "com.google.accompanist:accompanist-systemuicontroller:0.32.0"

// Kotest for property-based testing
testImplementation "io.kotest:kotest-runner-junit5:5.8.0"
testImplementation "io.kotest:kotest-assertions-core:5.8.0"
testImplementation "io.kotest:kotest-property:5.8.0"

// Compose UI testing
androidTestImplementation "androidx.compose.ui:ui-test-junit4:1.5.4"
debugImplementation "androidx.compose.ui:ui-tooling:1.5.4"
debugImplementation "androidx.compose.ui:ui-test-manifest:1.5.4"
```

### Existing Dependencies (Reused)

- Supabase Kotlin SDK
- Kotlin Coroutines
- AndroidX Lifecycle
- SharedPreferences

## Performance Considerations

### Compose Performance

- Use `remember` and `derivedStateOf` to avoid unnecessary recompositions
- Implement `key` for list items to optimize recomposition
- Use `LaunchedEffect` for side effects with proper keys
- Avoid creating new lambdas in composition

### State Management

- Use `StateFlow` instead of `LiveData` for better Compose integration
- Collect state with `collectAsStateWithLifecycle()` to respect lifecycle
- Debounce input validation to reduce computation

### Memory

- Clear sensitive data (passwords) from memory after use
- Properly cancel coroutines in ViewModel onCleared()
- Use weak references for callbacks if needed

## Security Considerations

### Input Sanitization

- Validate and sanitize all user inputs
- Prevent SQL injection through parameterized queries (handled by Supabase)
- Escape special characters in error messages

### Credential Handling

- Never log passwords or sensitive data
- Clear password fields after authentication
- Use secure storage for tokens (handled by Supabase SDK)

### Network Security

- Use HTTPS for all API calls (enforced by Supabase)
- Implement certificate pinning if required
- Handle network errors gracefully without exposing internals

## Monitoring and Analytics

### Events to Track

- Sign-in attempts (success/failure)
- Sign-up attempts (success/failure)
- Email verification resends
- Password reset requests
- OAuth button clicks
- Form validation errors
- Navigation events

### Performance Metrics

- Screen load times
- Authentication response times
- UI responsiveness (frame drops)
- Crash rates by screen

## Future Enhancements

### OAuth Implementation

- Google Sign-In integration
- Facebook Login integration
- Apple Sign In integration
- Social account linking

### Biometric Authentication

- Fingerprint authentication
- Face recognition
- Biometric prompt integration

### Advanced Features

- Remember me functionality
- Multi-factor authentication (MFA)
- Passwordless authentication (magic links)
- Social profile import
