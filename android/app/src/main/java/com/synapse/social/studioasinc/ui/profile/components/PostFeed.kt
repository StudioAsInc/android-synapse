package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.ui.components.PostCard

@Composable
fun PostFeed(
    posts: List<Post>,
    likedPostIds: Set<String>,
    savedPostIds: Set<String>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onUserClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onMenuClick: (String) -> Unit,
    onMediaClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        if (posts.isEmpty() && !isLoading) {
            EmptyPostsState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        isLiked = post.id in likedPostIds,
                        isSaved = post.id in savedPostIds,
                        onUserClick = { onUserClick(post.authorUid) },
                        onLikeClick = { onLikeClick(post.id) },
                        onCommentClick = { onCommentClick(post.id) },
                        onShareClick = { onShareClick(post.id) },
                        onSaveClick = { onSaveClick(post.id) },
                        onMenuClick = { onMenuClick(post.id) },
                        onMediaClick = { onMediaClick(post.id) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                
                item {
                    LaunchedEffect(Unit) {
                        onLoadMore()
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPostsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "No posts yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Posts will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun PostFeedPreview() {
    MaterialTheme {
        PostFeed(
            posts = listOf(
                Post(
                    id = "1",
                    authorUid = "user1",
                    postText = "Sample post 1",
                    username = "john_doe",
                    likesCount = 10
                ),
                Post(
                    id = "2",
                    authorUid = "user2",
                    postText = "Sample post 2",
                    username = "jane_smith",
                    likesCount = 5
                )
            ),
            likedPostIds = setOf("1"),
            savedPostIds = setOf(),
            isLoading = false,
            isRefreshing = false,
            onRefresh = {},
            onLoadMore = {},
            onUserClick = {},
            onLikeClick = {},
            onCommentClick = {},
            onShareClick = {},
            onSaveClick = {},
            onMenuClick = {},
            onMediaClick = {}
        )
    }
}
