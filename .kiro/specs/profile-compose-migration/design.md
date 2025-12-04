# Profile Compose Migration - Design Specification

## Overview
Migrate ProfileActivity to Jetpack Compose using Material 3 components. The design aims for a modern, fluid user experience similar to leading social platforms, featuring unified content flow, rich interactions, and adaptive layouts.

## Design System

### Material 3 Components
- **Surface**: Background containers with elevation (Surface, SurfaceVariant).
- **Card**: Post items, info sections (ElevatedCard, OutlinedCard).
- **Button**: Primary actions (Filled, Tonal, Outlined).
- **IconButton**: Secondary actions.
- **ModalBottomSheet**: Menus, Share, Analytics preview.
- **TopAppBar**: Large/Small collapsing toolbar behavior.
- **LazyVerticalGrid**: Media grids.
- **LazyColumn**: Main scroll container.
- **FilterChip**: Content filtering.
- **BadgedBox**: Notification indicators.
- **Scaffold**: Basic screen structure.

### Color Scheme
Inherit from `SettingsComposeActivity` theme (Dynamic Color support where available).
- **Primary**: Brand/Accent color.
- **OnPrimary**: Text on primary buttons.
- **Secondary**: Active states, highlights.
- **Surface**: Backgrounds.
- **Error**: Destructive actions (Unfollow, Block).

### Typography
- **HeadlineMedium**: Profile Name (28sp, Bold).
- **TitleMedium**: User stats numbers (16sp, Bold).
- **BodyLarge**: Bio text, Post content (16sp).
- **LabelLarge**: Button text (14sp, Medium).
- **LabelSmall**: Metadata, timestamps (11sp).

## Layout Structure

### 1. Collapsing Header & Navigation
```
┌───────────────────────────────────────────────┐
│ [←]  username_display           [🔔] [⋮]      │ TopAppBar (Pinned)
│      (Visible on scroll up)                   │ Background: Surface (Opaque on scroll)
├───────────────────────────────────────────────┤
│                                               │
│       [ Story Ring (Gradient/Grey) ]          │
│       [      Profile Image         ]          │ 120dp Circle
│       [         (120dp)            ]          │
│                                               │
│           Full Name [✓]                       │ HeadlineMedium
│           @username • pronouns                │ BodyMedium (Secondary color)
│                                               │
│    Bio text area...                           │ BodyLarge
│    #hashtag @mention link.com                 │ (Clickable spans)
│                                               │
│ ┌───────────────────────────────────────────┐ │
│ │  [ Edit Profile ]  [ Share ]  [  +  ]     │ │ Action Row (Scrollable if needed)
│ └───────────────────────────────────────────┘ │
│                                               │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐        │
│ │ 123      │ │ 1.5M     │ │ 500      │        │ Clickable Stats Area
│ │ Posts    │ │ Followers│ │ Following│        │
│ └──────────┘ └──────────┘ └──────────┘        │
│                                               │
└───────────────────────────────────────────────┘
```

### 2. Story Highlights (New)
*Horizontal ScrollRow*
```
┌───────────────────────────────────────────────┐
│ ( + )  ( O )  ( O )  ( O )  ( O )             │
│ New    Trip   Food   Art    Music             │
└───────────────────────────────────────────────┘
```
- **Item**: 64dp Circle with image, Text label below.
- **Animation**: Scale on press.

### 3. Sticky Content Filter
```
┌───────────────────────────────────────────────┐
│ [Grid] [Feed] [Reels] [Tagged]                │ Segmented Control / TabRow
│ ───────────────────────────────────────────── │ Indicator slides to selection
└───────────────────────────────────────────────┘
```

### 4. Content Area (LazyColumn/LazyVerticalGrid)

#### A. Media Grid (Photos/Reels)
- **Layout**: 3 columns (mobile), 1dp spacing.
- **Item**: Square (1:1) or Poster (9:16).
- **Interaction**: Tap -> Shared Element Expand to Full Screen.

#### B. Post Feed
- **Layout**: Linear vertical list.
- **Item**: Complex Post Card (Header, Media, Actions, Caption).

#### C. Empty States
- **Illustration**: Centered vector asset.
- **Text**: "No posts yet" / "Capture your first moment".

## Feature-Specific Designs

### Story Viewer (Overlay)
- **Background**: Black/Blurred backdrop.
- **Content**: Full screen media.
- **Top Overlay**: Progress bars (segmented), User info, Close button.
- **Bottom Overlay**: Reply field (text input), Like button, Send button.
- **Gestures**: Tap Left/Right (Nav), Long Press (Pause), Swipe Down (Close).

### Profile Analytics (Bottom Sheet)
- **Header**: "Professional Dashboard" / "Insights".
- **Cards**:
    - "Accounts Reached" (Big number + % change).
    - "Engagement" (Graph/Sparkline).
    - "Total Followers" (Bar chart).
- **Action**: "See all insights" -> Full Activity (Future).

### QR Code Card (Dialog/Modal)
- **Style**: Card with gradient background.
- **Content**: Large QR Code in center, Username below.
- **Actions**: "Save to Gallery", "Share".

## Animations & Transitions

### Scroll Effects
- **Top Bar**: Fade in background color and title (username) as header scrolls off.
- **Profile Image**: Scale down slightly (1.0 -> 0.8) and translate upwards.
- **Filters**: Sticky header behavior.

### Navigation Transitions
- **Enter**: Slide in from right (300ms).
- **Exit**: Slide out to left (300ms).
- **Shared Element**:
    - Profile Image -> Story Viewer.
    - Grid Image -> Media Viewer.

### Micro-interactions
- **Follow Button**: Morph from "Follow" (Filled Blue) to "Following" (Outlined Grey).
- **Like**: Heart animation (Scale/Bounce).
- **Tab Selection**: Sliding underline indicator.

## Responsive Layouts

### Compact (< 600dp - Phones)
- Single column vertical stack.
- 3-column grid.

### Medium (600dp - 840dp - Foldables/Small Tablets)
- **Header**: Side-by-side (Image Left, Stats/Bio Right).
- **Grid**: 4 columns.

### Expanded (> 840dp - Tablets/Desktop)
- **Layout**: Two-pane or Restricted width centered content.
- **Grid**: 5+ columns.

## Accessibility

- **TalkBack**: Group Stats into a single focusable element describing all three ("123 posts, 1.5M followers...").
- **Touch Targets**: Min 48x48dp for all icon buttons.
- **Scale**: Support system font scaling up to 200%.
- **Contrast**: Ensure text on images (Stories) has scrim protection.

## Loading & Error States

- **Skeleton**: Shimmer effect on Header (Circle + Lines) and Grid (Grey squares) while loading.
- **Error**: "Couldn't load profile" with "Retry" button.
- **Offline**: Show cached data if available, with "Offline" snackbar.
