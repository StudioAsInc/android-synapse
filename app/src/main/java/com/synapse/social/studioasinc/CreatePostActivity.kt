package com.synapse.social.studioasinc

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.synapse.social.studioasinc.adapter.SelectedMediaAdapter
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.databinding.ActivityCreatePostBinding
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.util.FileUtil
import com.synapse.social.studioasinc.util.MediaUploadManager
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private val authService = SupabaseAuthenticationService()
    private val postRepository = PostRepository()
    private lateinit var prefs: SharedPreferences

    // Media
    private val selectedMedia = mutableListOf<MediaItem>()
    private lateinit var mediaAdapter: SelectedMediaAdapter

    // Post data
    private var postVisibility = "public"
    private var pollData: PollData? = null
    private var locationData: LocationData? = null
    private var youtubeUrl: String? = null
    private val mentions = mutableListOf<String>()
    private val hashtags = mutableListOf<String>()

    // Settings
    private var hideViewsCount = false
    private var hideLikeCount = false
    private var hideCommentsCount = false
    private var disableComments = false

    // Edit mode
    private var editPostId: String? = null

    data class PollData(
        val question: String,
        val options: List<String>,
        val durationHours: Int
    )

    data class LocationData(
        val name: String,
        val address: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null
    )

    companion object {
        private const val MAX_CHARS = 500
        private const val MAX_MEDIA = 10
        private const val MAX_POLL_OPTIONS = 4
        private const val DRAFT_KEY = "post_draft"
    }

    private val mediaLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        handleMediaSelection(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = getSharedPreferences("synapse_prefs", MODE_PRIVATE)

        setupToolbar()
        setupMediaRecycler()
        setupTextInput()
        setupActionButtons()
        loadUserInfo()
        restoreDraft()

        // Check for edit mode
        intent.getStringExtra("edit_post_id")?.let { loadPostForEdit(it) }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { confirmExit() }
        binding.postButton.setOnClickListener { submitPost() }
    }

    private fun setupMediaRecycler() {
        mediaAdapter = SelectedMediaAdapter(selectedMedia) { position ->
            selectedMedia.removeAt(position)
            mediaAdapter.notifyItemRemoved(position)
            updateMediaPreview()
            updatePostButtonState()
        }
        binding.mediaRecyclerView.apply {
            layoutManager = GridLayoutManager(this@CreatePostActivity, 3)
            adapter = mediaAdapter
        }
    }

    private fun setupTextInput() {
        binding.postContentInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                val remaining = MAX_CHARS - text.length

                // Show warning when near limit
                binding.charCountWarning.apply {
                    isVisible = remaining <= 50
                    text = if (remaining < 0) "${-remaining} characters over limit" else "$remaining characters remaining"
                    setTextColor(if (remaining < 0) getColor(android.R.color.holo_red_dark) else getColor(android.R.color.darker_gray))
                }

                // Extract mentions and hashtags
                extractMentionsAndHashtags(text)
                updatePostButtonState()
            }
        })
    }

    private fun extractMentionsAndHashtags(text: String) {
        mentions.clear()
        hashtags.clear()

        Regex("@(\\w+)").findAll(text).forEach { mentions.add(it.groupValues[1]) }
        Regex("#(\\w+)").findAll(text).forEach { hashtags.add(it.groupValues[1].lowercase()) }
    }

    private fun setupActionButtons() {
        binding.addMediaButton.setOnClickListener {
            if (pollData != null) {
                Toast.makeText(this, "Remove poll to add media", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (checkMediaPermission()) selectMedia()
        }

        binding.addPollButton.setOnClickListener {
            if (selectedMedia.isNotEmpty()) {
                Toast.makeText(this, "Remove media to add poll", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showPollCreator()
        }

        binding.addYoutubeButton.setOnClickListener { showYoutubeDialog() }
        binding.addLocationButton.setOnClickListener { showLocationPicker() }
        binding.settingsButton.setOnClickListener { showSettingsSheet() }
        binding.privacyChip.setOnClickListener { showPrivacyPicker() }

        binding.removeAllMediaButton.setOnClickListener {
            selectedMedia.clear()
            mediaAdapter.notifyDataSetChanged()
            updateMediaPreview()
            updatePostButtonState()
        }

        binding.removePollButton.setOnClickListener {
            pollData = null
            binding.pollPreviewCard.isVisible = false
            updatePostButtonState()
        }

        binding.removeYoutubeButton.setOnClickListener {
            youtubeUrl = null
            binding.youtubePreviewCard.isVisible = false
            updatePostButtonState()
        }

        binding.removeLocationButton.setOnClickListener {
            locationData = null
            binding.locationPreviewCard.isVisible = false
        }
    }

    private fun loadUserInfo() {
        lifecycleScope.launch {
            authService.getCurrentUser()?.let { user ->
                binding.authorName.text = user.email ?: "You"
                // TODO: Load user profile image from users table
            }
        }
    }

    private fun updatePostButtonState() {
        val hasContent = binding.postContentInput.text?.isNotBlank() == true
        val hasMedia = selectedMedia.isNotEmpty()
        val hasPoll = pollData != null
        val hasYoutube = youtubeUrl != null
        val withinLimit = (binding.postContentInput.text?.length ?: 0) <= MAX_CHARS

        binding.postButton.isEnabled = (hasContent || hasMedia || hasPoll || hasYoutube) && withinLimit
    }

    private fun updateMediaPreview() {
        binding.mediaPreviewCard.isVisible = selectedMedia.isNotEmpty()
        binding.mediaCountLabel.text = "Media (${selectedMedia.size}/$MAX_MEDIA)"
    }

    // ==================== MEDIA ====================

    private fun checkMediaPermission(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 1000)
            false
        } else true
    }

    private fun selectMedia() {
        if (selectedMedia.size >= MAX_MEDIA) {
            Toast.makeText(this, "Maximum $MAX_MEDIA files allowed", Toast.LENGTH_SHORT).show()
            return
        }
        mediaLauncher.launch("*/*")
    }

    private fun handleMediaSelection(uris: List<Uri>) {
        val remaining = MAX_MEDIA - selectedMedia.size
        uris.take(remaining).forEach { uri ->
            val mimeType = contentResolver.getType(uri) ?: return@forEach
            val type = if (mimeType.startsWith("video")) MediaType.VIDEO else MediaType.IMAGE
            FileUtil.convertUriToFilePath(this, uri)?.let { path ->
                selectedMedia.add(MediaItem(url = path, type = type))
            }
        }
        mediaAdapter.notifyDataSetChanged()
        updateMediaPreview()
        updatePostButtonState()
    }

    // ==================== POLL ====================

    private fun showPollCreator() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_poll_creator, null)
        val questionInput = dialogView.findViewById<TextInputEditText>(R.id.pollQuestionInput)
        val optionsContainer = dialogView.findViewById<LinearLayout>(R.id.pollOptionsContainer)
        val addOptionButton = dialogView.findViewById<View>(R.id.addOptionButton)

        // Add initial 2 options
        repeat(2) { addPollOptionView(optionsContainer, it) }

        addOptionButton.setOnClickListener {
            if (optionsContainer.childCount < MAX_POLL_OPTIONS) {
                addPollOptionView(optionsContainer, optionsContainer.childCount)
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Create Poll")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val question = questionInput.text?.toString()?.trim() ?: ""
                val options = (0 until optionsContainer.childCount).mapNotNull {
                    optionsContainer.getChildAt(it).findViewById<EditText>(R.id.optionInput)?.text?.toString()?.trim()
                }.filter { it.isNotEmpty() }

                if (question.isEmpty()) {
                    Toast.makeText(this, "Enter a question", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (options.size < 2) {
                    Toast.makeText(this, "Add at least 2 options", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                pollData = PollData(question, options, 24)
                showPollPreview()
                updatePostButtonState()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addPollOptionView(container: LinearLayout, index: Int) {
        val view = layoutInflater.inflate(R.layout.item_poll_option_input, container, false)
        view.findViewById<TextInputEditText>(R.id.optionInput).hint = "Option ${index + 1}"
        container.addView(view)
    }

    private fun showPollPreview() {
        pollData?.let { poll ->
            binding.pollPreviewCard.isVisible = true
            binding.pollQuestionPreview.text = poll.question
            binding.pollDurationLabel.text = "Duration: ${poll.durationHours} hours"

            binding.pollOptionsContainer.removeAllViews()
            poll.options.forEach { option ->
                val chip = Chip(this).apply {
                    text = option
                    isClickable = false
                    setChipBackgroundColorResource(R.color.surface_container)
                }
                binding.pollOptionsContainer.addView(chip)
            }
        }
    }

    // ==================== YOUTUBE ====================

    private fun showYoutubeDialog() {
        val input = EditText(this).apply {
            hint = "https://youtube.com/watch?v=..."
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Add YouTube Video")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val url = input.text.toString().trim()
                if (isValidYoutubeUrl(url)) {
                    youtubeUrl = url
                    showYoutubePreview()
                    updatePostButtonState()
                } else {
                    Toast.makeText(this, "Invalid YouTube URL", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isValidYoutubeUrl(url: String): Boolean {
        return url.matches(Regex("^(https?://)?(www\\.)?(youtube\\.com/watch\\?v=|youtu\\.be/)[a-zA-Z0-9_-]{11}.*"))
    }

    private fun extractYoutubeId(url: String): String? {
        val match = Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})").find(url)
        return match?.groupValues?.get(1)
    }

    private fun showYoutubePreview() {
        youtubeUrl?.let { url ->
            extractYoutubeId(url)?.let { videoId ->
                binding.youtubePreviewCard.isVisible = true
                binding.youtubeWebView.settings.javaScriptEnabled = true
                binding.youtubeWebView.loadUrl("https://www.youtube.com/embed/$videoId")
            }
        }
    }

    // ==================== LOCATION ====================

    private fun showLocationPicker() {
        val input = EditText(this).apply {
            hint = "Enter location name"
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Location")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    locationData = LocationData(name)
                    showLocationPreview()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLocationPreview() {
        locationData?.let { loc ->
            binding.locationPreviewCard.isVisible = true
            binding.locationName.text = loc.name
            binding.locationAddress.text = loc.address ?: ""
            binding.locationAddress.isVisible = !loc.address.isNullOrEmpty()
        }
    }

    // ==================== PRIVACY ====================

    private fun showPrivacyPicker() {
        val options = arrayOf("Public", "Followers Only", "Private")
        val icons = arrayOf(R.drawable.ic_public, R.drawable.ic_people, R.drawable.ic_lock)

        MaterialAlertDialogBuilder(this)
            .setTitle("Post Visibility")
            .setItems(options) { _, which ->
                postVisibility = when (which) {
                    0 -> "public"
                    1 -> "followers"
                    else -> "private"
                }
                binding.privacyChip.text = options[which]
                binding.privacyChip.setChipIconResource(icons[which])
            }
            .show()
    }

    // ==================== SETTINGS ====================

    private fun showSettingsSheet() {
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.sheet_post_settings, null)

        view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.hideViewsSwitch)?.apply {
            isChecked = hideViewsCount
            setOnCheckedChangeListener { _, checked -> hideViewsCount = checked }
        }
        view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.hideLikesSwitch)?.apply {
            isChecked = hideLikeCount
            setOnCheckedChangeListener { _, checked -> hideLikeCount = checked }
        }
        view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.hideCommentsSwitch)?.apply {
            isChecked = hideCommentsCount
            setOnCheckedChangeListener { _, checked -> hideCommentsCount = checked }
        }
        view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.disableCommentsSwitch)?.apply {
            isChecked = disableComments
            setOnCheckedChangeListener { _, checked -> disableComments = checked }
        }

        sheet.setContentView(view)
        sheet.show()
    }

    // ==================== SUBMIT ====================

    private fun submitPost() {
        val text = binding.postContentInput.text?.toString()?.trim() ?: ""
        val currentUser = authService.getCurrentUser() ?: run {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        val postKey = "post_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val timestamp = System.currentTimeMillis()
        val publishDate = Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        // Determine post type
        val postType = when {
            selectedMedia.any { it.type == MediaType.VIDEO } -> "VIDEO"
            selectedMedia.isNotEmpty() -> "IMAGE"
            pollData != null -> "POLL"
            else -> "TEXT"
        }

        val post = Post(
            id = editPostId ?: UUID.randomUUID().toString(),
            key = postKey,
            authorUid = currentUser.id,
            postText = text.ifEmpty { null },
            postType = postType,
            postVisibility = postVisibility,
            postHideViewsCount = if (hideViewsCount) "true" else "false",
            postHideLikeCount = if (hideLikeCount) "true" else "false",
            postHideCommentsCount = if (hideCommentsCount) "true" else "false",
            postDisableComments = if (disableComments) "true" else "false",
            publishDate = publishDate,
            timestamp = timestamp,
            youtubeUrl = youtubeUrl,
            hasPoll = pollData != null,
            pollQuestion = pollData?.question,
            pollOptions = pollData?.options?.map { mapOf("text" to it, "votes" to 0) },
            hasLocation = locationData != null,
            locationName = locationData?.name,
            locationAddress = locationData?.address,
            locationLatitude = locationData?.latitude,
            locationLongitude = locationData?.longitude
        )

        if (selectedMedia.isEmpty()) {
            savePost(post)
        } else {
            uploadMediaAndSave(post)
        }
    }

    private fun uploadMediaAndSave(post: Post) {
        lifecycleScope.launch {
            try {
                MediaUploadManager.uploadMultipleMedia(
                    selectedMedia,
                    onProgress = { progress ->
                        runOnUiThread {
                            binding.uploadProgress.isVisible = true
                            binding.uploadProgress.progress = (progress * 100).toInt()
                        }
                    },
                    onComplete = { uploaded ->
                        val updatedPost = post.copy().apply {
                            mediaItems = uploaded.toMutableList()
                            postImage = uploaded.firstOrNull { it.type == MediaType.IMAGE }?.url
                        }
                        savePost(updatedPost)
                    },
                    onError = { error ->
                        runOnUiThread {
                            setLoading(false)
                            Toast.makeText(this@CreatePostActivity, "Upload failed: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            } catch (e: Exception) {
                runOnUiThread {
                    setLoading(false)
                    Toast.makeText(this@CreatePostActivity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun savePost(post: Post) {
        lifecycleScope.launch {
            postRepository.createPost(post)
                .onSuccess {
                    clearDraft()
                    runOnUiThread {
                        setLoading(false)
                        Toast.makeText(this@CreatePostActivity, "Post created!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .onFailure { e ->
                    runOnUiThread {
                        setLoading(false)
                        Toast.makeText(this@CreatePostActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.postButton.isEnabled = !loading
        binding.postButton.text = if (loading) "Posting..." else "Post"
        binding.uploadProgress.isVisible = loading
    }

    // ==================== DRAFT ====================

    override fun onPause() {
        super.onPause()
        saveDraft()
    }

    private fun saveDraft() {
        val text = binding.postContentInput.text?.toString() ?: ""
        if (text.isNotEmpty()) {
            prefs.edit().putString(DRAFT_KEY, text).apply()
        }
    }

    private fun restoreDraft() {
        prefs.getString(DRAFT_KEY, null)?.let { draft ->
            if (draft.isNotEmpty()) {
                binding.postContentInput.setText(draft)
                Toast.makeText(this, "Draft restored", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearDraft() {
        prefs.edit().remove(DRAFT_KEY).apply()
    }

    private fun confirmExit() {
        val hasContent = binding.postContentInput.text?.isNotBlank() == true || selectedMedia.isNotEmpty()
        if (hasContent) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Discard post?")
                .setMessage("Your draft will be saved")
                .setPositiveButton("Discard") { _, _ -> finish() }
                .setNegativeButton("Keep editing", null)
                .show()
        } else {
            finish()
        }
    }

    // ==================== EDIT MODE ====================

    private fun loadPostForEdit(postId: String) {
        editPostId = postId
        binding.toolbar.title = "Edit Post"
        binding.postButton.text = "Update"

        lifecycleScope.launch {
            postRepository.getPost(postId).onSuccess { post ->
                post?.let {
                    runOnUiThread {
                        binding.postContentInput.setText(it.postText)
                        postVisibility = it.postVisibility ?: "public"
                        updatePrivacyChip()
                    }
                }
            }
        }
    }

    private fun updatePrivacyChip() {
        val (text, icon) = when (postVisibility) {
            "followers" -> "Followers" to R.drawable.ic_people
            "private" -> "Private" to R.drawable.ic_lock
            else -> "Public" to R.drawable.ic_public
        }
        binding.privacyChip.text = text
        binding.privacyChip.setChipIconResource(icon)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            selectMedia()
        }
    }
}
