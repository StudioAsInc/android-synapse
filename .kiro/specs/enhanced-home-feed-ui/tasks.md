# Implementation Plan: Enhanced Home Feed UI/UX

- [ ] 1. Set up foundation and data models
  - Create enhanced data models and sealed classes for feed state management
  - Update Post model with new UI-related fields (location, isEdited, transient states)
  - Create Story model with serialization annotations
  - Create FeedItem sealed class for mixed content support
  - Create FeedUiState sealed class for state management
  - Create FeedError sealed class for error handling
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 6.1, 6.2, 6.3, 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 2. Implement StoryRepository
  - Create StoryRepository class in data/repository package
  - Implement getStories() function to fetch stories from Supabase
  - Add filtering logic for expired stories (>24 hours)
  - Implement markStoryAsViewed() function
  - Add error handling with Result wrapper
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [ ] 3. Implement RealtimeManager for live updates
  - Create RealtimeManager class in data package
  - Implement subscribeToFeedUpdates() using Supabase Realtime
  - Create SharedFlow for new posts
  - Implement unsubscribe() for cleanup
  - Add error handling and reconnection logic
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 4. Enhance HomeViewModel with pagination and real-time
  - Add StateFlow for stories, newPostsAvailable, and uiState
  - Implement pagination state (currentPage, isLoadingMore, hasMorePages)
  - Create loadInitialContent() function
  - Implement loadMorePosts() for infinite scroll
  - Add loadNewPosts() for real-time updates
  - Implement subscribeToRealtime() and unsubscribeFromRealtime()
  - Update refreshFeed() to reset pagination
  - _Requirements: 3.1, 3.2, 3.3, 7.1, 7.2, 7.3, 7.4, 7.5, 9.4_

- [ ] 5. Create Material Design 3 post card layout
  - Create/update item_post.xml with MaterialCardView
  - Add 16dp corner radius and 4dp elevation
  - Implement header section with circular avatar (40dp), username, timestamp, and options button
  - Add location TextView with icon (optional, initially gone)
  - Create content section with 14sp text, 1.2 line spacing, maxLines=3
  - Add "See more" link for truncated content
  - Implement media section with ShapeableImageView (max height 400dp)
  - Create interaction bar with like, comment, share, and bookmark buttons
  - Apply Material Design 3 color scheme using theme attributes
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 6. Create story bar layouts
  - Create header_stories.xml with horizontal RecyclerView
  - Add 12dp horizontal padding and 8dp vertical padding
  - Create item_story.xml with circular MaterialCardView (64dp diameter)
  - Add gradient border (3dp width) for unviewed stories
  - Include username TextView below avatar (12sp, maxLines=1)
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 7. Create empty and error state layouts
  - Create empty_state_feed.xml with illustration, message, and "Discover Users" button
  - Create error_state_feed.xml with error icon, message, and retry button
  - Add appropriate styling with Material Design 3 components
  - _Requirements: 5.1, 5.3, 5.4, 5.5_

- [ ] 8. Create NewPostsBanner custom view
  - Create NewPostsBanner class extending MaterialCardView
  - Implement show() method with count parameter and click listener
  - Implement hide() method
  - Add animateIn() with slide-down animation (250ms)
  - Add animateOut() with slide-up animation (200ms)
  - Style with primary color background and white text
  - _Requirements: 7.1, 7.2, 7.5_

- [ ] 9. Update fragment_home.xml layout
  - Wrap content in CoordinatorLayout
  - Add NewPostsBanner at top (initially gone)
  - Keep SwipeRefreshLayout with RecyclerView
  - Add shimmer_container LinearLayout for loading state
  - Include empty_state_feed layout (initially gone)
  - Include error_state_feed layout (initially gone)
  - Set RecyclerView clipToPadding=false with 8dp top padding
  - _Requirements: 3.1, 5.1, 5.2, 5.3, 7.1_

- [ ] 10. Implement animation utilities
  - Create AnimationUtils.kt in util package
  - Implement animatePostEntry() with fade-in and slide-up (300ms)
  - Implement animateLikeButton() with scale animation (1.0 to 1.3, 200ms)
  - Add haptic feedback to animateLikeButton()
  - Implement animateBannerIn() and animateBannerOut()
  - Use DecelerateInterpolator for entry, OvershootInterpolator for like
  - _Requirements: 2.1, 2.4, 2.5_

- [ ] 11. Enhance PostAdapter with animations and ViewBinding
  - Update PostAdapter to use ViewBinding (ItemPostBinding)
  - Add animatedPositions set to track animated items
  - Implement updatePosts() with animation parameter
  - Add addPosts() method for pagination
  - Update bind() to call animateEntry() for new items
  - Implement animateEntry() using AnimationUtils
  - Add setupInteractions() for all button click listeners
  - Implement animateLike() for like button animation
  - _Requirements: 2.1, 2.4, 2.5, 9.1, 9.3_

- [ ] 12. Implement PostInteractionListener interface
  - Create PostInteractionListener interface in PostAdapter file
  - Define onLikeClicked(post: Post) method
  - Define onCommentClicked(post: Post) method
  - Define onShareClicked(post: Post) method
  - Define onMoreOptionsClicked(post: Post) method
  - Define onUserClicked(userId: String) method
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [ ] 13. Enhance PostViewHolder with user data and like status loading
  - Add loadUserData() coroutine function using UserRepository
  - Update authorName and authorAvatar in Post object
  - Load and display user avatar with Glide (circular, 40dp)
  - Implement loadLikeStatus() using LikeRepository
  - Update like icon color based on isLiked state
  - Format like and comment counts (1000+ → 1K)
  - Add timestamp formatting (relative time: "2h ago", "1d ago")
  - _Requirements: 1.5, 4.1, 4.5, 8.1, 8.2, 8.4_

- [ ] 14. Implement interaction button handlers in PostAdapter
  - Update like button click to call animateLike() then onLikeClicked()
  - Add haptic feedback (50ms) to all interaction buttons
  - Update comment button to call onCommentClicked() with 200ms delay
  - Update share button to call onShareClicked() with 150ms delay
  - Update bookmark button with toggle animation and state change
  - Update more options button to call onMoreOptionsClicked()
  - Change icon colors on interaction (primary theme color within 100ms)
  - _Requirements: 2.2, 2.5, 4.1, 4.2, 4.3, 4.4_

- [ ] 15. Implement StoryAdapter
  - Create StoryAdapter class in home package
  - Implement ViewHolder with circular avatar and gradient border
  - Add click listener to navigate to story viewer
  - Implement updateStories() method
  - Add logic to show/hide gradient border based on isViewed
  - Load user avatars with Glide
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [ ] 16. Update HeaderAdapter to include stories
  - Update HeaderAdapter to display story bar
  - Initialize StoryAdapter within HeaderAdapter
  - Add method to update stories from HomeFragment
  - Hide story bar (height 0dp) when no stories available
  - _Requirements: 6.1, 6.5_

- [ ] 17. Implement pull-to-refresh in HomeFragment
  - Configure SwipeRefreshLayout with Material Design 3 colors
  - Set color scheme (primary, secondary, tertiary)
  - Set progress background color (surface)
  - Implement onRefresh listener to call viewModel.refreshFeed()
  - Stop refreshing animation when data loads
  - Show smooth loading animation (1000ms rotation)
  - _Requirements: 2.3, 3.1_

- [ ] 18. Implement infinite scroll in HomeFragment
  - Add scroll listener to RecyclerView
  - Detect when user is within 3 items of bottom
  - Call viewModel.loadMorePosts() when threshold reached
  - Prevent multiple simultaneous loads with isLoadingMore flag
  - Animate new posts with slide-up transition (250ms)
  - _Requirements: 2.4, 3.2, 3.3, 9.4_

- [ ] 19. Implement shimmer loading state
  - Create post_placeholder_layout.xml with shimmer views
  - Update showShimmer() to inflate 5 placeholder items
  - Add shimmer animation to placeholders
  - Implement hideShimmer() to remove placeholders
  - Show shimmer during initial load
  - _Requirements: 3.5, 5.2_

- [ ] 20. Implement state management in HomeFragment
  - Observe uiState StateFlow from ViewModel
  - Handle FeedUiState.Loading → show shimmer
  - Handle FeedUiState.Success → show content
  - Handle FeedUiState.Error → show error state with retry
  - Handle FeedUiState.Empty → show empty state
  - Implement retry button click to call viewModel.loadInitialContent()
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 21. Implement real-time updates in HomeFragment
  - Observe newPostsAvailable StateFlow from ViewModel
  - Show NewPostsBanner when count > 0
  - Display count text "X new posts available"
  - Implement banner click to call viewModel.loadNewPosts()
  - Scroll RecyclerView to top after loading new posts
  - Hide banner after loading
  - Call viewModel.subscribeToRealtime() in onResume()
  - Call viewModel.unsubscribeFromRealtime() in onPause()
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 22. Implement post interaction handlers in HomeFragment
  - Implement PostInteractionListener interface
  - Handle onLikeClicked() by calling LikeRepository.toggleLike()
  - Handle onCommentClicked() by showing PostCommentsBottomSheetDialog
  - Handle onShareClicked() by creating share intent
  - Handle onMoreOptionsClicked() by showing bottom sheet with options
  - Handle onUserClicked() by navigating to ProfileActivity
  - _Requirements: 4.1, 4.2, 4.3, 10.1, 10.2_

- [ ] 23. Enhance post options bottom sheet
  - Update bottom_sheet_post_options.xml with Material Design 3 styling
  - Show Edit/Delete/Statistics for own posts
  - Show Report/Hide for other users' posts
  - Show Copy Link and Share for all posts
  - Implement edit post navigation to EditPostActivity
  - Implement delete post with confirmation dialog
  - Implement report post to insert into reports table
  - Implement hide post to insert into hidden_posts table
  - Display bottom sheet within 150ms of button click
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 24. Implement image loading optimization
  - Configure Glide with placeholder and error images
  - Set disk cache strategy to AUTOMATIC
  - Use override(Target.SIZE_ORIGINAL) for proper sizing
  - Implement memory cache with 50MB limit
  - Cancel Glide requests in PostViewHolder.onViewRecycled()
  - Load images asynchronously to maintain scroll performance
  - _Requirements: 9.3, 9.5_

- [ ] 25. Implement pagination in PostRepository
  - Update getPosts() to accept page and limit parameters
  - Implement offset calculation (page * limit)
  - Add sorting by timestamp descending
  - Return Result with list of posts
  - Handle pagination errors gracefully
  - _Requirements: 3.2, 3.3, 9.4_

- [ ] 26. Implement error handling with retry
  - Show error state layout when load fails
  - Display error message from FeedError
  - Show retry button in error state
  - Implement retry with 500ms delay
  - Show toast for LoadMoreError (non-blocking)
  - Display "Unable to refresh. Check your connection." for network errors
  - _Requirements: 3.4, 5.3, 5.4_

- [ ] 27. Implement "Discover Users" navigation
  - Add "Discover Users" button to empty state layout
  - Implement click listener to navigate to SearchActivity
  - Pass intent extra to show user suggestions
  - _Requirements: 5.5_

- [ ] 28. Implement "See more" text expansion
  - Detect when post content exceeds 3 lines in PostViewHolder
  - Show "See more" link when content is truncated
  - Implement click listener to expand text (remove maxLines)
  - Change "See more" to "See less" after expansion
  - Animate expansion with smooth transition
  - _Requirements: 8.5_

- [ ] 29. Implement location display
  - Check if Post.location is not null in PostViewHolder
  - Show location TextView with location icon
  - Set visibility to VISIBLE when location exists
  - Format location text (12sp font size)
  - _Requirements: 8.3_

- [ ] 30. Implement bookmark functionality
  - Create BookmarkRepository in data/repository package
  - Implement toggleBookmark() method
  - Update bookmark icon state in PostViewHolder
  - Change icon to filled state within 100ms
  - Persist bookmark state to Supabase bookmarks table
  - _Requirements: 4.4_

- [ ] 31. Add accessibility support
  - Add content descriptions to all ImageViews and ImageButtons
  - Set minimum touch target size to 48dp for all interactive elements
  - Ensure color contrast meets WCAG AA standards
  - Add announcements for state changes (liked, bookmarked)
  - Test with TalkBack screen reader
  - _Requirements: All requirements (accessibility is cross-cutting)_

- [ ] 32. Optimize RecyclerView performance
  - Set setHasFixedSize(true) on RecyclerView
  - Implement setItemViewCacheSize(20) for better recycling
  - Use DiffUtil for efficient list updates in PostAdapter
  - Implement proper ViewHolder recycling pattern
  - Ensure scroll performance maintains 30+ FPS with 50 posts
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ] 33. Implement number formatting utility
  - Create NumberFormatter.kt in util package
  - Implement formatCount() to format numbers (1000+ → 1K, 1000000+ → 1M)
  - Use in PostViewHolder for like and comment counts
  - _Requirements: 4.5_

- [ ] 34. Implement relative time formatting
  - Create or update DateFormatter.kt in util package
  - Implement getRelativeTime() for timestamps
  - Format as "Just now", "2m ago", "1h ago", "1d ago", "1w ago"
  - Use in PostViewHolder for post timestamps
  - _Requirements: 8.2_

- [ ] 35. Add Material Design 3 theme colors
  - Update colors.xml with Material Design 3 color tokens
  - Define colorPrimary, colorSecondary, colorTertiary
  - Define colorSurface, colorOnSurface, colorOnSurfaceVariant
  - Apply theme colors to all UI components
  - _Requirements: 1.4_

- [ ] 36. Implement story viewer activity
  - Create StoryViewerActivity for full-screen story viewing
  - Implement swipe gestures for next/previous story
  - Add progress indicators at top
  - Implement auto-advance after 5 seconds
  - Mark story as viewed when opened
  - _Requirements: 6.4_

- [ ] 37. Write unit tests for HomeViewModel
  - Test loadInitialContent() loads posts successfully
  - Test refreshFeed() resets pagination and loads posts
  - Test loadMorePosts() increments page and appends posts
  - Test loadNewPosts() inserts new posts at beginning
  - Test subscribeToRealtime() creates subscription
  - Test error handling for network failures
  - _Requirements: All requirements (testing validates implementation)_

- [ ] 38. Write unit tests for repositories
  - Test PostRepository.getPosts() with pagination
  - Test StoryRepository.getStories() filters expired stories
  - Test LikeRepository.toggleLike() updates state correctly
  - Test BookmarkRepository.toggleBookmark() persists state
  - Mock Supabase client for isolated testing
  - _Requirements: All requirements (testing validates implementation)_

- [ ] 39. Write UI tests for HomeFragment
  - Test pull-to-refresh triggers data reload
  - Test infinite scroll loads more posts
  - Test post like button updates icon and count
  - Test comment button opens bottom sheet
  - Test empty state displays when no posts
  - Test error state displays with retry button
  - _Requirements: All requirements (testing validates implementation)_

- [ ] 40. Write integration tests for feed flow
  - Test complete flow: load feed → display posts → interact
  - Test pull-to-refresh → load new posts → update UI
  - Test scroll to bottom → load more → append posts
  - Test real-time update → show banner → load new posts
  - _Requirements: All requirements (testing validates implementation)_

- [ ] 41. Perform performance testing
  - Measure scroll FPS with 50+ posts loaded
  - Test memory usage during image loading
  - Measure initial load time (target < 2 seconds)
  - Test pagination load time (target < 1 second)
  - Profile RecyclerView recycling efficiency
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_
