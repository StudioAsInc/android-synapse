# Profile Compose Migration - Requirements Specification

## Functional Requirements

### FR-1: Profile Header
**Priority**: P0 (Critical)

#### FR-1.1: User Information Display
- Display profile image (circular, 120dp)
- Show name with verified badge if applicable
- Display username (@username format)
- Show nickname in parentheses beside name (small, 70% opacity)
- Display bio (max 3 lines, expandable with "See more")
- Support story ring indicator around profile image

#### FR-1.2: Action Buttons
- **Edit Profile**: Navigate to edit profile screen (own profile only)
- **Add Story**: Open story creation flow (own profile only)
- **More (⋮)**: Open bottom sheet with additional actions
- **Follow/Unfollow**: Toggle follow state (other profiles only)
- **Message**: Open chat with user (other profiles only)

#### FR-1.3: Statistics
- Display post count (e.g., "123 Posts")
- Display follower count (e.g., "1.2K Followers")
- Display following count (e.g., "890 Following")
- Make stats clickable to view detailed lists
- Format large numbers (1K, 1M, 1B)

### FR-2: Content Filtering
**Priority**: P0 (Critical)

#### FR-2.1: Filter Options
- **Photos**: Show photo grid view
- **Posts**: Show post feed
- **Reels**: Show reels grid
- Single selection (mutually exclusive)
- Persist selected filter across sessions
- Default to "Posts" view

#### FR-2.2: Filter Behavior
- Sticky filter bar below TopAppBar on scroll
- Smooth transition between content types (crossfade)
- Show loading state during content switch
- Display empty state if no content available

### FR-3: Photo Grid
**Priority**: P1 (High)

#### FR-3.1: Grid Display
- 3-column grid layout (phone)
- 4-column grid (tablet)
- 5-column grid (landscape)
- Square aspect ratio (1:1)
- Lazy loading (load 20 at a time)
- Infinite scroll with pagination

#### FR-3.2: Photo Interactions
- Tap to open full-screen viewer
- Swipe to navigate between photos
- Pinch to zoom in viewer
- Show photo metadata (date, likes, comments)
- Support video thumbnails with play icon overlay

### FR-4: User Details Section
**Priority**: P1 (High)

#### FR-4.1: Linked Social Media
- Display connected accounts (Instagram, Twitter, GitHub, LinkedIn, etc.)
- Show platform icons
- Tap to open external profile
- Support adding/removing accounts (own profile)

#### FR-4.2: Personal Information
- **Location**: City, Country (with map icon)
- **Joined Date**: Month Year (e.g., "January 2024")
- **Relationship Status**: Single, In a relationship, Married, etc.
- **Birthday**: Month Day (optional year)
- **Work**: Company name and position
- **Education**: University/School name
- **Current City**: Lives in location
- **Hometown**: From location
- **Website**: Personal website link
- **Gender**: Male, Female, Other, Prefer not to say
- **Pronouns**: He/Him, She/Her, They/Them, Custom

#### FR-4.3: Privacy Controls
- Each field has individual privacy setting (Public, Friends, Only Me)
- "Customize Details" button to edit information (own profile)
- Hide fields set to "Only Me" from other users
- Show lock icon for friends-only fields

### FR-5: Following Section
**Priority**: P1 (High)

#### FR-5.1: Following Display
- Show horizontal scrollable list of following users
- Display profile picture and name
- Show first 10 users, then "See All" button
- Indicate mutual following with badge

#### FR-5.2: Following Filters
- **All**: Show all following
- **Mutual**: Show mutual connections only
- **Recent**: Show recently followed users
- Filter chips with single selection

#### FR-5.3: Following Actions
- Tap user to navigate to their profile
- "See All Following" opens full list screen
- Support search in full list
- Show following count in section header

### FR-6: Posts Feed
**Priority**: P0 (Critical)

#### FR-6.1: Post Display
- Migrate PostCard to Compose
- Show posts in chronological order (newest first)
- Lazy loading (load 10 at a time)
- Infinite scroll with pagination
- Pull to refresh

#### FR-6.2: Post Interactions
- Like/Unlike post
- Comment on post
- Share post
- Save post
- Report post (other profiles)
- Delete post (own profile)
- Edit post (own profile)
- Long press for quick actions

### FR-7: Bottom Sheet Actions
**Priority**: P1 (High)

#### FR-7.1: More Menu
- **Share Profile**: Copy link, share to story, share via message, external apps
- **View As**: See profile as public, friends, or specific user
- **Lock Profile**: Make profile private (own profile)
- **Archive Profile**: Temporarily hide profile (own profile)
- **QR Code**: Generate and display profile QR code
- **Copy Profile Link**: Copy URL to clipboard
- **Settings**: Navigate to profile settings (own profile)
- **Activity Log**: View profile activity history (own profile)
- **Block User**: Block user (other profiles)
- **Report User**: Report user (other profiles)
- **Mute User**: Mute user's posts (other profiles)

#### FR-7.2: View As Feature
- Switch to selected view mode
- Show banner indicating current view mode
- "Exit View As" button to return to normal view
- Respect privacy settings in each view mode

### FR-8: Navigation & Scroll Behavior
**Priority**: P1 (High)

#### FR-8.1: TopAppBar Behavior
- Transparent when at top
- Transitions to solid color on scroll
- Shows username when scrolled
- Back button on left
- More menu on right
- Smooth color transition (200ms)

#### FR-8.2: Scroll Effects
- Parallax effect on profile image (scales down)
- Sticky filter bar
- Smooth scrolling with momentum
- Scroll to top on tab bar tap (if implemented)

### FR-9: Responsive Design
**Priority**: P1 (High)

#### FR-9.1: Phone Layout (< 600dp)
- Single column layout
- Compact spacing (8dp)
- 3-column photo grid
- Stacked action buttons

#### FR-9.2: Tablet Layout (≥ 600dp)
- Two-column layout for details section
- Expanded spacing (16dp)
- 4-column photo grid
- Side-by-side stats

#### FR-9.3: Landscape Layout
- Horizontal header (image left, info right)
- 5-column photo grid
- Optimized for wide screens

### FR-10: Accessibility
**Priority**: P1 (High)

#### FR-10.1: Screen Reader Support
- Content descriptions for all images
- Semantic ordering of content
- Announce state changes
- Group related content

#### FR-10.2: Touch Targets
- Minimum 48dp for all interactive elements
- Adequate spacing between buttons (8dp)
- Large enough tap areas for chips and filters

#### FR-10.3: Contrast & Readability
- WCAG AA compliance (4.5:1 for text)
- High contrast mode support
- Readable font sizes (minimum 12sp)

## Non-Functional Requirements

### NFR-1: Performance
**Priority**: P0 (Critical)

- Initial profile load < 2 seconds
- Smooth scrolling (60 FPS minimum)
- Image loading with progressive enhancement
- Efficient memory usage (< 100MB for profile)
- Lazy loading for all lists
- Cache profile data for offline viewing

### NFR-2: Architecture
**Priority**: P0 (Critical)

- Follow MVVM pattern
- Use Repository pattern for data access
- Implement UseCase/Interactor for business logic
- Separate UI state from business logic
- Use StateFlow for state management
- Implement proper error handling

### NFR-3: Code Quality
**Priority**: P0 (Critical)

- Follow DRY principle (Don't Repeat Yourself)
- SOLID principles compliance
- Kotlin coding conventions
- Null safety with proper operators (?, ?:, !!)
- Coroutines for async operations (viewModelScope)
- Proper resource management (strings, dimensions in XML)

### NFR-4: Testing
**Priority**: P1 (High)

- Unit tests for ViewModels (80% coverage)
- Unit tests for UseCases (90% coverage)
- Unit tests for Repositories (80% coverage)
- Compose UI tests for critical flows
- Integration tests for Supabase interactions
- Test RLS policies with multiple users
- Test null handling and edge cases

### NFR-5: Security
**Priority**: P0 (Critical)

- Respect Supabase RLS policies
- Validate user permissions before actions
- Sanitize user input
- Secure storage for cached data
- HTTPS only for API calls
- No sensitive data in logs

### NFR-6: Scalability
**Priority**: P1 (High)

- Support profiles with 10K+ posts
- Handle 1M+ followers efficiently
- Pagination for all large lists
- Efficient database queries
- Optimize image loading for large galleries

### NFR-7: Maintainability
**Priority**: P1 (High)

- Modular component structure
- Reusable composables
- Clear separation of concerns
- Comprehensive documentation
- Consistent naming conventions
- Version control with meaningful commits

### NFR-8: Compatibility
**Priority**: P1 (High)

- Android 8.0 (API 26) minimum
- Support Android 15 (API 35)
- Material 3 components
- Dark mode support
- RTL layout support
- Multiple screen sizes and densities

### NFR-9: Offline Support
**Priority**: P2 (Medium)

- Cache profile data locally
- Show cached data when offline
- Queue actions for sync when online
- Indicate offline status to user
- Sync on network restoration

### NFR-10: Analytics & Monitoring
**Priority**: P2 (Medium)

- Track profile view events
- Monitor performance metrics
- Log errors and crashes
- Track user interactions
- A/B testing capability for new features

## Data Requirements

### DR-1: Profile Data Model
```kotlin
data class UserProfile(
    val id: String,
    val username: String,
    val name: String,
    val nickname: String?,
    val bio: String?,
    val profileImageUrl: String?,
    val coverImageUrl: String?,
    val isVerified: Boolean,
    val isPrivate: Boolean,
    val postCount: Int,
    val followerCount: Int,
    val followingCount: Int,
    val joinedDate: Long,
    val location: String?,
    val relationshipStatus: String?,
    val birthday: String?,
    val work: String?,
    val education: String?,
    val currentCity: String?,
    val hometown: String?,
    val website: String?,
    val gender: String?,
    val pronouns: String?,
    val linkedAccounts: List<LinkedAccount>,
    val privacySettings: Map<String, PrivacyLevel>
)
```

### DR-2: Supabase Tables
- **profiles**: User profile information
- **posts**: User posts
- **photos**: User photos
- **reels**: User reels
- **followers**: Follower relationships
- **following**: Following relationships
- **linked_accounts**: Social media connections
- **privacy_settings**: Field-level privacy

### DR-3: API Endpoints
- `GET /profiles/{userId}`: Fetch profile data
- `GET /profiles/{userId}/posts`: Fetch user posts
- `GET /profiles/{userId}/photos`: Fetch user photos
- `GET /profiles/{userId}/reels`: Fetch user reels
- `GET /profiles/{userId}/followers`: Fetch followers
- `GET /profiles/{userId}/following`: Fetch following
- `PATCH /profiles/{userId}`: Update profile
- `POST /profiles/{userId}/follow`: Follow user
- `DELETE /profiles/{userId}/follow`: Unfollow user

## Dependencies

### Compose Dependencies
- `androidx.compose.ui:ui`
- `androidx.compose.material3:material3`
- `androidx.compose.ui:ui-tooling-preview`
- `androidx.activity:activity-compose`
- `androidx.lifecycle:lifecycle-viewmodel-compose`
- `androidx.navigation:navigation-compose`

### Image Loading
- `io.coil-kt:coil-compose`

### Supabase
- `io.github.jan-tennert.supabase:postgrest-kt`
- `io.github.jan-tennert.supabase:realtime-kt`
- `io.github.jan-tennert.supabase:storage-kt`

### Testing
- `androidx.compose.ui:ui-test-junit4`
- `androidx.compose.ui:ui-test-manifest`
- `junit:junit`
- `org.mockito:mockito-core`
- `org.jetbrains.kotlinx:kotlinx-coroutines-test`

## Migration Strategy

### Phase 1: Core Profile Screen
1. Create ProfileScreen composable
2. Implement ProfileViewModel
3. Migrate header section
4. Implement filter system
5. Test with existing data

### Phase 2: Content Sections
1. Migrate photo grid
2. Implement user details section
3. Create following section
4. Test all sections independently

### Phase 3: Post Migration
1. Migrate PostCard to Compose
2. Implement post feed
3. Migrate post interactions
4. Test post functionality

### Phase 4: Advanced Features
1. Implement bottom sheet actions
2. Add View As feature
3. Implement share functionality
4. Add QR code generation

### Phase 5: Polish & Optimization
1. Add animations and transitions
2. Optimize performance
3. Implement accessibility features
4. Comprehensive testing
5. Documentation

## Success Criteria

- [ ] All ProfileActivity features migrated to Compose
- [ ] Performance metrics met (< 2s load, 60 FPS)
- [ ] 80%+ test coverage
- [ ] Zero critical bugs
- [ ] Accessibility compliance (WCAG AA)
- [ ] Dark mode fully supported
- [ ] Consistent with SettingsComposeActivity design
- [ ] RLS policies respected
- [ ] Offline support functional
- [ ] Code review approved
- [ ] Documentation complete
