package com.synapse.social.studioasinc

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.synapse.social.studioasinc.databinding.ActivityProfileBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        // Initialize UserProfileManager
        val markwon = Markwon.create(this)
        userProfileManager = UserProfileManager(this, markwon)

        // Get user ID from intent
        val userId = intent.getStringExtra("uid") ?: return
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Load user profile
        loadUserProfile(userId, currentUid)

        // Observe user posts
        observeUserPosts(userId)

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
        postAdapter = PostAdapter()
        binding.ProfilePageTabUserPostsRecyclerView.adapter = postAdapter

        viewModel.getUserPosts(userId).observe(this) { posts ->
            if (posts.isNullOrEmpty()) {
                binding.ProfilePageTabUserPostsRecyclerView.visibility = View.GONE
                binding.ProfilePageTabUserPostsNoPostsSubtitle.visibility = View.VISIBLE
            } else {
                binding.ProfilePageTabUserPostsRecyclerView.visibility = View.VISIBLE
                binding.ProfilePageTabUserPostsNoPostsSubtitle.visibility = View.GONE
                postAdapter.submitList(posts)
            }
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
