# Profile Compose Migration - Task Breakdown

## Phase 1: Foundation & Architecture (Week 1)

### Task 1.1: Project Setup & Dependencies
**Estimated Time**: 2 hours
**Priority**: P0

- [ ] Add Compose dependencies to `app/build.gradle.kts`.
- [ ] Update Material 3 version to latest stable.
- [ ] Add Coil Compose for image loading.
- [ ] Add `androidx.navigation:navigation-compose`.
- [ ] Verify build health (`./gradlew build`).

### Task 1.2: Data Models
**Estimated Time**: 4 hours
**Priority**: P0

- [ ] Create `UserProfile` data class (including new fields: `stories`, `highlights`, `isFollowing`, `isBlocked`).
- [ ] Create `Story`, `Highlight`, `Post` (summary) models.
- [ ] Create `ProfileStats` data class.
- [ ] Create sealed classes for `ProfileUiState`.

### Task 1.3: Repository Layer
**Estimated Time**: 6 hours
**Priority**: P0

- [ ] Create `ProfileRepository` interface.
- [ ] Implement `ProfileRepositoryImpl` with Supabase.
- [ ] Add methods: `getProfile`, `getStories`, `getHighlights`, `getPosts`, `getPhotos`.
- [ ] Implement caching strategy (Room or in-memory cache).
- [ ] Implement `followUser`, `unfollowUser` with optimistic updates.

### Task 1.4: ViewModel & Business Logic
**Estimated Time**: 6 hours
**Priority**: P0

- [ ] Create `ProfileViewModel`.
- [ ] Implement `loadProfile` with coroutines.
- [ ] Implement separate flows for `headerState`, `contentState`, `storyState`.
- [ ] Handle error states and retries.
- [ ] Implement pagination logic for posts/grid.

## Phase 2: Core UI Components (Week 2)

### Task 2.1: Profile Header
**Estimated Time**: 8 hours
**Priority**: P0

- [ ] Implement `ProfileHeader` composable.
- [ ] **Profile Image**: Circle with "Story Ring" indicator.
- [ ] **Stats Row**: Clickable layout for Posts/Followers/Following.
- [ ] **Bio Section**: Text with expandable "See more" and auto-linking.
- [ ] **Action Buttons**: Edit, Share, Follow/Unfollow logic.

### Task 2.2: Story Highlights Bar
**Estimated Time**: 4 hours
**Priority**: P1

- [ ] Implement `HighlightsBar` horizontal scroll row.
- [ ] Create `HighlightItem` composable (Circle + Text).
- [ ] Add "New Highlight" (+) item for own profile.
- [ ] Add placeholder/shimmer loading state.

### Task 2.3: Content Filter & Navigation
**Estimated Time**: 4 hours
**Priority**: P0

- [ ] Implement `FilterBar` (Sticky header).
- [ ] Add tabs: Posts, Photos, Reels, Tagged.
- [ ] Implement sliding indicator animation.
- [ ] Manage tab state in ViewModel.

### Task 2.4: Media Grid
**Estimated Time**: 5 hours
**Priority**: P1

- [ ] Implement `MediaGrid` using `LazyVerticalGrid`.
- [ ] Handle aspect ratios (1:1 vs 9:16).
- [ ] Implement shared element transition key preparation.
- [ ] Add infinite scrolling (load more on scroll end).

## Phase 3: Advanced Features (Week 3)

### Task 3.1: Story Viewer
**Estimated Time**: 10 hours
**Priority**: P1

- [ ] Create `StoryViewerScreen` (Full screen overlay).
- [ ] Implement gesture navigation (Tap L/R, Hold, Swipe Down).
- [ ] Add progress bar segmentation.
- [ ] Connect to `ProfileViewModel` or separate `StoryViewModel`.

### Task 3.2: Profile Analytics (Own Profile)
**Estimated Time**: 6 hours
**Priority**: P2

- [ ] Create `AnalyticsBottomSheet`.
- [ ] Implement charts/graphs for engagement (using Canvas or library).
- [ ] Integrate with Analytics data source.

### Task 3.3: Interactions & Animations
**Estimated Time**: 6 hours
**Priority**: P1

- [ ] Implement TopAppBar scroll color transition.
- [ ] Add parallax effect to header.
- [ ] Implement "Follow" button morph animation.
- [ ] Add haptic feedback for interactions.

### Task 3.4: Post Feed Integration
**Estimated Time**: 8 hours
**Priority**: P0

- [ ] Migrate `PostCard` to Compose.
- [ ] Implement `PostFeed` list.
- [ ] Handle rich media in posts (Carousels, Video).
- [ ] Implement post actions (Like, Comment, Share).

## Phase 4: Integration & Polish (Week 4)

### Task 4.1: Navigation Integration
**Estimated Time**: 3 hours
**Priority**: P0

- [ ] Set up Navigation Graph (Profile -> Story, Profile -> Post Detail).
- [ ] Handle deep links.
- [ ] Replace `ProfileActivity` usage in app.

### Task 4.2: Settings & Bottom Sheets
**Estimated Time**: 5 hours
**Priority**: P1

- [ ] Implement "More" menu bottom sheet.
- [ ] Implement "Share Profile" sheet.
- [ ] Implement "QR Code" dialog.

### Task 4.3: Testing
**Estimated Time**: 10 hours
**Priority**: P0

- [ ] Unit tests for `ProfileViewModel` and UseCases.
- [ ] UI Tests for critical user flows (Follow, Tab Switch).
- [ ] Screenshot testing for different screen sizes.

### Task 4.4: Accessibility & Optimization
**Estimated Time**: 5 hours
**Priority**: P1

- [ ] Audit content descriptions.
- [ ] Verify touch targets.
- [ ] Profile memory usage (Bitmap handling).
- [ ] Optimize scrolling performance (Refactor unnecessary recompositions).

## Summary
- **Total Estimated Time**: ~80-90 hours.
- **Critical Path**: Data Layer -> Header -> Grid/Feed -> Navigation.
