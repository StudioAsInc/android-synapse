package com.synapse.social.studioasinc.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.synapse.social.studioasinc.ui.components.post.PostCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = viewModel(),
    onPostClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onMediaClick: (Int) -> Unit // Added missing parameter
) {
    val uiState by viewModel.uiState.collectAsState()
    val posts = viewModel.posts.collectAsLazyPagingItems()

    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            posts.refresh()
            viewModel.refresh()
            pullRefreshState.endRefresh()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        if (posts.loadState.refresh is LoadState.Loading) {
            FeedLoading()
        } else if (posts.loadState.refresh is LoadState.Error) {
            val e = posts.loadState.refresh as LoadState.Error
            FeedError(
                message = e.error.localizedMessage ?: "Unknown error",
                onRetry = { posts.retry() }
            )
        } else if (posts.itemCount == 0 && posts.loadState.refresh is LoadState.NotLoading) {
            FeedEmpty()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    count = posts.itemCount,
                    key = posts.itemKey { it.id },
                    contentType = posts.itemContentType { "post" }
                ) { index ->
                    val post = posts[index]
                    if (post != null) {
                        PostCard(
                            state = viewModel.mapPostToState(post),
                            onLikeClick = { viewModel.likePost(post) },
                            onCommentClick = { onCommentClick(post.id) },
                            onShareClick = { /* Implement share */ },
                            onBookmarkClick = { viewModel.bookmarkPost(post) },
                            onUserClick = { onUserClick(post.authorUid) },
                            onPostClick = { onPostClick(post.id) },
                            onMediaClick = onMediaClick,
                            onOptionsClick = { /* Show options */ },
                            onPollVote = { /* Handle vote */ }
                        )
                    }
                }

                if (posts.loadState.append is LoadState.Loading) {
                    item { PostShimmer() }
                }

                if (posts.loadState.append is LoadState.Error) {
                    item {
                         // Small retry button at bottom
                        val e = posts.loadState.append as LoadState.Error
                        FeedError(message = "Error loading more", onRetry = { posts.retry() })
                    }
                }
            }
        }

        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
