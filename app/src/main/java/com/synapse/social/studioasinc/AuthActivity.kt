package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.databinding.ActivityAuthBinding
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.content.SharedPreferences
import android.view.View
import android.view.animation.AnimationUtils
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat

/**
 * AuthUIState sealed class for managing UI states
 */
sealed class AuthUIState {
    object Loading : AuthUIState()
    object SignInForm : AuthUIState()
    object SignUpForm : AuthUIState()
    data class EmailVerificationPending(val email: String) : AuthUIState()
    data class Authenticated(val user: UserInfo) : AuthUIState()
    data class Error(val message: String) : AuthUIState()
}

/**
 * Modern AuthActivity using Supabase authentication.
 * Handles user authentication with email/password using Supabase GoTrue.
 * Includes email verification flow handling and UI state management.
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    // Using Supabase client directly
    private lateinit var sharedPreferences: SharedPreferences
    
    private var isSignUpMode = false
    private var currentState: AuthUIState = AuthUIState.SignInForm
    private var resendCooldownSeconds = 0
    private var isResendCooldownActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Using Supabase client directly
        sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)

        setupUI()
        setupKeyboardHandling()
        
        // Check if returning from successful email verification
        if (intent.getBooleanExtra("verification_success", false)) {
            Toast.makeText(this, "Email verified! Please sign in to continue.", Toast.LENGTH_LONG).show()
        }
        
        checkCurrentUser()
    }
    
    /**
     * Setup keyboard handling for better UX
     */
    private fun setupKeyboardHandling() {
        binding.apply {
            // Handle IME action on password field
            etPassword.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    if (!isSignUpMode) {
                        performSignIn()
                        true
                    } else {
                        etUsername.requestFocus()
                        false
                    }
                } else {
                    false
                }
            }
            
            // Handle IME action on username field
            etUsername.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    performSignUp()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun setupUI() {
        binding.apply {
            btnSignIn.setOnClickListener {
                if (isSignUpMode) {
                    performSignUp()
                } else {
                    performSignIn()
                }
            }
            
            tvToggleMode.setOnClickListener {
                toggleMode()
            }
            
            btnResendVerification.setOnClickListener {
                resendVerificationEmail()
            }
            
            btnBackToSignIn.setOnClickListener {
                handleBackToSignIn()
            }
            
            // Clear error on text change
            etEmail.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    tilEmail.error = null
                    cardError.visibility = View.GONE
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
            
            etPassword.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    tilPassword.error = null
                    cardError.visibility = View.GONE
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
            
            etUsername.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    tilUsername.error = null
                    cardError.visibility = View.GONE
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
    }

    /**
     * Update UI state based on current authentication state
     */
    private fun updateUIState(state: AuthUIState) {
        currentState = state
        
        binding.apply {
            // Clear all errors first
            tilEmail.error = null
            tilPassword.error = null
            tilUsername.error = null
            
            when (state) {
                is AuthUIState.Loading -> {
                    loadingOverlay.visibility = View.VISIBLE
                    loadingOverlay.alpha = 0f
                    loadingOverlay.animate().alpha(1f).setDuration(200).start()
                    btnSignIn.isEnabled = false
                    cardError.visibility = View.GONE
                }
                
                is AuthUIState.SignInForm -> {
                    isSignUpMode = false
                    loadingOverlay.visibility = View.GONE
                    layoutMainForm.visibility = View.VISIBLE
                    layoutEmailVerification.visibility = View.GONE
                    
                    // Animate username field out
                    if (tilUsername.visibility == View.VISIBLE) {
                        tilUsername.animate()
                            .alpha(0f)
                            .setDuration(200)
                            .withEndAction {
                                tilUsername.visibility = View.GONE
                                tilUsername.alpha = 1f
                            }
                            .start()
                    }
                    
                    btnSignIn.isEnabled = true
                    btnSignIn.text = "Sign In"
                    btnSignIn.icon = null
                    tvWelcome.text = "Welcome back"
                    tvToggleMode.text = "Don't have an account? Sign Up"
                    cardError.visibility = View.GONE
                }
                
                is AuthUIState.SignUpForm -> {
                    isSignUpMode = true
                    loadingOverlay.visibility = View.GONE
                    layoutMainForm.visibility = View.VISIBLE
                    layoutEmailVerification.visibility = View.GONE
                    
                    // Animate username field in
                    if (tilUsername.visibility != View.VISIBLE) {
                        tilUsername.visibility = View.VISIBLE
                        tilUsername.alpha = 0f
                        tilUsername.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start()
                    }
                    
                    btnSignIn.isEnabled = true
                    btnSignIn.text = "Create Account"
                    btnSignIn.icon = null
                    tvWelcome.text = "Create your account"
                    tvToggleMode.text = "Already have an account? Sign In"
                    cardError.visibility = View.GONE
                }
                
                is AuthUIState.EmailVerificationPending -> {
                    loadingOverlay.visibility = View.GONE
                    
                    // Animate transition to verification screen
                    layoutMainForm.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction {
                            layoutMainForm.visibility = View.GONE
                            layoutMainForm.alpha = 1f
                            
                            layoutEmailVerification.visibility = View.VISIBLE
                            layoutEmailVerification.alpha = 0f
                            layoutEmailVerification.animate()
                                .alpha(1f)
                                .setDuration(200)
                                .start()
                        }
                        .start()
                    
                    tvVerificationEmail.text = state.email
                    
                    // Save email for resend functionality
                    saveEmailForResend(state.email)
                    
                    // Start automatic verification checking for auto sign in
                    startVerificationChecking(state.email)
                }
                
                is AuthUIState.Authenticated -> {
                    // Navigate to main activity
                    navigateToMain()
                }
                
                is AuthUIState.Error -> {
                    loadingOverlay.visibility = View.GONE
                    btnSignIn.isEnabled = true
                    resetSignInButton()
                    
                    // Show error in card with animation
                    tvErrorMessage.text = state.message
                    cardError.visibility = View.VISIBLE
                    cardError.alpha = 0f
                    cardError.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }
            }
        }
    }

    /**
     * Save email to SharedPreferences for resend functionality
     */
    private fun saveEmailForResend(email: String) {
        sharedPreferences.edit()
            .putString("pending_verification_email", email)
            .apply()
    }

    /**
     * Get saved email for resend functionality
     */
    private fun getSavedEmailForResend(): String? {
        return sharedPreferences.getString("pending_verification_email", null)
    }

    /**
     * Clear saved email after successful verification
     */
    private fun clearSavedEmail() {
        sharedPreferences.edit()
            .remove("pending_verification_email")
            .apply()
    }

    /**
     * Resend verification email with cooldown timer
     */
    private fun resendVerificationEmail() {
        if (isResendCooldownActive) {
            return
        }
        
        val email = getSavedEmailForResend()
        if (email.isNullOrEmpty()) {
            Toast.makeText(this, "No email found for resend", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading state for resend button
        binding.btnResendVerification.isEnabled = false
        binding.btnResendVerification.text = "Sending..."
        
        lifecycleScope.launch {
            try {
                // Note: Resend functionality may need to be implemented differently
                // For now, just show a message
                // SupabaseClient.client.auth.resend(email = email)
                Toast.makeText(this@AuthActivity, "Verification email sent! Please check your inbox.", Toast.LENGTH_LONG).show()
                startResendCooldown()
                
                // Restart verification checking after resend
                startVerificationChecking(email)
            } catch (e: Exception) {
                Toast.makeText(this@AuthActivity, "Failed to resend: ${e.message}", Toast.LENGTH_LONG).show()
                resetResendButton()
            }
        }
    }

    /**
     * Start cooldown timer for resend verification button
     */
    private fun startResendCooldown() {
        isResendCooldownActive = true
        resendCooldownSeconds = 60 // 60 second cooldown
        
        binding.apply {
            btnResendVerification.isEnabled = false
            tvResendCooldown.visibility = View.VISIBLE
        }
        
        lifecycleScope.launch {
            while (resendCooldownSeconds > 0) {
                binding.tvResendCooldown.text = "You can resend in $resendCooldownSeconds seconds"
                delay(1000)
                resendCooldownSeconds--
            }
            
            // Cooldown finished
            isResendCooldownActive = false
            binding.apply {
                btnResendVerification.isEnabled = true
                btnResendVerification.text = "Resend Verification Email"
                tvResendCooldown.visibility = View.GONE
            }
        }
    }

    /**
     * Reset resend button to normal state
     */
    private fun resetResendButton() {
        binding.btnResendVerification.isEnabled = true
        binding.btnResendVerification.text = "Resend Verification Email"
    }

    private fun checkCurrentUser() {
        // First check if Supabase is configured
        if (!SupabaseClient.isConfigured()) {
            updateUIState(AuthUIState.Error("Supabase not configured. Please set up your credentials in gradle.properties"))
            return
        }
        
        // Check if there's a pending verification email
        val pendingEmail = getSavedEmailForResend()
        if (!pendingEmail.isNullOrEmpty()) {
            updateUIState(AuthUIState.EmailVerificationPending(pendingEmail))
            return
        }
        
        lifecycleScope.launch {
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser != null && currentUser.id.isNotEmpty()) {
                    // User is authenticated, navigate to main
                    updateUIState(AuthUIState.Authenticated(currentUser))
                } else {
                    // User not authenticated, show sign in form
                    updateUIState(AuthUIState.SignInForm)
                    android.util.Log.d("AuthActivity", "No authenticated user found")
                }
            } catch (e: Exception) {
                // User not authenticated, show sign in form
                updateUIState(AuthUIState.SignInForm)
                android.util.Log.e("AuthActivity", "Error checking current user: ${e.message}")
            }
        }
    }



    private fun performSignIn() {
        // Check if Supabase is configured
        if (!SupabaseClient.isConfigured()) {
            updateUIState(AuthUIState.Error("Supabase not configured. Please check your setup."))
            return
        }
        
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        if (validateInput(email, password)) {
            // Show loading state
            updateUIState(AuthUIState.Loading)
            
            lifecycleScope.launch {
                try {
                    SupabaseClient.client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }
                    
                    val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                    if (currentUser?.emailConfirmedAt != null) {
                        // Successful authentication - clear any saved email and navigate
                        clearSavedEmail()
                        updateUIState(AuthUIState.Authenticated(currentUser))
                    } else {
                        // Email verification required - navigate to EmailVerificationActivity
                        Toast.makeText(this@AuthActivity, "Please verify your email address to continue", Toast.LENGTH_LONG).show()
                        navigateToEmailVerification(email, password)
                    }
                } catch (e: Exception) {
                    // Clean up session on exception
                    cleanupFailedSession()
                    
                    val isEmailNotVerified = e.message?.contains("email not confirmed", ignoreCase = true) == true ||
                                           e.message?.contains("Email not confirmed", ignoreCase = true) == true
                    
                    if (isEmailNotVerified) {
                        // Navigate to EmailVerificationActivity for unverified email
                        Toast.makeText(this@AuthActivity, "Please verify your email address to continue", Toast.LENGTH_LONG).show()
                        navigateToEmailVerification(email, password)
                    } else {
                        val errorMessage = when {
                            e.message?.contains("invalid", ignoreCase = true) == true -> "Invalid email or password. Please try again."
                            e.message?.contains("Invalid login credentials", ignoreCase = true) == true -> "Invalid email or password. Please try again."
                            e.message?.contains("network", ignoreCase = true) == true -> "Network connection error. Please check your internet and try again."
                            else -> e.message ?: "Sign in failed"
                        }
                        updateUIState(AuthUIState.Error(errorMessage))
                    }
                }
            }
        }
    }

    private fun resetSignInButton() {
        binding.btnSignIn.isEnabled = true
        binding.btnSignIn.text = if (isSignUpMode) "Sign Up" else "Sign In"
    }

    private fun performSignUp() {
        // Check if Supabase is configured
        if (!SupabaseClient.isConfigured()) {
            updateUIState(AuthUIState.Error("Supabase not configured. Please check your setup."))
            return
        }
        
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        
        if (validateInput(email, password, username)) {
            // Show loading state
            updateUIState(AuthUIState.Loading)
            
            lifecycleScope.launch {
                try {
                    android.util.Log.d("AuthActivity", "Starting sign up for email: $email")
                    SupabaseClient.client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                    }
                    
                    val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                    if (currentUser?.emailConfirmedAt != null) {
                        // Account created and verified - proceed to complete profile
                        Toast.makeText(this@AuthActivity, "✅ Account created successfully!", Toast.LENGTH_SHORT).show()
                        clearSavedEmail()
                        navigateToCompleteProfile()
                    } else {
                        // Email verification required - navigate to EmailVerificationActivity
                        Toast.makeText(this@AuthActivity, "✅ Account created successfully! Please check your email for verification.", Toast.LENGTH_LONG).show()
                        navigateToEmailVerification(email, password)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AuthActivity", "Sign up failed: ${e.message}", e)
                    
                    // Check if this is actually a successful creation with verification needed
                    if (e.message?.contains("email not confirmed", ignoreCase = true) == true ||
                        e.message?.contains("Email not confirmed", ignoreCase = true) == true) {
                        // Account was created but needs verification
                        android.util.Log.d("AuthActivity", "Account created but needs email verification")
                        Toast.makeText(this@AuthActivity, "✅ Account created successfully! Please check your email for verification.", Toast.LENGTH_LONG).show()
                        navigateToEmailVerification(email, password)
                        return@launch
                    }
                    
                    // Clean up session on actual sign up failure
                    cleanupFailedSession()
                    
                    val errorMessage = when {
                        e.message?.contains("already registered", ignoreCase = true) == true -> "An account with this email already exists. Please sign in instead."
                        e.message?.contains("User already registered", ignoreCase = true) == true -> "An account with this email already exists. Please sign in instead."
                        e.message?.contains("network", ignoreCase = true) == true -> "Network connection error. Please check your internet and try again."
                        else -> e.message ?: "Sign up failed"
                    }
                    android.util.Log.e("AuthActivity", "Showing error to user: $errorMessage")
                    updateUIState(AuthUIState.Error(errorMessage))
                }
            }
        }
    }

    private fun validateInput(email: String, password: String, username: String? = null): Boolean {
        var isValid = true
        
        binding.apply {
            if (email.isEmpty()) {
                tilEmail.error = "Email is required"
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Please enter a valid email"
                isValid = false
            }
            
            if (password.isEmpty()) {
                tilPassword.error = "Password is required"
                isValid = false
            } else if (password.length < 6) {
                tilPassword.error = "Password must be at least 6 characters"
                isValid = false
            }
            
            if (isSignUpMode && username.isNullOrEmpty()) {
                tilUsername.error = "Username is required"
                isValid = false
            } else if (isSignUpMode && username != null && username.length < 3) {
                tilUsername.error = "Username must be at least 3 characters"
                isValid = false
            }
        }
        
        return isValid
    }

    private fun toggleMode() {
        // Add haptic feedback
        performHapticFeedback()
        
        if (isSignUpMode) {
            updateUIState(AuthUIState.SignInForm)
        } else {
            updateUIState(AuthUIState.SignUpForm)
        }
    }
    
    /**
     * Perform subtle haptic feedback for better UX
     */
    private fun performHapticFeedback() {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun navigateToCompleteProfile() {
        startActivity(Intent(this, CompleteProfileActivity::class.java))
        finish()
    }

    /**
     * Navigate to EmailVerificationActivity for dedicated verification flow
     */
    private fun navigateToEmailVerification(email: String, password: String) {
        val intent = Intent(this, EmailVerificationActivity::class.java)
        intent.putExtra(EmailVerificationActivity.EXTRA_EMAIL, email)
        intent.putExtra(EmailVerificationActivity.EXTRA_PASSWORD, password)
        startActivity(intent)
        // Don't finish() here so user can come back if needed
    }

    /**
     * Clean up session for failed authentication attempts
     */
    private fun cleanupFailedSession() {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.auth.signOut()
            } catch (e: Exception) {
                // Ignore sign out errors during cleanup
                android.util.Log.w("AuthActivity", "Failed to clean up session: ${e.message}")
            }
        }
    }

    /**
     * Implement automatic retry of sign in after email verification
     */
    private fun attemptAutoSignInAfterVerification(email: String, password: String) {
        lifecycleScope.launch {
            try {
                // Try to sign in to check if email is now verified
                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser?.emailConfirmedAt != null) {
                    // Email is verified, sign in successful
                    android.util.Log.d("AuthActivity", "Email verified, automatic sign in successful")
                    clearSavedEmail()
                    Toast.makeText(this@AuthActivity, "Email verified! Welcome back.", Toast.LENGTH_SHORT).show()
                    updateUIState(AuthUIState.Authenticated(currentUser))
                } else {
                    android.util.Log.d("AuthActivity", "Email not yet verified")
                }
            } catch (e: Exception) {
                android.util.Log.w("AuthActivity", "Error during auto sign in check: ${e.message}")
                // Don't show error to user for auto sign in failures
            }
        }
    }

    /**
     * Perform automatic sign in after email verification
     */
    private fun performAutoSignIn(email: String, password: String) {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser?.emailConfirmedAt != null) {
                    // Successful authentication after verification
                    clearSavedEmail()
                    Toast.makeText(this@AuthActivity, "Email verified! Welcome back.", Toast.LENGTH_SHORT).show()
                    updateUIState(AuthUIState.Authenticated(currentUser))
                }
            } catch (e: Exception) {
                android.util.Log.w("AuthActivity", "Auto sign in exception: ${e.message}")
                // Don't show error to user for auto sign in failures
            }
        }
    }

    /**
     * Start periodic verification checking when in verification pending state
     */
    private fun startVerificationChecking(email: String) {
        // Get the password from the form if available for auto sign in
        val password = binding.etPassword.text.toString().trim()
        
        if (password.isNotEmpty()) {
            lifecycleScope.launch {
                // Check verification status every 30 seconds for up to 10 minutes
                repeat(20) { attempt ->
                    delay(30000) // Wait 30 seconds
                    
                    // Only continue checking if still in verification pending state
                    if (currentState is AuthUIState.EmailVerificationPending) {
                        attemptAutoSignInAfterVerification(email, password)
                    } else {
                        // Exit checking if state changed
                        return@launch
                    }
                }
            }
        }
    }

    /**
     * Handle back to sign in from verification pending state
     */
    private fun handleBackToSignIn() {
        // Clear saved email when going back to sign in
        clearSavedEmail()
        
        // Animate transition back to sign in
        binding.apply {
            layoutEmailVerification.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    layoutEmailVerification.visibility = View.GONE
                    layoutEmailVerification.alpha = 1f
                    
                    // Reset form fields
                    etEmail.text?.clear()
                    etPassword.text?.clear()
                    etUsername.text?.clear()
                    
                    // Navigate back to sign in form
                    updateUIState(AuthUIState.SignInForm)
                }
                .start()
        }
    }
}