package com.synapse.social.studioasinc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
// import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.databinding.ActivityProfileBinding
import com.synapse.social.studioasinc.databinding.DpPreviewBinding
import com.synapse.social.studioasinc.model.Post
import android.util.Log
import com.bumptech.glide.Glide

import io.noties.markwon.Markwon
import java.util.Calendar
import java.util.HashMap

/**
 * Activity for displaying a user's profile.
 *
 * This activity is responsible for displaying a user's profile information,
 * including their posts, followers, and other details. It follows the MVVM
 * architecture pattern, with a [ProfileViewModel] handling the data and
 * business logic.
 */
class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var viewModel: ProfileViewModel
    private lateinit var postAdapter: PostsAdapter
    private lateinit var markwon: Markwon

    companion object {
        private const val TAG = "ProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Lifecycle: onCreate")
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        // Initialize Markwon
        markwon = Markwon.create(this)

        // Get user ID from intent
        val userId = intent.getStringExtra("uid") ?: return
        val currentUser = SupabaseClient.client.auth.currentUserOrNull()
        val currentUid = currentUser?.id ?: ""

        // Load user profile
        loadUserProfile(userId, currentUid)

        // Fetch initial states
        viewModel.fetchInitialFollowState(userId)
        viewModel.fetchInitialProfileLikeState(userId)

        // Observe user profile data
        observeUserProfile()

        // Observe user posts
        observeUserPosts(userId)

        // Observe UI feedback
        observeUIFeedback()

        // Setup UI listeners
        setupUIListeners(userId, currentUid)
    }

    /**
     * Loads the user profile information using the ViewModel.
     *
     * @param userId The ID of the user whose profile is to be loaded.
     * @param currentUid The ID of the current user.
     */
    private fun loadUserProfile(userId: String, currentUid: String) {
        viewModel.loadUserProfile(userId)
        viewModel.getUserPosts(userId)
    }

    /**
     * Observes the user's posts from the [ProfileViewModel] and updates the UI.
     *
     * @param userId The ID of the user whose posts are to be observed.
     */
    private fun observeUserPosts(userId: String) {
        postAdapter = PostsAdapter(
            context = this,
            lifecycleOwner = this,
            markwon = markwon,
            onLikeClicked = { post -> viewModel.togglePostLike(post.id) },
            onCommentClicked = { post -> showCommentsDialog(post) },
            onShareClicked = { post -> sharePost(post) },
            onMoreOptionsClicked = { post -> showMoreOptionsDialog(post) },
            onFavoriteClicked = { post -> viewModel.toggleFavorite(post.id) },
            onUserClicked = { uid ->
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("uid", uid)
                startActivity(intent)
            }
        )
        
        // Set up RecyclerView with layout manager
        binding.ProfilePageTabUserPostsRecyclerView.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@ProfileActivity)
            adapter = postAdapter
        }

        viewModel.userPosts.observe(this) { state ->
            when (state) {
                is ProfileViewModel.State.Loading -> {
                    binding.ProfilePageLoadingBody.visibility = View.VISIBLE
                    binding.ProfilePageSwipeLayout.visibility = View.GONE
                    binding.ProfilePageNoInternetBody.visibility = View.GONE
                }
                is ProfileViewModel.State.Success -> {
                    binding.ProfilePageLoadingBody.visibility = View.GONE
                    binding.ProfilePageSwipeLayout.visibility = View.VISIBLE
                    binding.ProfilePageNoInternetBody.visibility = View.GONE
                    postAdapter.submitList(state.data)
                }
                is ProfileViewModel.State.Error -> {
                    binding.ProfilePageLoadingBody.visibility = View.GONE
                    binding.ProfilePageSwipeLayout.visibility = View.GONE
                    binding.ProfilePageNoInternetBody.visibility = View.VISIBLE
                    android.util.Log.e("ProfileActivity", "Posts load error: ${state.message}")
                    Toast.makeText(this, "Posts error: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
            binding.ProfilePageSwipeLayout.isRefreshing = false
        }
        viewModel.getUserPosts(userId)
    }

    private fun showCommentsDialog(post: Post) {
        val intent = Intent(this, PostDetailActivity::class.java).apply {
            putExtra("postKey", post.key)
            putExtra("postPublisherUID", post.authorUid)
        }
        startActivity(intent)
    }

    private fun showMoreOptionsDialog(post: Post) {
        val currentUser = SupabaseClient.client.auth.currentUserOrNull()
        val currentUid = currentUser?.id
        val isOwnPost = post.authorUid == currentUid
        
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val rootView = layoutInflater.inflate(R.layout.bottom_sheet_post_options, null)
        bottomSheet.setContentView(rootView)
        
        val recyclerView = rootView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.optionsRecyclerView)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        
        val items = buildPostMenuItems(post, isOwnPost)
        recyclerView.adapter = com.synapse.social.studioasinc.adapters.PostOptionsAdapter(items)
        
        bottomSheet.show()
    }
    
    private fun buildPostMenuItems(post: Post, isOwner: Boolean): List<com.synapse.social.studioasinc.model.PostActionItem> {
        val items = mutableListOf<com.synapse.social.studioasinc.model.PostActionItem>()
        
        if (isOwner) {
            items.add(com.synapse.social.studioasinc.model.PostActionItem("Edit", R.drawable.ic_edit_note_48px) { 
                editPost(post)
            })
            items.add(com.synapse.social.studioasinc.model.PostActionItem("Delete", R.drawable.ic_delete_48px, true) { 
                deletePost(post)
            })
            items.add(com.synapse.social.studioasinc.model.PostActionItem("Statistics", R.drawable.data_usage_24px) { 
                showPostStatistics(post)
            })
        } else {
            items.add(com.synapse.social.studioasinc.model.PostActionItem("Report", R.drawable.ic_report_48px, true) { 
                reportPost(post)
            })
            items.add(com.synapse.social.studioasinc.model.PostActionItem("Hide", R.drawable.mobile_block_24px) { 
                hidePost(post)
            })
        }
        
        items.add(com.synapse.social.studioasinc.model.PostActionItem("Copy Link", R.drawable.ic_content_copy_48px) { 
            copyPostLink(post)
        })
        
        return items
    }

    private fun showFollowOptionsDialog(userId: String) {
        val options = arrayOf("Followers", "Following")
        AlertDialog.Builder(this)
            .setTitle("View")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Show followers
                        val intent = Intent(this, FollowListActivity::class.java)
                        intent.putExtra(FollowListActivity.EXTRA_USER_ID, userId)
                        intent.putExtra(FollowListActivity.EXTRA_LIST_TYPE, FollowListActivity.TYPE_FOLLOWERS)
                        startActivity(intent)
                    }
                    1 -> {
                        // Show following
                        val intent = Intent(this, FollowListActivity::class.java)
                        intent.putExtra(FollowListActivity.EXTRA_USER_ID, userId)
                        intent.putExtra(FollowListActivity.EXTRA_LIST_TYPE, FollowListActivity.TYPE_FOLLOWING)
                        startActivity(intent)
                    }
                }
            }
            .show()
    }



    /**
     * Observes the user profile data from the ViewModel and updates the UI.
     */
    private fun observeUserProfile() {
        viewModel.userProfile.observe(this) { state ->
            when (state) {
                is ProfileViewModel.State.Loading -> {
                    // Show loading state if needed
                }
                is ProfileViewModel.State.Success -> {
                    val user = state.data
                    
                    // Update UI with user data
                    binding.ProfilePageTabUserInfoNickname.text = user.displayName ?: user.username ?: "Unknown"
                    binding.ProfilePageTabUserInfoBioLayoutText.text = user.bio ?: "No bio available"
                    binding.ProfilePageTabUserInfoUsername.text = "@${user.username ?: "unknown"}"
                    binding.ProfilePageTabUserInfoStatus.text = user.status ?: "offline"
                    binding.userUidLayoutText.text = user.uid
                    
                    // Format and display creation date
                    val creationDate = user.joinDate ?: user.createdAt
                    if (creationDate != null) {
                        binding.joinDateLayoutText.text = formatDate(creationDate)
                    } else {
                        binding.joinDateLayoutText.text = "Unknown"
                    }
                    
                    // Update follower counts
                    binding.ProfilePageTabUserInfoFollowersCount.text = "${user.followersCount} followers"
                    binding.ProfilePageTabUserInfoFollowingCount.text = "${user.followingCount} following"
                    
                    // Load profile image if available
                    user.profileImageUrl?.let { imageUrl ->
                        com.bumptech.glide.Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ph_imgbluredsqure)
                            .into(binding.ProfilePageTabUserInfoProfileImage)
                    }
                    
                    // Load cover image if available
                    user.profileCoverImage?.let { coverUrl ->
                        com.bumptech.glide.Glide.with(this)
                            .load(coverUrl)
                            .placeholder(R.drawable.user_null_cover_photo)
                            .into(binding.ProfilePageTabUserInfoCoverImage)
                    }
                }
                is ProfileViewModel.State.Error -> {
                    // Handle error state
                    binding.joinDateLayoutText.text = "Error loading data"
                    android.util.Log.e("ProfileActivity", "Profile load error: ${state.message}")
                    Toast.makeText(this, "Profile error: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Formats a date string for display
     */
    private fun formatDate(dateString: String): String {
        return try {
            // Try to parse as timestamp (milliseconds) first
            val timestamp = dateString.toLongOrNull()
            if (timestamp != null) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = timestamp
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, java.util.Locale.getDefault())
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                "Joined $month $day, $year"
            } else {
                // Try to parse as ISO timestamp (e.g., "2025-10-24 15:06:24.382365")
                val parts = dateString.split(" ")[0].split("-")
                if (parts.size >= 3) {
                    val year = parts[0]
                    val monthNum = parts[1].toIntOrNull() ?: 1
                    val day = parts[2]
                    val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    val month = if (monthNum in 1..12) monthNames[monthNum - 1] else "Unknown"
                    "Joined $month $day, $year"
                } else {
                    "Joined $dateString"
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ProfileActivity", "Error formatting date: $dateString", e)
            "Joined recently"
        }
    }

    /**
     * Observes the LiveData streams from the [ProfileViewModel] to provide UI feedback.
     */
    private fun observeUIFeedback() {
        viewModel.isFollowing.observe(this) { isFollowing ->
            binding.btnFollow.text = if (isFollowing) getString(R.string.unfollow) else getString(R.string.follow)
        }

        viewModel.isProfileLiked.observe(this) { isLiked ->
            val icon = if (isLiked) R.drawable.post_icons_1_2 else R.drawable.post_icons_1_1
            val color = if (isLiked) R.color.md_theme_primary else R.color.md_theme_onSurface
            binding.likeUserProfileButtonIc.setImageResource(icon)
            binding.likeUserProfileButtonIc.setColorFilter(ContextCompat.getColor(this, color))
        }
    }

    /**
     * Sets up the listeners for the UI elements.
     *
     * @param userId The ID of the user whose profile is being viewed.
     * @param currentUid The ID of the current user.
     */
    private fun setupUIListeners(userId: String, currentUid: String) {
        // Show/hide buttons based on whether this is the current user's profile
        val isOwnProfile = userId == currentUid
        binding.btnEditProfile.visibility = if (isOwnProfile) View.VISIBLE else View.GONE
        binding.btnFollow.visibility = if (isOwnProfile) View.GONE else View.VISIBLE
        binding.btnMessage.visibility = if (isOwnProfile) View.GONE else View.VISIBLE
        binding.ProfilePageTopBarBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.ProfilePageSwipeLayout.setOnRefreshListener {
            viewModel.getUserPosts(userId)
            loadUserProfile(userId, currentUid)
        }
        binding.ProfilePageTopBarMenu.setOnClickListener {
            showProfileMenu(userId, currentUid)
        }
        binding.btnFollow.setOnClickListener {
            android.util.Log.d("ProfileActivity", "Follow button clicked for user: $userId, current user: $currentUid")
            if (userId.isEmpty() || currentUid.isEmpty()) {
                android.util.Log.e("ProfileActivity", "Invalid user IDs for follow action")
                Toast.makeText(this, "Failed to get user info", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.toggleFollow(userId)
        }

        binding.likeUserProfileButton.setOnClickListener {
            viewModel.toggleProfileLike(userId)
        }

        binding.btnEditProfile.setOnClickListener {
            // Only allow editing if this is the current user's profile
            if (userId == currentUid) {
                try {
                    val intent = Intent(this, ProfileEditActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error opening profile editor: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "You can only edit your own profile", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnMessage.setOnClickListener {
            android.util.Log.d("ProfileActivity", "Message button clicked for user: $userId")
            startDirectChat(userId, currentUid)
        }

        binding.ProfilePageTabUserInfoFollowsDetails.setOnClickListener {
            showFollowOptionsDialog(userId)
        }

        binding.ProfilePageTabUserInfoProfileImage.setOnClickListener {
            showProfileImagePreview(userId)
        }

        binding.userUidLayoutText.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("UID", binding.userUidLayoutText.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "UID copied to clipboard", Toast.LENGTH_SHORT).show()
            true
        }

        binding.ProfilePageTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.ProfilePageTabUserInfo.visibility = View.VISIBLE
                        binding.ProfilePageTabUserPosts.visibility = View.GONE
                    }
                    1 -> {
                        binding.ProfilePageTabUserInfo.visibility = View.GONE
                        binding.ProfilePageTabUserPosts.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showProfileImagePreview(userId: String) {
        val dialogBinding = DpPreviewBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        lifecycleScope.launch {
            try {
                val userData = SupabaseClient.client.from("users")
                    .select(columns = Columns.raw("*")) {
                        filter { eq("uid", userId) }
                    }.decodeSingleOrNull<JsonObject>()
                
                if (userData != null) {
                    val avatar = userData["avatar"] as? String
                    if (avatar != null && avatar != "null") {
                        com.bumptech.glide.Glide.with(this@ProfileActivity).load(avatar).into(dialogBinding.avatar)
                        dialogBinding.saveToHistory.setOnClickListener {
                            saveToHistory(avatar)
                            dialog.dismiss()
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
        dialog.show()
    }
    private fun saveToHistory(imageUrl: String) {
        val currentUser = SupabaseClient.client.auth.currentUserOrNull()
        val currentUid = currentUser?.id ?: return
        
        lifecycleScope.launch {
            try {
                val historyItem = mapOf(
                    "user_id" to currentUid,
                    "image_url" to imageUrl,
                    "upload_date" to java.util.Calendar.getInstance().timeInMillis.toString(),
                    "type" to "url"
                )
                
                // TODO: Implement direct Supabase insert
                // SupabaseClient.client.from("profile_history").insert(historyItem)
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Saved to History", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Failed to save to history", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startDirectChat(targetUserId: String, currentUserId: String?) {
        android.util.Log.d("ProfileActivity", "startDirectChat called - Target: $targetUserId, Current: $currentUserId")
        lifecycleScope.launch {
            try {
                // Get current user UID (not auth UUID)
                val authRepository = com.synapse.social.studioasinc.data.repository.AuthRepository()
                val currentUserUid = authRepository.getCurrentUserUid()
                android.util.Log.d("ProfileActivity", "Got current user UID: $currentUserUid")

                if (currentUserUid == null) {
                    android.util.Log.e("ProfileActivity", "Failed to get current user UID")
                    Toast.makeText(this@ProfileActivity, "Failed to get user info", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (targetUserId == currentUserUid) {
                    Toast.makeText(this@ProfileActivity, "You cannot message yourself", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Show loading
                val progressDialog = android.app.ProgressDialog(this@ProfileActivity).apply {
                    setMessage("Starting chat...")
                    setCancelable(false)
                    show()
                }

                android.util.Log.d("ProfileActivity", "Creating/getting chat...")
                val chatService = com.synapse.social.studioasinc.backend.SupabaseChatService()
                val result = chatService.getOrCreateDirectChat(currentUserUid, targetUserId)
                
                result.fold(
                    onSuccess = { chatId ->
                        progressDialog.dismiss()
                        android.util.Log.d("ProfileActivity", "Chat created successfully: $chatId")
                        
                        // Navigate to ChatActivity
                        val intent = Intent(this@ProfileActivity, ChatActivity::class.java)
                        intent.putExtra("chatId", chatId)
                        intent.putExtra("uid", targetUserId)
                        intent.putExtra("isGroup", false)
                        startActivity(intent)
                    },
                    onFailure = { error ->
                        progressDialog.dismiss()
                        
                        // Provide user-friendly error messages based on error type
                        val userMessage = when {
                            error.message?.contains("Cannot create chat with yourself", ignoreCase = true) == true -> 
                                "You cannot message yourself"
                            error.message?.contains("Supabase not configured", ignoreCase = true) == true -> 
                                "Service unavailable. Please try again later."
                            error.message?.contains("Invalid user IDs", ignoreCase = true) == true -> 
                                "Failed to get user information. Please try again."
                            error.message?.contains("network", ignoreCase = true) == true ||
                            error.message?.contains("connection", ignoreCase = true) == true -> 
                                "Network error. Please check your connection."
                            else -> 
                                "Failed to start chat. Please try again."
                        }
                        
                        android.util.Log.e("ProfileActivity", "Failed to create chat: ${error.message}", error)
                        Toast.makeText(this@ProfileActivity, userMessage, Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("ProfileActivity", "Unexpected error starting chat", e)
                Toast.makeText(
                    this@ProfileActivity, 
                    "An unexpected error occurred. Please try again.", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Share post functionality
     */
    private fun sharePost(post: Post) {
        val shareText = buildString {
            append("Check out this post on Synapse!\n\n")
            if (!post.postText.isNullOrEmpty()) {
                append(post.postText)
                append("\n\n")
            }
            append("Posted by @${post.authorUid}\n")
            append("https://synapse.app/post/${post.id}")
        }
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Synapse Post")
        }
        
        startActivity(Intent.createChooser(shareIntent, "Share post via"))
    }

    /**
     * Show profile menu options
     */
    private fun showProfileMenu(userId: String, currentUid: String) {
        val isOwnProfile = userId == currentUid
        
        val options = if (isOwnProfile) {
            arrayOf("Settings", "QR Code", "Share Profile", "Archive")
        } else {
            arrayOf("Share Profile", "Block User", "Report User")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Profile Options")
            .setItems(options) { _, which ->
                when {
                    isOwnProfile -> {
                        when (which) {
                            0 -> openSettings()
                            1 -> showQRCode(userId)
                            2 -> shareProfile(userId)
                            3 -> openArchive()
                        }
                    }
                    else -> {
                        when (which) {
                            0 -> shareProfile(userId)
                            1 -> blockUser(userId)
                            2 -> reportUser(userId)
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openSettings() {
        Toast.makeText(this, "Settings feature coming soon", Toast.LENGTH_SHORT).show()
        // TODO: Implement settings activity
    }

    private fun showQRCode(userId: String) {
        Toast.makeText(this, "QR Code feature coming soon", Toast.LENGTH_SHORT).show()
        // TODO: Implement QR code generation
    }

    private fun shareProfile(userId: String) {
        val shareText = "Check out this profile on Synapse!\nhttps://synapse.app/profile/$userId"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Share profile via"))
    }

    private fun openArchive() {
        Toast.makeText(this, "Archive feature coming soon", Toast.LENGTH_SHORT).show()
        // TODO: Implement archive activity
    }

    private fun blockUser(userId: String) {
        AlertDialog.Builder(this)
            .setTitle("Block User")
            .setMessage("Are you sure you want to block this user? You won't see their posts and they won't be able to message you.")
            .setPositiveButton("Block") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val currentUid = SupabaseClient.client.auth.currentUserOrNull()?.id
                        if (currentUid != null) {
                            val blockData = kotlinx.serialization.json.buildJsonObject {
                                put("blocker_id", kotlinx.serialization.json.JsonPrimitive(currentUid))
                                put("blocked_id", kotlinx.serialization.json.JsonPrimitive(userId))
                                // Let database handle created_at with default value
                            }
                            SupabaseClient.client.from("blocks").insert(blockData)
                            Toast.makeText(this@ProfileActivity, "User blocked", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@ProfileActivity, "Failed to block user: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reportUser(userId: String) {
        val reasons = arrayOf(
            "Spam",
            "Harassment",
            "Inappropriate Content",
            "Impersonation",
            "Other"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Report User")
            .setItems(reasons) { _, which ->
                val reason = reasons[which]
                submitUserReport(userId, reason)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitUserReport(userId: String, reason: String) {
        lifecycleScope.launch {
            try {
                val currentUid = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (currentUid != null) {
                    val reportData = kotlinx.serialization.json.buildJsonObject {
                        put("reporter_id", kotlinx.serialization.json.JsonPrimitive(currentUid))
                        put("reported_user_id", kotlinx.serialization.json.JsonPrimitive(userId))
                        put("reason", kotlinx.serialization.json.JsonPrimitive(reason))
                        put("status", kotlinx.serialization.json.JsonPrimitive("pending"))
                        // Let database handle created_at with default value
                    }
                    SupabaseClient.client.from("user_reports").insert(reportData)
                    Toast.makeText(this@ProfileActivity, "Report submitted. Thank you for helping keep Synapse safe.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Failed to submit report: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Post action methods
     */
    private fun editPost(post: Post) {
        val intent = Intent(this, EditPostActivity::class.java)
        intent.putExtra("post_id", post.id)
        intent.putExtra("post_text", post.postText)
        startActivity(intent)
    }

    private fun deletePost(post: Post) {
        AlertDialog.Builder(this)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        SupabaseClient.client.from("posts").delete {
                            filter {
                                eq("id", post.id)
                            }
                        }
                        Toast.makeText(this@ProfileActivity, "Post deleted", Toast.LENGTH_SHORT).show()
                        // Refresh posts
                        val userId = intent.getStringExtra("uid") ?: return@launch
                        viewModel.getUserPosts(userId)
                    } catch (e: Exception) {
                        Toast.makeText(this@ProfileActivity, "Failed to delete post: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun copyPostLink(post: Post) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Post Link", "https://synapse.app/post/${post.id}")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun showPostStatistics(post: Post) {
        val message = buildString {
            append("Post Statistics\n\n")
            append("Likes: ${post.likesCount}\n")
            append("Comments: ${post.commentsCount}\n")
            append("Views: ${post.viewsCount}\n")
            append("Posted: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(post.timestamp))}")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Statistics")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun reportPost(post: Post) {
        val reasons = arrayOf(
            "Spam",
            "Harassment or Bullying",
            "Violence or Dangerous Content",
            "Hate Speech",
            "Nudity or Sexual Content",
            "False Information",
            "Other"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Report Post")
            .setItems(reasons) { _, which ->
                val reason = reasons[which]
                submitPostReport(post.id, reason)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitPostReport(postId: String, reason: String) {
        lifecycleScope.launch {
            try {
                val currentUid = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (currentUid != null) {
                    val reportData = kotlinx.serialization.json.buildJsonObject {
                        put("reporter_id", kotlinx.serialization.json.JsonPrimitive(currentUid))
                        put("post_id", kotlinx.serialization.json.JsonPrimitive(postId))
                        put("reason", kotlinx.serialization.json.JsonPrimitive(reason))
                        put("status", kotlinx.serialization.json.JsonPrimitive("pending"))
                        // Let database handle created_at with default value
                    }
                    SupabaseClient.client.from("post_reports").insert(reportData)
                    Toast.makeText(this@ProfileActivity, "Report submitted. Thank you for your feedback.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Failed to submit report: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hidePost(post: Post) {
        lifecycleScope.launch {
            try {
                val currentUid = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (currentUid != null) {
                    val hideData = kotlinx.serialization.json.buildJsonObject {
                        put("user_id", kotlinx.serialization.json.JsonPrimitive(currentUid))
                        put("post_id", kotlinx.serialization.json.JsonPrimitive(post.id))
                        // Let database handle created_at with default value
                    }
                    SupabaseClient.client.from("hidden_posts").insert(hideData)
                    Toast.makeText(this@ProfileActivity, "Post hidden. You won't see posts like this.", Toast.LENGTH_SHORT).show()
                    // Refresh posts
                    val userId = intent.getStringExtra("uid") ?: return@launch
                    viewModel.getUserPosts(userId)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Failed to hide post: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Lifecycle: onDestroy")

        // Clear Glide image loading requests to free up memory when the activity is finishing.
        // This is essential to prevent Glide from holding references to views that are no longer valid.
        if (isFinishing) {
            Log.d(TAG, "Clearing Glide resources")
            Glide.with(applicationContext).clear(binding.ProfilePageTabUserInfoProfileImage)
            Glide.with(applicationContext).clear(binding.ProfilePageTabUserInfoCoverImage)
        }

        // Nullify the RecyclerView adapter to break the reference cycle.
        // The RecyclerView can hold a strong reference to the adapter, which in turn can hold a
        // reference to the activity, causing a memory leak.
        binding.ProfilePageTabUserPostsRecyclerView.adapter = null

        // Cancel all coroutines launched in the lifecycleScope of this activity.
        // This ensures that no background work continues after the activity is destroyed.
        lifecycleScope.coroutineContext.cancelChildren()
    }
}
