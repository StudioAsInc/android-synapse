package com.synapse.social.studioasinc.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.MainActivity
import com.synapse.social.studioasinc.databinding.ActivityAuthBinding
import com.synapse.social.studioasinc.di.Dependencies
import com.synapse.social.studioasinc.presentation.viewmodel.AuthViewModel
import com.synapse.social.studioasinc.presentation.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var authViewModel: AuthViewModel
    
    private var isSignUpMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel with dependencies
        val factory = AuthViewModelFactory(
            Dependencies.signInUseCase,
            Dependencies.signUpUseCase,
            Dependencies.signOutUseCase,
            Dependencies.getCurrentUserUseCase,
            Dependencies.observeAuthStateUseCase
        )
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        setupUI()
        observeViewModel()
        
        // Check if user is already authenticated
        lifecycleScope.launch {
            authViewModel.isAuthenticated.collect { isAuthenticated ->
                if (isAuthenticated) {
                    navigateToMain()
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
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            authViewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
                
                state.error?.let { error ->
                    Toast.makeText(this@AuthActivity, error, Toast.LENGTH_LONG).show()
                    authViewModel.clearError()
                }
            }
        }
    }

    private fun performSignIn() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        if (validateInput(email, password)) {
            authViewModel.signIn(email, password)
        }
    }

    private fun performSignUp() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        
        if (validateInput(email, password, username)) {
            authViewModel.signUp(email, password, username)
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