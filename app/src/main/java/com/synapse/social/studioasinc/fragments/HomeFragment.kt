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
import com.synapse.social.studioasinc.home.StoryAdapter

class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var postAdapter: PostAdapter
    private lateinit var storyAdapter: StoryAdapter
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
        setupViewModel()
        setupRecyclerView()
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
        postAdapter = PostAdapter(requireContext(), this)
        storyAdapter = StoryAdapter(requireContext(), emptyList())
        headerAdapter = HeaderAdapter(requireContext(), storyAdapter)

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
}
