# Design Document: Pull-to-Refresh and Infinite Scroll

## Overview

This document outlines the design for implementing pull-to-refresh and infinite scroll functionality in the Synapse Android application. The solution will provide a reusable, performant pagination system that integrates seamlessly with the existing MVVM architecture, Supabase backend, and Material Design 3 UI components.

The design focuses on creating a generic `PaginationManager` component that can be easily integrated into any RecyclerView-based list in the app, starting with the home feed (posts) and chat messages.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ HomeFragment │  │ ChatActivity │  │ Notifications│      │
│  │              │  │              │  │   Fragment   │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
│         │ SwipeRefreshLayout + RecyclerView   │              │
│         └──────────────────┼──────────────────┘              │
└────────────────────────────┼─────────────────────────────────┘
                             │
┌────────────────────────────┼─────────────────────────────────┐
│                   Presentation Layer                          │
│         ┌────────────────────────────────┐                   │
│         │      PaginationManager         │                   │
│         │  - State Management            │                   │
│         │  - Scroll Detection            │                   │
│         │  - Load Coordination           │                   │
│         └────────────┬───────────────────┘                   │
│                      │                                        │
│         ┌────────────┴───────────────┐                       │
│         │                             │                       │
│  ┌──────▼──────┐            ┌────────▼────────┐             │
│  │ HomeViewModel│            │  ChatViewModel  │             │
│  │  - StateFlow │            │   - StateFlow   │             │
│  │  - Coroutines│            │   - Coroutines  │             │
│  └──────┬───────┘            └────────┬────────┘             │
└─────────┼──────────────────────────────┼────────────────────┘
          │                              │
┌─────────┼──────────────────────────────┼────────────────────┐
│         │         Data Layer           │                     │
│  ┌──────▼──────┐            ┌──────────▼────────┐           │
│  │PostRepository│            │  ChatRepository   │           │
│  │  - Pagination│            │   - Pagination    │           │
│  │  - Caching   │            │   - Caching       │           │
│  └──────┬───────┘            └──────────┬────────┘           │
└─────────┼──────────────────────────────┼────────────────────┘
          │                              │
          └──────────────┬───────────────┘
                         │
                ┌────────▼────────┐
                │ SupabaseClient  │
                │   (Singleton)   │
                └─────────────────┘
```

### Component Interaction Flow

**Pull-to-Refresh Flow:**
1. User pulls down on SwipeRefreshLayout
2. SwipeRefreshLayout triggers refresh callback
3. Fragment calls ViewModel.refresh()
4. ViewModel calls PaginationManager.refresh()
5. PaginationManager resets state and calls Repository.getFirstPage()
6. Repository fetches data from Supabase
7. ViewModel updates StateFlow with new data
8. Fragment observes StateFlow and updates RecyclerView
9. SwipeRefreshLayout hides loading indicator

**Infinite Scroll Flow:**
1. User scrolls near bottom of list
2. PaginationManager detects scroll threshold
3. PaginationManager checks if already loading or at end
4. PaginationManager calls Repository.getNextPage(currentPage)
5. Repository fetches next page from Supabase
6. ViewModel appends new data to StateFlow
7. Fragment observes StateFlow and updates RecyclerView
8. Loading indicator at bottom is hidden

## Components and Interfaces

### 1. PaginationManager

A reusable component that manages pagination state and coordinates data loading.

```kotlin
/**
 * Generic pagination manager for RecyclerView lists
 * Handles pull-to-refresh and infinite scroll logic
 */
class PaginationManager<T>(
    private val pageSize: Int = 20,
    private val scrollThreshold: Int = 5,
    private val onLoadPage: suspend (page: Int, pageSize: Int) -> Result<List<T>>,
    private val onError: (String) -> Unit
) {
    // State management
    private val _paginationState = MutableStateFlow<PaginationState<T>>(PaginationState.Initial)
    val paginationState: StateFlow<PaginationState<T>> = _paginationState.asStateFlow()
    
    private var currentPage = 0
    private var isLoading = false
    private var hasMoreData = true
    private val loadedItems = mutableListOf<T>()
    
    // Public API
    suspend fun refresh()
    suspend fun loadNextPage()
    fun reset()
    fun attachToRecyclerView(recyclerView: RecyclerView)
}

sealed class PaginationState<T> {
    object Initial : PaginationState<Nothing>()
    object Refreshing : PaginationState<Nothing>()
    data class LoadingMore<T>(val currentItems: List<T>) : PaginationState<T>()
    data class Success<T>(val items: List<T>, val hasMore: Boolean) : PaginationState<T>()
    data class Error<T>(val message: String, val currentItems: List<T>) : PaginationState<T>()
    data class EndOfList<T>(val items: List<T>) : PaginationState<T>()
}
```

### 2. Repository Layer Extensions

Extend existing repositories to support pagination.

```kotlin
// PostRepository.kt - Add pagination methods
class PostRepository {
    private val client = SupabaseClient.client
    
    /**
     * Fetch posts with pagination support
     * @param page Page number (0-indexed)
     * @param pageSize Number of items per page
     * @return Result containing list of posts
     */
    suspend fun getPostsPage(page: Int, pageSize: Int): Result<List<Post>> {
        return try {
            val offset = page * pageSize
            val posts = client.from("posts")
                .select() {
                    limit(pageSize.toLong())
                    // Supabase uses range for pagination
                    // range(offset, offset + pageSize - 1)
                }
                .decodeList<Post>()
                .sortedByDescending { it.timestamp }
            
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ChatRepository.kt - Add pagination for messages
class ChatRepository {
    /**
     * Fetch messages with pagination (reverse chronological)
     * @param chatId Chat conversation ID
     * @param beforeTimestamp Load messages before this timestamp (for loading older)
     * @param limit Number of messages to fetch
     */
    suspend fun getMessagesPage(
        chatId: String,
        beforeTimestamp: Long? = null,
        limit: Int = 50
    ): Result<List<Message>> {
        return try {
            val query = client.from("messages")
                .select() {
                    filter {
                        eq("chat_id", chatId)
                        beforeTimestamp?.let { lt("created_at", it) }
                    }
                    order("created_at", ascending = false)
                    limit(limit.toLong())
                }
            
            val messages = query.decodeList<Message>()
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 3. ViewModel Layer

ViewModels will integrate PaginationManager and expose StateFlow for UI observation.

```kotlin
class HomeViewModel : ViewModel() {
    private val postRepository = PostRepository()
    
    // Pagination manager instance
    private val paginationManager = PaginationManager<Post>(
        pageSize = 20,
        scrollThreshold = 5,
        onLoadPage = { page, pageSize ->
            postRepository.getPostsPage(page, pageSize)
        },
        onError = { error ->
            _error.value = error
        }
    )
    
    // Expose pagination state
    val posts: StateFlow<List<Post>> = paginationManager.paginationState
        .map { state ->
            when (state) {
                is PaginationState.Success -> state.items
                is PaginationState.LoadingMore -> state.currentItems
                is PaginationState.Error -> state.currentItems
                is PaginationState.EndOfList -> state.items
                else -> emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val isLoading: StateFlow<Boolean> = paginationManager.paginationState
        .map { it is PaginationState.Refreshing || it is PaginationState.Initial }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    
    val isLoadingMore: StateFlow<Boolean> = paginationManager.paginationState
        .map { it is PaginationState.LoadingMore }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Public API
    fun loadPosts() {
        viewModelScope.launch {
            paginationManager.refresh()
        }
    }
    
    fun loadNextPage() {
        viewModelScope.launch {
            paginationManager.loadNextPage()
        }
    }
}
```

### 4. Fragment/Activity Integration

Fragments will set up SwipeRefreshLayout and attach PaginationManager to RecyclerView.

```kotlin
class HomeFragment : Fragment() {
    private lateinit var viewModel: HomeViewModel
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        
        // Initial load
        viewModel.loadPosts()
    }
    
    private fun setupRecyclerView() {
        postAdapter = PostAdapter(/* ... */)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
            
            // Attach scroll listener for infinite scroll
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                    
                    // Trigger load when within threshold of end
                    if (!viewModel.isLoadingMore.value && 
                        totalItemCount - lastVisibleItem <= 5) {
                        viewModel.loadNextPage()
                    }
                }
            })
        }
    }
    
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadPosts()
        }
        
        // Material Design 3 colors
        swipeRefreshLayout.setColorSchemeResources(
            R.color.md_theme_primary,
            R.color.md_theme_secondary,
            R.color.md_theme_tertiary
        )
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.posts.collect { posts ->
                postAdapter.submitList(posts)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                swipeRefreshLayout.isRefreshing = isLoading
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoadingMore.collect { isLoadingMore ->
                // Show/hide bottom loading indicator
                postAdapter.setLoadingMore(isLoadingMore)
            }
        }
    }
}
```

### 5. Adapter Modifications

Adapters need to support loading indicators at the bottom.

```kotlin
class PostAdapter : ListAdapter<Post, RecyclerView.ViewHolder>(PostDiffCallback()) {
    
    companion object {
        private const val VIEW_TYPE_POST = 0
        private const val VIEW_TYPE_LOADING = 1
    }
    
    private var isLoadingMore = false
    
    fun setLoadingMore(loading: Boolean) {
        val wasLoading = isLoadingMore
        isLoadingMore = loading
        
        if (loading && !wasLoading) {
            notifyItemInserted(itemCount)
        } else if (!loading && wasLoading) {
            notifyItemRemoved(itemCount)
        }
    }
    
    override fun getItemCount(): Int {
        return super.getItemCount() + if (isLoadingMore) 1 else 0
    }
    
    override fun getItemViewType(position: Int): Int {
        return if (position < super.getItemCount()) {
            VIEW_TYPE_POST
        } else {
            VIEW_TYPE_LOADING
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LOADING -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_loading_indicator, parent, false)
                LoadingViewHolder(view)
            }
            else -> {
                // Existing post view holder creation
                PostViewHolder(/* ... */)
            }
        }
    }
    
    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
    }
}
```

## Data Models

### Pagination Configuration

```kotlin
data class PaginationConfig(
    val pageSize: Int = 20,
    val scrollThreshold: Int = 5,
    val maxCachedItems: Int = 200,
    val enableScrollPositionRestore: Boolean = true,
    val scrollPositionRestoreTimeout: Long = 5 * 60 * 1000 // 5 minutes
)
```

### Scroll Position State

```kotlin
data class ScrollPositionState(
    val position: Int,
    val offset: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isExpired(timeout: Long): Boolean {
        return System.currentTimeMillis() - timestamp > timeout
    }
}
```

## Error Handling

### Error Types

1. **Network Errors**: Connection failures, timeouts
2. **API Errors**: Supabase query failures, authentication issues
3. **Data Errors**: Parsing failures, invalid data
4. **State Errors**: Invalid pagination state transitions

### Error Handling Strategy

```kotlin
sealed class PaginationError {
    data class NetworkError(val message: String) : PaginationError()
    data class ApiError(val code: String, val message: String) : PaginationError()
    data class DataError(val message: String) : PaginationError()
    object NoMoreData : PaginationError()
}

// In PaginationManager
private fun handleError(error: Throwable, currentItems: List<T>) {
    val paginationError = when {
        error is IOException -> PaginationError.NetworkError(error.message ?: "Network error")
        error.message?.contains("unauthorized") == true -> 
            PaginationError.ApiError("AUTH_ERROR", "Authentication failed")
        error.message?.contains("serialization") == true -> 
            PaginationError.DataError("Failed to parse data")
        else -> PaginationError.NetworkError(error.message ?: "Unknown error")
    }
    
    _paginationState.value = PaginationState.Error(
        message = getUserFriendlyMessage(paginationError),
        currentItems = currentItems
    )
    
    onError(getUserFriendlyMessage(paginationError))
}

private fun getUserFriendlyMessage(error: PaginationError): String {
    return when (error) {
        is PaginationError.NetworkError -> "Check your internet connection and try again"
        is PaginationError.ApiError -> "Unable to load data. Please try again later"
        is PaginationError.DataError -> "Something went wrong. Please try again"
        PaginationError.NoMoreData -> "No more items to load"
    }
}
```

### Retry Mechanism

```kotlin
// In Fragment
private fun showRetryOption() {
    Snackbar.make(
        binding.root,
        "Failed to load posts",
        Snackbar.LENGTH_INDEFINITE
    ).setAction("Retry") {
        viewModel.loadNextPage()
    }.show()
}
```

## Testing Strategy

### Unit Tests

1. **PaginationManager Tests**
   - Test state transitions
   - Test page calculation
   - Test scroll threshold detection
   - Test error handling
   - Test reset functionality

2. **Repository Tests**
   - Test pagination queries
   - Test offset calculation
   - Test error scenarios
   - Test empty result handling

3. **ViewModel Tests**
   - Test StateFlow emissions
   - Test refresh logic
   - Test load next page logic
   - Test error propagation

### Integration Tests

1. **End-to-End Pagination Flow**
   - Test pull-to-refresh updates UI
   - Test infinite scroll loads more items
   - Test scroll position restoration
   - Test network error handling

2. **Adapter Tests**
   - Test loading indicator display
   - Test DiffUtil updates
   - Test item count with loading state

### UI Tests (Espresso)

1. **Pull-to-Refresh Tests**
   - Test swipe gesture triggers refresh
   - Test loading indicator appears
   - Test list updates after refresh

2. **Infinite Scroll Tests**
   - Test scrolling to bottom loads more
   - Test loading indicator at bottom
   - Test end-of-list indicator

## Performance Considerations

### Memory Management

1. **Item Limit**: Maintain maximum 200 items in memory
2. **Cache Eviction**: Remove oldest items when limit reached
3. **Image Loading**: Use Glide with proper caching and downsampling

```kotlin
private fun enforceItemLimit() {
    if (loadedItems.size > maxCachedItems) {
        val itemsToRemove = loadedItems.size - maxCachedItems
        loadedItems.subList(0, itemsToRemove).clear()
        currentPage = (loadedItems.size / pageSize)
    }
}
```

### Scroll Performance

1. **DiffUtil**: Use for efficient RecyclerView updates
2. **ViewHolder Recycling**: Ensure proper view recycling
3. **Background Threading**: All data operations on IO dispatcher
4. **Debouncing**: Prevent rapid scroll triggers

```kotlin
private var scrollJob: Job? = null

private fun onScrollThresholdReached() {
    scrollJob?.cancel()
    scrollJob = viewModelScope.launch {
        delay(300) // Debounce
        loadNextPage()
    }
}
```

### Network Optimization

1. **Request Batching**: Fetch appropriate page sizes
2. **Caching**: Cache responses for quick restoration
3. **Cancellation**: Cancel pending requests on refresh

## Accessibility

### Screen Reader Support

1. **Loading Announcements**: Announce when loading starts/completes
2. **Error Messages**: Ensure errors are announced
3. **End of List**: Announce when no more items available

```kotlin
// In Fragment
private fun announceLoadingState(isLoading: Boolean) {
    val message = if (isLoading) {
        "Loading more posts"
    } else {
        "Posts loaded"
    }
    view?.announceForAccessibility(message)
}
```

### Content Descriptions

1. **Loading Indicators**: Proper content descriptions
2. **Retry Buttons**: Clear action descriptions
3. **Pull-to-Refresh**: Announce refresh action

## Material Design 3 Integration

### Visual Design

1. **Loading Indicators**
   - Use Material CircularProgressIndicator
   - Apply theme colors
   - Appropriate sizing (48dp for bottom indicator)

2. **Pull-to-Refresh**
   - Use SwipeRefreshLayout with MD3 colors
   - Smooth animations
   - Proper elevation and shadows

3. **Error States**
   - Use Snackbar for transient errors
   - Material buttons for retry actions
   - Appropriate iconography

### Layout Resources

```xml
<!-- item_loading_indicator.xml -->
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="16dp">
    
    <com.google.android.material.progressindicator.CircularProgressIndicator
        android:id="@+id/progressBar"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:layout_gravity="center"
        android:indeterminate="true"
        app:indicatorColor="?attr/colorPrimary" />
</FrameLayout>

<!-- item_end_of_list.xml -->
<TextView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="24dp"
    android:text="@string/end_of_list"
    android:textAlignment="center"
    android:textAppearance="?attr/textAppearanceBodyMedium"
    android:textColor="?attr/colorOnSurfaceVariant" />
```

## Migration Strategy

### Phase 1: Core Implementation
- Implement PaginationManager
- Add pagination methods to PostRepository
- Update HomeViewModel to use PaginationManager

### Phase 2: UI Integration
- Update HomeFragment with SwipeRefreshLayout
- Add scroll listener for infinite scroll
- Update PostAdapter for loading indicators

### Phase 3: Additional Screens
- Apply to ChatActivity for message history
- Apply to NotificationsFragment
- Apply to ProfileActivity for user posts

### Phase 4: Optimization
- Add scroll position restoration
- Implement memory management
- Performance testing and tuning

## Future Enhancements

1. **Bidirectional Pagination**: Support loading both older and newer items (for chat)
2. **Prefetching**: Load next page before threshold is reached
3. **Offline Support**: Cache data for offline viewing
4. **Smart Refresh**: Only refresh if data is stale
5. **Analytics**: Track pagination performance metrics
