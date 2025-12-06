package com.synapse.social.studioasinc.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapse.social.studioasinc.ui.components.EmptyState
import com.synapse.social.studioasinc.ui.components.ErrorState
import com.synapse.social.studioasinc.ui.profile.animations.crossfadeContent
import com.synapse.social.studioasinc.ui.profile.components.*

/**
 * Main Profile screen composable displaying user profile information and content.
 * 
 * Features:
 * - Profile header with cover and profile images
 * - User stats (followers, following, posts)
 * - Content tabs (Posts, Photos, Reels)
 * - Pull-to-refresh
 * - Bottom sheet actions (Share, View As, QR Code, etc.)
 * 
 * @param userId The ID of the user whose profile to display
 * @param currentUserId The ID of the currently logged-in user
 * @param onNavigateBack Callback for back navigation
 * @param onNavigateToEditProfile Callback to navigate to edit profile
 * @param onNavigateToFollowers Callback to navigate to followers list
 * @param onNavigateToFollowing Callback to navigate to following list
 * @param onNavigateToSettings Callback to navigate to settings
 * @param onNavigateToActivityLog Callback to navigate to activity log
 * @param onNavigateToUserProfile Callback to navigate to another user's profile
 * @param viewModel ProfileViewModel instance for state management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    currentUserId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToFollowers: () -> Unit = {},
    onNavigateToFollowing: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToActivityLog: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId, currentUserId)
    }

    Scaffold(
        topBar = {
            ProfileTopAppBar(
                username = (state.profileState as? ProfileUiState.Success)?.profile?.username ?: "",
                scrollProgress = if (listState.firstVisibleItemIndex > 0) 1f else 0f,
                onBackClick = onNavigateBack,
                onMoreClick = { viewModel.toggleMoreMenu() }
            )
        },
        modifier = Modifier.semantics { isTraversalGroup = true }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.refreshProfile(userId)
                isRefreshing = false
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val profileState = state.profileState) {
                is ProfileUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize())
                }
                is ProfileUiState.Success -> {
                    ProfileContent(
                        state = state,
                        profile = profileState.profile,
                        listState = listState,
                        viewModel = viewModel,
                        onNavigateToEditProfile = onNavigateToEditProfile,
                        onNavigateToFollowers = onNavigateToFollowers,
                        onNavigateToFollowing = onNavigateToFollowing,
                        onNavigateToUserProfile = onNavigateToUserProfile
                    )
                }
                is ProfileUiState.Error -> {
                    ErrorState(
                        title = "Error Loading Profile",
                        message = profileState.message,
                        onRetry = { viewModel.refreshProfile(userId) }
                    )
                }
                is ProfileUiState.Empty -> {
                    EmptyState(
                        icon = Icons.Default.Person,
                        title = "Profile Not Found",
                        message = "This profile doesn't exist or has been removed."
                    )
                }
            }
        }
    }

    // Bottom Sheets
    if (state.showMoreMenu) {
        val profile = (state.profileState as? ProfileUiState.Success)?.profile
        ProfileMoreMenuBottomSheet(
            isOwnProfile = state.isOwnProfile,
            onDismiss = { viewModel.toggleMoreMenu() },
            onShareProfile = { viewModel.showShareSheet() },
            onViewAs = { viewModel.showViewAsSheet() },
            onLockProfile = { 
                profile?.let { viewModel.lockProfile(!it.isPrivate) }
            },
            onArchiveProfile = { 
                profile?.let { viewModel.archiveProfile(true) }
            },
            onQrCode = { viewModel.showQrCode() },
            onCopyLink = { /* TODO: Copy to clipboard */ },
            onSettings = onNavigateToSettings,
            onActivityLog = onNavigateToActivityLog,
            onBlockUser = { 
                profile?.let { viewModel.blockUser(it.id) }
            },
            onReportUser = { viewModel.showReportDialog() },
            onMuteUser = { 
                profile?.let { viewModel.muteUser(it.id) }
            }
        )
    }

    if (state.showShareSheet) {
        ShareProfileBottomSheet(
            onDismiss = { viewModel.hideShareSheet() },
            onCopyLink = { /* TODO */ },
            onShareToStory = { /* TODO */ },
            onShareViaMessage = { /* TODO */ },
            onShareExternal = { /* TODO */ }
        )
    }

    if (state.showViewAsSheet) {
        ViewAsBottomSheet(
            onDismiss = { viewModel.hideViewAsSheet() },
            onViewAsPublic = { 
                viewModel.setViewAsMode(ViewAsMode.PUBLIC)
                viewModel.hideViewAsSheet()
            },
            onViewAsFriends = { 
                viewModel.setViewAsMode(ViewAsMode.FRIENDS)
                viewModel.hideViewAsSheet()
            },
            onViewAsSpecificUser = { 
                // TODO: Show user search dialog
                viewModel.setViewAsMode(ViewAsMode.SPECIFIC_USER, "User")
                viewModel.hideViewAsSheet()
            }
        )
    }

    if (state.showQrCode) {
        val profile = (state.profileState as? ProfileUiState.Success)?.profile
        QRCodeDialog(
            profileUrl = "https://synapse.app/profile/${profile?.username ?: ""}",
            username = profile?.username ?: "",
            onDismiss = { viewModel.hideQrCode() }
        )
    }

    if (state.showReportDialog) {
        val profile = (state.profileState as? ProfileUiState.Success)?.profile
        profile?.let {
            ReportUserDialog(
                username = it.username,
                onDismiss = { viewModel.hideReportDialog() },
                onReport = { reason -> viewModel.reportUser(it.id, reason) }
            )
        }
    }
}

@Composable
private fun ProfileContent(
    state: ProfileScreenState,
    profile: com.synapse.social.studioasinc.data.model.UserProfile,
    listState: androidx.compose.foundation.lazy.LazyListState,
    viewModel: ProfileViewModel,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToFollowers: () -> Unit,
    onNavigateToFollowing: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        // View As Banner
        if (state.viewAsMode != null) {
            item {
                ViewAsBanner(
                    viewMode = state.viewAsMode,
                    specificUserName = state.viewAsUserName,
                    onExitViewAs = { viewModel.exitViewAs() }
                )
            }
        }

        // Profile Header
        item {
            ProfileHeader(
                profileImageUrl = profile.profileImageUrl,
                name = profile.name,
                username = profile.username,
                nickname = profile.nickname,
                bio = profile.bio,
                isVerified = profile.isVerified,
                hasStory = false, // TODO: Implement story check
                postsCount = profile.postCount,
                followersCount = profile.followerCount,
                followingCount = profile.followingCount,
                isOwnProfile = state.isOwnProfile,
                onProfileImageClick = { /* TODO: Open full screen */ },
                onEditProfileClick = onNavigateToEditProfile,
                onAddStoryClick = { /* TODO: Open story creation */ },
                onMoreClick = { viewModel.toggleMoreMenu() },
                onStatsClick = { stat ->
                    when (stat) {
                        "followers" -> onNavigateToFollowers()
                        "following" -> onNavigateToFollowing()
                    }
                }
            )
        }

        // Content Filter Bar
        item {
            ContentFilterBar(
                selectedFilter = state.contentFilter,
                onFilterSelected = { filter -> viewModel.switchContentFilter(filter) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Content Section
        item {
            crossfadeContent(targetState = state.contentFilter) { filter ->
                when (filter) {
                    ProfileContentFilter.PHOTOS -> {
                        if (state.photos.isEmpty()) {
                            EmptyState(
                                icon = Icons.Default.PhotoLibrary,
                                title = "No Photos",
                                message = "Photos you share will appear here."
                            )
                        } else {
                            PhotoGrid(
                                items = state.photos.filterIsInstance<com.synapse.social.studioasinc.ui.profile.components.MediaItem>(),
                                onItemClick = { /* TODO: Open photo viewer */ },
                                isLoading = state.isLoadingMore
                            )
                        }
                    }
                    ProfileContentFilter.POSTS -> {
                        Column {
                            // User Details Section
                            UserDetailsSection(
                                details = com.synapse.social.studioasinc.ui.profile.components.UserDetails(
                                    location = profile.location,
                                    joinedDate = profile.joinedDate.toString(),
                                    relationshipStatus = profile.relationshipStatus,
                                    birthday = profile.birthday,
                                    work = profile.work,
                                    education = profile.education,
                                    website = profile.website,
                                    gender = profile.gender,
                                    pronouns = profile.pronouns,
                                    linkedAccounts = profile.linkedAccounts.map { 
                                        com.synapse.social.studioasinc.ui.profile.components.LinkedAccount(
                                            platform = it.platform,
                                            username = it.username
                                        )
                                    }
                                ),
                                isOwnProfile = state.isOwnProfile,
                                onCustomizeClick = { /* TODO: Navigate to edit details */ },
                                onWebsiteClick = { /* TODO: Open website */ }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Following Section
                            FollowingSection(
                                users = emptyList(),
                                selectedFilter = com.synapse.social.studioasinc.ui.profile.components.FollowingFilter.ALL,
                                onFilterSelected = { },
                                onUserClick = { user -> onNavigateToUserProfile(user.id) },
                                onSeeAllClick = onNavigateToFollowing
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Posts Feed
                            if (state.posts.isEmpty()) {
                                EmptyState(
                                    icon = Icons.Default.Article,
                                    title = "No Posts",
                                    message = "Posts will appear here when shared."
                                )
                            } else {
                                PostFeed(
                                    posts = state.posts.filterIsInstance<com.synapse.social.studioasinc.model.Post>(),
                                    likedPostIds = state.likedPostIds,
                                    savedPostIds = state.savedPostIds,
                                    currentUserId = state.currentUserId,
                                    isLoading = false,
                                    isRefreshing = false,
                                    onRefresh = { viewModel.refreshProfile(profile.id) },
                                    onLoadMore = { viewModel.loadMoreContent(ProfileContentFilter.POSTS) },
                                    onUserClick = onNavigateToUserProfile,
                                    onLikeClick = { postId -> viewModel.toggleLike(postId) },
                                    onCommentClick = { /* TODO: Navigate to comments */ },
                                    onShareClick = { /* TODO: Share post */ },
                                    onSaveClick = { postId -> viewModel.toggleSave(postId) },
                                    onDeletePost = { postId -> viewModel.deletePost(postId) },
                                    onReportPost = { postId, reason -> viewModel.reportPost(postId, reason) },
                                    onEditPost = { /* TODO: Edit post */ },
                                    onMediaClick = { /* TODO: Open media */ }
                                )
                            }
                        }
                    }
                    ProfileContentFilter.REELS -> {
                        if (state.reels.isEmpty()) {
                            EmptyState(
                                icon = Icons.Default.VideoLibrary,
                                title = "No Reels",
                                message = "Reels you create will appear here."
                            )
                        } else {
                            // TODO: Implement ReelsGrid component
                            Text("Reels coming soon")
                        }
                    }
                }
            }
        }
    }
}
