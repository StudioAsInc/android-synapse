package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.synapse.social.studioasinc.databinding.ActivityProfileBinding
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.util.UserProfileManager
import com.synapse.social.studioasinc.util.adapter.PostAdapter
import io.noties.markwon.Markwon

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
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Load user profile
        loadUserProfile(userId, currentUid)

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
            onFavoriteClicked = { post -> viewModel.toggleFavorite(post) }
        )
        binding.ProfilePageTabUserPostsRecyclerView.adapter = postAdapter

        viewModel.userPosts.observe(this) { posts ->
            if (posts.isNullOrEmpty()) {
                binding.ProfilePageTabUserPostsRecyclerView.visibility = View.GONE
                binding.ProfilePageTabUserPostsNoPostsSubtitle.visibility = View.VISIBLE
            } else {
                binding.ProfilePageTabUserPostsRecyclerView.visibility = View.VISIBLE
                binding.ProfilePageTabUserPostsNoPostsSubtitle.visibility = View.GONE
                postAdapter.submitList(posts)
            }
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
}
