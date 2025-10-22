package com.synapse.social.studioasinc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.google.android.material.button.MaterialButton
import com.google.android.material.appbar.MaterialToolbar
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.synapse.social.studioasinc.model.UserProfile
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import android.view.View
import kotlinx.serialization.json.jsonObject

/**
 * CompleteProfileActivity - Migrated to Supabase
 * Allows users to complete their profile after registration
 */
class CompleteProfileActivity : AppCompatActivity() {

    private lateinit var authService: SupabaseAuthenticationService
    private lateinit var dbService: SupabaseDatabaseService
    private lateinit var storageService: SupabaseStorageService
    
    private lateinit var toolbar: MaterialToolbar
    private lateinit var profileImageCard: CardView
    private lateinit var profileImage: ImageView
    private lateinit var usernameInput: FadeEditText
    private lateinit var nicknameInput: FadeEditText
    private lateinit var biographyInput: FadeEditText
    private lateinit var skipButton: MaterialButton
    private lateinit var completeButton: MaterialButton
    
    private var selectedImageUri: Uri? = null
    private var isUsernameValid = false

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this)
                .load(it)
                .circleCrop()
                .into(profileImage)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            initializeLogic()
        } else {
            Toast.makeText(this, "Storage permissions are required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complete_profile)
        
        initializeViews()
        initializeServices()
        setupToolbar()
        setupListeners()
        checkPermissions()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        profileImageCard = findViewById(R.id.profile_image_card)
        profileImage = findViewById(R.id.profile_image)
        usernameInput = findViewById(R.id.username_input)
        nicknameInput = findViewById(R.id.nickname_input)
        biographyInput = findViewById(R.id.biography_input)
        skipButton = findViewById(R.id.skip_button)
        completeButton = findViewById(R.id.complete_button)
    }

    private fun initializeServices() {
        authService = SupabaseAuthenticationService()
        dbService = SupabaseDatabaseService()
        storageService = SupabaseStorageService(SupabaseClient.client.storage)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = ""
        }
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupListeners() {
        profileImageCard.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        profileImageCard.setOnLongClickListener {
            selectedImageUri = null
            profileImage.setImageResource(R.drawable.avatar)
            true
        }

        usernameInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                validateUsername(s.toString())
            }
        })

        skipButton.setOnClickListener {
            navigateToMain()
        }

        completeButton.setOnClickListener {
            completeProfile()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (allGranted) {
            initializeLogic()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    private fun initializeLogic() {
        lifecycleScope.launch {
            try {
                val currentUser = authService.getCurrentUser()
                if (currentUser == null) {
                    Toast.makeText(this@CompleteProfileActivity, "Please sign in first", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }
                
                val emailVerificationView = findViewById<View>(R.id.email_verification)
                if (currentUser.isEmailConfirmed) {
                    emailVerificationView.visibility = View.GONE
                } else {
                    emailVerificationView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Toast.makeText(this@CompleteProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateUsername(username: String) {
        when {
            username.trim().isEmpty() -> {
                usernameInput.error = "Username is required"
                isUsernameValid = false
            }
            username.length < 3 -> {
                usernameInput.error = "Username must be at least 3 characters"
                isUsernameValid = false
            }
            username.length > 25 -> {
                usernameInput.error = "Username must be less than 25 characters"
                isUsernameValid = false
            }
            !username.matches(Regex("[a-z0-9_.]+")) -> {
                usernameInput.error = "Username can only contain lowercase letters, numbers, dots and underscores"
                isUsernameValid = false
            }
            else -> {
                usernameInput.error = null
                isUsernameValid = true
                checkUsernameAvailability(username)
            }
        }
    }

    private fun checkUsernameAvailability(username: String) {
        lifecycleScope.launch {
            try {
                val result = dbService.selectWithFilter(
                    table = "users",
                    columns = "username",
                    filter = "username",
                    value = username
                )
                
                result.onSuccess { users ->
                    if (users.isNotEmpty()) {
                        usernameInput.error = "Username already taken"
                        isUsernameValid = false
                    } else {
                        usernameInput.error = null
                        isUsernameValid = true
                    }
                }
            } catch (e: Exception) {
                // Handle error silently or show message
            }
        }
    }

    private fun completeProfile() {
        val username = usernameInput.text.toString().trim()
        val nickname = nicknameInput.text.toString().trim().ifEmpty { username }
        val bio = biographyInput.text.toString().trim()

        if (!isUsernameValid) {
            Toast.makeText(this, "Please enter a valid username", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val currentUser = authService.getCurrentUser()
                if (currentUser == null) {
                    Toast.makeText(this@CompleteProfileActivity, "User not found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                var imageUrl: String? = null
                selectedImageUri?.let { uri ->
                    val result = storageService.uploadImage(this, uri)
                    result.onSuccess {
                        imageUrl = it
                    }.onFailure {
                        Toast.makeText(this@CompleteProfileActivity, "Failed to upload image", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }

                // Create user profile data
                val userProfile = UserProfile(
                    uid = currentUser.id,
                    username = username,
                    display_name = nickname,
                    email = currentUser.email ?: "",
                    bio = bio,
                    profile_image_url = imageUrl
                )

                // Insert user profile into Supabase
                val result = dbService.insert("users", userProfile)
                
                result.onSuccess {
                    Toast.makeText(this@CompleteProfileActivity, "Profile created successfully!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }.onFailure { error ->
                    Toast.makeText(this@CompleteProfileActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CompleteProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onBackPressed() {
        // Prevent going back to auth screen
        finishAffinity()
    }
}
