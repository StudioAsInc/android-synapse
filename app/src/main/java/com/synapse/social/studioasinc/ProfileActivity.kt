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
import com.synapse.social.studioasinc.util.UserProfileManager
import com.synapse.social.studioasinc.util.adapter.PostAdapter
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
    private lateinit var userProfileManager: UserProfileManager
    private lateinit var postAdapter: PostAdapter
    private lateinit var markwon: Markwon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        // Initialize UserProfileManager and Markwon
        markwon = Markwon.create(this)
        userProfileManager = UserProfileManager(this, markwon)

        // Get user ID from intent
        val userId = intent.getStringExtra("uid") ?: return
        val authService = SupabaseAuthenticationService()
        val currentUid = authService.getCurrentUserId() ?: ""

        // Load user profile
        loadUserProfile(userId, currentUid)

        // Fetch initial states
        viewModel.fetchInitialFollowState(userId, currentUid)
        viewModel.fetchInitialProfileLikeState(userId, currentUid)

        // Observe user posts
        observeUserPosts(userId)

        // Observe UI feedback
        observeUIFeedback()

        // Setup UI listeners
        setupUIListeners(userId, currentUid)
    }

    /**
     * Loads the user profile information using the [UserProfileManager].
     *
     * @param userId The ID of the user whose profile is to be loaded.
     * @param currentUid The ID of the current user.
     */
    private fun loadUserProfile(userId: String, currentUid: String) {
        val views = UserProfileManager.ProfileViews(
            profileImage = binding.ProfilePageTabUserInfoProfileImage,
            coverImage = binding.ProfilePageTabUserInfoCoverImage,
            nickname = binding.ProfilePageTabUserInfoNickname,
            username = binding.ProfilePageTabUserInfoUsername,
            bio = binding.ProfilePageTabUserInfoBioLayoutText,
            joinDate = binding.joinDateLayoutText,
            status = binding.ProfilePageTabUserInfoStatus,
            followersCount = binding.ProfilePageTabUserInfoFollowersCount,
            followingCount = binding.ProfilePageTabUserInfoFollowingCount,
            btnEditProfile = binding.btnEditProfile,
            secondaryButtons = binding.ProfilePageTabUserInfoSecondaryButtons
        )
        userProfileManager.loadUserProfile(userId, currentUid, views)
    }

    /**
     * Observes the user's posts from the [ProfileViewModel] and updates the UI.
     *
     * @param userId The ID of the user whose posts are to be observed.
     */
    private fun observeUserPosts(userId: String) {
        postAdapter = PostAdapter(
            markwon = markwon,
            onLikeClicked = { post -> viewModel.togglePostLike(post) },
            onCommentClicked = { post -> showCommentsDialog(post) },
            onShareClicked = { post -> /* Handle share click */ },
            onMoreOptionsClicked = { post -> showMoreOptionsDialog(post) },
            onFavoriteClicked = { post -> viewModel.toggleFavorite(post) },
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
                    postAdapter.submitList(state.posts)
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
        val bundle = Bundle()
        bundle.putString("postKey", post.key)
        bundle.putString("postPublisherUID", post.uid)
        bundle.putString("postType", post.postType)
        val moreOptionsDialog = PostMoreBottomSheetDialog()
        moreOptionsDialog.arguments = bundle
        moreOptionsDialog.show(supportFragmentManager, moreOptionsDialog.tag)
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
            val intent = Intent(this, ChatsettingsActivity::class.java)
            startActivity(intent)
        }
        binding.btnFollow.setOnClickListener {
            viewModel.toggleFollow(userId, currentUid)
        }

        binding.likeUserProfileButton.setOnClickListener {
            viewModel.toggleProfileLike(userId, currentUid)
        }

        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(this, ProfileEditActivity::class.java)
            startActivity(intent)
        }

        binding.btnMessage.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("uid", userId)
            startActivity(intent)
        }

        binding.ProfilePageTabUserInfoFollowsDetails.setOnClickListener {
            val intent = Intent(this, UserFollowsListActivity::class.java)
            intent.putExtra("uid", userId)
            startActivity(intent)
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
                val users = dbService.selectWithFilter<Map<String, Any?>>(
                    table = "users",
                    columns = "*"
                ) { /* Add filter for userId */ }
                
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