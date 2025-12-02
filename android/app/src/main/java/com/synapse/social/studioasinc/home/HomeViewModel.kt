package com.synapse.social.studioasinc.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.data.Result
import com.synapse.social.studioasinc.util.PaginationManager
import com.synapse.social.studioasinc.util.ScrollPositionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val postRepository: PostRepository = PostRepository()
) : ViewModel() {

    private val paginationManager = PaginationManager<Post>(
        pageSize = 20,
        scrollThreshold = 5,
        onLoadPage = { page, pageSize ->
            when (val result = postRepository.getPostsPage(page, pageSize)) {
                is Result.Success -> result.data
                is Result.Error -> {
                    _error.value = result.message
                    emptyList()
                }
                else -> emptyList()
            }
        },
        onError = { error ->
            _error.value = error
        },
        coroutineScope = viewModelScope
    )
    
    val paginationState: StateFlow<PaginationManager.PaginationState<Post>> = 
        paginationManager.paginationState.stateIn(
            viewModelScope, 
            SharingStarted.Lazily, 
            PaginationManager.PaginationState.Initial
        )
    
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
    
    val isLoading: StateFlow<Boolean> = paginationManager.paginationState
        .map { it is PaginationManager.PaginationState.Refreshing || it is PaginationManager.PaginationState.Initial }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    
    val isLoadingMore: StateFlow<Boolean> = paginationManager.paginationState
        .map { it is PaginationManager.PaginationState.LoadingMore }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var savedScrollPosition: ScrollPositionState? = null

    init {
        loadPosts()
    }

    fun loadPosts() {
        postRepository.invalidateCache()
        savedScrollPosition = null
        paginationManager.refresh()
    }

    fun loadNextPage() {
        paginationManager.loadNextPage()
    }

    fun refreshPosts() {
        loadPosts()
    }

    fun clearError() {
        _error.value = null
    }
    
    fun saveScrollPosition(position: Int, offset: Int) {
        savedScrollPosition = ScrollPositionState(position, offset)
    }
    
    fun restoreScrollPosition(): ScrollPositionState? {
        val position = savedScrollPosition
        return if (position != null && !position.isExpired()) {
            position
        } else {
            savedScrollPosition = null
            null
        }
    }
}
