package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import android.view.View
// import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.synapse.social.studioasinc.adapters.SearchResultsAdapter
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.model.SearchResult
import kotlinx.coroutines.*

class SearchActivity : BaseActivity() {

    // Supabase services
    private val authService = SupabaseAuthenticationService()
    private val databaseService = SupabaseDatabaseService()

    // UI Components
    private lateinit var toolbar: MaterialToolbar
    private lateinit var searchView: SearchView
    private lateinit var chipGroup: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var chipPeople: Chip
    private lateinit var chipPosts: Chip
    private lateinit var chipPhotos: Chip
    private lateinit var chipVideos: Chip
    private lateinit var recyclerViewResults: RecyclerView
    private lateinit var emptyStateLayout: View
    private lateinit var loadingLayout: View

    private lateinit var searchAdapter: SearchResultsAdapter
    private var searchJob: Job? = null
    private var currentFilter = SearchFilter.ALL
    private var chatMode = false

    enum class SearchFilter {
        ALL, PEOPLE, POSTS, PHOTOS, VIDEOS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        
        // Check if we're in chat mode
        chatMode = intent.getBooleanExtra("mode", false) || intent.getStringExtra("mode") == "chat"
        
        initializeViews()
        setupToolbar()
        setupSearchView()
        setupChips()
        setupRecyclerView()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        searchView = findViewById(R.id.searchView)
        chipGroup = findViewById(R.id.chipGroup)
        chipAll = findViewById(R.id.chipAll)
        chipPeople = findViewById(R.id.chipPeople)
        chipPosts = findViewById(R.id.chipPosts)
        chipPhotos = findViewById(R.id.chipPhotos)
        chipVideos = findViewById(R.id.chipVideos)
        recyclerViewResults = findViewById(R.id.recyclerViewResults)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        loadingLayout = findViewById(R.id.loadingLayout)
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()
                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(300) // Debounce
                    newText?.let { 
                        if (it.isNotEmpty()) {
                            performSearch(it)
                        } else {
                            showEmptyState()
                        }
                    }
                }
                return true
            }
        })
    }



    private fun setupChips() {
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                currentFilter = when (checkedIds[0]) {
                    R.id.chipAll -> SearchFilter.ALL
                    R.id.chipPeople -> SearchFilter.PEOPLE
                    R.id.chipPosts -> SearchFilter.POSTS
                    R.id.chipPhotos -> SearchFilter.PHOTOS
                    R.id.chipVideos -> SearchFilter.VIDEOS
                    else -> SearchFilter.ALL
                }
                
                val query = searchView.query.toString()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        searchAdapter = SearchResultsAdapter(
            onUserClick = { user ->
                if (chatMode) {
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("uid", user.uid)
                    intent.putExtra("ORIGIN_KEY", "SearchActivity")
                    startActivity(intent)
                    finish()
                } else {
                    val intent = Intent(this, ProfileComposeActivity::class.java)
                    intent.putExtra("uid", user.uid)
                    intent.putExtra("origin", "SearchActivity")
                    startActivity(intent)
                }
            },
            onPostClick = { post ->
                // Navigate to post - you can implement PostDetailActivity or navigate to home
                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("post_id", post.postId)
                startActivity(intent)
            },
            onMediaClick = { media ->
                // Navigate to media post - you can implement PostDetailActivity or navigate to home
                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("post_id", media.postId)
                startActivity(intent)
            }
        )
        
        recyclerViewResults.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(this@SearchActivity)
        }
    }

    private fun performSearch(query: String) {
        showLoading()
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val results = mutableListOf<SearchResult>()
                
                when (currentFilter) {
                    SearchFilter.ALL -> {
                        // Search all types
                        results.addAll(searchUsers(query))
                        results.addAll(searchPosts(query))
                        results.addAll(searchMedia(query, null))
                    }
                    SearchFilter.PEOPLE -> {
                        results.addAll(searchUsers(query))
                    }
                    SearchFilter.POSTS -> {
                        results.addAll(searchPosts(query))
                    }
                    SearchFilter.PHOTOS -> {
                        results.addAll(searchMedia(query, SearchResult.MediaType.PHOTO))
                    }
                    SearchFilter.VIDEOS -> {
                        results.addAll(searchMedia(query, SearchResult.MediaType.VIDEO))
                    }
                }
                
                if (results.isNotEmpty()) {
                    showResults(results)
                } else {
                    showEmptyState()
                }
            } catch (e: Exception) {
                showEmptyState()
                SketchwareUtil.showMessage(applicationContext, "Search error: ${e.message}")
            }
        }
    }

    private suspend fun searchUsers(query: String): List<SearchResult.User> = withContext(Dispatchers.IO) {
        try {
            val result = databaseService.searchUsers(query, 20)
            result.fold(
                onSuccess = { users ->
                    users.map { user ->
                        SearchResult.User(
                            uid = user["uid"]?.toString() ?: "",
                            username = user["username"]?.toString() ?: "",
                            nickname = user["nickname"]?.toString(),
                            avatar = user["avatar"]?.toString(),
                            gender = user["gender"]?.toString(),
                            accountType = user["account_type"]?.toString(),
                            isPremium = user["account_premium"]?.toString() == "true",
                            isVerified = user["verify"]?.toString() == "true",
                            isBanned = user["banned"]?.toString() == "true",
                            status = user["status"]?.toString()
                        )
                    }
                },
                onFailure = { emptyList() }
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun searchPosts(query: String): List<SearchResult.Post> = withContext(Dispatchers.IO) {
        try {
            val result = databaseService.searchPosts(query, 20)
            result.fold(
                onSuccess = { posts ->
                    posts.mapNotNull { post ->
                        val authorId = post["uid"]?.toString() ?: return@mapNotNull null
                        val authorResult = databaseService.selectWhere("users", "*", "uid", authorId)
                        
                        authorResult.fold(
                            onSuccess = { users ->
                                val author = users.firstOrNull()
                                SearchResult.Post(
                                    postId = post["post_id"]?.toString() ?: "",
                                    authorId = authorId,
                                    authorName = author?.get("nickname")?.toString() 
                                        ?: "@${author?.get("username")?.toString() ?: ""}",
                                    authorAvatar = author?.get("avatar")?.toString(),
                                    content = post["content"]?.toString() ?: "",
                                    timestamp = post["timestamp"]?.toString()?.toLongOrNull() ?: 0L,
                                    likesCount = post["likes_count"]?.toString()?.toIntOrNull() ?: 0,
                                    commentsCount = post["comments_count"]?.toString()?.toIntOrNull() ?: 0
                                )
                            },
                            onFailure = { null }
                        )
                    }
                },
                onFailure = { emptyList() }
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun searchMedia(query: String, mediaType: SearchResult.MediaType?): List<SearchResult.Media> = withContext(Dispatchers.IO) {
        try {
            val result = databaseService.select("posts", "*")
            result.fold(
                onSuccess = { posts ->
                    posts.filter { post ->
                        val hasMedia = when (mediaType) {
                            SearchResult.MediaType.PHOTO -> {
                                val image = post["image"]?.toString()
                                !image.isNullOrEmpty() && image != "null"
                            }
                            SearchResult.MediaType.VIDEO -> {
                                val video = post["video"]?.toString()
                                !video.isNullOrEmpty() && video != "null"
                            }
                            null -> {
                                val image = post["image"]?.toString()
                                val video = post["video"]?.toString()
                                (!image.isNullOrEmpty() && image != "null") || 
                                (!video.isNullOrEmpty() && video != "null")
                            }
                        }
                        hasMedia
                    }.take(20).mapNotNull { post ->
                        val authorId = post["uid"]?.toString() ?: return@mapNotNull null
                        val authorResult = databaseService.select("users", "*")
                        
                        val video = post["video"]?.toString()
                        val image = post["image"]?.toString()
                        
                        val actualMediaType = when {
                            !video.isNullOrEmpty() && video != "null" -> SearchResult.MediaType.VIDEO
                            !image.isNullOrEmpty() && image != "null" -> SearchResult.MediaType.PHOTO
                            else -> return@mapNotNull null
                        }
                        
                        val mediaUrl = if (actualMediaType == SearchResult.MediaType.VIDEO) video else image
                        
                        authorResult.fold(
                            onSuccess = { users ->
                                val author = users.find { it["uid"]?.toString() == authorId }
                                SearchResult.Media(
                                    postId = post["post_id"]?.toString() ?: "",
                                    authorId = authorId,
                                    authorName = author?.get("nickname")?.toString() 
                                        ?: "@${author?.get("username")?.toString() ?: ""}",
                                    authorAvatar = author?.get("avatar")?.toString(),
                                    mediaUrl = mediaUrl ?: "",
                                    mediaType = actualMediaType,
                                    timestamp = post["timestamp"]?.toString()?.toLongOrNull() ?: 0L
                                )
                            },
                            onFailure = { null }
                        )
                    }
                },
                onFailure = { emptyList() }
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun showLoading() {
        loadingLayout.visibility = View.VISIBLE
        recyclerViewResults.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE
    }

    private fun showResults(results: List<SearchResult>) {
        loadingLayout.visibility = View.GONE
        recyclerViewResults.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
        searchAdapter.submitList(results)
    }

    private fun showEmptyState() {
        loadingLayout.visibility = View.GONE
        recyclerViewResults.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        searchJob?.cancel()
    }
}