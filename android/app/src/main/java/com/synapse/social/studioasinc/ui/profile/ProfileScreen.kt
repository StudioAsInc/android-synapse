package com.synapse.social.studioasinc.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapse.social.studioasinc.ui.components.EmptyState
import com.synapse.social.studioasinc.ui.components.ErrorState
import com.synapse.social.studioasinc.ui.profile.animations.crossfadeContent
import com.synapse.social.studioasinc.ui.profile.components.*

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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId, currentUserId)
    }

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refreshProfile(userId)
            pullRefreshState.endRefresh()
        }
    }

    Scaffold(
        topBar = {
            ProfileTopAppBar(
                username = (state.profileState as? ProfileUiState.Success)?.profile?.username ?: "",
                isScrolled = listState.firstVisibleItemIndex > 0,
                onBackClick = onNavigateBack,
                onMoreClick = { viewModel.toggleMoreMenu() },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            when (val profileState = state.profileState) {
                is ProfileUiState.Loading -> {
                    ProfileSkeleton()
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
                        message = profileState.message,
                        onRetry = { viewModel.refreshProfile() }
                    )
                }
                is ProfileUiState.Empty -> {
                    EmptyState(
                        message = "Profile not found",
                        onAction = onNavigateBack,
                        actionLabel = "Go Back"
                    )
                }
            }

            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.fillMaxWidth()
            )
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
            onQRCode = { viewModel.showQrCode() },
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
            profileUrl = "https://synapse.app/profile/${state.currentUserId}",
            onDismiss = { viewModel.hideShareSheet() }
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
        QRCodeDialog(
            profileUrl = "https://synapse.app/profile/${state.currentUserId}",
            onDismiss = { viewModel.hideQrCode() }
        )
    }

    if (state.showReportDialog) {
        val profile = (state.profileState as? ProfileUiState.Success)?.profile
        profile?.let {
            ReportUserDialog(
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
                    viewAsMode = state.viewAsMode,
                    userName = state.viewAsUserName,
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
                                message = "No photos yet",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp)
                            )
                        } else {
                            PhotoGrid(
                                photos = state.photos,
                                onPhotoClick = { /* TODO: Open photo viewer */ },
                                onLoadMore = { viewModel.loadMoreContent(ProfileContentFilter.PHOTOS) },
                                isLoadingMore = state.isLoadingMore
                            )
                        }
                    }
                    ProfileContentFilter.POSTS -> {
                        Column {
                            // User Details Section
                            UserDetailsSection(
                                location = profile.location,
                                joinedDate = profile.joinedDate,
                                relationshipStatus = profile.relationshipStatus,
                                birthday = profile.birthday,
                                work = profile.work,
                                education = profile.education,
                                currentCity = profile.currentCity,
                                hometown = profile.hometown,
                                website = profile.website,
                                gender = profile.gender,
                                pronouns = profile.pronouns,
                                linkedAccounts = profile.linkedAccounts,
                                isOwnProfile = state.isOwnProfile,
                                onCustomizeClick = { /* TODO: Navigate to edit details */ }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Following Section
                            FollowingSection(
                                following = emptyList(), // TODO: Load following
                                onUserClick = onNavigateToUserProfile,
                                onSeeAllClick = onNavigateToFollowing
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Posts Feed
                            if (state.posts.isEmpty()) {
                                EmptyState(
                                    message = if (state.isOwnProfile) "Share your first post" else "No posts yet",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp)
                                )
                            } else {
                                PostFeed(
                                    posts = state.posts,
                                    likedPostIds = state.likedPostIds,
                                    savedPostIds = state.savedPostIds,
                                    currentUserId = state.currentUserId,
                                    onLikeClick = { postId -> viewModel.toggleLike(postId) },
                                    onCommentClick = { /* TODO: Navigate to comments */ },
                                    onShareClick = { /* TODO: Share post */ },
                                    onSaveClick = { postId -> viewModel.toggleSave(postId) },
                                    onDeleteClick = { postId -> viewModel.deletePost(postId) },
                                    onReportClick = { postId -> viewModel.reportPost(postId, "Inappropriate content") },
                                    onLoadMore = { viewModel.loadMoreContent(ProfileContentFilter.POSTS) },
                                    isLoadingMore = state.isLoadingMore
                                )
                            }
                        }
                    }
                    ProfileContentFilter.REELS -> {
                        if (state.reels.isEmpty()) {
                            EmptyState(
                                message = "No reels yet",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp)
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
