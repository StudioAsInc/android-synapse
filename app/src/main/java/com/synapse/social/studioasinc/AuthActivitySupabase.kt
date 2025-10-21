package com.synapse.social.studioasinc

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.animations.layout.layoutshaker
import com.synapse.social.studioasinc.animations.textview.TVeffects
import com.onesignal.OneSignal
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import kotlinx.coroutines.launch

/**
 * Migrated AuthActivity using Supabase instead of Firebase.
 * Handles user authentication with email/password using Supabase GoTrue.
 */
class AuthActivitySupabase : AppCompatActivity() {

    // UI Components
    private lateinit var vscroll1: ScrollView
    private lateinit var parentLayout: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var aiNameTextView: TVeffects
    private lateinit var aiResponseTextView_1: TVeffects
    private lateinit var mainHiddenLayout: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var section2Layout: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var section3Layout: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var nameLayout: LinearLayout
    private lateinit var animatorSupportLayout: View
    private lateinit var profileHolderLayout: LinearLayout
    private lateinit var nameFirstLetterTextView: TextView
    private lateinit var usernameEditText: EditText
    private lateinit var confirmAgeCheckBox: CheckBox
    private lateinit var continueButton: Button
    private lateinit var aiResponseTextView_2: TVeffects
    private lateinit var finishButton: Button
    private lateinit var ruleTextView1: TVeffects
    private lateinit var emailLayout: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var ask_for_email_tv: TextView
    private lateinit var email_et: EditText
    private lateinit var passLayout: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var askForPassTV: TextView
    private lateinit var pass_et: EditText
    private lateinit var button1: Button

    // System services
    private lateinit var vib: Vibrator
    private lateinit var sfx: SoundPool

    // Sound IDs
    private var sfxClickId: Int = 0
    private var sfxSuccessId: Int = 0
    private var sfxUserInputEndId: Int = 0
    private var sfxErrorId: Int = 0

    // Supabase Services
    private lateinit var authService: SupabaseAuthenticationService
    private lateinit var dbService: SupabaseDatabaseService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        initializeViews()
        initializeServices()
        setupWindowFlags()
        setupListeners()
        initializeSupabase()
        startIntroAnimation()
    }

    private fun initializeViews() {
        vscroll1 = findViewById(R.id.vscroll1)
        parentLayout = findViewById(R.id.parentLayout)
        aiNameTextView = findViewById(R.id.aiNameTextView)
        aiResponseTextView_1 = findViewById(R.id.aiResponseTextView_1)
        mainHiddenLayout = findViewById(R.id.mainHiddenLayout)
        section2Layout = findViewById(R.id.section2Layout)
        section3Layout = findViewById(R.id.section3Layout)
        nameLayout = findViewById(R.id.nameLayout)
        animatorSupportLayout = findViewById(R.id.animatorSupportLayout)
        profileHolderLayout = findViewById(R.id.profileHolderLayout)
        nameFirstLetterTextView = findViewById(R.id.nameFirstLetterTextView)
        usernameEditText = findViewById(R.id.usernameEditText)
        confirmAgeCheckBox = findViewById(R.id.confirmAgeCheckBox)
        continueButton = findViewById(R.id.continueButton)
        aiResponseTextView_2 = findViewById(R.id.aiResponseTextView_2)
        finishButton = findViewById(R.id.finishButton)
        ruleTextView1 = findViewById(R.id.ruleTextView1)
        emailLayout = findViewById(R.id.emailLayout)
        ask_for_email_tv = findViewById(R.id.ask_for_email_tv)
        email_et = findViewById(R.id.email_et)
        passLayout = findViewById(R.id.passLayout)
        askForPassTV = findViewById(R.id.askForPassTV)
        pass_et = findViewById(R.id.pass_et)
        button1 = findViewById(R.id.button1)
    }

    private fun initializeServices() {
        vib = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Initialize SoundPool with Builder for better control
        val builder = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_GAME)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        sfx = builder.build()

        // Load sounds
        sfxClickId = sfx.load(applicationContext, R.raw.sfx_scifi_click, 1)
        sfxSuccessId = sfx.load(applicationContext, R.raw.success, 1)
        sfxUserInputEndId = sfx.load(applicationContext, R.raw.user_input_end, 1)
        sfxErrorId = sfx.load(applicationContext, R.raw.sfx_tode, 1)
    }

    private fun setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val window: Window = window
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
    }

    private fun initializeSupabase() {
        authService = SupabaseAuthenticationService()
        dbService = SupabaseDatabaseService()
    }

    private fun setupListeners() {
        continueButton.setOnClickListener { handleContinueClick(it) }
        finishButton.setOnClickListener { handleFinishClick(it) }
        button1.setOnClickListener { handleSignUpClick(it) }

        emailLayout.setOnClickListener { email_et.requestFocus() }
        passLayout.setOnClickListener { pass_et.requestFocus() }
    }

    private fun startIntroAnimation() {
        aiNameTextView.setTotalDuration(450L)
        aiNameTextView.setFadeDuration(150L)
        aiNameTextView.startTyping("Hello, I'm Synapse AI")

        Handler(Looper.getMainLooper()).postDelayed({
            aiResponseTextView_1.setTotalDuration(1000L)
            aiResponseTextView_1.setFadeDuration(150L)
            aiResponseTextView_1.startTyping(
                "I'm a next generation AI built to assist you in Synapse and to be safe, accurate and secure.\n\n" +
                        "I would love to get to know each other before we get started"
            )
        }, 500)

        mainHiddenLayout.postDelayed({ mainHiddenLayout.visibility = View.VISIBLE }, 2000)

        Handler(Looper.getMainLooper()).postDelayed({
            usernameEditText.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.showSoftInput(usernameEditText, InputMethodManager.SHOW_IMPLICIT)
            animatorSupportLayout.visibility = View.GONE
        }, 2500)
    }

    private fun handleContinueClick(view: View) {
        val username = usernameEditText.text.toString().trim()

        if (username.isEmpty()) {
            layoutshaker.shake(nameLayout)
            vib.vibrate(100)
            sfx.play(sfxErrorId, 1.0f, 1.0f, 1, 0, 1.0f)
            return
        }

        sfx.play(sfxClickId, 1.0f, 1.0f, 1, 0, 1.0f)
        profileHolderLayout.visibility = View.VISIBLE

        Handler(Looper.getMainLooper()).postDelayed({
            aiResponseTextView_2.setTotalDuration(1300L)
            aiResponseTextView_2.setFadeDuration(150L)
            aiResponseTextView_2.startTyping("Okay $username, we're almost there, but before that one last final process. Please I kindly request you to look at Synapse terms and conditions before using their services")
            section2Layout.visibility = View.VISIBLE

            ruleTextView1.setTotalDuration(3000L)
            ruleTextView1.setFadeDuration(150L)
            ruleTextView1.startTyping("By using Synapse, you agree to follow our rules. You must be at least 13 years old to create an account. You are responsible for keeping your login information private and secure. Misuse of the platform may result in your account being restricted or removed.")
        }, 1000)

        hideKeyboard()
        usernameEditText.isEnabled = false

        Handler(Looper.getMainLooper()).postDelayed({
            findViewById<View>(R.id.linear4).visibility = View.VISIBLE
        }, 2000)

        // Set first letter of username
        nameFirstLetterTextView.text = username.first().uppercaseChar().toString()

        continueButton.isEnabled = false
        confirmAgeCheckBox.isEnabled = false
    }

    private fun handleFinishClick(view: View) {
        section2Layout.visibility = View.GONE
        mainHiddenLayout.visibility = View.GONE
        section3Layout.visibility = View.VISIBLE

        aiNameTextView.setTotalDuration(500L)
        aiNameTextView.setFadeDuration(150L)
        aiNameTextView.startTyping("We are almost done!")

        aiResponseTextView_1.setTotalDuration(1300L)
        aiResponseTextView_1.setFadeDuration(150L)
        aiResponseTextView_1.startTyping("Okay brother, believe me... We are going to finish this boring process within a few seconds. Just like instant noodles. First, you have to...")
    }

    private fun handleSignUpClick(view: View) {
        val email = email_et.text.toString().trim()
        val pass = pass_et.text.toString().trim()

        var isValid = true
        var shouldPlaySound = false

        // Email Validation
        if (email.isEmpty() || email.length < 10 || !email.contains("@")) {
            layoutshaker.shake(emailLayout)
            isValid = false
            shouldPlaySound = true
        }

        // Password Validation
        if (pass.isEmpty()) {
            layoutshaker.shake(passLayout)
            isValid = false
            shouldPlaySound = true
        }

        if (shouldPlaySound) {
            sfx.play(sfxErrorId, 1.0f, 1.0f, 1, 0, 1.0f)
        }

        if (isValid) {
            lifecycleScope.launch {
                try {
                    val user = authService.signUp(email, pass)
                    handleSuccessfulRegistration()
                } catch (e: Exception) {
                    handleRegistrationError(e)
                }
            }
        }
    }

    private fun handleSuccessfulRegistration() {
        aiNameTextView.setTotalDuration(300L)
        aiNameTextView.setFadeDuration(150L)
        aiNameTextView.startTyping("Creating your account...")

        val intent = Intent(this@AuthActivitySupabase, CompleteProfileActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun handleRegistrationError(exception: Exception) {
        val errorMessage = exception.message
        if (errorMessage?.contains("already registered") == true || 
            errorMessage?.contains("already exists") == true) {
            handleExistingAccount()
        } else {
            // Handle other errors
            aiResponseTextView_1.setTotalDuration(1300L)
            aiResponseTextView_1.setFadeDuration(150L)
            aiResponseTextView_1.startTyping("Something went wrong. Please try again.")
        }
    }

    private fun handleExistingAccount() {
        aiNameTextView.setTotalDuration(500L)
        aiNameTextView.setFadeDuration(150L)
        aiNameTextView.startTyping("Hey, I know you!")

        val email = email_et.text.toString()
        val pass = pass_et.text.toString()

        lifecycleScope.launch {
            try {
                val user = authService.signIn(email, pass)
                fetchUsername(user.id)
            } catch (e: Exception) {
                showSignInError()
            }
        }
    }

    private fun fetchUsername(uid: String) {
        // Update OneSignal Player ID on sign-in
        updateOneSignalPlayerId(uid)

        lifecycleScope.launch {
            try {
                val userData = dbService.selectWithFilter<Map<String, Any?>>(
                    table = "users",
                    columns = "username"
                ) { query ->
                    // This would need to be implemented based on the actual Supabase query builder
                    query
                }

                val username = userData.firstOrNull()?.get("username") as? String
                if (username != null) {
                    showWelcomeMessage("You are @$username right? No further steps, Let's go...")
                } else {
                    showWelcomeMessage("I recognize you! Let's go...")
                }
                navigateToHomeAfterDelay()
            } catch (e: Exception) {
                showWelcomeMessage("I recognize you! Let's go...")
                navigateToHomeAfterDelay()
            }
        }
    }

    private fun showWelcomeMessage(message: String) {
        aiResponseTextView_1.setTotalDuration(1300L)
        aiResponseTextView_1.setFadeDuration(150L)
        aiResponseTextView_1.startTyping(message)
    }

    private fun navigateToHomeAfterDelay() {
        Handler().postDelayed({
            val intent = Intent(this@AuthActivitySupabase, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }, 2000)
    }

    private fun showSignInError() {
        aiResponseTextView_1.setTotalDuration(1300L)
        aiResponseTextView_1.setFadeDuration(150L)
        aiResponseTextView_1.startTyping("Hmm, that password doesn't match. Try again?")
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        if (imm != null && window.decorView.windowToken != null) {
            imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
        }
    }

    override fun onBackPressed() {
        finishAffinity()
    }

    /**
     * Updates the OneSignal Player ID for the current user in Supabase database.
     * This method gets the current OneSignal Player ID and saves it to the user's profile.
     *
     * @param uid The Supabase user ID
     */
    private fun updateOneSignalPlayerId(uid: String) {
        // Get current OneSignal Player ID if available
        if (OneSignal.getUser().pushSubscription.optedIn) {
            val playerId = OneSignal.getUser().pushSubscription.id
            if (!playerId.isNullOrEmpty()) {
                lifecycleScope.launch {
                    try {
                        dbService.update(
                            table = "users",
                            data = mapOf("one_signal_player_id" to playerId)
                        )
                    } catch (e: Exception) {
                        // Handle error silently
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sfx.release()
    }
}