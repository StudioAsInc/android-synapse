package com.synapse.social.studioasinc

import android.content.Context
import android.content.Intent
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.FirebaseApp
import com.onesignal.OneSignal
import com.synapse.social.studioasinc.animations.layout.layoutshaker
import com.synapse.social.studioasinc.animations.textview.TVeffects
import com.synapse.social.studioasinc.backend.AuthenticationService
import com.synapse.social.studioasinc.backend.DatabaseService
import com.synapse.social.studioasinc.backend.interfaces.*
import java.lang.Exception

class AuthActivity : AppCompatActivity() {

    // --- UI Components ---
    private lateinit var aiNameTextView: TVeffects
    private lateinit var aiResponseTextView1: TVeffects
    private lateinit var aiResponseTextView2: TVeffects
    private lateinit var mainHiddenLayout: ConstraintLayout
    private lateinit var section2Layout: ConstraintLayout
    private lateinit var section3Layout: ConstraintLayout
    private lateinit var nameLayout: LinearLayout
    private lateinit var animatorSupportLayout: View
    private lateinit var profileHolderLayout: LinearLayout
    private lateinit var nameFirstLetterTextView: TextView
    private lateinit var usernameEditText: EditText
    private lateinit var confirmAgeCheckBox: CheckBox
    private lateinit var continueButton: Button
    private lateinit var finishButton: Button
    private lateinit var ruleTextView1: TVeffects
    private lateinit var emailLayout: ConstraintLayout
    private lateinit var emailEditText: EditText
    private lateinit var passLayout: ConstraintLayout
    private lateinit var passEditText: EditText
    private lateinit var signUpButton: Button

    // --- System Services ---
    private val vibrator: Vibrator by lazy { getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    private var soundPool: SoundPool? = null

    // --- Sound IDs ---
    private var sfxClickId: Int = 0
    private var sfxSuccessId: Int = 0
    private var sfxUserInputEndId: Int = 0
    private var sfxErrorId: Int = 0

    // --- Backend Services ---
    private val authService: IAuthenticationService by lazy { AuthenticationService() }
    private val dbService: IDatabaseService by lazy { DatabaseService() }

    private val authCreateUserListener: ICompletionListener<IAuthResult> =
        ICompletionListener { result, error ->
            if (result?.isSuccessful == true) {
                handleSuccessfulRegistration()
            } else {
                handleRegistrationError(error)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        initializeViews()
        initializeServices()
        setupWindowFlags()
        initializeBackend()
        setupListeners()
        startIntroAnimation()
    }

    private fun initializeViews() {
        aiNameTextView = findViewById(R.id.aiNameTextView)
        aiResponseTextView1 = findViewById(R.id.aiResponseTextView_1)
        aiResponseTextView2 = findViewById(R.id.aiResponseTextView_2)
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
        finishButton = findViewById(R.id.finishButton)
        ruleTextView1 = findViewById(R.id.ruleTextView1)
        emailLayout = findViewById(R.id.emailLayout)
        emailEditText = findViewById(R.id.email_et)
        passLayout = findViewById(R.id.passLayout)
        passEditText = findViewById(R.id.pass_et)
        signUpButton = findViewById(R.id.button1)
    }

    private fun initializeServices() {
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_GAME)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            ).build()

        soundPool?.let {
            sfxClickId = it.load(applicationContext, R.raw.sfx_scifi_click, 1)
            sfxSuccessId = it.load(applicationContext, R.raw.success, 1)
            sfxUserInputEndId = it.load(applicationContext, R.raw.user_input_end, 1)
            sfxErrorId = it.load(applicationContext, R.raw.sfx_tode, 1)
        }
    }

    private fun initializeBackend() {
        FirebaseApp.initializeApp(this)
    }

    private fun setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
    }

    private fun setupListeners() {
        continueButton.setOnClickListener { handleContinueClick(it) }
        finishButton.setOnClickListener { handleFinishClick(it) }
        signUpButton.setOnClickListener { handleSignUpClick(it) }
        emailLayout.setOnClickListener { emailEditText.requestFocus() }
        passLayout.setOnClickListener { passEditText.requestFocus() }
    }

    private fun startIntroAnimation() {
        aiNameTextView.totalDuration = 450L
        aiNameTextView.fadeDuration = 150L
        aiNameTextView.startTyping("Hello, I'm Synapse AI")

        Handler(Looper.getMainLooper()).postDelayed({
            aiResponseTextView1.totalDuration = 1000L
            aiResponseTextView1.fadeDuration = 150L
            aiResponseTextView1.startTyping(
                "I'm a next generation AI built to assist you in Synapse and to be safe, accurate and secure.\n\n" +
                        "I would love to get to know each other before we get started"
            )
        }, 500)

        mainHiddenLayout.postDelayed({ mainHiddenLayout.visibility = View.VISIBLE }, 2000)

        Handler(Looper.getMainLooper()).postDelayed({
            usernameEditText.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(usernameEditText, InputMethodManager.SHOW_IMPLICIT)
            animatorSupportLayout.visibility = View.GONE
        }, 2500)
    }

    private fun handleContinueClick(view: View) {
        val username = usernameEditText.text.toString().trim()
        if (username.isEmpty()) {
            layoutshaker.shake(nameLayout)
            vibrator.vibrate(100)
            soundPool?.play(sfxErrorId, 1.0f, 1.0f, 1, 0, 1.0f)
            return
        }

        soundPool?.play(sfxClickId, 1.0f, 1.0f, 1, 0, 1.0f)
        profileHolderLayout.visibility = View.VISIBLE

        Handler(Looper.getMainLooper()).postDelayed({
            aiResponseTextView2.totalDuration = 1300L
            aiResponseTextView2.fadeDuration = 150L
            aiResponseTextView2.startTyping("Okay $username, we're almost there, but before that one last final process. Please I kindly request you to look at Synapse terms and conditions before using their services")
            section2Layout.visibility = View.VISIBLE

            ruleTextView1.totalDuration = 3000L
            ruleTextView1.fadeDuration = 150L
            ruleTextView1.startTyping("By using Synapse, you agree to follow our rules. You must be at least 13 years old to create an account. You are responsible for keeping your login information private and secure. Misuse of the platform may result in your account being restricted or removed.")
        }, 1000)

        hideKeyboard()
        usernameEditText.isEnabled = false
        Handler(Looper.getMainLooper()).postDelayed({
            findViewById<View>(R.id.linear4).visibility = View.VISIBLE
        }, 2000)

        nameFirstLetterTextView.text = username.firstOrNull()?.toString()?.uppercase()
        continueButton.isEnabled = false
        confirmAgeCheckBox.isEnabled = false
    }

    private fun handleFinishClick(view: View) {
        section2Layout.visibility = View.GONE
        mainHiddenLayout.visibility = View.GONE
        section3Layout.visibility = View.VISIBLE

        aiNameTextView.totalDuration = 500L
        aiNameTextView.fadeDuration = 150L
        aiNameTextView.startTyping("We are almost done!")

        aiResponseTextView1.totalDuration = 1300L
        aiResponseTextView1.fadeDuration = 150L
        aiResponseTextView1.startTyping("Okay brother, believe me... We are going to finish this boring process within a few seconds. Just like instant noodles. First, you have to...")
    }

    private fun handleSignUpClick(view: View) {
        val email = emailEditText.text.toString().trim()
        val pass = passEditText.text.toString().trim()
        var isValid = true
        var shouldPlaySound = false

        if (email.isEmpty() || email.length < 10 || !email.contains("@")) {
            layoutshaker.shake(emailLayout)
            isValid = false
            shouldPlaySound = true
        }

        if (pass.isEmpty()) {
            layoutshaker.shake(passLayout)
            isValid = false
            shouldPlaySound = true
        }

        if (shouldPlaySound) {
            soundPool?.play(sfxErrorId, 1.0f, 1.0f, 1, 0, 1.0f)
        }

        if (isValid) {
            authService.signUp(email, pass, authCreateUserListener)
        }
    }

    private fun handleSuccessfulRegistration() {
        aiNameTextView.totalDuration = 300L
        aiNameTextView.fadeDuration = 150L
        aiNameTextView.startTyping("Creating your account...")
        startActivity(Intent(this, CompleteProfileActivity::class.java))
        finish()
    }

    private fun handleRegistrationError(exception: Exception?) {
        if (exception?.message == "The email address is already in use by another account.") {
            handleExistingAccount()
        }
    }

    private fun handleExistingAccount() {
        aiNameTextView.totalDuration = 500L
        aiNameTextView.fadeDuration = 150L
        aiNameTextView.startTyping("Hey, I know you!")

        val email = emailEditText.text.toString()
        val pass = passEditText.text.toString()

        authService.signIn(email, pass) { result, _ ->
            if (result?.isSuccessful == true) {
                authService.currentUser?.uid?.let { fetchUsername(it) }
            } else {
                showSignInError()
            }
        }
    }

    private fun fetchUsername(uid: String) {
        updateOneSignalPlayerId(uid)
        val path = "skyline/users/$uid/username"

        dbService.getData(dbService.getReference(path), object : IDataListener {
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                val username = dataSnapshot.getValue(String::class.java)
                if (username != null) {
                    showWelcomeMessage("You are @$username right? No further steps, Let's go...")
                } else {
                    showWelcomeMessage("I recognize you! Let's go...")
                }
                navigateToHomeAfterDelay()
            }

            override fun onCancelled(databaseError: IDatabaseError) {
                showWelcomeMessage("I recognize you! Let's go...")
                navigateToHomeAfterDelay()
            }
        })
    }

    private fun showWelcomeMessage(message: String) {
        aiResponseTextView1.totalDuration = 1300L
        aiResponseTextView1.fadeDuration = 150L
        aiResponseTextView1.startTyping(message)
    }

    private fun navigateToHomeAfterDelay() {
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }, 2000)
    }

    private fun showSignInError() {
        aiResponseTextView1.totalDuration = 1300L
        aiResponseTextView1.fadeDuration = 150L
        aiResponseTextView1.startTyping("Hmm, that password doesn't match. Try again?")
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (imm != null && window.decorView.windowToken != null) {
            imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
        }
    }

    private fun updateOneSignalPlayerId(uid: String) {
        if (OneSignal.getUser().pushSubscription.optedIn) {
            val playerId = OneSignal.getUser().pushSubscription.id
            if (!playerId.isNullOrEmpty()) {
                OneSignalManager.savePlayerIdToRealtimeDatabase(uid, playerId)
            }
        }
    }

    override fun onBackPressed() {
        finishAffinity()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        soundPool = null
    }
}