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
        lifecycleScope.launch {
            try {
                val currentUser = authService.getCurrentUser()
                if (currentUser != null) {
                    navigateToMain()
                }
            } catch (e: Exception) {
                // User not authenticated, stay on auth screen
            }
        }
    }

    private fun performSignIn() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        if (validateInput(email, password)) {
            lifecycleScope.launch {
                try {
                    val user = authService.signIn(email, password)
                    if (user != null) {
                        Toast.makeText(this@AuthActivity, "Welcome back!", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@AuthActivity, "Sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun performSignUp() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        
        if (validateInput(email, password, username)) {
            lifecycleScope.launch {
                try {
                    val user = authService.signUp(email, password)
                    if (user != null) {
                        Toast.makeText(this@AuthActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@AuthActivity, "Sign up failed: ${e.message}", Toast.LENGTH_LONG).show()
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
}