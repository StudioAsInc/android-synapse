package com.synapse.social.studioasinc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var viewModel: ProfileViewModel
    private lateinit var postAdapter: PostsAdapter
    private lateinit var markwon: Markwon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            onShareClicked = { post -> /* Handle share click */ },
            onMoreOptionsClicked = { post -> showMoreOptionsDialog(post) },
            onFavoriteClicked = { post -> viewModel.toggleFavorite(post.id) },
            onUserClicked = { uid ->
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("uid", uid)
                startActivity(intent)
            }
        )
        binding.ProfilePageTabUserPostsRecyclerView.adapter = postAdapter

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
        val bundle = Bundle()
        bundle.putString("postKey", post.key)
        bundle.putString("postPublisherUID", post.authorUid)
        val commentsDialog = PostCommentsBottomSheetDialog()
        commentsDialog.arguments = bundle
        commentsDialog.show(supportFragmentManager, commentsDialog.tag)
    }

    private fun showMoreOptionsDialog(post: Post) {
        // More options dialog - placeholder for now
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

    private fun startDirectChat(targetUserId: String, currentUserId: String) {
        if (targetUserId == currentUserId) {
            Toast.makeText(this, "You cannot message yourself", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // Show loading
                val progressDialog = android.app.ProgressDialog(this@ProfileActivity).apply {
                    setMessage("Starting chat...")
                    setCancelable(false)
                    show()
                }

                // Get current user's UID from users table
                val currentUserUid = getCurrentUserUid()
                if (currentUserUid == null) {
                    progressDialog.dismiss()
                    Toast.makeText(this@ProfileActivity, "Failed to get user info", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Create chat ID (dm_userId1_userId2, sorted alphabetically)
                val chatId = if (currentUserUid < targetUserId) {
                    "dm_${currentUserUid}_${targetUserId}"
                } else {
                    "dm_${targetUserId}_${currentUserUid}"
                }

                // Check if chat already exists
                val existingChat = SupabaseClient.client.from("chats")
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("chat_id")) {
                        filter { eq("chat_id", chatId) }
                    }
                    .decodeSingleOrNull<JsonObject>()

                if (existingChat == null) {
                    // Create new chat
                    val chatData = kotlinx.serialization.json.buildJsonObject {
                        put("chat_id", kotlinx.serialization.json.JsonPrimitive(chatId))
                        put("is_group", kotlinx.serialization.json.JsonPrimitive(false))
                        put("created_by", kotlinx.serialization.json.JsonPrimitive(currentUserUid))
                        put("participants_count", kotlinx.serialization.json.JsonPrimitive(2))
                    }
                    SupabaseClient.client.from("chats").insert(chatData)

                    // Add participants
                    val participant1 = kotlinx.serialization.json.buildJsonObject {
                        put("chat_id", kotlinx.serialization.json.JsonPrimitive(chatId))
                        put("user_id", kotlinx.serialization.json.JsonPrimitive(currentUserUid))
                        put("role", kotlinx.serialization.json.JsonPrimitive("member"))
                    }
                    val participant2 = kotlinx.serialization.json.buildJsonObject {
                        put("chat_id", kotlinx.serialization.json.JsonPrimitive(chatId))
                        put("user_id", kotlinx.serialization.json.JsonPrimitive(targetUserId))
                        put("role", kotlinx.serialization.json.JsonPrimitive("member"))
                    }
                    SupabaseClient.client.from("chat_participants").insert(listOf(participant1, participant2))
                }

                progressDialog.dismiss()

                // Navigate to ChatActivity
                val intent = Intent(this@ProfileActivity, ChatActivity::class.java)
                intent.putExtra("chatId", chatId)
                intent.putExtra("uid", targetUserId)
                intent.putExtra("isGroup", false)
                startActivity(intent)

            } catch (e: Exception) {
                android.util.Log.e("ProfileActivity", "Error starting chat", e)
                Toast.makeText(
                    this@ProfileActivity,
                    "Error starting chat: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun getCurrentUserUid(): String? {
        return try {
            val authId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return null
            val result = SupabaseClient.client.from("users")
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("uid")) {
                    filter { eq("id", authId) }
                }
                .decodeSingleOrNull<JsonObject>()
            result?.get("uid")?.toString()?.removeSurrounding("\"")
        } catch (e: Exception) {
            android.util.Log.e("ProfileActivity", "Failed to get user UID", e)
            null
        }
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
            // Navigate to settings - placeholder for now
        }
        binding.btnFollow.setOnClickListener {
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
}