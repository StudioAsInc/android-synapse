package com.synapse.social.studioasinc

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.synapse.social.studioasinc.adapter.SelectedMediaAdapter
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.toHashMap
import com.synapse.social.studioasinc.util.MediaUploadManager
import java.util.*

class CreatePostActivity : AppCompatActivity() {

    // UI Components
    private lateinit var toolbar: Toolbar
    private lateinit var backButton: ImageView
    private lateinit var publishButton: Button
    private lateinit var postDescriptionEditText: EditText
    private lateinit var selectedMediaRecyclerView: RecyclerView
    private lateinit var addMediaButton: LinearLayout
    private lateinit var addPhotoIcon: ImageView
    private lateinit var addVideoIcon: ImageView
    private lateinit var postSettingsLayout: LinearLayout
    private lateinit var hideViewsCountSwitch: MaterialSwitch
    private lateinit var hideLikeCountSwitch: MaterialSwitch
    private lateinit var hideCommentsCountSwitch: MaterialSwitch
    private lateinit var disableCommentsSwitch: MaterialSwitch
    private lateinit var postVisibilitySpinner: Spinner

    // Data
    private val selectedMediaItems = mutableListOf<MediaItem>()
    private lateinit var selectedMediaAdapter: SelectedMediaAdapter
    private var progressDialog: androidx.appcompat.app.AlertDialog? = null
    private var progressBar: ProgressBar? = null
    private var progressPercentage: TextView? = null
    
    // Firebase
    private val firebase = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val postsRef = firebase.getReference("skyline/posts")
    
    // Media selection
    private val selectImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        handleSelectedImages(uris)
    }
    
    private val selectVideoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleSelectedVideo(it) }
    }
    
    companion object {
        private const val MAX_IMAGES = 10
        private const val MAX_VIDEOS = 1
        private const val PERMISSION_REQUEST_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post_multi)
        
        initialize()
        initializeLogic()
    }

    private fun initialize() {
        // Find views
        toolbar = findViewById(R.id.toolbar)
        backButton = findViewById(R.id.backButton)
        publishButton = findViewById(R.id.publishButton)
        postDescriptionEditText = findViewById(R.id.postDescriptionEditText)
        selectedMediaRecyclerView = findViewById(R.id.selectedMediaRecyclerView)
        addMediaButton = findViewById(R.id.addMediaButton)
        addPhotoIcon = findViewById(R.id.addPhotoIcon)
        addVideoIcon = findViewById(R.id.addVideoIcon)
        postSettingsLayout = findViewById(R.id.postSettingsLayout)
        hideViewsCountSwitch = findViewById(R.id.hideViewsCountSwitch)
        hideLikeCountSwitch = findViewById(R.id.hideLikeCountSwitch)
        hideCommentsCountSwitch = findViewById(R.id.hideCommentsCountSwitch)
        disableCommentsSwitch = findViewById(R.id.disableCommentsSwitch)
        postVisibilitySpinner = findViewById(R.id.postVisibilitySpinner)
        
        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        // Setup RecyclerView
        selectedMediaAdapter = SelectedMediaAdapter(selectedMediaItems) { position ->
            removeMedia(position)
        }
        selectedMediaRecyclerView.apply {
            layoutManager = GridLayoutManager(this@CreatePostActivity, 3)
            adapter = selectedMediaAdapter
        }
        
        // Setup visibility spinner
        val visibilityOptions = arrayOf("Public", "Private")
        postVisibilitySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            visibilityOptions
        )
        
        // Setup click listeners
        backButton.setOnClickListener { finish() }
        publishButton.setOnClickListener { createPost() }
        addPhotoIcon.setOnClickListener { selectImages() }
        addVideoIcon.setOnClickListener { selectVideo() }

        val userMention = UserMention(postDescriptionEditText)
        postDescriptionEditText.addTextChangedListener(userMention)
        
        // Initially hide media recycler if empty
        updateMediaVisibility()
    }

    private fun initializeLogic() {
        // Check if we have media from intent
        intent.getStringExtra("path")?.let { path ->
            val type = intent.getStringExtra("type")
            if (type == "image") {
                selectedMediaItems.add(MediaItem(url = path, type = MediaType.IMAGE))
                updateMediaVisibility()
            }
        }
    }

    private fun selectImages() {
        if (checkMediaPermission()) {
            val remainingSlots = MAX_IMAGES - selectedMediaItems.count { it.type == MediaType.IMAGE }
            if (remainingSlots > 0) {
                selectImagesLauncher.launch("image/*")
            } else {
                Toast.makeText(this, "Maximum $MAX_IMAGES images allowed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun selectVideo() {
        if (checkMediaPermission()) {
            val videoCount = selectedMediaItems.count { it.type == MediaType.VIDEO }
            if (videoCount < MAX_VIDEOS) {
                selectVideoLauncher.launch("video/*")
            } else {
                Toast.makeText(this, "Maximum $MAX_VIDEOS video allowed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkMediaPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_IMAGES and READ_MEDIA_VIDEO
            val imagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
            val videoPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
            
            if (imagePermission != PackageManager.PERMISSION_GRANTED || 
                videoPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO),
                    PERMISSION_REQUEST_CODE
                )
                false
            } else {
                true
            }
        } else {
            // Android 12 and below
            val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
                false
            } else {
                true
            }
        }
    }

    private fun handleSelectedImages(uris: List<Uri>) {
        val remainingSlots = MAX_IMAGES - selectedMediaItems.count { it.type == MediaType.IMAGE }
        val itemsToAdd = minOf(uris.size, remainingSlots)
        
        for (i in 0 until itemsToAdd) {
            val path = FileUtil.convertUriToFilePath(this, uris[i])
            if (path != null) {
                selectedMediaItems.add(MediaItem(url = path, type = MediaType.IMAGE))
            }
        }
        
        updateMediaVisibility()
        
        if (uris.size > itemsToAdd) {
            Toast.makeText(
                this, 
                "Only $itemsToAdd images added. Maximum $MAX_IMAGES images allowed", 
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleSelectedVideo(uri: Uri) {
        val path = FileUtil.convertUriToFilePath(this, uri)
        if (path != null) {
            selectedMediaItems.add(MediaItem(url = path, type = MediaType.VIDEO))
            updateMediaVisibility()
        }
    }

    private fun removeMedia(position: Int) {
        selectedMediaItems.removeAt(position)
        selectedMediaAdapter.notifyItemRemoved(position)
        updateMediaVisibility()
    }

    private fun updateMediaVisibility() {
        selectedMediaRecyclerView.visibility = if (selectedMediaItems.isEmpty()) View.GONE else View.VISIBLE
        
        // Update add buttons based on limits
        val imageCount = selectedMediaItems.count { it.type == MediaType.IMAGE }
        val videoCount = selectedMediaItems.count { it.type == MediaType.VIDEO }
        
        addPhotoIcon.alpha = if (imageCount >= MAX_IMAGES) 0.5f else 1f
        addVideoIcon.alpha = if (videoCount >= MAX_VIDEOS) 0.5f else 1f
    }

    private fun createPost() {
        val postText = postDescriptionEditText.text.toString().trim()
        
        if (postText.isEmpty() && selectedMediaItems.isEmpty()) {
            Toast.makeText(this, "Please add some text or media to your post", Toast.LENGTH_SHORT).show()
            return
        }
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        
        showLoading(true)
        
        // Create post object
        val postKey = postsRef.push().key ?: return
        val post = Post(
            key = postKey,
            uid = currentUser.uid,
            postText = if (postText.isNotEmpty()) postText else null,
            postHideViewsCount = if (hideViewsCountSwitch.isChecked) "true" else "false",
            postHideLikeCount = if (hideLikeCountSwitch.isChecked) "true" else "false",
            postHideCommentsCount = if (hideCommentsCountSwitch.isChecked) "true" else "false",
            postDisableComments = if (disableCommentsSwitch.isChecked) "true" else "false",
            postVisibility = if (postVisibilitySpinner.selectedItemPosition == 0) "public" else "private",
            publishDate = System.currentTimeMillis().toString()
        )
        
        if (selectedMediaItems.isEmpty()) {
            // Text-only post
            post.postType = "TEXT"
            savePostToDatabase(post)
        } else {
            // Upload media first
            uploadMediaAndSavePost(post)
        }
    }

    private fun uploadMediaAndSavePost(post: Post) {
        MediaUploadManager.uploadMultipleMedia(
            selectedMediaItems,
            onProgress = { progress ->
                runOnUiThread {
                    val progressInt = (progress * 100).toInt()
                    progressBar?.progress = progressInt
                    progressPercentage?.text = "$progressInt%"
                }
            },
            onComplete = { uploadedItems ->
                post.mediaItems = uploadedItems.toMutableList()
                post.determinePostType()
                
                // Set legacy image field for backward compatibility
                uploadedItems.firstOrNull { it.type == MediaType.IMAGE }?.let {
                    post.postImage = it.url
                }
                
                savePostToDatabase(post)
            },
            onError = { error ->
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this, "Failed to upload media: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun savePostToDatabase(post: Post) {
        postsRef.child(post.key).setValue(post.toHashMap())
            .addOnSuccessListener {
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this, "Post created successfully!", Toast.LENGTH_SHORT).show()
                    handleMentions(post.postText, post.key)
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this, "Failed to create post: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun handleMentions(text: String?, postKey: String) {
        com.synapse.social.studioasinc.util.MentionUtils.sendMentionNotifications(text, postKey, null, "post")
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_progress, null)
            progressBar = dialogView.findViewById(R.id.progressBar)
            progressPercentage = dialogView.findViewById(R.id.progressPercentage)

            progressDialog = MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()
                .apply {
                    show()
                }
        } else {
            progressDialog?.dismiss()
            progressDialog = null
            progressBar = null
            progressPercentage = null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permission granted, user can now select media
                Toast.makeText(this, "Permission granted. You can now select media.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

