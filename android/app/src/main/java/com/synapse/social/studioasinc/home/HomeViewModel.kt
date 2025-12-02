package com.synapse.social.studioasinc.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.util.PaginationManager
import com.synapse.social.studioasinc.util.ScrollPositionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository: AuthRepository = AuthRepository()
    private val postRepository: PostRepository = PostRepository(AppDatabase.getDatabase(application).postDao())

    // Pagination manager instance
    private val paginationManager = PaginationManager<Post>(
        pageSize = 20,
        scrollThreshold = 5,
        onLoadPage = { page, pageSize ->
            var result: Result<List<Post>> = Result.success(emptyList())
            postRepository.getPosts().collect { flowResult ->
                result = flowResult
            }
            result
        },
        onError = { error ->
            _error.value = error
        },
        coroutineScope = viewModelScope
    )
    
    // Expose pagination state for accessibility announcements
    val paginationState: StateFlow<PaginationManager.PaginationState<Post>> = 
        paginationManager.paginationState.stateIn(
            viewModelScope, 
            SharingStarted.Lazily, 
            PaginationManager.PaginationState.Initial
        )
    
    // Expose posts StateFlow by mapping PaginationState to List<Post>
    val posts: StateFlow<List<Post>> = paginationManager.paginationState
        .map { state ->
            when (state) {
                is PaginationManager.PaginationState.Success -> state.items
                is PaginationManager.PaginationState.LoadingMore -> state.currentItems
                is PaginationManager.PaginationState.Error -> state.currentItems
                is PaginationManager.PaginationState.EndOfList -> state.items
                else -> emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // Expose isLoading StateFlow for pull-to-refresh indicator
    val isLoading: StateFlow<Boolean> = paginationManager.paginationState
        .map { it is PaginationManager.PaginationState.Refreshing || it is PaginationManager.PaginationState.Initial }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    
    // Expose isLoadingMore StateFlow for bottom loading indicator
    val isLoadingMore: StateFlow<Boolean> = paginationManager.paginationState
        .map { it is PaginationManager.PaginationState.LoadingMore }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    // Expose error StateFlow for error messages
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Scroll position state for restoration
    private var savedScrollPosition: ScrollPositionState? = null

    init {
        loadPosts()
    }

    // Implement loadPosts() function calling paginationManager.refresh()
    fun loadPosts() {
        // Invalidate cache on refresh
        postRepository.invalidateCache()
        // Clear saved position on refresh
        savedScrollPosition = null
        paginationManager.refresh()
    }

    // Implement loadNextPage() function calling paginationManager.loadNextPage()
    fun loadNextPage() {
        paginationManager.loadNextPage()
    }

    fun refreshPosts() {
        loadPosts()
    }

    fun clearError() {
        _error.value = null
    }
    
    /**
     * Save scroll position for restoration
     * Called when navigating away from the feed
     * 
     * @param position The scroll position (item index)
     * @param offset The offset within the item
     */
    fun saveScrollPosition(position: Int, offset: Int) {
        savedScrollPosition = ScrollPositionState(position, offset)
    }
    
    /**
     * Restore scroll position if not expired
     * Called when returning to the feed
     * 
     * @return ScrollPositionState if valid and not expired, null otherwise
     */
    fun restoreScrollPosition(): ScrollPositionState? {
        val position = savedScrollPosition
        
        // Check if position exists and is not expired (5 minutes)
        return if (position != null && !position.isExpired()) {
            position
        } else {
            // Clear expired position
            savedScrollPosition = null
            null
        }
    }
}