# Implementation Plan

- [x] 1. Create MediaGridView custom component





  - Create `MediaGridView.kt` as a custom FrameLayout that displays media in Facebook-style grid layouts
  - Implement layout logic for 2, 3, 4, and 5+ media items with proper spacing
  - Add `OnMediaClickListener` interface for handling media taps
  - Apply rounded corners (8dp) and centerCrop scaling to all media items
  - Add play icon overlay for video thumbnails
  - _Requirements: 1.3, 2.1, 2.2, 2.3, 2.4, 2.5, 9.1, 9.2, 9.4, 9.5_

- [x] 1.1 Write property test for grid spacing consistency


  - **Property 2: Media grid spacing is consistent**
  - **Validates: Requirements 2.5**

- [x] 1.2 Write property test for centerCrop scaling

  - **Property 18: Media grid uses centerCrop scaling**
  - **Validates: Requirements 9.1**

- [x] 1.3 Write property test for square aspect ratios

  - **Property 19: Grid cells maintain square aspect ratio**
  - **Validates: Requirements 9.2**

- [x] 1.4 Write property test for rounded corners

  - **Property 20: Grid corners are rounded consistently**
  - **Validates: Requirements 9.4**

- [x] 1.5 Write property test for video play icons

  - **Property 21: Video thumbnails show play icon**
  - **Validates: Requirements 9.5**

- [x] 1.6 Write unit tests for grid layouts


  - Test 2-item horizontal layout
  - Test 3-item asymmetric layout
  - Test 4-item 2x2 grid
  - Test 5+ item grid with "+N" overlay
  - Test single item with original aspect ratio
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 9.3_

- [x] 2. Update layout XML to use MediaGridView





  - Modify `item_post_enhanced.xml` to replace ViewPager2 with MediaGridView
  - Remove page indicator container and media count badge (no longer needed)
  - Ensure MediaGridView has proper constraints and sizing
  - _Requirements: 1.2, 1.3, 1.5_

- [x] 3. Enhance PostRepository with proper joins and error handling





  - Update `getPostsPage()` to use single query with joins for profiles and post_media tables
  - Add media URL construction helper method
  - Implement user profile caching with 5-minute expiration
  - Enhance error mapping to provide user-friendly messages for all error types
  - Add comprehensive logging for all Supabase operations
  - _Requirements: 3.1, 3.2, 4.1, 4.4, 7.1, 7.3, 8.1, 8.2, 10.5_

- [x] 3.1 Write property test for media URL fetching


  - **Property 3: Media URLs are properly fetched and constructed**
  - **Validates: Requirements 3.1, 3.2, 3.3**

- [x] 3.2 Write property test for profile caching


  - **Property 6: User profile data is cached**
  - **Validates: Requirements 4.4**

- [x] 3.3 Write property test for media ordering


  - **Property 16: Media attachments are ordered correctly**
  - **Validates: Requirements 7.3**

- [x] 3.4 Write property test for error logging and mapping


  - **Property 17: Errors are logged and mapped to user-friendly messages**
  - **Validates: Requirements 8.1, 8.2**

- [x] 3.5 Write property test for RLS error messages


  - **Property 22: RLS errors provide clear messages**
  - **Validates: Requirements 10.5**

- [x] 3.6 Write unit tests for error handling edge cases


  - Test null/empty username displays "Unknown User"
  - Test RLS error shows permission denied message
  - Test network error shows connection failed message
  - Test null avatar_url handled gracefully
  - _Requirements: 4.3, 5.3, 7.4, 8.3, 8.4_

- [x] 3.7 Write unit test for single query with joins


  - Test query includes profiles join
  - _Requirements: 7.1_

- [x] 4. Update Post model to include joined profile data



  - Add transient fields for username, avatarUrl, and isVerified
  - Update Post deserialization to handle nested profile data from joins
  - _Requirements: 4.1, 4.2_

- [x] 5. Implement image loading with retry logic





  - Create `ImageLoader` utility class with Glide integration
  - Implement retry logic with exponential backoff (up to 2 retries)
  - Add placeholder display logic (show during loading and after all retries fail)
  - Include proper authentication headers for Supabase Storage URLs
  - _Requirements: 3.3, 3.4, 3.5_

- [x] 5.1 Write property test for image retry logic


  - **Property 4: Image loading retries on failure**
  - **Validates: Requirements 3.4**

- [x] 5.2 Write property test for placeholder display


  - **Property 5: Placeholders shown appropriately**
  - **Validates: Requirements 3.5**
-

- [x] 6. Update EnhancedPostsAdapter to use MediaGridView




  - Remove ViewPager2 setup code from PostViewHolder
  - Bind MediaGridView with post media items
  - Set up OnMediaClickListener to open full-screen viewer
  - Update username display to use joined profile data with "Unknown User" fallback
  - Use ImageLoader for avatar and media loading
  - _Requirements: 1.1, 1.4, 2.1, 2.2, 2.3, 2.4, 3.3, 4.3, 4.5, 6.1, 6.2, 6.3_

- [x] 6.1 Write property test for vertical scrolling


  - **Property 1: Vertical scrolling is unobstructed**
  - **Validates: Requirements 1.1, 1.4**

- [x] 6.2 Write property test for username display performance


  - **Property 7: Username displays within performance threshold**
  - **Validates: Requirements 4.5**

- [x] 6.3 Write property test for media viewer opening


  - **Property 12: Media taps open full-screen viewer**
  - **Validates: Requirements 6.1**

- [x] 6.4 Write property test for viewer initialization


  - **Property 13: Viewer initialized with correct data and position**
  - **Validates: Requirements 6.2, 6.3**

- [x] 7. Enhance reaction error handling in PostRepository




  - Update `toggleReaction()` to verify authentication before proceeding
  - Add retry logic for network failures (up to 2 retries)
  - Implement specific error messages for RLS policy failures
  - Add logging for all reaction operations
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 8.1, 8.2_

- [x] 7.1 Write property test for authentication verification


  - **Property 8: Authentication verified before reactions**
  - **Validates: Requirements 5.1**

- [x] 7.2 Write property test for reaction persistence


  - **Property 9: Reactions persist to database correctly**
  - **Validates: Requirements 5.2**

- [x] 7.3 Write property test for reaction retry logic


  - **Property 10: Reaction failures trigger retries**
  - **Validates: Requirements 5.4**

- [x] 7.4 Write unit test for RLS error handling


  - Test RLS error shows specific permission denied message
  - _Requirements: 5.3_

- [x] 8. Update reaction UI to show immediate feedback





  - Modify EnhancedPostsAdapter to update reaction count immediately after successful reaction
  - Add optimistic UI updates (update UI before server response)
  - Revert UI changes if server operation fails
  - _Requirements: 5.5_


- [x] 8.1 Write property test for immediate UI updates

  - **Property 11: Reaction UI updates immediately**
  - **Validates: Requirements 5.5**

- [x] 9. Implement or verify full-screen media viewer




  - Ensure viewer receives all media URLs and starting position
  - Implement swipe gesture support for navigation between media items
  - Add position indicator display (e.g., "2 of 5")
  - Wire up MediaGridView click listener to open viewer
  - _Requirements: 6.2, 6.3, 6.4, 6.5_

- [x] 9.1 Write property test for viewer swipe navigation


  - **Property 14: Viewer supports swipe navigation**
  - **Validates: Requirements 6.4**

- [x] 9.2 Write property test for position indicator


  - **Property 15: Viewer displays position indicator**
  - **Validates: Requirements 6.5**
- [x] 10. Checkpoint - Ensure all tests pass




- [ ] 10. Checkpoint - Ensure all tests pass

  - Ensure all tests pass, ask the user if questions arise.

- [-] 11. Test with real Supabase data


  - Verify posts load with real usernames from profiles table
  - Verify media items load from Supabase Storage with correct URLs
  - Test reactions work end-to-end with proper error handling
  - Verify RLS policies allow proper access
  - Test error scenarios (network failures, RLS blocks, etc.)
  - _Requirements: 3.1, 3.2, 4.1, 5.1, 5.2, 10.1, 10.2, 10.3, 10.4_

- [ ] 12. Final Checkpoint - Ensure all tests pass

  - Ensure all tests pass, ask the user if questions arise.
