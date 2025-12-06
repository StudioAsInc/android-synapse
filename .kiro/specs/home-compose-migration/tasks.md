# Home, Fragments, Posts & Custom Views - Compose Migration Tasks

## Phase 1: Foundation & Setup

### Task 1.1: Project Configuration
**Priority**: P0  
**Estimate**: 2 hours

- [ ] Add Compose BOM dependency
- [ ] Add Material 3 Compose dependency
- [ ] Add Compose Navigation dependency
- [ ] Add Coil Compose dependency
- [ ] Configure Compose compiler options
- [ ] Enable Compose in build.gradle

**Files**:
- `android/app/build.gradle`

**Acceptance Criteria**:
- All Compose dependencies added
- Project builds successfully
- Compose preview works

---

### Task 1.2: Theme Setup
**Priority**: P0  
**Estimate**: 4 hours

- [ ] Create `ui/theme/Color.kt` with Material 3 colors
- [ ] Create `ui/theme/Type.kt` with typography
- [ ] Create `ui/theme/Theme.kt` with SynapseTheme
- [ ] Migrate existing colors from colors.xml
- [ ] Support light/dark themes
- [ ] Add custom color tokens

**Files**:
- `app/src/main/java/com/synapse/social/ui/theme/Color.kt`
- `app/src/main/java/com/synapse/social/ui/theme/Type.kt`
- `app/src/main/java/com/synapse/social/ui/theme/Theme.kt`

**Acceptance Criteria**:
- Theme matches existing design
- Dark mode works correctly
- Typography scales properly

---

### Task 1.3: Base Composables
**Priority**: P0  
**Estimate**: 6 hours

- [ ] Create `CircularAvatar` composable
- [ ] Create `VerifiedBadge` composable
- [ ] Create `GenderBadge` composable
- [ ] Create `LoadingIndicator` composable
- [ ] Create `ErrorState` composable
- [ ] Create `EmptyState` composable
- [ ] Add previews for all components

**Files**:
- `app/src/main/java/com/synapse/social/ui/components/Avatar.kt`
- `app/src/main/java/com/synapse/social/ui/components/Badges.kt`
- `app/src/main/java/com/synapse/social/ui/components/States.kt`

**Acceptance Criteria**:
- All components render correctly
- Previews work
- Accessibility labels present

---

## Phase 2: Post Components

### Task 2.1: PostHeader Composable
**Priority**: P0  
**Estimate**: 4 hours

- [ ] Create PostHeader composable
- [ ] Add user avatar with click handler
- [ ] Add username with badges
- [ ] Add timestamp formatting
- [ ] Add options menu button
- [ ] Add follow button (conditional)
- [ ] Handle long usernames (ellipsize)

**Files**:
- `app/src/main/java/com/synapse/social/ui/components/post/PostHeader.kt`

**Acceptance Criteria**:
- Matches existing design
- All interactions work
- Handles edge cases

---

### Task 2.2: PostContent Composable
**Priority**: P0  
**Estimate**: 8 hours

- [ ] Create PostContent composable
- [ ] Handle text content with mentions/hashtags
- [ ] Create SingleImageContent
- [ ] Create MultiImageContent (grid layout)
- [ ] Create VideoContent with thumbnail
- [ ] Create PollContent
- [ ] Add click handlers for media
- [ ] Add content expansion for long text

**Files**:
- `app/src/main/java/com/synapse/social/ui/components/post/PostContent.kt`
- `app/src/main/java/com/synapse/social/ui/components/post/MediaContent.kt`
- `app/src/main/java/com/synapse/social/ui/components/post/PollContent.kt`

**Acceptance Criteria**:
- All content types render
- Media loads with placeholders
- Polls are interactive
- Text formatting works

---

### Task 2.3: PostInteractionBar Composable
**Priority**: P0  
**Estimate**: 4 hours

- [ ] Create PostInteractionBar composable
- [ ] Add like button with animation
- [ ] Add comment button with count
- [ ] Add share button
- [ ] Add bookmark button
- [ ] Add reaction picker trigger
- [ ] Handle count formatting (1K, 1M)

**Files**:
- `app/src/main/java/com/synapse/social/ui/components/post/PostInteractionBar.kt`

**Acceptance Criteria**:
- All buttons work
- Counts display correctly
- Animations smooth
- Ripple effects present

---

### Task 2.4: PostCard Composable
**Priority**: P0  
**Estimate**: 6 hours

- [ ] Create PostCard composable
- [ ] Integrate PostHeader
- [ ] Integrate PostContent
- [ ] Integrate PostInteractionBar
- [ ] Add reaction summary section
- [ ] Add comment preview section
- [ ] Add card elevation/shadow
- [ ] Handle loading state
- [ ] Add click handler for full post

**Files**:
- `app/src/main/java/com/synapse/social/ui/components/post/PostCard.kt`

**Acceptance Criteria**:
- Complete post renders
- All sections integrated
- Matches design specs
- Performance acceptable

---

### Task 2.5: ReactionPicker Composable
**Priority**: P1  
**Estimate**: 4 hours

- [ ] Create ReactionPicker bottom sheet
- [ ] Add emoji reactions (like, love, haha, wow, sad, angry)
- [ ] Add selection animation
- [ ] Handle dismiss
- [ ] Add haptic feedback
- [ ] Show current reaction

**Files**:
- `app/src/main/java/com/synapse/social/ui/components/post/ReactionPicker.kt`

**Acceptance Criteria**:
- All reactions selectable
- Smooth animations
- Proper dismiss behavior

---

### Task 2.6: CommentPreview Composable
**Priority**: P1  
**Estimate**: 3 hours

- [ ] Create CommentPreview composable
- [ ] Show top 2 comments
- [ ] Add "View all comments" button
- [ ] Handle no comments state
- [ ] Add comment author info
- [ ] Add like count per comment

**Files**:
- `app/src/main/java/com/synapse/social/ui/components/post/CommentPreview.kt`

**Acceptance Criteria**:
- Comments display correctly
- Navigation works
- Handles empty state

---

## Phase 3: Feed Screen

### Task 3.1: FeedViewModel Adaptation
**Priority**: P0  
**Estimate**: 4 hours

- [ ] Review existing HomeViewModel
- [ ] Adapt for Compose state management
- [ ] Use StateFlow for UI state
- [ ] Add pagination logic
- [ ] Add refresh logic
- [ ] Handle like/unlike
- [ ] Handle bookmark
- [ ] Add error handling

**Files**:
- `app/src/main/java/com/synapse/social/ui/home/FeedViewModel.kt`

**Acceptance Criteria**:
- State flows correctly
- All operations work
- Error handling robust

---

### Task 3.2: FeedScreen Composable
**Priority**: P0  
**Estimate**: 8 hours

- [ ] Create FeedScreen composable
- [ ] Add LazyColumn for posts
- [ ] Implement pull-to-refresh
- [ ] Add loading shimmer
- [ ] Add empty state
- [ ] Add error state
- [ ] Implement infinite scroll
- [ ] Add scroll-to-top FAB
- [ ] Preserve scroll position

**Files**:
- `app/src/main/java/com/synapse/social/ui/home/FeedScreen.kt`

**Acceptance Criteria**:
- Smooth scrolling
- Pull-to-refresh works
- Pagination works
- States handled correctly

---

### Task 3.3: Feed Loading States
**Priority**: P1  
**Estimate**: 3 hours

- [ ] Create shimmer loading composable
- [ ] Create empty feed state
- [ ] Create error state with retry
- [ ] Add loading indicator for pagination
- [ ] Handle network errors
- [ ] Handle no posts state

**Files**:
- `app/src/main/java/com/synapse/social/ui/home/FeedLoadingStates.kt`

**Acceptance Criteria**:
- All states render correctly
- Retry works
- User feedback clear

---

## Phase 4: Home Screen

### Task 4.1: HomeScreen Composable
**Priority**: P0  
**Estimate**: 6 hours

- [ ] Create HomeScreen composable
- [ ] Add Scaffold with TopAppBar
- [ ] Add BottomNavigationBar
- [ ] Add NavHost for tabs
- [ ] Handle tab selection
- [ ] Add search icon with navigation
- [ ] Add notification icon with badge
- [ ] Handle back press

**Files**:
- `app/src/main/java/com/synapse/social/ui/home/HomeScreen.kt`

**Acceptance Criteria**:
- Navigation works
- Tab switching smooth
- Icons functional
- Back press handled

---

### Task 4.2: HomeActivity Migration
**Priority**: P0  
**Estimate**: 4 hours

- [ ] Replace setContentView with setContent
- [ ] Remove fragment transactions
- [ ] Add SynapseTheme wrapper
- [ ] Handle navigation
- [ ] Test configuration changes
- [ ] Remove XML layout references

**Files**:
- `app/src/main/java/com/synapse/social/ui/home/HomeActivity.kt`

**Acceptance Criteria**:
- Activity uses Compose
- No XML dependencies
- Configuration changes handled

---

### Task 4.3: Navigation Setup
**Priority**: P0  
**Estimate**: 4 hours

- [ ] Create HomeNavGraph
- [ ] Add feed destination
- [ ] Add reels destination
- [ ] Add notifications destination
- [ ] Add post detail destination
- [ ] Add profile destination
- [ ] Handle deep links
- [ ] Add navigation animations

**Files**:
- `app/src/main/java/com/synapse/social/ui/navigation/HomeNavGraph.kt`

**Acceptance Criteria**:
- All routes work
- Deep links functional
- Animations smooth

---

## Phase 5: Reels Screen

### Task 5.1: ReelItem Composable
**Priority**: P1  
**Estimate**: 8 hours

- [ ] Create ReelItem composable
- [ ] Integrate ExoPlayer with Compose
- [ ] Add video player controls
- [ ] Add user info overlay
- [ ] Add interaction buttons (like, comment, share)
- [ ] Add double-tap to like
- [ ] Handle video lifecycle
- [ ] Add loading state
- [ ] Add error state

**Files**:
- `app/src/main/java/com/synapse/social/ui/reels/ReelItem.kt`
- `app/src/main/java/com/synapse/social/ui/components/VideoPlayer.kt`

**Acceptance Criteria**:
- Video plays correctly
- Controls work
- Lifecycle handled
- Performance good

---

### Task 5.2: ReelsScreen Composable
**Priority**: P1  
**Estimate**: 6 hours

- [ ] Create ReelsScreen composable
- [ ] Add VerticalPager
- [ ] Implement auto-play logic
- [ ] Pause on interaction
- [ ] Handle page changes
- [ ] Add loading state
- [ ] Preload next video
- [ ] Handle errors

**Files**:
- `app/src/main/java/com/synapse/social/ui/reels/ReelsScreen.kt`

**Acceptance Criteria**:
- Smooth vertical scrolling
- Auto-play works
- Preloading efficient
- No memory leaks

---

### Task 5.3: ReelsViewModel
**Priority**: P1  
**Estimate**: 4 hours

- [ ] Create ReelsViewModel
- [ ] Load reels from repository
- [ ] Handle pagination
- [ ] Track current reel
- [ ] Handle like/unlike
- [ ] Handle follow/unfollow
- [ ] Add error handling

**Files**:
- `app/src/main/java/com/synapse/social/ui/reels/ReelsViewModel.kt`

**Acceptance Criteria**:
- State management works
- All operations functional
- Error handling robust

---

## Phase 6: Notifications Screen

### Task 6.1: NotificationItem Composable
**Priority**: P1  
**Estimate**: 4 hours

- [ ] Create NotificationItem composable
- [ ] Handle like notification type
- [ ] Handle comment notification type
- [ ] Handle follow notification type
- [ ] Handle mention notification type
- [ ] Add user avatar
- [ ] Add notification text
- [ ] Add timestamp
- [ ] Add unread indicator

**Files**:
- `app/src/main/java/com/synapse/social/ui/notifications/NotificationItem.kt`

**Acceptance Criteria**:
- All types render correctly
- Click navigation works
- Unread state visible

---

### Task 6.2: NotificationsScreen Composable
**Priority**: P1  
**Estimate**: 4 hours

- [ ] Create NotificationsScreen composable
- [ ] Add LazyColumn for notifications
- [ ] Implement pull-to-refresh
- [ ] Add empty state
- [ ] Add loading state
- [ ] Mark as read on view
- [ ] Handle navigation

**Files**:
- `app/src/main/java/com/synapse/social/ui/notifications/NotificationsScreen.kt`

**Acceptance Criteria**:
- List renders correctly
- Pull-to-refresh works
- Mark as read works
- Navigation functional

---

### Task 6.3: NotificationsViewModel
**Priority**: P1  
**Estimate**: 3 hours

- [ ] Create NotificationsViewModel
- [ ] Load notifications
- [ ] Handle mark as read
- [ ] Handle refresh
- [ ] Track unread count
- [ ] Add error handling

**Files**:
- `app/src/main/java/com/synapse/social/ui/notifications/NotificationsViewModel.kt`

**Acceptance Criteria**:
- State flows correctly
- Operations work
- Unread count accurate

---

## Phase 7: Custom Views Migration

### Task 7.1: MediaViewer Composable
**Priority**: P1  
**Estimate**: 6 hours

- [ ] Create MediaViewer composable
- [ ] Add HorizontalPager for images
- [ ] Add zoom/pan gestures
- [ ] Add page indicator
- [ ] Add close button
- [ ] Add share button
- [ ] Add download button
- [ ] Handle video playback

**Files**:
- `app/src/main/java/com/synapse/social/ui/components/MediaViewer.kt`

**Acceptance Criteria**:
- Images zoomable
- Swipe navigation works
- All buttons functional

---

### Task 7.2: CommentSection Composable
**Priority**: P1  
**Estimate**: 8 hours

- [ ] Create CommentSection composable
- [ ] Add LazyColumn for comments
- [ ] Add comment input field
- [ ] Handle nested replies
- [ ] Add like comment
- [ ] Add delete comment
- [ ] Add edit comment
- [ ] Add loading states
- [ ] Handle pagination

**Files**:
- `app/src/main/java/com/synapse/social/ui/components/CommentSection.kt`

**Acceptance Criteria**:
- Comments display correctly
- Input works
- Nested replies work
- All operations functional

---

### Task 7.3: PostOptionsMenu Composable
**Priority**: P1  
**Estimate**: 3 hours

- [ ] Create PostOptionsMenu bottom sheet
- [ ] Add edit option (own posts)
- [ ] Add delete option (own posts)
- [ ] Add report option
- [ ] Add block user option
- [ ] Add hide post option
- [ ] Add copy link option
- [ ] Handle permissions

**Files**:
- `app/src/main/java/com/synapse/social/ui/components/PostOptionsMenu.kt`

**Acceptance Criteria**:
- All options work
- Permissions respected
- Confirmation dialogs present

---

## Phase 8: Performance & Polish

### Task 8.1: Performance Optimization
**Priority**: P0  
**Estimate**: 8 hours

- [ ] Add stable keys to LazyColumn items
- [ ] Optimize recomposition with remember
- [ ] Add derivedStateOf where needed
- [ ] Implement image caching strategy
- [ ] Optimize video preloading
- [ ] Profile with Compose tools
- [ ] Fix any jank issues
- [ ] Reduce overdraw

**Files**:
- Various

**Acceptance Criteria**:
- 60fps scrolling
- No visible jank
- Memory usage acceptable
- Battery usage reasonable

---

### Task 8.2: Animations
**Priority**: P1  
**Estimate**: 6 hours

- [ ] Add like button animation
- [ ] Add follow button animation
- [ ] Add tab switch animation
- [ ] Add post card enter animation
- [ ] Add pull-to-refresh animation
- [ ] Add reaction picker animation
- [ ] Add smooth transitions
- [ ] Test on different devices

**Files**:
- Various

**Acceptance Criteria**:
- All animations smooth
- No performance impact
- Feels polished

---

### Task 8.3: Accessibility
**Priority**: P0  
**Estimate**: 4 hours

- [ ] Add content descriptions
- [ ] Test with TalkBack
- [ ] Ensure 48dp touch targets
- [ ] Add semantic properties
- [ ] Test color contrast
- [ ] Add heading semantics
- [ ] Test keyboard navigation
- [ ] Fix any issues

**Files**:
- Various

**Acceptance Criteria**:
- TalkBack works correctly
- All elements accessible
- Meets WCAG guidelines

---

### Task 8.4: Error Handling
**Priority**: P0  
**Estimate**: 4 hours

- [ ] Add network error handling
- [ ] Add image load error handling
- [ ] Add video playback error handling
- [ ] Add retry mechanisms
- [ ] Add user-friendly error messages
- [ ] Add error logging
- [ ] Test edge cases

**Files**:
- Various

**Acceptance Criteria**:
- All errors handled gracefully
- User feedback clear
- Retry works

---

## Phase 9: Testing

### Task 9.1: Unit Tests
**Priority**: P0  
**Estimate**: 8 hours

- [ ] Test FeedViewModel
- [ ] Test ReelsViewModel
- [ ] Test NotificationsViewModel
- [ ] Test state transformations
- [ ] Test business logic
- [ ] Achieve 80%+ coverage

**Files**:
- `app/src/test/java/com/synapse/social/ui/home/`

**Acceptance Criteria**:
- All ViewModels tested
- Coverage target met
- All tests pass

---

### Task 9.2: UI Tests
**Priority**: P0  
**Estimate**: 8 hours

- [ ] Test PostCard rendering
- [ ] Test like interaction
- [ ] Test comment interaction
- [ ] Test navigation
- [ ] Test pull-to-refresh
- [ ] Test infinite scroll
- [ ] Test error states
- [ ] Test accessibility

**Files**:
- `app/src/androidTest/java/com/synapse/social/ui/home/`

**Acceptance Criteria**:
- All critical paths tested
- Tests reliable
- All tests pass

---

### Task 9.3: Integration Tests
**Priority**: P1  
**Estimate**: 6 hours

- [ ] Test feed loading
- [ ] Test post creation flow
- [ ] Test real-time updates
- [ ] Test offline behavior
- [ ] Test data persistence

**Files**:
- `app/src/androidTest/java/com/synapse/social/integration/`

**Acceptance Criteria**:
- End-to-end flows work
- Real-time updates work
- Offline mode works

---

## Phase 10: Cleanup & Documentation

### Task 10.1: Remove Old Code
**Priority**: P0  
**Estimate**: 4 hours

- [ ] Remove XML layout files
- [ ] Remove Fragment classes
- [ ] Remove old adapters
- [ ] Remove unused resources
- [ ] Update ProGuard rules
- [ ] Clean up imports

**Files**:
- Various

**Acceptance Criteria**:
- No dead code
- Build size reduced
- No warnings

---

### Task 10.2: Documentation
**Priority**: P0  
**Estimate**: 4 hours

- [ ] Document composable APIs
- [ ] Add KDoc comments
- [ ] Create migration guide
- [ ] Document state management
- [ ] Add code examples
- [ ] Update README

**Files**:
- `Docs/COMPOSE_MIGRATION.md`

**Acceptance Criteria**:
- All public APIs documented
- Examples clear
- Guide complete

---

### Task 10.3: Performance Benchmarks
**Priority**: P1  
**Estimate**: 3 hours

- [ ] Benchmark scroll performance
- [ ] Benchmark memory usage
- [ ] Benchmark startup time
- [ ] Compare with XML version
- [ ] Document results

**Files**:
- `Docs/PERFORMANCE_BENCHMARKS.md`

**Acceptance Criteria**:
- Benchmarks complete
- Results documented
- No regressions

---

## Summary

**Total Tasks**: 43  
**Estimated Time**: ~180 hours (~4.5 weeks)

**Priority Breakdown**:
- P0 (Critical): 25 tasks
- P1 (High): 18 tasks

**Phase Breakdown**:
- Phase 1: 12 hours
- Phase 2: 29 hours
- Phase 3: 15 hours
- Phase 4: 14 hours
- Phase 5: 18 hours
- Phase 6: 11 hours
- Phase 7: 17 hours
- Phase 8: 22 hours
- Phase 9: 22 hours
- Phase 10: 11 hours

**Dependencies**:
- Phase 1 must complete before all others
- Phase 2 must complete before Phase 3
- Phase 3 must complete before Phase 4
- Phases 5, 6, 7 can run in parallel after Phase 4
- Phase 8 requires most features complete
- Phase 9 requires all features complete
- Phase 10 is final cleanup
