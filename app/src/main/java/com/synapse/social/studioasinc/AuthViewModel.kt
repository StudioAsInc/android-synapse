package com.synapse.social.studioasinc

import android.app.Application
import android.content.Context
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Password strength levels with associated colors and messages
 */
sealed class PasswordStrength(val color: Int, val message: String, val progress: Int) {
    object Weak : PasswordStrength(R.color.password_weak, "Weak password", 33)
    object Fair : PasswordStrength(R.color.password_fair, "Fair password", 66)
    object Strong : PasswordStrength(R.color.password_strong, "Strong password", 100)
}

/**
 * Error field enum for field-specific error handling
 */
enum class ErrorField {
    EMAIL, PASSWORD, USERNAME, GENERAL
}

/**
 * UI State for Auth Screen
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val isSignUpMode: Boolean = false,
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val usernameError: String? = null,
    val isEmailValid: Boolean = false,
    val passwordStrength: PasswordStrength = PasswordStrength.Weak,
    val showPasswordStrength: Boolean = false,
    val error: String? = null, // General error
    val errorField: ErrorField = ErrorField.GENERAL,
    val verificationEmail: String? = null, // If not null, show verification screen
    val resendCooldown: Int = 0,
    val isResendCooldownActive: Boolean = false,
    val isAutoCheckingVerification: Boolean = false
)

sealed class AuthEvent {
    data class NavigateToMain(val user: UserInfo) : AuthEvent()
    data class NavigateToCompleteProfile(val user: UserInfo) : AuthEvent()
    data class NavigateToEmailVerification(val email: String, val password: String) : AuthEvent()
    data class ShowToast(val message: String) : AuthEvent()
    object StartSuccessAnimation : AuthEvent()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    private val sharedPreferences = application.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private var emailValidationJob: Job? = null
    private var passwordValidationJob: Job? = null
    private var resendJob: Job? = null
    private var verificationCheckJob: Job? = null

    init {
        // Check for pending verification email
        val pendingEmail = getSavedEmailForResend()
        if (!pendingEmail.isNullOrEmpty()) {
            _uiState.update { it.copy(verificationEmail = pendingEmail) }
            startVerificationChecking(pendingEmail)
        } else {
            checkCurrentUser()
        }
    }

    private fun checkCurrentUser() {
        if (!SupabaseClient.isConfigured()) {
            _uiState.update { it.copy(error = "Supabase not configured. Please set up your credentials in gradle.properties", errorField = ErrorField.GENERAL) }
            return
        }

        viewModelScope.launch {
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser != null && currentUser.id.isNotEmpty()) {
                    _events.emit(AuthEvent.NavigateToMain(currentUser))
                }
            } catch (e: Exception) {
                // Not authenticated, stay on login
            }
        }
    }

    fun toggleMode() {
        _uiState.update {
            it.copy(
                isSignUpMode = !it.isSignUpMode,
                error = null,
                emailError = null,
                passwordError = null,
                usernameError = null
            )
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, error = null) }

        emailValidationJob?.cancel()
        emailValidationJob = viewModelScope.launch {
            delay(300) // Debounce
            validateEmail(email)
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, error = null) }

        passwordValidationJob?.cancel()
        passwordValidationJob = viewModelScope.launch {
            delay(300) // Debounce
            if (_uiState.value.isSignUpMode) {
                validatePasswordStrength(password)
            }
        }
    }

    fun onUsernameChanged(username: String) {
        _uiState.update { it.copy(username = username, usernameError = null, error = null) }
    }

    private fun validateEmail(email: String) {
        if (email.isEmpty()) return

        val isValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
        _uiState.update {
            it.copy(
                isEmailValid = isValid,
                emailError = if (isValid) null else "Please enter a valid email address"
            )
        }
    }

    private fun validatePasswordStrength(password: String) {
        if (password.isEmpty()) {
            _uiState.update { it.copy(showPasswordStrength = false) }
            return
        }

        val strength = evaluatePasswordStrength(password)
        _uiState.update {
            it.copy(
                showPasswordStrength = true,
                passwordStrength = strength
            )
        }
    }

    private fun evaluatePasswordStrength(password: String): PasswordStrength {
        val length = password.length
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        val complexityScore = listOf(hasUpperCase, hasLowerCase, hasDigit, hasSpecialChar).count { it }

        return when {
            length < 6 -> PasswordStrength.Weak
            length < 8 && complexityScore < 2 -> PasswordStrength.Weak
            length < 10 && complexityScore < 3 -> PasswordStrength.Fair
            length >= 10 && complexityScore >= 3 -> PasswordStrength.Strong
            length >= 8 && complexityScore >= 2 -> PasswordStrength.Fair
            else -> PasswordStrength.Weak
        }
    }

    fun signIn() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password.trim()

        if (!validateInput(email, password)) return

        _uiState.update { it.copy(isLoading = true, loadingMessage = "Signing you in...") }

        viewModelScope.launch {
            try {
                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser?.emailConfirmedAt != null) {
                    clearSavedEmail()
                    _events.emit(AuthEvent.StartSuccessAnimation)
                    delay(500) // Wait for animation
                    _events.emit(AuthEvent.NavigateToMain(currentUser))
                } else {
                    _events.emit(AuthEvent.ShowToast("Please verify your email address to continue"))
                    _events.emit(AuthEvent.NavigateToEmailVerification(email, password))
                }
            } catch (e: Exception) {
                handleAuthError(e, "signin")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun signUp() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password.trim()
        val username = _uiState.value.username.trim()

        if (!validateInput(email, password, username)) return

        _uiState.update { it.copy(isLoading = true, loadingMessage = "Creating your account...") }

        viewModelScope.launch {
            try {
                SupabaseClient.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                // If we got here, sign up was successful (or at least sent the email)

                if (currentUser?.emailConfirmedAt != null) {
                    _events.emit(AuthEvent.ShowToast("✅ Account created successfully!"))
                    clearSavedEmail()
                    _events.emit(AuthEvent.NavigateToCompleteProfile(currentUser))
                } else {
                     _events.emit(AuthEvent.ShowToast("✅ Account created successfully! Please check your email."))
                     // Usually for email auth, it requires verification
                     _events.emit(AuthEvent.NavigateToEmailVerification(email, password))
                }

            } catch (e: Exception) {
                 // Check if it's the "email not confirmed" case which is actually a success for signup flow usually?
                 // No, standard Supabase signUp returns user with null emailConfirmedAt
                 // But if error says "email not confirmed", it might be login attempt?
                 // Let's stick to existing logic
                 if (e.message?.contains("email not confirmed", ignoreCase = true) == true) {
                      _events.emit(AuthEvent.ShowToast("✅ Account created! Please check your email."))
                      _events.emit(AuthEvent.NavigateToEmailVerification(email, password))
                 } else {
                      handleAuthError(e, "signup")
                 }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun handleAuthError(e: Exception, context: String) {
        // Clean up session
        viewModelScope.launch {
            try { SupabaseClient.client.auth.signOut() } catch (_: Exception) {}
        }

        val (errorMessage, errorField) = getUserFriendlyErrorMessage(e, context)
        _uiState.update {
            it.copy(
                error = errorMessage,
                errorField = errorField
            )
        }
    }

    private fun validateInput(email: String, password: String, username: String? = null): Boolean {
        var isValid = true
        var firstErrorField: ErrorField? = null
        var firstErrorMessage: String? = null

        if (email.isEmpty()) {
            _uiState.update { it.copy(emailError = "Email is required") }
            firstErrorField = ErrorField.EMAIL
            firstErrorMessage = "Please enter your email address."
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(emailError = "Please enter a valid email address") }
            firstErrorField = ErrorField.EMAIL
            firstErrorMessage = "Please enter a valid email address."
            isValid = false
        }

        if (password.isEmpty()) {
            _uiState.update { it.copy(passwordError = "Password is required") }
            if (firstErrorField == null) {
                firstErrorField = ErrorField.PASSWORD
                firstErrorMessage = "Please enter your password."
            }
            isValid = false
        } else if (password.length < 6) {
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            if (firstErrorField == null) {
                firstErrorField = ErrorField.PASSWORD
                firstErrorMessage = "Password too short."
            }
            isValid = false
        }

        if (_uiState.value.isSignUpMode) {
             if (username.isNullOrEmpty()) {
                _uiState.update { it.copy(usernameError = "Username is required") }
                if (firstErrorField == null) {
                    firstErrorField = ErrorField.USERNAME
                    firstErrorMessage = "Please choose a username."
                }
                isValid = false
             } else if (username.length < 3) {
                _uiState.update { it.copy(usernameError = "Username must be at least 3 characters") }
                if (firstErrorField == null) {
                    firstErrorField = ErrorField.USERNAME
                    firstErrorMessage = "Username too short."
                }
                isValid = false
             }
        }

        if (!isValid && firstErrorField != null && firstErrorMessage != null) {
            _uiState.update { it.copy(error = firstErrorMessage, errorField = firstErrorField!!) }
        }

        return isValid
    }

    private fun getUserFriendlyErrorMessage(exception: Exception, context: String): Pair<String, ErrorField> {
        val message = exception.message?.lowercase() ?: ""
        return when {
            message.contains("network") || message.contains("connection") ->
                Pair("Unable to connect. Check internet.", ErrorField.GENERAL)
            message.contains("invalid login credentials") ->
                Pair("Incorrect email or password.", ErrorField.EMAIL)
            message.contains("user already registered") ->
                Pair("Account already exists. Sign in instead.", ErrorField.EMAIL)
             // ... Add more mappings as needed ...
            else -> Pair(exception.message ?: "An error occurred.", ErrorField.GENERAL)
        }
    }

    fun resendVerificationEmail() {
        val email = _uiState.value.verificationEmail ?: return

        _uiState.update { it.copy(resendCooldown = 60, isResendCooldownActive = true) }

        // Start cooldown
        resendJob?.cancel()
        resendJob = viewModelScope.launch {
            // SupabaseClient.client.auth.resend(email = email) // Uncomment when implemented
            _events.emit(AuthEvent.ShowToast("Verification email sent!"))

            for (i in 60 downTo 0) {
                _uiState.update { it.copy(resendCooldown = i) }
                delay(1000)
            }
            _uiState.update { it.copy(isResendCooldownActive = false) }
        }

        startVerificationChecking(email)
    }

    fun startVerificationChecking(email: String) {
        verificationCheckJob?.cancel()
        verificationCheckJob = viewModelScope.launch {
            _uiState.update { it.copy(isAutoCheckingVerification = true) }
            val password = _uiState.value.password // Ideally we don't store password but for auto-login we need it

            repeat(20) {
                 delay(30000)
                 try {
                     SupabaseClient.client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }
                    val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                    if (currentUser?.emailConfirmedAt != null) {
                        clearSavedEmail()
                        _uiState.update { it.copy(verificationEmail = null) } // Close verification screen
                        _events.emit(AuthEvent.ShowToast("Email verified!"))
                        _events.emit(AuthEvent.NavigateToMain(currentUser))
                        return@launch
                    }
                 } catch (e: Exception) {
                     // ignore
                 }
            }
            _uiState.update { it.copy(isAutoCheckingVerification = false) }
        }
    }

    fun cancelVerification() {
        clearSavedEmail()
        _uiState.update { it.copy(verificationEmail = null) }
        verificationCheckJob?.cancel()
    }

    private fun saveEmailForResend(email: String) {
        sharedPreferences.edit().putString("pending_verification_email", email).apply()
    }

    private fun getSavedEmailForResend(): String? {
        return sharedPreferences.getString("pending_verification_email", null)
    }

    private fun clearSavedEmail() {
        sharedPreferences.edit().remove("pending_verification_email").apply()
    }
}
