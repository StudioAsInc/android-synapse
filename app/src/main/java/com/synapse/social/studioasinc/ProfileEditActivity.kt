package com.synapse.social.studioasinc

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import kotlinx.coroutines.*
import java.util.*

class ProfileEditActivity : AppCompatActivity() {

    companion object {
        const val REQ_CD_FP = 101
        const val REQ_CD_FPCOVER = 102
        const val REQ_CD_REGION = 103
    }

    // Supabase services
    private val authService = SupabaseAuthenticationService()
    private val databaseService = SupabaseDatabaseService()
    private val storageService = com.synapse.social.studioasinc.backend.SupabaseStorageService()

    private var synapseLoadingDialog: ProgressDialog? = null
    private var userLastProfileUri = ""
    private var userLastCoverUri = ""
    private var userNameErr = false
    private var nickNameErr = false
    private var biographyErr = false
    private var currentUsername = ""

    // UI Components
    private lateinit var body: LinearLayout
    private lateinit var top: LinearLayout
    private lateinit var mScroll: ScrollView
    private lateinit var mLoadingBody: LinearLayout
    private lateinit var mBack: ImageView
    private lateinit var mTitle: TextView
    private lateinit var mSave: ImageView
    private lateinit var mScrollBody: LinearLayout
    private lateinit var profileRelativeCard: CardView
    private lateinit var mUsernameInput: FadeEditText
    private lateinit var mNicknameInput: FadeEditText
    private lateinit var mBiographyInput: FadeEditText
    private lateinit var gender: LinearLayout
    private lateinit var region: LinearLayout
    private lateinit var profileImageHistoryStage: LinearLayout
    private lateinit var coverImageHistoryStage: LinearLayout
    private lateinit var stage1Relative: RelativeLayout
    private lateinit var profileCoverImage: ImageView
    private lateinit var stage1RelativeUp: LinearLayout
    private lateinit var stage1RelativeUpProfileCard: CardView
    private lateinit var stage1RelativeUpProfileImage: ImageView
    private lateinit var genderTitle: TextView
    private lateinit var genderSubtitle: TextView
    private lateinit var genderMale: LinearLayout
    private lateinit var genderFemale: LinearLayout
    private lateinit var genderGone: LinearLayout
    private lateinit var genderMaleIc: ImageView
    private lateinit var genderMaleTitle: TextView
    private lateinit var genderMaleCheckbox: ImageView
    private lateinit var genderFemaleIc: ImageView
    private lateinit var genderFemaleTitle: TextView
    private lateinit var genderFemaleCheckbox: ImageView
    private lateinit var genderGoneIc: ImageView
    private lateinit var genderGoneTitle: TextView
    private lateinit var genderGoneCheckbox: ImageView
    private lateinit var regionTop: LinearLayout
    private lateinit var regionSubtitle: TextView
    private lateinit var regionTitle: TextView
    private lateinit var regionArrow: ImageView
    private lateinit var profileImageHistoryStageTop: LinearLayout
    private lateinit var profileImageHistoryStageSubtext: TextView
    private lateinit var profileImageHistoryStageTitle: TextView
    private lateinit var profileImageHistoryStageArrow: ImageView
    private lateinit var coverImageHistoryStageTop: LinearLayout
    private lateinit var coverImageHistoryStageSubtext: TextView
    private lateinit var coverImageHistoryStageTitle: TextView
    private lateinit var coverImageHistoryStageArrow: ImageView
    private lateinit var mLoadingBar: ProgressBar

    private lateinit var vbr: Vibrator
    private val fp = Intent(Intent.ACTION_GET_CONTENT)
    private val fpcover = Intent(Intent.ACTION_GET_CONTENT)
    private val cc = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)
        initialize(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1000)
        } else {
            initializeLogic()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000) {
            initializeLogic()
        }
    }

    private fun initialize(savedInstanceState: Bundle?) {
        // Initialize UI components
        body = findViewById(R.id.body)
        top = findViewById(R.id.top)
        mScroll = findViewById(R.id.mScroll)
        mLoadingBody = findViewById(R.id.mLoadingBody)
        mBack = findViewById(R.id.mBack)
        mTitle = findViewById(R.id.mTitle)
        mSave = findViewById(R.id.mSave)
        mScrollBody = findViewById(R.id.mScrollBody)
        profileRelativeCard = findViewById(R.id.profileRelativeCard)
        mUsernameInput = findViewById(R.id.mUsernameInput)
        mNicknameInput = findViewById(R.id.mNicknameInput)
        mBiographyInput = findViewById(R.id.mBiographyInput)
        gender = findViewById(R.id.gender)
        region = findViewById(R.id.region)
        profileImageHistoryStage = findViewById(R.id.profile_image_history_stage)
        coverImageHistoryStage = findViewById(R.id.cover_image_history_stage)
        stage1Relative = findViewById(R.id.stage1Relative)
        profileCoverImage = findViewById(R.id.profileCoverImage)
        stage1RelativeUp = findViewById(R.id.stage1RelativeUp)
        stage1RelativeUpProfileCard = findViewById(R.id.stage1RelativeUpProfileCard)
        stage1RelativeUpProfileImage = findViewById(R.id.stage1RelativeUpProfileImage)
        genderTitle = findViewById(R.id.gender_title)
        genderSubtitle = findViewById(R.id.gender_subtitle)
        genderMale = findViewById(R.id.gender_male)
        genderFemale = findViewById(R.id.gender_female)
        genderGone = findViewById(R.id.gender_gone)
        genderMaleIc = findViewById(R.id.gender_male_ic)
        genderMaleTitle = findViewById(R.id.gender_male_title)
        genderMaleCheckbox = findViewById(R.id.gender_male_checkbox)
        genderFemaleIc = findViewById(R.id.gender_female_ic)
        genderFemaleTitle = findViewById(R.id.gender_female_title)
        genderFemaleCheckbox = findViewById(R.id.gender_female_checkbox)
        genderGoneIc = findViewById(R.id.gender_gone_ic)
        genderGoneTitle = findViewById(R.id.gender_gone_title)
        genderGoneCheckbox = findViewById(R.id.gender_gone_checkbox)
        regionTop = findViewById(R.id.region_top)
        regionSubtitle = findViewById(R.id.region_subtitle)
        regionTitle = findViewById(R.id.region_title)
        regionArrow = findViewById(R.id.region_arrow)
        profileImageHistoryStageTop = findViewById(R.id.profile_image_history_stage_top)
        profileImageHistoryStageSubtext = findViewById(R.id.profile_image_history_stage_subtext)
        profileImageHistoryStageTitle = findViewById(R.id.profile_image_history_stage_title)
        profileImageHistoryStageArrow = findViewById(R.id.profile_image_history_stage_arrow)
        coverImageHistoryStageTop = findViewById(R.id.cover_image_history_stage_top)
        coverImageHistoryStageSubtext = findViewById(R.id.cover_image_history_stage_subtext)
        coverImageHistoryStageTitle = findViewById(R.id.cover_image_history_stage_title)
        coverImageHistoryStageArrow = findViewById(R.id.cover_image_history_stage_arrow)
        mLoadingBar = findViewById(R.id.mLoadingBar)

        vbr = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        fp.type = "image/*"
        fp.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        fpcover.type = "image/*"
        fpcover.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        setupClickListeners()
        setupTextWatchers()
    }

    private fun setupClickListeners() {
        mBack.setOnClickListener { onBackPressed() }

        mSave.setOnClickListener {
            if (mUsernameInput.text.toString().trim().isNotEmpty()) {
                if (!(userNameErr || nickNameErr || biographyErr)) {
                    saveProfile()
                }
            } else {
                SketchwareUtil.showMessage(applicationContext, resources.getString(R.string.username_err_invalid))
            }
            vbr.vibrate(48)
        }

        region.setOnClickListener {
            val intent = Intent(applicationContext, SelectRegionActivity::class.java)
            intent.putExtra(SelectRegionActivity.EXTRA_CURRENT_REGION, regionSubtitle.text.toString())
            startActivityForResult(intent, REQ_CD_REGION)
        }

        profileImageHistoryStage.setOnClickListener {
            val intent = Intent(applicationContext, ProfilePhotoHistoryActivity::class.java)
            startActivity(intent)
        }

        coverImageHistoryStage.setOnClickListener {
            val intent = Intent(applicationContext, ProfileCoverPhotoHistoryActivity::class.java)
            startActivity(intent)
        }

        genderMale.setOnClickListener {
            setGenderSelection("male")
            vbr.vibrate(48)
        }

        genderFemale.setOnClickListener {
            setGenderSelection("female")
            vbr.vibrate(48)
        }

        genderGone.setOnClickListener {
            setGenderSelection("hidden")
            vbr.vibrate(48)
        }

        stage1RelativeUpProfileCard.setOnClickListener {
            startActivityForResult(fp, REQ_CD_FP)
        }

        profileCoverImage.setOnClickListener {
            startActivityForResult(fpcover, REQ_CD_FPCOVER)
        }
    }

    private fun setupTextWatchers() {
        mUsernameInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                validateUsername(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })

        mNicknameInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                validateNickname(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })

        mBiographyInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                validateBiography(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })
    }

    private fun initializeLogic() {
        top.elevation = 4f
        
        mTitle.setTypeface(null, Typeface.BOLD)
        genderTitle.setTypeface(null, Typeface.BOLD)
        regionTitle.setTypeface(null, Typeface.BOLD)
        coverImageHistoryStageTitle.setTypeface(null, Typeface.BOLD)
        profileImageHistoryStageTitle.setTypeface(null, Typeface.BOLD)

        getUserReference()
    }

    private fun validateUsername(username: String) {
        when {
            username.trim().isEmpty() -> {
                setUsernameError(resources.getString(R.string.enter_username))
                userNameErr = true
            }
            !username.matches(Regex("[a-z0-9_.]+")) -> {
                setUsernameError(resources.getString(R.string.username_err_invalid_characters))
                userNameErr = true
            }
            !username.any { it.isLetter() } -> {
                setUsernameError(resources.getString(R.string.username_err_one_letter))
                userNameErr = true
            }
            username.length < 3 -> {
                setUsernameError(resources.getString(R.string.username_err_3_characters))
                userNameErr = true
            }
            username.length > 25 -> {
                setUsernameError(resources.getString(R.string.username_err_25_characters))
                userNameErr = true
            }
            else -> {
                checkUsernameAvailability(username.trim())
            }
        }
    }

    private fun checkUsernameAvailability(username: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val currentUserId = authService.getCurrentUserId()
                val result = databaseService.selectWhere("users", "*", "username", username)
                
                result.fold(
                    onSuccess = { users ->
                        val existingUser = users.firstOrNull()
                        if (existingUser != null && existingUser["uid"] != currentUserId) {
                            setUsernameError(resources.getString(R.string.username_err_already_taken))
                            userNameErr = true
                        } else {
                            clearUsernameError()
                            userNameErr = false
                        }
                    },
                    onFailure = {
                        clearUsernameError()
                        userNameErr = false
                    }
                )
            } catch (e: Exception) {
                clearUsernameError()
                userNameErr = false
            }
        }
    }

    private fun validateNickname(nickname: String) {
        if (nickname.length > 30) {
            setNicknameError(resources.getString(R.string.nickname_err_30_characters))
            nickNameErr = true
        } else {
            clearNicknameError()
            nickNameErr = false
        }
    }

    private fun validateBiography(biography: String) {
        if (biography.length > 250) {
            setBiographyError(resources.getString(R.string.biography_err_250_characters))
            biographyErr = true
        } else {
            clearBiographyError()
            biographyErr = false
        }
    }

    private fun setUsernameError(error: String) {
        mUsernameInput.isActivated = true
        mUsernameInput.error = error
    }

    private fun clearUsernameError() {
        mUsernameInput.isActivated = false
        mUsernameInput.error = null
    }

    private fun setNicknameError(error: String) {
        mNicknameInput.isActivated = true
        mNicknameInput.error = error
    }

    private fun clearNicknameError() {
        mNicknameInput.isActivated = false
        mNicknameInput.error = null
    }

    private fun setBiographyError(error: String) {
        mBiographyInput.isActivated = true
        mBiographyInput.error = error
    }

    private fun clearBiographyError() {
        mBiographyInput.isActivated = false
        mBiographyInput.error = null
    }

    private fun setGenderSelection(gender: String) {
        when (gender) {
            "male" -> {
                genderMaleCheckbox.setImageResource(R.drawable.checkbox_checked)
                genderFemaleCheckbox.setImageResource(R.drawable.checkbox_not_checked)
                genderGoneCheckbox.setImageResource(R.drawable.checkbox_not_checked)
                genderMaleCheckbox.isEnabled = true
                genderFemaleCheckbox.isEnabled = false
                genderGoneCheckbox.isEnabled = false
            }
            "female" -> {
                genderMaleCheckbox.setImageResource(R.drawable.checkbox_not_checked)
                genderFemaleCheckbox.setImageResource(R.drawable.checkbox_checked)
                genderGoneCheckbox.setImageResource(R.drawable.checkbox_not_checked)
                genderMaleCheckbox.isEnabled = false
                genderFemaleCheckbox.isEnabled = true
                genderGoneCheckbox.isEnabled = false
            }
            "hidden" -> {
                genderMaleCheckbox.setImageResource(R.drawable.checkbox_not_checked)
                genderFemaleCheckbox.setImageResource(R.drawable.checkbox_not_checked)
                genderGoneCheckbox.setImageResource(R.drawable.checkbox_checked)
                genderMaleCheckbox.isEnabled = false
                genderFemaleCheckbox.isEnabled = false
                genderGoneCheckbox.isEnabled = true
            }
        }
    }

    private fun saveProfile() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                loadingDialog(true)
                
                val currentUserId = authService.getCurrentUserId() ?: return@launch
                val currentUser = authService.getCurrentUser() ?: return@launch
                
                val selectedGender = when {
                    genderMaleCheckbox.isEnabled -> "male"
                    genderFemaleCheckbox.isEnabled -> "female"
                    genderGoneCheckbox.isEnabled -> "hidden"
                    else -> "hidden"
                }
                
                // Create update data as map
                val regionText = regionSubtitle.text.toString()
                
                val updateData = mapOf(
                    "username" to mUsernameInput.text.toString().trim(),
                    "nickname" to mNicknameInput.text.toString().trim().takeIf { it.isNotEmpty() },
                    "biography" to mBiographyInput.text.toString().trim().takeIf { it.isNotEmpty() },
                    "gender" to selectedGender,
                    "region" to if (regionText.isNotEmpty() && regionText != "Not set") regionText else null,
                    "avatar" to if (userLastProfileUri.isNotEmpty() && userLastProfileUri != "null") userLastProfileUri else null,
                    "profile_cover_image" to if (userLastCoverUri.isNotEmpty() && userLastCoverUri != "null") userLastCoverUri else null
                )
                
                // Update user profile
                val updateResult = databaseService.update("users", updateData, "uid", currentUserId)
                
                updateResult.fold(
                    onSuccess = {
                        // Remove old username from username table if it changed
                        if (currentUsername != mUsernameInput.text.toString().trim()) {
                            databaseService.delete("usernames", "username", currentUsername)
                            
                            // Add new username to username table
                            val usernameData = mapOf(
                                "uid" to currentUserId,
                                "email" to currentUser.email,
                                "username" to mUsernameInput.text.toString().trim()
                            )
                            databaseService.upsert("usernames", usernameData)
                        }
                        
                        loadingDialog(false)
                        SketchwareUtil.showMessage(applicationContext, resources.getString(R.string.changes_saved))
                    },
                    onFailure = { error ->
                        loadingDialog(false)
                        SketchwareUtil.showMessage(applicationContext, "Error: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                loadingDialog(false)
                SketchwareUtil.showMessage(applicationContext, "Error: ${e.message}")
            }
        }
    }

    private fun getUserReference() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                mScroll.visibility = View.GONE
                mLoadingBody.visibility = View.VISIBLE
                
                val currentUserId = authService.getCurrentUserId() ?: return@launch
                val result = databaseService.selectWhere("users", "*", "uid", currentUserId)
                
                result.fold(
                    onSuccess = { users ->
                        val user = users.firstOrNull()
                        if (user != null) {
                            mScroll.visibility = View.VISIBLE
                            mLoadingBody.visibility = View.GONE
                            
                            userLastProfileUri = user["avatar"]?.toString() ?: ""
                            userLastCoverUri = user["profile_cover_image"]?.toString() ?: ""
                            
                            // Load cover image
                            if (userLastCoverUri.isEmpty() || userLastCoverUri == "null") {
                                profileCoverImage.setImageResource(R.drawable.user_null_cover_photo)
                            } else {
                                Glide.with(applicationContext).load(Uri.parse(userLastCoverUri)).into(profileCoverImage)
                            }
                            
                            // Load profile image
                            if (userLastProfileUri.isEmpty() || userLastProfileUri == "null") {
                                stage1RelativeUpProfileImage.setImageResource(R.drawable.avatar)
                            } else {
                                Glide.with(applicationContext).load(Uri.parse(userLastProfileUri)).into(stage1RelativeUpProfileImage)
                            }
                            
                            // Set form data
                            mUsernameInput.setText(user["username"]?.toString() ?: "")
                            currentUsername = user["username"]?.toString() ?: ""
                            
                            val nickname = user["nickname"]?.toString()
                            mNicknameInput.setText(if (nickname == "null" || nickname == null) "" else nickname)
                            
                            val biography = user["biography"]?.toString()
                            mBiographyInput.setText(if (biography == "null" || biography == null) "" else biography)
                            
                            // Set gender selection
                            val gender = user["gender"]?.toString() ?: "hidden"
                            setGenderSelection(gender)
                            
                            // Set region
                            val region = user["region"]?.toString()
                            regionSubtitle.text = if (region.isNullOrEmpty() || region == "null") "Not set" else region
                        }
                    },
                    onFailure = { error ->
                        mScroll.visibility = View.VISIBLE
                        mLoadingBody.visibility = View.GONE
                        SketchwareUtil.showMessage(applicationContext, "Error loading profile: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                mScroll.visibility = View.VISIBLE
                mLoadingBody.visibility = View.GONE
                SketchwareUtil.showMessage(applicationContext, "Error: ${e.message}")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQ_CD_FP -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    handleProfileImageSelection(data)
                }
            }
            REQ_CD_FPCOVER -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    handleCoverImageSelection(data)
                }
            }
            REQ_CD_REGION -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val selectedRegion = data.getStringExtra(SelectRegionActivity.EXTRA_SELECTED_REGION)
                    if (!selectedRegion.isNullOrEmpty()) {
                        regionSubtitle.text = selectedRegion
                    }
                }
            }
        }
    }

    private fun handleProfileImageSelection(data: Intent) {
        val filePaths = getFilePathsFromIntent(data)
        if (filePaths.isNotEmpty()) {
            loadingDialog(true)
            stage1RelativeUpProfileImage.setImageBitmap(FileUtil.decodeSampleBitmapFromPath(filePaths[0], 1024, 1024))
            
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val currentUserId = authService.getCurrentUserId() ?: return@launch
                    
                    // Upload to Supabase Storage
                    val uploadResult = storageService.uploadAvatar(currentUserId, filePaths[0])
                    
                    uploadResult.fold(
                        onSuccess = { imageUrl ->
                            val profileData = mapOf("avatar" to imageUrl)
                            
                            val result = databaseService.update("users", profileData, "uid", currentUserId)
                            result.fold(
                                onSuccess = {
                                    userLastProfileUri = imageUrl
                                    addToProfileHistory(imageUrl, "url")
                                    loadingDialog(false)
                                    SketchwareUtil.showMessage(applicationContext, "Profile photo updated")
                                },
                                onFailure = { error ->
                                    loadingDialog(false)
                                    SketchwareUtil.showMessage(applicationContext, error.message ?: "Upload failed")
                                }
                            )
                        },
                        onFailure = { error ->
                            loadingDialog(false)
                            SketchwareUtil.showMessage(applicationContext, "Upload failed: ${error.message}")
                        }
                    )
                } catch (e: Exception) {
                    loadingDialog(false)
                    SketchwareUtil.showMessage(applicationContext, "Error: ${e.message}")
                }
            }
        }
    }

    private fun handleCoverImageSelection(data: Intent) {
        val filePaths = getFilePathsFromIntent(data)
        if (filePaths.isNotEmpty()) {
            loadingDialog(true)
            profileCoverImage.setImageBitmap(FileUtil.decodeSampleBitmapFromPath(filePaths[0], 1024, 1024))
            
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val currentUserId = authService.getCurrentUserId() ?: return@launch
                    
                    // Upload to Supabase Storage
                    val uploadResult = storageService.uploadCover(currentUserId, filePaths[0])
                    
                    uploadResult.fold(
                        onSuccess = { imageUrl ->
                            val profileData = mapOf("profile_cover_image" to imageUrl)
                            val result = databaseService.update("users", profileData, "uid", currentUserId)
                            
                            result.fold(
                                onSuccess = {
                                    userLastCoverUri = imageUrl
                                    addToCoverHistory(imageUrl, "url")
                                    loadingDialog(false)
                                    SketchwareUtil.showMessage(applicationContext, "Cover photo updated")
                                },
                                onFailure = { error ->
                                    loadingDialog(false)
                                    SketchwareUtil.showMessage(applicationContext, error.message ?: "Upload failed")
                                }
                            )
                        },
                        onFailure = { error ->
                            loadingDialog(false)
                            SketchwareUtil.showMessage(applicationContext, "Upload failed: ${error.message}")
                        }
                    )
                } catch (e: Exception) {
                    loadingDialog(false)
                    SketchwareUtil.showMessage(applicationContext, "Error: ${e.message}")
                }
            }
        }
    }

    private fun getFilePathsFromIntent(data: Intent): List<String> {
        val filePaths = mutableListOf<String>()
        
        if (data.clipData != null) {
            for (i in 0 until data.clipData!!.itemCount) {
                val item = data.clipData!!.getItemAt(i)
                FileUtil.convertUriToFilePath(applicationContext, item.uri)?.let { filePaths.add(it) }
            }
        } else if (data.data != null) {
            FileUtil.convertUriToFilePath(applicationContext, data.data!!)?.let { filePaths.add(it) }
        }
        
        return filePaths
    }

    private fun addToProfileHistory(imageUrl: String, type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUserId = authService.getCurrentUserId() ?: return@launch
                val historyKey = UUID.randomUUID().toString()
                
                val historyData = mapOf(
                    "key" to historyKey,
                    "user_id" to currentUserId,
                    "image_url" to imageUrl.trim(),
                    "upload_date" to cc.timeInMillis.toString(),
                    "type" to type
                )
                
                databaseService.insert("profile_history", historyData)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    private fun addToCoverHistory(imageUrl: String, type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUserId = authService.getCurrentUserId() ?: return@launch
                val historyKey = UUID.randomUUID().toString()
                
                val historyData = mapOf(
                    "key" to historyKey,
                    "user_id" to currentUserId,
                    "image_url" to imageUrl.trim(),
                    "upload_date" to cc.timeInMillis.toString(),
                    "type" to type
                )
                
                databaseService.insert("cover_image_history", historyData)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    override fun onResume() {
        super.onResume()
        getUserReference()
    }

    override fun onBackPressed() {
        finish()
    }



    private fun loadingDialog(visibility: Boolean) {
        if (visibility) {
            if (synapseLoadingDialog == null) {
                synapseLoadingDialog = ProgressDialog(this).apply {
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                }
            }
            synapseLoadingDialog?.show()
            synapseLoadingDialog?.setContentView(R.layout.loading_synapse)
        } else {
            synapseLoadingDialog?.dismiss()
        }
    }
}