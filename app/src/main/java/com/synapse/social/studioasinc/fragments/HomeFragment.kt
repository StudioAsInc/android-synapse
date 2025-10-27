package com.synapse.social.studioasinc.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.home.HeaderAdapter
import com.synapse.social.studioasinc.home.HomeViewModel
import com.synapse.social.studioasinc.home.PostAdapter
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.PostCommentsBottomSheetDialog
import com.synapse.social.studioasinc.SupabaseClient
import android.content.Intent
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.gotrue.auth
class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var postAdapter: PostAdapter
    private lateinit var headerAdapter: HeaderAdapter
    private lateinit var concatAdapter: ConcatAdapter

    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var publicPostsList: RecyclerView
    private lateinit var loadingBar: ProgressBar
    private lateinit var shimmerContainer: LinearLayout

    private val SHIMMER_ITEM_COUNT = 5

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupRecyclerView()
        setupViewModel()
        setupListeners()

        viewModel.loadPosts()
    }

    private fun initializeViews(view: View) {
        swipeLayout = view.findViewById(R.id.swipeLayout)
        publicPostsList = view.findViewById(R.id.PublicPostsList)
        loadingBar = view.findViewById(R.id.loading_bar)
        shimmerContainer = view.findViewById(R.id.shimmer_container)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        observePosts()
        observeStories()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            context = requireContext(),
            lifecycleOwner = this,
            onMoreOptionsClicked = { post -> showMoreOptionsDialog(post) },
            onCommentClicked = { post -> showCommentsDialog(post) },
            onShareClicked = { post -> sharePost(post) }
        )
        headerAdapter = HeaderAdapter(requireContext(), this)

        concatAdapter = ConcatAdapter(headerAdapter, postAdapter)
        publicPostsList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = concatAdapter
        }
    }

    private fun setupListeners() {
        swipeLayout.setOnRefreshListener {
            viewModel.loadPosts()
        }
    }

    private fun observePosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.posts.collect { posts ->
                hideShimmer()
                postAdapter.updatePosts(posts)
                swipeLayout.isRefreshing = false
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                if (isLoading) {
                    showShimmer()
                } else {
                    hideShimmer()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    hideShimmer()
                    Toast.makeText(context, "Failed to fetch posts: $it", Toast.LENGTH_LONG).show()
                    swipeLayout.isRefreshing = false
                }
            }
        }
    }

    private fun observeStories() {
        // Stories functionality can be implemented later
        // For now, we'll just show empty stories
    }

    private fun showShimmer() {
        shimmerContainer.removeAllViews()
        shimmerContainer.visibility = View.VISIBLE
        val inflater = LayoutInflater.from(context)
        for (i in 0 until SHIMMER_ITEM_COUNT) {
            val shimmerView = inflater.inflate(R.layout.post_placeholder_layout, shimmerContainer, false)
            shimmerContainer.addView(shimmerView)
        }
    }

    private fun hideShimmer() {
        shimmerContainer.visibility = View.GONE
    }
    
    private fun showMoreOptionsDialog(post: Post) {
        val currentUser = SupabaseClient.client.auth.currentUserOrNull()
        val currentUid = currentUser?.id
        val isOwnPost = post.authorUid == currentUid
        
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val sheetBinding = com.synapse.social.studioasinc.databinding.BottomSheetPostOptionsBinding.inflate(layoutInflater)
        bottomSheet.setContentView(sheetBinding.root)
        
        // Show/hide options based on ownership
        if (isOwnPost) {
            sheetBinding.optionEdit.visibility = View.VISIBLE
            sheetBinding.optionDelete.visibility = View.VISIBLE
            sheetBinding.optionStatistics.visibility = View.VISIBLE
            sheetBinding.optionReport.visibility = View.GONE
            sheetBinding.optionHide.visibility = View.GONE
        } else {
            sheetBinding.optionEdit.visibility = View.GONE
            sheetBinding.optionDelete.visibility = View.GONE
            sheetBinding.optionStatistics.visibility = View.GONE
            sheetBinding.optionReport.visibility = View.VISIBLE
            sheetBinding.optionHide.visibility = View.VISIBLE
        }
        
        // Set click listeners
        sheetBinding.optionEdit.setOnClickListener {
            editPost(post)
            bottomSheet.dismiss()
        }
        
        sheetBinding.optionDelete.setOnClickListener {
            bottomSheet.dismiss()
            deletePost(post)
        }
        
        sheetBinding.optionCopyLink.setOnClickListener {
            copyPostLink(post)
            bottomSheet.dismiss()
        }
        
        sheetBinding.optionStatistics.setOnClickListener {
            bottomSheet.dismiss()
            showPostStatistics(post)
        }
        
        sheetBinding.optionReport.setOnClickListener {
            bottomSheet.dismiss()
            reportPost(post)
        }
        
        sheetBinding.optionHide.setOnClickListener {
            bottomSheet.dismiss()
            hidePost(post)
        }
        
        bottomSheet.show()
    }
    
    private fun showCommentsDialog(post: Post) {
        val commentsDialog = PostCommentsBottomSheetDialog()
        val bundle = Bundle()
        bundle.putString("postKey", post.id)
        bundle.putString("postAuthorUid", post.authorUid)
        commentsDialog.arguments = bundle
        commentsDialog.show(parentFragmentManager, commentsDialog.tag)
    }
    
    private fun sharePost(post: Post) {
        val shareText = "${post.postText}\n\nShared via Synapse"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share post via"))
    }
    
    private fun editPost(post: Post) {
        val editIntent = Intent(requireActivity(), com.synapse.social.studioasinc.EditPostActivity::class.java).apply {
            putExtra("postKey", post.id)
            putExtra("postText", post.postText)
            putExtra("postImg", post.postImage)
        }
        startActivity(editIntent)
    }
    
    private fun deletePost(post: Post) {
        lifecycleScope.launch {
            try {
                // Delete from Supabase
                SupabaseClient.client.from("posts").delete {
                    filter {
                        eq("id", post.id)
                    }
                }
                Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show()
                // Refresh posts
                viewModel.loadPosts()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to delete post: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun copyPostLink(post: Post) {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Post Link", "https://synapse.app/post/${post.id}")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Link copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    private fun showPostStatistics(post: Post) {
        // TODO: Implement post statistics dialog
        Toast.makeText(requireContext(), "Statistics feature coming soon", Toast.LENGTH_SHORT).show()
    }
    
    private fun reportPost(post: Post) {
        lifecycleScope.launch {
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser != null) {
                    val reportData = mapOf(
                        "reporter_uid" to currentUser.id,
                        "reported_post_id" to post.id,
                        "reported_user_uid" to post.authorUid,
                        "report_reason" to "Inappropriate content",
                        "created_at" to System.currentTimeMillis()
                    )
                    SupabaseClient.client.from("reports").insert(reportData)
                    Toast.makeText(requireContext(), "Post reported. Thank you for keeping Synapse safe.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to report post: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun hidePost(post: Post) {
        lifecycleScope.launch {
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser != null) {
                    val hideData = mapOf(
                        "user_uid" to currentUser.id,
                        "hidden_post_id" to post.id,
                        "hidden_at" to System.currentTimeMillis()
                    )
                    SupabaseClient.client.from("hidden_posts").insert(hideData)
                    Toast.makeText(requireContext(), "Post hidden. You won't see posts like this.", Toast.LENGTH_SHORT).show()
                    // Refresh posts
                    viewModel.loadPosts()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to hide post: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
