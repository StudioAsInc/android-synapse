# Profile Compose Migration - Design Specification

## Overview
Migrate ProfileActivity to Jetpack Compose using Material 3 components, implementing a Facebook-like scrolling experience with unified content flow instead of TabLayout.

## Design System

### Material 3 Components
- **Surface**: Background containers with elevation
- **Card**: Post items, info sections
- **Button**: Primary actions (Edit Profile, Add Story)
- **IconButton**: Secondary actions (More, Filter)
- **BottomSheet**: Modal actions (Share, View As, Lock Profile)
- **TopAppBar**: Navigation with scroll behavior
- **LazyVerticalGrid**: Photo grid (3 columns)
- **LazyColumn**: Main scrollable content
- **Chip**: Filters (Photos, Posts, Reels, Mutual Following)
- **Divider**: Section separators
- **Badge**: Notification indicators

### Color Scheme
Follow existing theme from SettingsComposeActivity:
- **Primary**: Brand color for CTAs
- **Surface**: Card backgrounds
- **SurfaceVariant**: Secondary containers
- **OnSurface**: Text and icons
- **Outline**: Borders and dividers
- **Error**: Destructive actions

### Typography
- **displaySmall**: Name (24sp, Bold)
- **titleMedium**: Username (16sp, Medium)
- **bodySmall**: Nickname (12sp, Regular, 70% opacity)
- **labelLarge**: Buttons (14sp, Medium)
- **bodyMedium**: Bio, details (14sp, Regular)
- **labelMedium**: Metadata (12sp, Medium)

## Layout Structure

### 1. Collapsing Header Section
```
┌─────────────────────────────────┐
│  [←]              [⋮ More]      │ TopAppBar (transparent → solid on scroll)
├─────────────────────────────────┤
│                                 │
│     ┌─────────────────┐         │
│     │  Profile Image  │         │ 120dp circular
│     │    + Story      │         │
│     └─────────────────┘         │
│                                 │
│  Name (Nickname)         [✓]    │ Verified badge if applicable
│  @username                      │
│                                 │
│  Bio text here...               │ Max 3 lines, expandable
│                                 │
│  [Edit Profile] [Add Story] [⋮] │ Action buttons
│                                 │
│  📊 123 Posts  👥 1.2K  👥 890  │ Stats (Posts, Followers, Following)
│                                 │
└─────────────────────────────────┘
```

### 2. Filter Section (Sticky)
```
┌─────────────────────────────────┐
│ [📷 Photos] [📝 Posts] [🎬 Reels]│ Chip group - single select
└─────────────────────────────────┘
```

### 3. Content Sections (Scrollable)

#### Photos Grid (when Photos filter active)
```
┌─────────────────────────────────┐
│ ┌─────┐ ┌─────┐ ┌─────┐         │
│ │     │ │     │ │     │         │ 3-column grid
│ └─────┘ └─────┘ └─────┘         │ AspectRatio 1:1
│ ┌─────┐ ┌─────┐ ┌─────┐         │
│ │     │ │     │ │     │         │
│ └─────┘ └─────┘ └─────┘         │
└─────────────────────────────────┘
```

#### User Details Section
```
┌─────────────────────────────────┐
│ About                           │
│ ─────────────────────────────── │
│ 🔗 Linked Accounts              │
│    Instagram • Twitter • GitHub │
│                                 │
│ 📍 Location: City, Country      │
│ 📅 Joined: January 2024         │
│ 💑 Relationship: Single         │
│ 🎂 Birthday: Jan 1              │
│ 🏢 Works at: Company            │
│ 🎓 Studied at: University       │
│ 🏠 Lives in: City               │
│                                 │
│ [Customize Details]             │
└─────────────────────────────────┘
```

#### Following Section
```
┌─────────────────────────────────┐
│ Following (890)                 │
│ [All] [Mutual] [Recent]         │ Filter chips
│ ─────────────────────────────── │
│ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐  │
│ │ A │ │ B │ │ C │ │ D │ │ E │  │ Horizontal scroll
│ └───┘ └───┘ └───┘ └───┘ └───┘  │
│ Name  Name  Name  Name  Name    │
│                                 │
│ [See All Following]             │
└─────────────────────────────────┘
```

#### Posts Section
```
┌─────────────────────────────────┐
│ Posts (123)                     │
│ ─────────────────────────────── │
│ ┌─────────────────────────────┐ │
│ │ Post Card Component         │ │ Reusable PostCard
│ │ (Migrated to Compose)       │ │
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ Post Card Component         │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

## Interaction Patterns

### Scroll Behavior
- **Parallax Effect**: Profile image scales down on scroll
- **TopAppBar**: Transparent → Solid with username on scroll
- **Sticky Filters**: Filter chips stick below TopAppBar
- **Infinite Scroll**: Load more posts/photos on reach bottom

### Gestures
- **Pull to Refresh**: Reload profile data
- **Swipe on Photos**: Navigate between photos (full-screen viewer)
- **Long Press on Post**: Quick actions menu
- **Pinch to Zoom**: On profile image

### Animations
- **Enter**: Fade in + Slide up (300ms)
- **Exit**: Fade out (200ms)
- **Shared Element**: Profile image transition from previous screen
- **Filter Switch**: Crossfade content (250ms)
- **Expand Bio**: Animated height change
- **Button Press**: Scale down to 0.95 (100ms)

## Bottom Sheet Actions

### More Menu (⋮)
```
┌─────────────────────────────────┐
│ Share Profile                   │
│ View As...                      │
│ Lock Profile                    │
│ Archive Profile                 │
│ QR Code                         │
│ Copy Profile Link               │
│ ─────────────────────────────── │
│ Settings                        │
│ Activity Log                    │
└─────────────────────────────────┘
```

### View As Options
- Public View
- Friends View
- Specific User View (search)

### Share Profile Options
- Copy Link
- Share to Story
- Share via Message
- Share to External Apps

## Responsive Design

### Phone (< 600dp)
- Single column layout
- 3-column photo grid
- Compact spacing (8dp)

### Tablet (≥ 600dp)
- Two-column layout for details
- 4-column photo grid
- Expanded spacing (16dp)
- Side-by-side stats

### Landscape
- Horizontal header layout
- Profile image on left, info on right
- 5-column photo grid

## Accessibility

### Content Descriptions
- Profile image: "Profile picture of [Name]"
- Action buttons: Clear labels
- Stats: "123 posts, 1200 followers, 890 following"
- Filter chips: "Show photos", "Show posts", "Show reels"

### Touch Targets
- Minimum 48dp for all interactive elements
- Adequate spacing between buttons (8dp)

### Screen Reader
- Semantic ordering of content
- Announce state changes (filter selection)
- Group related content

### Contrast
- WCAG AA compliance (4.5:1 for text)
- High contrast mode support

## State Management

### Loading States
- Skeleton screens for initial load
- Shimmer effect on placeholders
- Progress indicators for actions

### Empty States
- No posts: Illustration + "No posts yet"
- No photos: "No photos to show"
- No following: "Not following anyone"

### Error States
- Network error: Retry button
- Load failure: Error message + Refresh
- Permission denied: Explanation + Settings link

## Performance Considerations

### Image Loading
- Coil for async image loading
- Thumbnail → Full resolution
- Cache strategy: Memory + Disk
- Placeholder while loading

### Lazy Loading
- LazyColumn for posts (load 10 at a time)
- LazyVerticalGrid for photos (load 20 at a time)
- Pagination with loading indicator

### Memory Management
- Dispose unused composables
- Clear image cache on low memory
- Limit cached posts (50 max)

## Dark Mode Support
- Follow system theme
- Adjust elevation for visibility
- Ensure contrast in both modes
- Test all components in dark mode

## Consistency with SettingsComposeActivity
- Same spacing system (4dp grid)
- Matching color scheme
- Consistent button styles
- Similar navigation patterns
- Unified animation timings
