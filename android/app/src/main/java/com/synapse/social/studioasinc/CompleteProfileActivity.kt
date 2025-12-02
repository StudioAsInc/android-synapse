package com.synapse.social.studioasinc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
// import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.JsonObject
import com.google.android.material.button.MaterialButton
import com.google.android.material.appbar.MaterialToolbar
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.synapse.social.studioasinc.model.UserProfile
import kotlinx.coroutines.launch
import android.view.View

/**
 * CompleteProfileActivity - Migrated to Supabase
 * Allows users to complete their profile after registration
 */
class CompleteProfileActivity : BaseActivity() {

    // No wrapper services needed - using Supabase client directly
    
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
    private var isCheckingUsername = false

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
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            initializeLogic()
        } else {
            Toast.makeText(this, "Storage permission is required for profile images", Toast.LENGTH_SHORT).show()
            // Continue without permission - user can still complete profile without image
            initializeLogic()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complete_profile)
        
        initializeViews()
        // No services to initialize - using Supabase client directly
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

    // Removed - using Supabase client directly

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
                val username = s.toString().trim()
                if (username.isNotEmpty()) {
                    validateUsername(username)
                } else {
                    usernameInput.error = null
                    isUsernameValid = false
                }
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
        // Only request READ_EXTERNAL_STORAGE for image selection
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            initializeLogic()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun initializeLogic() {
        lifecycleScope.launch {
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser == null) {
                    Toast.makeText(this@CompleteProfileActivity, "Please sign in first", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }
                
                val emailVerificationView = findViewById<View>(R.id.email_verification)
                if (currentUser.emailConfirmedAt != null) {
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
            username.startsWith("_") || username.startsWith(".") -> {
                usernameInput.error = "Username cannot start with underscore or dot"
                isUsernameValid = false
            }
            username.endsWith("_") || username.endsWith(".") -> {
                usernameInput.error = "Username cannot end with underscore or dot"
                isUsernameValid = false
            }
            username.contains("..") || username.contains("__") -> {
                usernameInput.error = "Username cannot contain consecutive dots or underscores"
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
        if (isCheckingUsername) return
        
        isCheckingUsername = true
        lifecycleScope.launch {
            try {
                val users = SupabaseClient.client.from("users")
                    .select(columns = Columns.raw("username")) {
                        filter { 
                            eq("username", username)
                        }
                    }.decodeList<JsonObject>()
                
                if (users.isNotEmpty()) {
                    val suggestions = generateUsernameSuggestions(username)
                    usernameInput.error = "Username already taken. Try: ${suggestions.joinToString(", ")}"
                    isUsernameValid = false
                } else {
                    usernameInput.error = null
                    isUsernameValid = true
                }
            } catch (e: Exception) {
                // Handle error silently or show message
                android.util.Log.w("CompleteProfile", "Username check failed: ${e.message}")
                // Don't mark as invalid if check fails - let user proceed
                usernameInput.error = null
                isUsernameValid = true
            } finally {
                isCheckingUsername = false
            }
        }
    }

    private fun generateUsernameSuggestions(baseUsername: String): List<String> {
        val suggestions = mutableListOf<String>()
        val random = (100..999).random()
        
        // Add number suffix
        suggestions.add("${baseUsername}$random")
        
        // Add underscore and number
        if (!baseUsername.endsWith("_")) {
            suggestions.add("${baseUsername}_$random")
        }
        
        // Add dot and number if not ending with dot
        if (!baseUsername.endsWith(".")) {
            suggestions.add("${baseUsername}.$random")
        }
        
        return suggestions.take(2) // Return max 2 suggestions
    }

    private fun completeProfile() {
        android.util.Log.d("CompleteProfile", "=== COMPLETE PROFILE FUNCTION STARTED ===")
        val username = usernameInput.text.toString().trim()
        val nickname = nicknameInput.text.toString().trim().ifEmpty { username }
        val bio = biographyInput.text.toString().trim()

        android.util.Log.d("CompleteProfile", "Username: $username, Nickname: $nickname, Bio: $bio")

        if (!isUsernameValid || username.isEmpty()) {
            Toast.makeText(this, "Please enter a valid username", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable the button to prevent double submission
        completeButton.isEnabled = false
        completeButton.text = "Creating Profile..."

        lifecycleScope.launch {
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser == null) {
                    Toast.makeText(this@CompleteProfileActivity, "User not found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                var imageUrl: String? = null
                selectedImageUri?.let { uri ->
                    android.util.Log.d("CompleteProfile", "Starting image upload for URI: $uri")
                    try {
                        // Upload image directly using Supabase storage
                        val fileName = "profile_${currentUser.id}_${System.currentTimeMillis()}.jpg"
                        val bucket = SupabaseClient.client.storage.from("avatars")
                        
                        // Convert URI to byte array for upload
                        val inputStream = contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()
                        
                        if (bytes != null) {
                            bucket.upload(fileName, bytes)
                            imageUrl = bucket.publicUrl(fileName)
                            android.util.Log.d("CompleteProfile", "Image uploaded successfully: $imageUrl")
                        }
                    } catch (error: Exception) {
                        android.util.Log.e("CompleteProfile", "Image upload failed", error)
                        
                        val errorMessage = when {
                            error.message?.contains("bucket", ignoreCase = true) == true -> 
                                "Storage bucket not found. Continuing without profile image."
                            error.message?.contains("permission", ignoreCase = true) == true -> 
                                "Permission denied. Continuing without profile image."
                            error.message?.contains("network", ignoreCase = true) == true -> 
                                "Network error during upload. Continuing without profile image."
                            else -> "Failed to upload image. Continuing without profile image."
                        }
                        Toast.makeText(this@CompleteProfileActivity, errorMessage, Toast.LENGTH_SHORT).show()
                        imageUrl = null
                    }
                }

                // Validate required fields
                if (currentUser.id.isEmpty()) {
                    Toast.makeText(this@CompleteProfileActivity, "User ID is missing. Please sign in again.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                if (currentUser.email.isNullOrEmpty()) {
                    Toast.makeText(this@CompleteProfileActivity, "User email is missing. Please sign in again.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Create user profile data for insertion
                val userEmail = currentUser.email ?: ""
                val userProfile = UserProfile(
                    uid = currentUser.id,
                    username = username,
                    displayName = nickname,
                    email = userEmail,
                    bio = bio.ifEmpty { null },
                    profileImageUrl = imageUrl
                )
                
                // Check if user profile already exists for this UID
                val existingUserProfile = SupabaseClient.client.from("users")
                    .select(columns = Columns.raw("uid")) {
                        filter { 
                            eq("uid", currentUser.id)
                        }
                    }.decodeList<JsonObject>()
                
                if (existingUserProfile.isNotEmpty()) {
                    Toast.makeText(this@CompleteProfileActivity, "Profile already exists! Redirecting to main app.", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                    return@launch
                }

                // Final username availability check before insertion
                val existingUsers = SupabaseClient.client.from("users")
                    .select(columns = Columns.raw("username")) {
                        filter { 
                            eq("username", username)
                        }
                    }.decodeList<JsonObject>()
                
                if (existingUsers.isNotEmpty()) {
                    runOnUiThread {
                        val suggestions = generateUsernameSuggestions(username)
                        usernameInput.error = "This username is already taken. Try: ${suggestions.joinToString(", ")}"
                        isUsernameValid = false
                        completeButton.isEnabled = true
                        completeButton.text = "Complete Profile"
                    }
                    Toast.makeText(this@CompleteProfileActivity, "Username already taken. Please choose a different username.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // Debug logging
                android.util.Log.d("CompleteProfile", "Inserting user profile: $userProfile")

                // Insert user profile directly into Supabase
                try {
                    SupabaseClient.client.from("users").insert(userProfile)
                    Toast.makeText(this@CompleteProfileActivity, "Profile created successfully!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } catch (error: Exception) {
                    android.util.Log.e("CompleteProfile", "=== DATABASE INSERTION FAILED ===")
                    android.util.Log.e("CompleteProfile", "Error message: ${error.message}")
                    android.util.Log.e("CompleteProfile", "Error type: ${error.javaClass.simpleName}")
                    android.util.Log.e("CompleteProfile", "Failed data: $userProfile")
                    android.util.Log.e("CompleteProfile", "Stack trace:", error)
                    
                    // Handle specific error cases
                    val errorMessage = when {
                        error.message?.contains("duplicate key value violates unique constraint \"users_username_key\"", ignoreCase = true) == true -> {
                            // Username is already taken - update UI to show this
                            runOnUiThread {
                                usernameInput.error = "This username is already taken. Please choose a different one."
                                isUsernameValid = false
                            }
                            "Username already taken. Please choose a different username."
                        }
                        error.message?.contains("duplicate key", ignoreCase = true) == true && error.message?.contains("username", ignoreCase = true) == true -> {
                            runOnUiThread {
                                usernameInput.error = "This username is already taken. Please choose a different one."
                                isUsernameValid = false
                            }
                            "Username already taken. Please choose a different username."
                        }
                        error.message?.contains("violates unique constraint", ignoreCase = true) == true -> {
                            "This username or email is already in use. Please try different values."
                        }
                        error.message?.contains("network", ignoreCase = true) == true -> {
                            "Network error. Please check your connection and try again."
                        }
                        error.message?.contains("permission", ignoreCase = true) == true -> {
                            "Permission denied. Please try signing in again."
                        }
                        else -> "Failed to create profile: ${error.message ?: "Unknown error"}"
                    }
                    
                    android.util.Log.e("CompleteProfile", "Showing toast with message: $errorMessage")
                    Toast.makeText(this@CompleteProfileActivity, errorMessage, Toast.LENGTH_LONG).show()
                    
                    // Re-enable the button
                    runOnUiThread {
                        completeButton.isEnabled = true
                        completeButton.text = "Complete Profile"
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@CompleteProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                // Re-enable the button
                completeButton.isEnabled = true
                completeButton.text = "Complete Profile"
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent going back to auth screen
        finishAffinity()
    }
}
