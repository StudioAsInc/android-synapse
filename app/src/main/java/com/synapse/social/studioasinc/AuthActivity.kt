package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.databinding.ActivityAuthBinding
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import kotlinx.coroutines.launch

/**
 * Modern AuthActivity using Supabase authentication.
 * Handles user authentication with email/password using Supabase GoTrue.
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var authService: SupabaseAuthenticationService
    
    private var isSignUpMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Supabase authentication service
        authService = SupabaseAuthenticationService()

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
        }
    }

    private fun checkCurrentUser() {
        // First check if Supabase is configured
        if (!SupabaseClient.isConfigured()) {
            showSupabaseConfigurationError()
            return
        }
        
        lifecycleScope.launch {
            try {
                val currentUser = authService.getCurrentUser()
                if (currentUser != null && currentUser.id.isNotEmpty()) {
                    // User is authenticated, navigate to main
                    navigateToMain()
                } else {
                    // User not authenticated, stay on auth screen
                    android.util.Log.d("AuthActivity", "No authenticated user found")
                }
            } catch (e: Exception) {
                // User not authenticated, stay on auth screen
                android.util.Log.e("AuthActivity", "Error checking current user: ${e.message}")
            }
        }
    }

    private fun showSupabaseConfigurationError() {
        Toast.makeText(
            this, 
            "Supabase not configured. Please set up your Supabase credentials in gradle.properties", 
            Toast.LENGTH_LONG
        ).show()
        
        // Disable authentication buttons
        binding.btnSignIn.isEnabled = false
        binding.btnSignIn.text = "Supabase Not Configured"
    }

    private fun performSignIn() {
        // Check if Supabase is configured
        if (!SupabaseClient.isConfigured()) {
            Toast.makeText(this, "Supabase not configured. Please check your setup.", Toast.LENGTH_LONG).show()
            return
        }
        
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        if (validateInput(email, password)) {
            // Show loading state
            binding.btnSignIn.isEnabled = false
            binding.btnSignIn.text = "Signing in..."
            
            lifecycleScope.launch {
                try {
                    val result = authService.signIn(email, password)
                    result.fold(
                        onSuccess = { user ->
                            Toast.makeText(this@AuthActivity, "Welcome back!", Toast.LENGTH_SHORT).show()
                            navigateToMain()
                        },
                        onFailure = { error ->
                            Toast.makeText(this@AuthActivity, "Sign in failed: ${error.message}", Toast.LENGTH_LONG).show()
                            resetSignInButton()
                        }
                    )
                } catch (e: Exception) {
                    Toast.makeText(this@AuthActivity, "Sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
                    resetSignInButton()
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
            Toast.makeText(this, "Supabase not configured. Please check your setup.", Toast.LENGTH_LONG).show()
            return
        }
        
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        
        if (validateInput(email, password, username)) {
            // Show loading state
            binding.btnSignIn.isEnabled = false
            binding.btnSignIn.text = "Creating account..."
            
            lifecycleScope.launch {
                try {
                    val result = authService.signUp(email, password)
                    result.fold(
                        onSuccess = { user ->
                            Toast.makeText(this@AuthActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                            navigateToCompleteProfile()
                        },
                        onFailure = { error ->
                            Toast.makeText(this@AuthActivity, "Sign up failed: ${error.message}", Toast.LENGTH_LONG).show()
                            resetSignInButton()
                        }
                    )
                } catch (e: Exception) {
                    Toast.makeText(this@AuthActivity, "Sign up failed: ${e.message}", Toast.LENGTH_LONG).show()
                    resetSignInButton()
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
        isSignUpMode = !isSignUpMode
        
        binding.apply {
            if (isSignUpMode) {
                etUsername.visibility = android.view.View.VISIBLE
                btnSignIn.text = "Sign Up"
                tvToggleMode.text = "Already have an account? Sign In"
            } else {
                etUsername.visibility = android.view.View.GONE
                btnSignIn.text = "Sign In"
                tvToggleMode.text = "Don't have an account? Sign Up"
            }
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