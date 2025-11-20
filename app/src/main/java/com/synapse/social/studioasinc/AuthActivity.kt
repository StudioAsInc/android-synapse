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
import android.widget.EditText
import android.widget.ImageButton
import android.view.HapticFeedbackConstants
import android.os.Build
import android.graphics.RenderEffect
import android.graphics.Shader
import android.text.InputType
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed class AuthUIState {
    object Loading : AuthUIState()
    object SignInForm : AuthUIState()
    object SignUpForm : AuthUIState()
    data class Authenticated(val user: UserInfo) : AuthUIState()
    data class Error(val message: String) : AuthUIState()
}

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var sharedPreferences: SharedPreferences
    
    private var isSignUpMode = false
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)

        setupUI()
        checkCurrentUser()
    }

    private fun setupUI() {
        binding.apply {
            btnBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            btnMain.setOnClickListener {
                if (isSignUpMode) {
                    performSignUp()
                } else {
                    performSignIn()
                }
            }

            tvToggleAction.setOnClickListener {
                toggleMode()
            }
            
            // Toggle Password Visibility
            btnTogglePassword.setOnClickListener {
                isPasswordVisible = !isPasswordVisible
                updatePasswordVisibility(etPassword, btnTogglePassword, isPasswordVisible)
            }

            // Toggle Confirm Password Visibility
            btnToggleConfirmPassword.setOnClickListener {
                isConfirmPasswordVisible = !isConfirmPasswordVisible
                updatePasswordVisibility(etConfirmPassword, btnToggleConfirmPassword, isConfirmPasswordVisible)
            }

            tvForgotPassword.setOnClickListener {
                 startActivity(Intent(this@AuthActivity, ForgotPasswordActivity::class.java))
            }

            // Social Auth (Mock/Placeholder)
            btnGoogle.setOnClickListener { handleSocialAuth("Google") }
            btnFacebook.setOnClickListener { handleSocialAuth("Facebook") }
            btnApple.setOnClickListener { handleSocialAuth("Apple") }
        }

        updateUIState(AuthUIState.SignInForm)
    }

    private fun updatePasswordVisibility(editText: EditText, toggleButton: ImageButton, isVisible: Boolean) {
        if (isVisible) {
            editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleButton.setColorFilter(ContextCompat.getColor(this, R.color.auth_green))
            toggleButton.setImageResource(R.drawable.ic_visibility)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleButton.setColorFilter(ContextCompat.getColor(this, R.color.auth_grey_light))
             toggleButton.setImageResource(R.drawable.ic_visibility_off)
        }
        editText.setSelection(editText.text.length)
        // Need to reset typeface because changing input type might change it
        try {
            editText.typeface = ResourcesCompat.getFont(this, R.font.pt_sans)
        } catch (e: Exception) {
            // Fallback if font loading fails
        }
    }

    private fun updateUIState(state: AuthUIState) {
        binding.apply {
            tvError.visibility = View.GONE
            loadingOverlay.visibility = View.GONE

            when (state) {
                is AuthUIState.Loading -> {
                    loadingOverlay.visibility = View.VISIBLE
                }
                is AuthUIState.SignInForm -> {
                    isSignUpMode = false
                    tvTitle.text = "Log in"
                    tvSubtitle.text = "Enter your email and password to securely access your account and manage your services."
                    
                    etName.visibility = View.GONE
                    layoutConfirmPassword.visibility = View.GONE
                    cbRememberMe.visibility = View.VISIBLE
                    tvForgotPassword.visibility = View.VISIBLE
                    
                    btnMain.text = "Login"
                    tvToggleText.text = "Don't have an account? "
                    tvToggleAction.text = "Sign Up here"
                }
                is AuthUIState.SignUpForm -> {
                    isSignUpMode = true
                    tvTitle.text = "Create Account"
                    tvSubtitle.text = "Create a new account to get started and enjoy seamless access to our features."
                    
                    etName.visibility = View.VISIBLE
                    layoutConfirmPassword.visibility = View.VISIBLE
                    cbRememberMe.visibility = View.GONE
                    tvForgotPassword.visibility = View.GONE
                    
                    btnMain.text = "Sign Up"
                    tvToggleText.text = "Already have an account? "
                    tvToggleAction.text = "Sign In here"
                }
                is AuthUIState.Authenticated -> {
                     startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                     finish()
                }
                is AuthUIState.Error -> {
                    tvError.text = state.message
                    tvError.visibility = View.VISIBLE
                }
            }
        }
    }
    
    private fun toggleMode() {
        if (isSignUpMode) {
            updateUIState(AuthUIState.SignInForm)
        } else {
            updateUIState(AuthUIState.SignUpForm)
        }
    }

    private fun handleSocialAuth(provider: String) {
        Toast.makeText(this, "$provider login coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun performSignIn() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            updateUIState(AuthUIState.Error("Please fill in all fields"))
            return
        }

        updateUIState(AuthUIState.Loading)
        
        lifecycleScope.launch {
            try {
                if (!SupabaseClient.isConfigured()) {
                     updateUIState(AuthUIState.Error("Supabase not configured"))
                     return@launch
                }
                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                val user = SupabaseClient.client.auth.currentUserOrNull()
                if (user != null) {
                    if (user.emailConfirmedAt != null) {
                         updateUIState(AuthUIState.Authenticated(user))
                    } else {
                         // Email not verified
                         navigateToEmailVerification(email, password)
                    }
                } else {
                    updateUIState(AuthUIState.Error("Login failed"))
                }
            } catch (e: Exception) {
                // Check for email not confirmed error message if Supabase throws it
                 if (e.message?.contains("Email not confirmed", ignoreCase = true) == true) {
                     navigateToEmailVerification(email, password)
                 } else {
                     updateUIState(AuthUIState.Error(e.message ?: "Login failed"))
                 }
            }
        }
    }

    private fun performSignUp() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            updateUIState(AuthUIState.Error("Please fill in all fields"))
            return
        }

        if (password != confirmPassword) {
            updateUIState(AuthUIState.Error("Passwords do not match"))
            return
        }

        updateUIState(AuthUIState.Loading)

        lifecycleScope.launch {
            try {
                 if (!SupabaseClient.isConfigured()) {
                     updateUIState(AuthUIState.Error("Supabase not configured"))
                     return@launch
                }
                SupabaseClient.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    this.data = buildJsonObject {
                        put("full_name", name)
                    }
                }
                
                val user = SupabaseClient.client.auth.currentUserOrNull()
                if (user?.emailConfirmedAt != null) {
                    Toast.makeText(this@AuthActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@AuthActivity, CompleteProfileActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@AuthActivity, "Account created! Please check email for verification.", Toast.LENGTH_LONG).show()
                    navigateToEmailVerification(email, password)
                }

            } catch (e: Exception) {
                 // Check for email not confirmed error message if Supabase throws it (sometimes sign up returns user but throws if auto confirm off?)
                 // Actually sign up usually succeeds and returns user with null confirmed_at.
                 // If user already exists but unconfirmed, might throw.
                 if (e.message?.contains("Email not confirmed", ignoreCase = true) == true) {
                     navigateToEmailVerification(email, password)
                 } else {
                     updateUIState(AuthUIState.Error(e.message ?: "Sign up failed"))
                 }
            }
        }
    }
    
    private fun navigateToEmailVerification(email: String, password: String) {
        val intent = Intent(this, EmailVerificationActivity::class.java)
        intent.putExtra(EmailVerificationActivity.EXTRA_EMAIL, email)
        intent.putExtra(EmailVerificationActivity.EXTRA_PASSWORD, password)
        startActivity(intent)
    }
    
    private fun checkCurrentUser() {
         if (!SupabaseClient.isConfigured()) return
         lifecycleScope.launch {
             try {
                 val user = SupabaseClient.client.auth.currentUserOrNull()
                 if (user != null) {
                     if (user.emailConfirmedAt != null) {
                        updateUIState(AuthUIState.Authenticated(user))
                     }
                 }
             } catch (e: Exception) {
                 // Ignore
             }
         }
    }
}