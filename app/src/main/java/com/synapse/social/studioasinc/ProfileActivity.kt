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
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
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
        val authService = SupabaseAuthenticationService()
        val currentUid = authService.getCurrentUserId() ?: ""

        // Load user profile
        loadUserProfile(userId, currentUid)

        // Fetch initial states
        viewModel.fetchInitialFollowState(userId)
        viewModel.fetchInitialProfileLikeState(userId)

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
                }
            }
            binding.ProfilePageSwipeLayout.isRefreshing = false
        }
        viewModel.getUserPosts(userId)
    }

    private fun showCommentsDialog(post: Post) {
        val bundle = Bundle()
        bundle.putString("postKey", post.key)
        bundle.putString("postPublisherUID", post.uid)
        val commentsDialog = PostCommentsBottomSheetDialog()
        commentsDialog.arguments = bundle
        commentsDialog.show(supportFragmentManager, commentsDialog.tag)
    }

    private fun showMoreOptionsDialog(post: Post) {
        // More options dialog - placeholder for now
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
            // Navigate to edit profile - placeholder for now
        }

        binding.btnMessage.setOnClickListener {
            // Navigate to chat - placeholder for now
        }

        binding.ProfilePageTabUserInfoFollowsDetails.setOnClickListener {
            // Navigate to follows list - placeholder for now
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

        val dbService = SupabaseDatabaseService()
        lifecycleScope.launch {
            try {
                val users = dbService.selectWithFilter(
                    table = "users",
                    columns = "*",
                    filter = "uid",
                    value = userId
                ).getOrNull() ?: emptyList()
                
                if (users.isNotEmpty()) {
                    val userData = users.first()
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
        val authService = SupabaseAuthenticationService()
        val currentUid = authService.getCurrentUserId() ?: return
        val dbService = SupabaseDatabaseService()
        
        lifecycleScope.launch {
            try {
                val historyItem = mapOf(
                    "user_id" to currentUid,
                    "image_url" to imageUrl,
                    "upload_date" to java.util.Calendar.getInstance().timeInMillis.toString(),
                    "type" to "url"
                )
                
                dbService.insert("profile_history", historyItem)
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