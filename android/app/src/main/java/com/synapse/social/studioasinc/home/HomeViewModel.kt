package com.synapse.social.studioasinc.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.model.Post
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.util.ScrollPositionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val postRepository: PostRepository = PostRepository()
) : ViewModel() {

    val posts: Flow<PagingData<Post>> = postRepository.getPosts()
        .cachedIn(viewModelScope)

    private var savedScrollPosition: ScrollPositionState? = null
    
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