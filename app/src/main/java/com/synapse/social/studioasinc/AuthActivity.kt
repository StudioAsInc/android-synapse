package com.synapse.social.studioasinc

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.synapse.social.studioasinc.databinding.ActivityAuthBinding
import com.synapse.social.studioasinc.util.HapticFeedbackUtil
import com.synapse.social.studioasinc.util.HapticFeedbackUtil.FeedbackType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()
    private var currentGradientIndex = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupGlassmorphism()
        setupBackgroundAnimation()
        setupObservers()
        setupEntryAnimation()
    }

    private fun setupUI() {
        binding.apply {
            // Text Watchers
            etEmail.doAfterTextChanged { viewModel.onEmailChanged(it.toString()) }
            etPassword.doAfterTextChanged { viewModel.onPasswordChanged(it.toString()) }
            etUsername.doAfterTextChanged { viewModel.onUsernameChanged(it.toString()) }

            // Buttons
            btnSignIn.setOnClickListener {
                HapticFeedbackUtil.performHapticFeedback(it, FeedbackType.LIGHT_CLICK)
                if (viewModel.uiState.value.isSignUpMode) {
                    viewModel.signUp()
                } else {
                    viewModel.signIn()
                }
            }

            tvToggleMode.setOnClickListener {
                HapticFeedbackUtil.performHapticFeedback(it, FeedbackType.LIGHT_CLICK)
                viewModel.toggleMode()
            }

            tvForgotPassword.setOnClickListener {
                 HapticFeedbackUtil.performHapticFeedback(it, FeedbackType.LIGHT_CLICK)
                 startActivity(Intent(this@AuthActivity, ForgotPasswordActivity::class.java))
            }

            // Social Buttons
            val socialClickListener = View.OnClickListener { v ->
                HapticFeedbackUtil.performHapticFeedback(v, FeedbackType.LIGHT_CLICK)
                Toast.makeText(this@AuthActivity, "Coming soon", Toast.LENGTH_SHORT).show()
            }
            btnGoogleAuth.setOnClickListener(socialClickListener)
            btnFacebookAuth.setOnClickListener(socialClickListener)
            btnAppleAuth.setOnClickListener(socialClickListener)

            // Verification Buttons
            btnResendVerify.setOnClickListener {
                viewModel.resendVerificationEmail()
            }
            btnCancelVerify.setOnClickListener {
                viewModel.cancelVerification()
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        updateUI(state)
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun updateUI(state: AuthUiState) {
        binding.apply {
            // Loading Overlay
            loadingOverlay.isVisible = state.isLoading

            // Errors
            tilEmail.error = state.emailError
            tilPassword.error = state.passwordError
            tilUsername.error = state.usernameError
            
            // Password Strength
            if (state.showPasswordStrength && state.isSignUpMode) {
                if (!layoutPasswordStrength.isVisible) {
                     TransitionManager.beginDelayedTransition(layoutContent)
                     layoutPasswordStrength.visibility = View.VISIBLE
                }
                val color = ContextCompat.getColor(this@AuthActivity, state.passwordStrength.color)
                progressPasswordStrength.setIndicatorColor(color)
                progressPasswordStrength.setProgressCompat(state.passwordStrength.progress, true)
                tvPasswordStrength.text = state.passwordStrength.message
                tvPasswordStrength.setTextColor(color)
            } else {
                layoutPasswordStrength.visibility = View.GONE
            }

            // Mode Switching (Login vs SignUp)
            val isSignUp = state.isSignUpMode
            if (tilUsername.isVisible != isSignUp) {
                val transition = TransitionSet()
                    .addTransition(Fade())
                    .addTransition(ChangeBounds())
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setDuration(300)

                TransitionManager.beginDelayedTransition(layoutContent, transition)

                tilUsername.visibility = if (isSignUp) View.VISIBLE else View.GONE
                tvForgotPassword.visibility = if (isSignUp) View.GONE else View.VISIBLE

                tvWelcome.text = if (isSignUp) "Create Account" else "Welcome to Synapse"
                tvSubtitle.text = if (isSignUp) "Sign up to get started" else "Sign in to continue"
                btnSignIn.text = if (isSignUp) "Sign Up" else "Sign In"
                tvToggleMode.text = if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up"
            }
            
            // Verification Screen
            if (state.verificationEmail != null) {
                if (!layoutVerification.isVisible) {
                    layoutVerification.alpha = 0f
                    layoutVerification.visibility = View.VISIBLE
                    layoutVerification.animate().alpha(1f).setDuration(300).start()
                }
                btnResendVerify.isEnabled = !state.isResendCooldownActive
                btnResendVerify.text = if (state.isResendCooldownActive) "Resend in ${state.resendCooldown}s" else "Resend Email"
            } else {
                if (layoutVerification.isVisible) {
                    layoutVerification.animate().alpha(0f).setDuration(300).withEndAction {
                        layoutVerification.visibility = View.GONE
                    }.start()
                }
            }
        }
    }

    private fun handleEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.ShowToast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
            is AuthEvent.NavigateToMain -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            is AuthEvent.NavigateToCompleteProfile -> {
                startActivity(Intent(this, CompleteProfileActivity::class.java))
                finish()
            }
            is AuthEvent.NavigateToEmailVerification -> {
                // Handled by state verificationEmail
            }
            is AuthEvent.StartSuccessAnimation -> {
                HapticFeedbackUtil.performHapticFeedback(binding.btnSignIn, FeedbackType.SUCCESS)
                // Add success animation here if needed (e.g. checkmark on button)
            }
        }
    }

    private fun setupBackgroundAnimation() {
        lifecycleScope.launch {
            while (isActive) {
                delay(5000)
                animateNextGradient()
            }
        }
    }

    private fun animateNextGradient() {
        currentGradientIndex++
        if (currentGradientIndex > 14) currentGradientIndex = 1
        
        val resId = resources.getIdentifier("bg_gradient_$currentGradientIndex", "drawable", packageName)
        if (resId == 0) return

        binding.apply {
            // Load next gradient into invisible view
            val visibleView = if (ivGradient1.alpha > 0.5f) ivGradient1 else ivGradient2
            val invisibleView = if (visibleView == ivGradient1) ivGradient2 else ivGradient1
            
            invisibleView.setImageResource(resId)
            
            // Crossfade
            invisibleView.animate().alpha(1f).setDuration(2000).start()
            visibleView.animate().alpha(0f).setDuration(2000).start()
        }
    }

    private fun setupGlassmorphism() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Apply blur to the background to create a smooth gradient mesh effect
            val blurEffect = RenderEffect.createBlurEffect(
                80f, 80f, Shader.TileMode.MIRROR
            )
            binding.layoutBackground.setRenderEffect(blurEffect)
        }
    }

    private fun setupEntryAnimation() {
        // Staggered fade-up
        val views = listOf(
            binding.tvWelcome,
            binding.tvSubtitle,
            binding.tilEmail,
            binding.tilPassword,
            binding.btnSignIn,
            binding.tvToggleMode,
            binding.layoutSocialAuth
        )

        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(100L * index)
                .setDuration(500)
                .setInterpolator(OvershootInterpolator())
                .start()
        }
    }
}
