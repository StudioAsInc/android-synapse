# Home, Fragments, Posts & Custom Views - Compose Migration Requirements

## Overview
Migrate the Home activity, its fragments (Home, Reels, Notifications), post rendering components, and custom views from XML-based layouts to Jetpack Compose while maintaining feature parity and improving performance.

## Scope

### In Scope
1. **HomeActivity** - Main container activity
2. **Fragments**
   - HomeFragment (feed)
   - ReelsFragment (short videos)
   - NotificationsFragment
3. **Post Components**
   - Post card layouts (synapse_post_cv.xml, item_post_md3.xml, item_post_enhanced.xml)
   - Post detail views
   - Post creation UI
4. **Custom Views**
   - Custom post card views
   - Media viewers (image, video)
   - Reaction pickers
   - Comment sections

### Out of Scope
- Profile screens (separate migration)
- Settings screens (separate migration)
- Chat/messaging features
- Authentication screens

## Goals

### Primary Goals
1. **Performance**: Improve scroll performance and reduce jank in feed
2. **Maintainability**: Simplify UI code with declarative Compose
3. **Consistency**: Unified Material 3 design system
4. **Reusability**: Create composable components for reuse

### Secondary Goals
1. Reduce layout inflation overhead
2. Improve animation smoothness
3. Better state management
4. Easier testing

## Current Architecture

### HomeActivity (activity_home.xml)
- CoordinatorLayout with AppBarLayout
- Top bar with app name, search, notifications icons
- BottomNavigationView with 3 tabs
- Fragment container for content

### Fragments
1. **HomeFragment** (fragment_home.xml)
   - SwipeRefreshLayout
   - RecyclerView for posts
   - Loading states
   - Empty states

2. **ReelsFragment** (fragment_reels.xml)
   - ViewPager2 for vertical scrolling
   - Video player integration
   - Interaction controls

3. **NotificationsFragment** (fragment_notifications.xml)
   - RecyclerView for notifications
   - Different notification types

### Post Layouts
1. **synapse_post_cv.xml** - Legacy post card
2. **item_post_md3.xml** - Material 3 post card
3. **item_post_enhanced.xml** - Enhanced post card

Each contains:
- User info header (avatar, username, badges, timestamp)
- Post content (text, images, videos, polls)
- Interaction bar (like, comment, share)
- Comment preview section

## Requirements

### Functional Requirements

#### FR-1: Home Screen
- Display app branding in top bar
- Show search and notification icons
- Support bottom navigation between tabs
- Maintain scroll position on tab switches
- Support pull-to-refresh

#### FR-2: Feed (HomeFragment)
- Display infinite scrolling post feed
- Show loading shimmer on initial load
- Support pull-to-refresh
- Handle empty states
- Maintain scroll position
- Prefetch images/videos

#### FR-3: Post Cards
- Display user avatar with click navigation
- Show username with verification badges
- Display gender badges
- Show post timestamp
- Render text content with mentions/hashtags
- Display single/multiple images
- Display videos with playback controls
- Show polls with voting
- Display like/comment/share counts
- Show reaction summary
- Preview top comments
- Support post options menu

#### FR-4: Post Interactions
- Like/unlike posts
- React with emoji reactions
- Comment on posts
- Share posts
- Save/bookmark posts
- Report posts
- Follow/unfollow users from post

#### FR-5: Reels
- Vertical swipe navigation
- Auto-play videos
- Pause on interaction
- Show creator info overlay
- Display interaction buttons
- Support double-tap to like

#### FR-6: Notifications
- Display notification list
- Show different notification types (like, comment, follow, mention)
- Mark as read on view
- Navigate to relevant content
- Support pull-to-refresh

### Non-Functional Requirements

#### NFR-1: Performance
- Feed scroll at 60fps minimum
- Post card render < 16ms
- Image loading with placeholders
- Video thumbnail generation
- Lazy loading for off-screen content

#### NFR-2: Accessibility
- Content descriptions for all interactive elements
- Support TalkBack
- Minimum touch target 48dp
- Sufficient color contrast
- Semantic structure

#### NFR-3: Responsive Design
- Support phone and tablet layouts
- Adapt to different screen sizes
- Handle landscape orientation
- Support split-screen mode

#### NFR-4: State Management
- Preserve scroll position
- Handle configuration changes
- Maintain like/comment states
- Cache loaded posts

#### NFR-5: Error Handling
- Network error states
- Image load failures
- Video playback errors
- Graceful degradation

## Technical Constraints

### Must Use
- Jetpack Compose UI
- Material 3 components
- Compose Navigation
- ViewModel for state
- Coil for image loading
- ExoPlayer for video (wrapped in Compose)

### Must Maintain
- MVVM architecture
- Repository pattern
- Supabase backend integration
- Existing ViewModels (adapt for Compose)
- RLS policies

### Must Avoid
- Mixing XML and Compose in same screen
- Direct database access from UI
- Blocking main thread
- Memory leaks

## Migration Strategy

### Phase 1: Foundation
1. Create base composables (PostCard, UserHeader, etc.)
2. Set up Compose navigation
3. Migrate HomeActivity to Compose

### Phase 2: Core Features
1. Migrate HomeFragment (feed)
2. Implement post rendering
3. Add interactions (like, comment, share)

### Phase 3: Additional Features
1. Migrate ReelsFragment
2. Migrate NotificationsFragment
3. Add animations and transitions

### Phase 4: Polish
1. Performance optimization
2. Accessibility improvements
3. Testing and bug fixes

## Success Criteria

### Must Have
- ✅ All existing features working
- ✅ No performance regression
- ✅ Passes accessibility audit
- ✅ All tests passing

### Should Have
- ✅ Improved scroll performance
- ✅ Smoother animations
- ✅ Reduced code complexity
- ✅ Better state management

### Nice to Have
- ✅ Shared element transitions
- ✅ Advanced animations
- ✅ Improved loading states
- ✅ Better error handling

## Dependencies

### Internal
- Existing ViewModels (HomeViewModel, PostViewModel, etc.)
- Repository layer
- Data models
- SupabaseClient

### External
- Compose BOM 2024.02.00+
- Material 3 Compose
- Compose Navigation
- Coil Compose
- ExoPlayer (for video)

## Risks & Mitigations

### Risk 1: Performance Regression
**Mitigation**: Benchmark before/after, use Compose performance tools, lazy loading

### Risk 2: Complex State Management
**Mitigation**: Use established patterns, StateFlow, remember/rememberSaveable

### Risk 3: Video Playback Issues
**Mitigation**: Wrap ExoPlayer properly, handle lifecycle, test extensively

### Risk 4: Breaking Changes
**Mitigation**: Feature flags, gradual rollout, A/B testing

## Testing Requirements

### Unit Tests
- ViewModel logic
- State transformations
- Business logic

### UI Tests
- Post rendering
- Interactions
- Navigation
- State persistence

### Integration Tests
- Feed loading
- Post creation
- Real-time updates

### Performance Tests
- Scroll performance
- Memory usage
- Frame rate monitoring

## Documentation Requirements

1. Architecture decision records
2. Component documentation
3. Migration guide
4. Code examples
5. Performance benchmarks

## Timeline Estimate

- Phase 1: 1 week
- Phase 2: 2 weeks
- Phase 3: 1 week
- Phase 4: 1 week

**Total: 5 weeks**

## Open Questions

1. Should we migrate all post layouts or consolidate to one?
2. How to handle video playback in feed vs reels?
3. Should we use Paging 3 for feed?
4. How to handle real-time post updates?
5. Should we implement shared element transitions?
