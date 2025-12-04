# Profile Compose Migration - Requirements Specification

## Functional Requirements

### FR-1: Profile Header
**Priority**: P0 (Critical)

#### FR-1.1: User Information Display
- Display profile image (circular, 120dp) with optional "Story Ring" indicator (gradient border for active story, grey for viewed).
- Show name with verified badge if applicable (blue checkmark).
- Display username (@username format) with copy-to-clipboard on long press.
- Show nickname in parentheses beside name (small, 70% opacity).
- Display bio (max 150 characters initially, expandable with "See more" to full text).
- Detect and hyperlink hashtags (#), mentions (@), and URLs in bio.

#### FR-1.2: Action Buttons
- **Edit Profile**: Navigate to edit profile screen (own profile only).
- **Add Story**: Open story creation flow (own profile only).
- **Share Profile**: Quickly share profile link (new button).
- **More (⋮)**: Open bottom sheet with additional actions.
- **Follow/Unfollow**: Toggle follow state (other profiles only) with optimistic UI update.
- **Message**: Open chat with user (other profiles only).
- **Email/Call**: Visible if user has public contact info (other profiles only).

#### FR-1.3: Statistics
- Display post count (e.g., "123 Posts").
- Display follower count (e.g., "1.2K Followers").
- Display following count (e.g., "890 Following").
- **Interaction**: Tapping stats on own profile opens **Profile Analytics** (see FR-13). Tapping on other profiles opens the respective list.
- Format large numbers (1K, 1M, 1B).

### FR-2: Story Highlights (New Feature)
**Priority**: P1 (High)

#### FR-2.1: Highlights Display
- Horizontal scrollable list below action buttons/bio.
- Circular items (64dp) with cover image and title.
- "New Highlight" button (+) as the first item for own profile.
- Tapping a highlight opens the Story Viewer for that collection.
- Long press on highlight (own profile) to Edit/Delete.

#### FR-2.2: Create/Edit Highlight
- Interface to select past stories to add to a highlight.
- Option to set a cover image (from story or gallery) and title.

### FR-3: Content Filtering & Navigation
**Priority**: P0 (Critical)

#### FR-3.1: Filter Options
- **Photos**: Show photo grid view.
- **Posts**: Show post feed (linear layout).
- **Reels**: Show reels grid (9:16 aspect ratio).
- **Tagged**: Show posts where user is tagged.
- Single selection (mutually exclusive) with sliding indicator animation.
- Persist selected filter across sessions (optional, default to "Posts").

#### FR-3.2: Filter Behavior
- Sticky filter bar below TopAppBar on scroll.
- Smooth transition between content types (crossfade).
- Display specific empty states for each filter type (e.g., "No photos yet").

### FR-4: Photo/Media Grid
**Priority**: P1 (High)

#### FR-4.1: Grid Display
- 3-column grid layout (phone) with 1dp spacing.
- 4-column grid (tablet).
- Aspect ratio: 1:1 for Photos, 9:16 for Reels.
- Lazy loading (load 20 at a time).
- Infinite scroll with pagination.
- Multi-select capability (own profile) for bulk actions (Archive, Delete).

#### FR-4.2: Media Viewer Interaction
- Tap to open **Media Viewer** (full-screen, immersive).
- **Shared Element Transition**: Smooth expansion from grid to full screen.
- Swipe down to dismiss viewer and return to grid.
- Swipe left/right to navigate between media items.
- Pinch to zoom in viewer.
- Show photo metadata overlay (tap to toggle visibility).

### FR-5: User Details Section
**Priority**: P1 (High)

#### FR-5.1: Linked Social Media
- Display connected accounts (Instagram, Twitter, GitHub, LinkedIn).
- Show platform icons.
- Tap to open external profile via Intent.

#### FR-5.2: Personal Information
- **Location**: City, Country (clickable to search posts in location).
- **Joined Date**: Month Year.
- **Work/Education**: "Role at Company", "Studied at University".
- **Website**: Clickable link with link preview if possible.
- **Pronouns**: Visible next to name or in details.

#### FR-5.3: Privacy Controls
- Each field has individual privacy setting (Public, Friends, Only Me).
- "Customize Details" interface to toggle visibility.

### FR-6: Following Section & Connections
**Priority**: P1 (High)

#### FR-6.1: Following Display
- Show horizontal scrollable list of following users (suggested or recent).
- **Mutual Friends**: Explicitly highlight "Followed by X, Y, and 10 others".

#### FR-6.2: Following Filters
- **All**: Show all following.
- **Mutual**: Show mutual connections only.
- **Recent**: Show recently followed users.

### FR-7: Posts Feed
**Priority**: P0 (Critical)

#### FR-7.1: Post Display
- Migrate PostCard to Compose.
- Show posts in chronological order (newest first).
- Optimize for complex content (carousels, video auto-play).
- Lazy loading (load 10 at a time).

#### FR-7.2: Post Interactions
- Like (Double tap animation), Comment, Share, Save.
- **Quick Send**: Long press share button to send to recent contacts.
- **Edit/Delete**: Context menu for own posts.

### FR-8: Bottom Sheet Actions
**Priority**: P1 (High)

#### FR-8.1: More Menu
- **Settings**: Navigate to app settings.
- **Archive**: View archived posts/stories.
- **Activity Log**: View history.
- **QR Code**: Open QR code card.
- **Saved**: View saved posts.
- **Close Friends**: Manage close friends list.

#### FR-8.2: View As Feature
- Preview profile as Public or Specific User to verify privacy settings.

### FR-9: Navigation & Scroll Behavior
**Priority**: P1 (High)

#### FR-9.1: TopAppBar Behavior
- Transparent when at top, fades to solid surface color on scroll.
- Shows "Name" (and "Verified" badge) in toolbar when header scrolls off-screen.
- Back button (left), Notification/Menu icons (right).

#### FR-9.2: Scroll Effects
- **Overscroll**: Elastic stretch effect at top/bottom (Android 12+).
- **Parallax**: Cover image (if added in future) or profile header background parallax.

### FR-10: Responsive Design
**Priority**: P1 (High)

#### FR-10.1: Adaptive Layouts
- **Foldable Support**: Split layout (List on left, Details on right) for large foldables.
- **Tablet**: Two-column layout (Profile Info Left (30%), Content Right (70%)).

### FR-11: Story Viewer (New Feature)
**Priority**: P1 (High)

#### FR-11.1: Viewing Experience
- Full-screen immersive view.
- Progress bar at top segmented by number of stories.
- Tap right to next story, tap left to previous.
- Long press to pause.
- Swipe down to exit.
- Reply text field at bottom (for other profiles).
- Viewers list (swipe up) for own stories.

### FR-12: Profile Analytics (New Feature - Own Profile)
**Priority**: P2 (Medium)

#### FR-12.1: Insights Dashboard
- Accessible via tapping "Stats" or from Menu.
- **Overview**: Accounts reached, Accounts engaged, Total followers.
- **Content Interactions**: Likes, comments, shares breakdown.
- **Audience**: Growth graph, top locations, age range (visual charts).

### FR-13: Accessibility
**Priority**: P1 (High)

#### FR-13.1: Screen Reader Support
- Content descriptions for all images.
- Semantic headings (Header, Bio, Feed).
- **Custom Actions**: "Double tap to like", "Swipe to dismiss".

## Non-Functional Requirements

### NFR-1: Performance
**Priority**: P0 (Critical)
- **First Frame**: < 100ms.
- **Scroll Jank**: 0 dropped frames on Pixel 6 or equivalent.
- **Memory**: Aggressive recycling of bitmaps in media grid. Use `derivedStateOf` to minimize recompositions during scroll.

### NFR-2: Architecture
**Priority**: P0 (Critical)
- **MVI/MVVM**: Unidirectional Data Flow.
- **State Management**: `StateFlow` for UI state, `SharedFlow` for one-off events (navigation, toast).
- **Modularization**: Feature-based module structure (`feature:profile`).

### NFR-3: Code Quality
- **Compose Stability**: Ensure data classes are `@Stable` or `@Immutable` to aid compiler optimizations.
- **Previews**: Provide `@Preview` for Light/Dark mode and Font Scaling.

## Data Requirements

### DR-1: Profile Data Model Extensions
- `stories: List<Story>`
- `highlights: List<Highlight>`
- `isFollowing: Boolean`
- `isBlocked: Boolean`
- `isMuted: Boolean`
- `relationship: RelationshipStatus` (Follows You, Mutual)

### DR-2: Analytics Data
- Requires new endpoint or aggregate query: `GET /analytics/{userId}/summary`

## Success Criteria
- [ ] Profile migration complete with new Story and Highlight features.
- [ ] Analytics dashboard functional.
- [ ] 60fps performance on scroll.
- [ ] Seamless transitions (Shared Element) implemented.
