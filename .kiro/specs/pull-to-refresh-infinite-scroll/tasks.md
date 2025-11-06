# Implementation Plan

- [x] 1. Create core pagination infrastructure





  - [x] 1.1 Create PaginationManager class with state management


    - Create `util/PaginationManager.kt` with generic type parameter
    - Implement PaginationState sealed class with all states (Initial, Refreshing, LoadingMore, Success, Error, EndOfList)
    - Implement StateFlow for pagination state management
    - Add configuration parameters (pageSize, scrollThreshold, onLoadPage callback, onError callback)
    - Implement internal state tracking (currentPage, isLoading, hasMoreData, loadedItems)
    - _Requirements: 7.1, 7.2, 7.4_

  - [x] 1.2 Implement PaginationManager refresh logic


    - Implement `refresh()` suspend function to reset state and load first page
    - Add logic to prevent concurrent refresh operations
    - Implement state transition from any state to Refreshing
    - Call onLoadPage callback with page 0
    - Update state to Success or Error based on result
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 7.3_

  - [x] 1.3 Implement PaginationManager infinite scroll logic


    - Implement `loadNextPage()` suspend function
    - Add checks to prevent loading when already loading or at end of list
    - Implement state transition to LoadingMore with current items
    - Calculate next page number and call onLoadPage callback
    - Append new items to loadedItems list
    - Update state to Success with hasMore flag or EndOfList
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 7.3_

  - [x] 1.4 Implement PaginationManager scroll detection


    - Implement `attachToRecyclerView()` function
    - Create RecyclerView.OnScrollListener
    - Calculate distance from end using LinearLayoutManager
    - Trigger loadNextPage when within scrollThreshold
    - Add debouncing to prevent rapid triggers (300ms delay)
    - _Requirements: 2.1, 8.2_

  - [x] 1.5 Implement PaginationManager memory management


    - Add maxCachedItems configuration parameter (default 200)
    - Implement `enforceItemLimit()` private function
    - Remove oldest items when limit exceeded
    - Recalculate currentPage after eviction
    - _Requirements: 8.3, 8.4_


  - [x] 1.6 Implement PaginationManager error handling

    - Create PaginationError sealed class (NetworkError, ApiError, DataError, NoMoreData)
    - Implement `handleError()` private function
    - Map exceptions to PaginationError types
    - Generate user-friendly error messages
    - Preserve current items in Error state
    - Call onError callback with message
    - _Requirements: 6.1, 6.2, 6.4, 6.5_

  - [x] 1.7 Add PaginationManager reset and utility methods


    - Implement `reset()` function to clear all state
    - Implement `detachFromRecyclerView()` to remove scroll listener
    - Add `getCurrentItems()` function to access loaded items
    - Add `isAtEnd()` function to check if more data available
    - _Requirements: 7.3_

- [x] 2. Extend repository layer for pagination






  - [x] 2.1 Add pagination methods to PostRepository

    - Add `getPostsPage(page: Int, pageSize: Int)` suspend function
    - Calculate offset as page * pageSize
    - Use Supabase `limit()` and `range()` for pagination
    - Sort posts by timestamp descending
    - Return Result<List<Post>> with proper error handling
    - Add detailed error messages for common failures
    - _Requirements: 1.2, 2.1, 6.5_

  - [x] 2.2 Add pagination methods to ChatRepository


    - Add `getMessagesPage(chatId: String, beforeTimestamp: Long?, limit: Int)` suspend function
    - Filter messages by chat_id
    - Use beforeTimestamp for loading older messages
    - Order by created_at descending
    - Return Result<List<Message>> with error handling
    - _Requirements: 3.2, 3.3_


  - [x] 2.3 Add caching layer to repositories

    - Create in-memory cache for recently fetched pages
    - Implement cache expiration (5 minutes)
    - Check cache before making network requests
    - Invalidate cache on refresh operations
    - _Requirements: 4.2, 4.3_

- [x] 3. Update ViewModels to use PaginationManager




  - [x] 3.1 Update HomeViewModel with pagination


    - Create PaginationManager instance with PostRepository.getPostsPage callback
    - Expose posts StateFlow by mapping PaginationState to List<Post>
    - Expose isLoading StateFlow for pull-to-refresh indicator
    - Expose isLoadingMore StateFlow for bottom loading indicator
    - Expose error StateFlow for error messages
    - Implement `loadPosts()` function calling paginationManager.refresh()
    - Implement `loadNextPage()` function calling paginationManager.loadNextPage()
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 7.4_

  - [x] 3.2 Create ChatViewModel with pagination


    - Create new ChatViewModel class extending ViewModel
    - Create PaginationManager instance with ChatRepository.getMessagesPage callback
    - Expose messages StateFlow by mapping PaginationState
    - Expose loading states (isLoading, isLoadingMore)
    - Implement `loadMessages()` and `loadOlderMessages()` functions
    - Handle scroll position preservation for prepending messages
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [x] 3.3 Add scroll position restoration to ViewModels


    - Create ScrollPositionState data class (position, offset, timestamp)
    - Add `saveScrollPosition()` function in ViewModels
    - Add `restoreScrollPosition()` function in ViewModels
    - Check timestamp to expire old positions (5 minutes)
    - Clear saved position on refresh
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 4. Update UI layer with pull-to-refresh




  - [x] 4.1 Add SwipeRefreshLayout to HomeFragment


    - Update `fragment_home.xml` to wrap RecyclerView with SwipeRefreshLayout
    - Set Material Design 3 color scheme (primary, secondary, tertiary)
    - Set up refresh listener to call viewModel.loadPosts()
    - Observe isLoading StateFlow to control refreshing state
    - _Requirements: 1.1, 1.2, 1.3, 5.1, 5.4_

  - [x] 4.2 Update HomeFragment with infinite scroll


    - Add RecyclerView.OnScrollListener to detect scroll position
    - Calculate distance from end using LinearLayoutManager
    - Trigger viewModel.loadNextPage() when within threshold
    - Observe isLoadingMore StateFlow to show bottom loading indicator
    - Observe error StateFlow to show error messages
    - _Requirements: 2.1, 2.2, 5.2, 6.1_

  - [x] 4.3 Implement scroll position restoration in HomeFragment


    - Save scroll position in onPause() using viewModel.saveScrollPosition()
    - Restore scroll position in onResume() using viewModel.restoreScrollPosition()
    - Use LinearLayoutManager.scrollToPositionWithOffset()
    - Only restore if position is not expired
    - _Requirements: 4.1, 4.2, 4.5_

  - [x] 4.4 Add SwipeRefreshLayout to ChatActivity


    - Update chat layout XML to include SwipeRefreshLayout
    - Configure for loading older messages (pull from top)
    - Set up refresh listener to call viewModel.loadOlderMessages()
    - Handle scroll position to maintain user's view after prepending
    - _Requirements: 3.1, 3.2, 3.3_

  - [x] 4.5 Add error handling UI to fragments


    - Implement Snackbar display for errors with Retry action
    - Add retry button click handler to call loadNextPage()
    - Show user-friendly error messages from ViewModel
    - Ensure errors are announced for accessibility
    - _Requirements: 6.1, 6.2, 6.3_

- [x] 5. Update adapters for loading indicators




  - [x] 5.1 Add loading indicator support to PostAdapter

    - Add VIEW_TYPE_LOADING constant
    - Create LoadingViewHolder class
    - Add `isLoadingMore` boolean field
    - Implement `setLoadingMore(loading: Boolean)` function
    - Override getItemCount() to include loading item
    - Override getItemViewType() to return loading type for last item
    - Update onCreateViewHolder() to create LoadingViewHolder
    - Notify adapter of loading item insertion/removal
    - _Requirements: 2.2, 5.2_


  - [x] 5.2 Create loading indicator layout resource

    - Create `item_loading_indicator.xml` layout file
    - Add Material CircularProgressIndicator (48dp size)
    - Center indicator with proper padding (16dp)
    - Apply theme color using ?attr/colorPrimary
    - Set indeterminate mode
    - Add content description for accessibility
    - _Requirements: 5.2, 5.4, 5.5_


  - [x] 5.3 Create end-of-list indicator layout

    - Create `item_end_of_list.xml` layout file
    - Add TextView with "No more items" message
    - Center text with proper padding (24dp)
    - Use Material textAppearanceBodyMedium
    - Apply colorOnSurfaceVariant for subtle appearance
    - Add string resource for localization
    - _Requirements: 2.4_

  - [x] 5.4 Add end-of-list support to PostAdapter


    - Add VIEW_TYPE_END_OF_LIST constant
    - Create EndOfListViewHolder class
    - Add `isAtEnd` boolean field
    - Implement `setEndOfList(atEnd: Boolean)` function
    - Update getItemCount() and getItemViewType() logic
    - Show end-of-list indicator when no more data available
    - _Requirements: 2.4_

  - [x] 5.5 Update ChatAdapter for message pagination


    - Add loading indicator at top for loading older messages
    - Implement smooth scroll position preservation when prepending
    - Calculate scroll offset before adding items
    - Restore scroll position after items added
    - _Requirements: 3.3_
-

- [x] 6. Add accessibility support




  - [x] 6.1 Add accessibility announcements for loading states


    - Implement `announceLoadingState()` function in fragments
    - Announce "Loading more posts" when loading starts
    - Announce "Posts loaded" when loading completes
    - Announce "No more posts available" at end of list
    - Use View.announceForAccessibility()
    - _Requirements: 5.5_

  - [x] 6.2 Add content descriptions to loading indicators


    - Set contentDescription on CircularProgressIndicator
    - Set contentDescription on retry buttons
    - Set contentDescription on end-of-list indicator
    - Use string resources for localization
    - _Requirements: 5.5_

  - [x] 6.3 Add accessibility support for pull-to-refresh


    - Ensure SwipeRefreshLayout announces refresh action
    - Add content description to SwipeRefreshLayout
    - Announce when refresh completes
    - _Requirements: 5.5_

- [x] 7. Add string resources and localization



  - [x] 7.1 Add pagination-related strings to strings.xml


    - Add "Loading more posts..." string
    - Add "Posts loaded" string
    - Add "No more posts available" string
    - Add "Failed to load posts" string
    - Add "Check your internet connection" string
    - Add "Retry" string
    - Add error message strings
    - _Requirements: 5.5, 6.1, 6.2_
-

- [x] 8. Performance optimization



  - [x] 8.1 Ensure DiffUtil is used in adapters


    - Verify PostAdapter uses ListAdapter with DiffUtil.ItemCallback
    - Implement proper areItemsTheSame() and areContentsTheSame()
    - Ensure efficient list updates without full refresh
    - _Requirements: 8.1_

  - [x] 8.2 Add coroutine dispatchers for background work


    - Ensure all repository operations use Dispatchers.IO
    - Ensure StateFlow updates use Dispatchers.Main
    - Use withContext() for dispatcher switching
    - _Requirements: 8.2_

  - [x] 8.3 Implement request cancellation on refresh


    - Cancel pending loadNextPage jobs when refresh is triggered
    - Use Job tracking in PaginationManager
    - Cancel jobs in refresh() function
    - _Requirements: 1.5_

  - [x] 8.4 Add performance monitoring


    - Log pagination timing metrics
    - Track frame rate during scroll with loading
    - Monitor memory usage with large lists
    - Add analytics events for pagination errors
    - _Requirements: 8.5_


- [x] 9. Integration and testing


  - [ ] 9.1 Test pull-to-refresh on HomeFragment
    - Manually test swipe-down gesture triggers refresh
    - Verify loading indicator appears and disappears
    - Verify list updates with new data
    - Test refresh clears scroll position
    - _Requirements: 1.1, 1.2, 1.3_

  - [ ] 9.2 Test infinite scroll on HomeFragment
    - Manually test scrolling to bottom loads more posts
    - Verify loading indicator appears at bottom
    - Verify new posts are appended to list
    - Test end-of-list indicator when no more data
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [ ] 9.3 Test error handling scenarios
    - Test network error during refresh (airplane mode)
    - Test network error during infinite scroll
    - Verify error messages are user-friendly
    - Test retry button functionality
    - Verify existing data is preserved on error
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

  - [ ] 9.4 Test scroll position restoration
    - Navigate away from HomeFragment and return
    - Verify scroll position is restored
    - Test that refresh clears saved position
    - Test position expiration after 5 minutes
    - _Requirements: 4.1, 4.2, 4.3, 4.5_

  - [ ] 9.5 Test chat message pagination
    - Test pull-to-refresh loads older messages
    - Verify scroll position is maintained when prepending
    - Test loading indicator at top of chat
    - Test beginning-of-conversation indicator
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [ ] 9.6 Test performance with large lists
    - Load 200+ items and verify smooth scrolling
    - Verify memory management evicts old items
    - Test scroll performance during active loading
    - Verify frame rate stays above 30 FPS
    - _Requirements: 8.3, 8.4, 8.5_

  - [ ] 9.7 Test accessibility features
    - Enable TalkBack and test announcements
    - Verify loading states are announced
    - Test content descriptions on all interactive elements
    - Verify error messages are announced
    - _Requirements: 5.5_
