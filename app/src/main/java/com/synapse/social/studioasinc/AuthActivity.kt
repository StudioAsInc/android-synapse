package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.databinding.ActivityAuthBinding
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.User
import com.synapse.social.studioasinc.backend.AuthError
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.content.SharedPreferences
import android.view.View

/**
 * AuthUIState sealed class for managing UI states
 */
sealed class AuthUIState {
    object Loading : AuthUIState()
    object SignInForm : AuthUIState()
    object SignUpForm : AuthUIState()
    data class EmailVerificationPending(val email: String) : AuthUIState()
    data class Authenticated(val user: User) : AuthUIState()
    data class Error(val error: AuthError, val message: String) : AuthUIState()
}

/**
 * Modern AuthActivity using Supabase authentication.
 * Handles user authentication with email/password using Supabase GoTrue.
 * Includes email verification flow handling and UI state management.
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var authService: SupabaseAuthenticationService
    private lateinit var sharedPreferences: SharedPreferences
    
    private var isSignUpMode = false
    private var currentState: AuthUIState = AuthUIState.SignInForm
    private var resendCooldownSeconds = 0
    private var isResendCooldownActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize services
        authService = SupabaseAuthenticationService()
        sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)

        setupUI()
        checkCurrentUser()
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
                updateUIState(AuthUIState.SignInForm)
            }
        }
    }

    /**
     * Update UI state based on current authentication state
     */
    private fun updateUIState(state: AuthUIState) {
        currentState = state
        
        binding.apply {
            // Hide all sections first
            layoutEmailVerification.visibility = View.GONE
            tvErrorMessage.visibility = View.GONE
            progressBar.visibility = View.GONE
            
            // Show/hide main form elements
            etEmail.visibility = View.VISIBLE
            etPassword.visibility = View.VISIBLE
            btnSignIn.visibility = View.VISIBLE
            tvToggleMode.visibility = View.VISIBLE
            
            when (state) {
                is AuthUIState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnSignIn.isEnabled = false
                    btnSignIn.text = "Loading..."
                }
                
                is AuthUIState.SignInForm -> {
                    isSignUpMode = false
                    etUsername.visibility = View.GONE
                    btnSignIn.isEnabled = true
                    btnSignIn.text = "Sign In"
                    tvToggleMode.text = "Don't have an account? Sign Up"
                }
                
                is AuthUIState.SignUpForm -> {
                    isSignUpMode = true
                    etUsername.visibility = View.VISIBLE
                    btnSignIn.isEnabled = true
                    btnSignIn.text = "Sign Up"
                    tvToggleMode.text = "Already have an account? Sign In"
                }
                
                is AuthUIState.EmailVerificationPending -> {
                    // Hide main form elements
                    etEmail.visibility = View.GONE
                    etPassword.visibility = View.GONE
                    etUsername.visibility = View.GONE
                    btnSignIn.visibility = View.GONE
                    tvToggleMode.visibility = View.GONE
                    
                    // Show verification section
                    layoutEmailVerification.visibility = View.VISIBLE
                    tvVerificationEmail.text = "Email sent to: ${state.email}"
                    
                    // Save email for resend functionality
                    saveEmailForResend(state.email)
                }
                
                is AuthUIState.Authenticated -> {
                    // Navigate to main activity
                    navigateToMain()
                }
                
                is AuthUIState.Error -> {
                    tvErrorMessage.visibility = View.VISIBLE
                    tvErrorMessage.text = state.message
                    btnSignIn.isEnabled = true
                    resetSignInButton()
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
                val result = authService.resendVerificationEmail(email)
                result.fold(
                    onSuccess = {
                        Toast.makeText(this@AuthActivity, "Verification email sent!", Toast.LENGTH_SHORT).show()
                        startResendCooldown()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@AuthActivity, "Failed to resend: ${error.message}", Toast.LENGTH_LONG).show()
                        resetResendButton()
                    }
                )
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
            updateUIState(AuthUIState.Error(AuthError.SUPABASE_NOT_CONFIGURED, "Supabase not configured. Please set up your credentials in gradle.properties"))
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
                val currentUser = authService.getCurrentUser()
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
            updateUIState(AuthUIState.Error(AuthError.SUPABASE_NOT_CONFIGURED, "Supabase not configured. Please check your setup."))
            return
        }
        
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        if (validateInput(email, password)) {
            // Show loading state
            updateUIState(AuthUIState.Loading)
            
            lifecycleScope.launch {
                try {
                    val result = authService.signIn(email, password)
                    result.fold(
                        onSuccess = { authResult ->
                            if (authResult.needsEmailVerification) {
                                // Email verification required
                                updateUIState(AuthUIState.EmailVerificationPending(email))
                            } else {
                                // Successful authentication
                                authResult.user?.let { user ->
                                    clearSavedEmail()
                                    updateUIState(AuthUIState.Authenticated(user))
                                } ?: run {
                                    updateUIState(AuthUIState.Error(AuthError.UNKNOWN_ERROR, "Authentication failed"))
                                }
                            }
                        },
                        onFailure = { error ->
                            val authError = when {
                                error.message?.contains("email not confirmed", ignoreCase = true) == true -> 
                                    AuthError.EMAIL_NOT_VERIFIED
                                error.message?.contains("invalid", ignoreCase = true) == true -> 
                                    AuthError.INVALID_CREDENTIALS
                                else -> AuthError.UNKNOWN_ERROR
                            }
                            
                            if (authError == AuthError.EMAIL_NOT_VERIFIED) {
                                updateUIState(AuthUIState.EmailVerificationPending(email))
                            } else {
                                updateUIState(AuthUIState.Error(authError, error.message ?: "Sign in failed"))
                            }
                        }
                    )
                } catch (e: Exception) {
                    val authError = when {
                        e.message?.contains("email not confirmed", ignoreCase = true) == true -> 
                            AuthError.EMAIL_NOT_VERIFIED
                        e.message?.contains("invalid", ignoreCase = true) == true -> 
                            AuthError.INVALID_CREDENTIALS
                        else -> AuthError.UNKNOWN_ERROR
                    }
                    
                    if (authError == AuthError.EMAIL_NOT_VERIFIED) {
                        updateUIState(AuthUIState.EmailVerificationPending(email))
                    } else {
                        updateUIState(AuthUIState.Error(authError, e.message ?: "Sign in failed"))
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
            updateUIState(AuthUIState.Error(AuthError.SUPABASE_NOT_CONFIGURED, "Supabase not configured. Please check your setup."))
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
                    val result = authService.signUp(email, password)
                    result.fold(
                        onSuccess = { authResult ->
                            if (authResult.needsEmailVerification) {
                                // Email verification required - show verification pending state
                                Toast.makeText(this@AuthActivity, "Account created! Please check your email for verification.", Toast.LENGTH_LONG).show()
                                updateUIState(AuthUIState.EmailVerificationPending(email))
                            } else {
                                // Account created and verified - proceed to complete profile
                                Toast.makeText(this@AuthActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                navigateToCompleteProfile()
                            }
                        },
                        onFailure = { error ->
                            val authError = when {
                                error.message?.contains("already registered", ignoreCase = true) == true -> 
                                    AuthError.INVALID_CREDENTIALS
                                error.message?.contains("network", ignoreCase = true) == true -> 
                                    AuthError.NETWORK_ERROR
                                else -> AuthError.UNKNOWN_ERROR
                            }
                            updateUIState(AuthUIState.Error(authError, error.message ?: "Sign up failed"))
                        }
                    )
                } catch (e: Exception) {
                    val authError = when {
                        e.message?.contains("already registered", ignoreCase = true) == true -> 
                            AuthError.INVALID_CREDENTIALS
                        e.message?.contains("network", ignoreCase = true) == true -> 
                            AuthError.NETWORK_ERROR
                        else -> AuthError.UNKNOWN_ERROR
                    }
                    updateUIState(AuthUIState.Error(authError, e.message ?: "Sign up failed"))
                }
            }
        }
    }

    private fun validateInput(email: String, password: String, username: String? = null): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return false
        }
        
        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            return false
        }
        
        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            return false
        }
        
        if (isSignUpMode && username.isNullOrEmpty()) {
            binding.etUsername.error = "Username is required"
            return false
        }
        
        return true
    }

    private fun toggleMode() {
        if (isSignUpMode) {
            updateUIState(AuthUIState.SignInForm)
        } else {
            updateUIState(AuthUIState.SignUpForm)
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
}