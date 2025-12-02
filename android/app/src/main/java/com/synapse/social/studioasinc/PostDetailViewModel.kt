package com.synapse.social.studioasinc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.*
import com.synapse.social.studioasinc.model.*
import com.synapse.social.studioasinc.data.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PostDetailViewModel : ViewModel() {
    private val postDetailRepository = PostDetailRepository()
    private val commentRepository = CommentRepository()
    private val reactionRepository = ReactionRepository()
    private val pollRepository = PollRepository()
    private val bookmarkRepository = BookmarkRepository()
    private val reshareRepository = ReshareRepository()
    private val reportRepository = ReportRepository()

    private val _postState = MutableStateFlow<PostDetailState>(PostDetailState.Loading)
    val postState: StateFlow<PostDetailState> = _postState.asStateFlow()

    private val _commentsState = MutableStateFlow<CommentsState>(CommentsState.Loading)
    val commentsState: StateFlow<CommentsState> = _commentsState.asStateFlow()

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    private var currentPostId: String? = null

    fun loadPost(postId: String) {
        currentPostId = postId
        viewModelScope.launch {
            _postState.value = PostDetailState.Loading
            when (val result = postDetailRepository.getPostWithDetails(postId)) {
                is Result.Success -> _postState.value = PostDetailState.Success(result.data)
                is Result.Error -> _postState.value = PostDetailState.Error(result.message)
                is Result.Loading -> {}
            }
            postDetailRepository.incrementViewCount(postId)
        }
    }

    fun loadComments(postId: String, limit: Int = 20, offset: Int = 0) {
        viewModelScope.launch {
            _commentsState.value = CommentsState.Loading
            when (val result = commentRepository.getComments(postId, limit, offset)) {
                is Result.Success -> _commentsState.value = CommentsState.Success(result.data, result.data.size >= limit)
                is Result.Error -> _commentsState.value = CommentsState.Error(result.message)
                is Result.Loading -> {}
            }
        }
    }

    fun toggleReaction(reactionType: ReactionType) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            when (val result = reactionRepository.togglePostReaction(postId, reactionType)) {
                is Result.Success -> loadPost(postId)
                is Result.Error -> _errorEvent.emit(result.message)
            }
        }
    }

    fun toggleCommentReaction(commentId: String, reactionType: ReactionType) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            when (val result = reactionRepository.toggleCommentReaction(commentId, reactionType)) {
                is Result.Success -> loadComments(postId)
                is Result.Error -> _errorEvent.emit(result.message)
            }
        }
    }

    fun addComment(content: String, parentCommentId: String? = null) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            when (val result = commentRepository.createComment(postId, content, null, parentCommentId)) {
                is Result.Success -> loadComments(postId)
                is Result.Error -> _errorEvent.emit(result.message)
            }
        }
    }

    fun deleteComment(commentId: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            when (val result = commentRepository.deleteComment(commentId)) {
                is Result.Success -> loadComments(postId)
                is Result.Error -> _errorEvent.emit(result.message)
            }
        }
    }

    fun editComment(commentId: String, content: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            when (val result = commentRepository.editComment(commentId, content)) {
                is Result.Success -> loadComments(postId)
                is Result.Error -> _errorEvent.emit(result.message)
            }
        }
    }

    fun votePoll(optionIndex: Int) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            when (val result = pollRepository.submitVote(postId, optionIndex)) {
                is Result.Success -> loadPost(postId)
                is Result.Error -> _errorEvent.emit(result.message)
            }
        }
    }

    fun toggleBookmark() {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            when (val result = bookmarkRepository.toggleBookmark(postId, null)) {
                is Result.Success -> loadPost(postId)
                is Result.Error -> _errorEvent.emit(result.message)
            }
        }
    }

    fun createReshare(commentary: String?) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            when (val result = reshareRepository.createReshare(postId, commentary)) {
                is Result.Success -> loadPost(postId)
                is Result.Error -> _errorEvent.emit(result.message)
            }
        }
    }

    fun reportPost(reason: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            val result = reportRepository.createReport(postId, reason, null)
            if (result is Result.Error) {
                _errorEvent.emit(result.message)
            }
        }
    }
    
    fun pinComment(commentId: String, postId: String) {
        viewModelScope.launch {
            when (val result = commentRepository.pinComment(commentId, postId)) {
                is Result.Success -> loadComments(postId)
                is Result.Error -> _errorEvent.emit(result.message)
            }
        }
    }
    
    fun hideComment(commentId: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            when (val result = commentRepository.hideComment(commentId)) {
                is Result.Success -> loadComments(postId)
                is Result.Error -> _errorEvent.emit(result.message)
            }
        }
    }
    
    fun reportComment(commentId: String, reason: String, description: String?) {
        viewModelScope.launch {
            val result = commentRepository.reportComment(commentId, reason, description)
            if (result is Result.Error) {
                _errorEvent.emit(result.message)
            }
        }
    }
}
