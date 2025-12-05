# Profile Compose Components

## Component Hierarchy

```
ProfileScreen
├── ProfileTopAppBar
├── ProfileContent
│   ├── ProfileHeader
│   │   ├── ProfileCoverImage
│   │   ├── ProfileImage
│   │   └── ProfileInfo
│   ├── ProfileStats
│   ├── ProfileBio
│   ├── ProfileActionButtons
│   ├── ProfileTabs
│   └── ProfileContentGrid
├── ProfileSkeleton (loading state)
└── Bottom Sheets
    ├── ProfileMoreSheet
    ├── ProfileShareSheet
    ├── ViewAsSheet
    └── QRCodeSheet
```

## Core Components

### ProfileScreen
Main entry point composable that manages state and navigation.

**Parameters**:
- `userId`: Target user ID
- `currentUserId`: Logged-in user ID
- `onNavigateBack`: Back navigation callback
- `viewModel`: ProfileViewModel instance

### ProfileHeader
Displays cover image, profile picture, username, and verification badge.

### ProfileStats
Shows followers, following, and posts count with click handlers.

### ProfileActionButtons
Context-aware buttons (Follow/Edit Profile, Message, More).

### ProfileTabs
Tab row for Posts, Photos, Reels with filter state.

### ProfileContentGrid
Lazy grid displaying filtered content with pagination.

## Utility Components

### ProfileSkeleton
Loading placeholder with shimmer effect.

### EmptyState
Shown when no content available.

### ErrorState
Error display with retry action.

## Bottom Sheets

### ProfileMoreSheet
Options: View As, QR Code, Share, Report, Block, Mute.

### ProfileShareSheet
Share profile via different methods.

### ViewAsSheet
Preview profile as different user types.

### QRCodeSheet
Display profile QR code for scanning.

## Animations

### CrossfadeContent
Smooth transitions between content states.

### SlideInVertically
Bottom sheet entrance animation.

## Accessibility

All components include:
- Content descriptions
- Semantic ordering
- 48dp minimum touch targets
- Screen reader support
