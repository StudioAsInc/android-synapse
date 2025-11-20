# Requirements Document

## Introduction

This feature addresses critical issues in the multi-post display system, including fixing swipe gesture conflicts with the Home fragment, implementing a Facebook-style grid layout for multiple media items, resolving data fetching issues that show placeholders instead of real images, fixing username display showing UIDs instead of actual usernames, and resolving reaction failures.

## Glossary

- **Post System**: The social media post display and interaction module in the Synapse application
- **Multi-Post**: A post containing multiple media attachments (images or videos)
- **Media Grid**: A grid-based layout for displaying multiple media items within a single post card
- **Home Fragment**: The main feed fragment that displays a scrollable list of posts
- **Supabase Storage**: The S3-compatible object storage service where media files are stored
- **Post Repository**: The data layer component responsible for fetching post data from Supabase
- **User Profile**: The user account information including username, avatar, and metadata
- **Reaction System**: The feature allowing users to react to posts with emojis or likes
- **ViewPager**: An Android UI component that enables swipe navigation between pages
- **RecyclerView**: An Android UI component for displaying scrollable lists efficiently

## Requirements

### Requirement 1

**User Story:** As a user, I want to scroll through my home feed smoothly without swipe gestures on posts interfering, so that I can navigate the app naturally

#### Acceptance Criteria

1. WHEN a user scrolls vertically in the Home Fragment, THE Post System SHALL NOT intercept touch events for horizontal swipes
2. WHEN a multi-post uses a ViewPager for media, THE Post System SHALL disable ViewPager swipe gestures
3. THE Post System SHALL use a grid layout instead of swipeable ViewPager for displaying multiple media items
4. WHEN a user performs a vertical scroll gesture on a post card, THE Home Fragment SHALL scroll normally without interference
5. THE Post System SHALL NOT use any swipe-enabled components within post cards that conflict with vertical scrolling

### Requirement 2

**User Story:** As a user, I want to see multiple photos in a post displayed in a Facebook-style grid, so that I can view all media at once without swiping

#### Acceptance Criteria

1. WHEN a post contains 2 media items, THE Media Grid SHALL display them in a 1x2 horizontal layout
2. WHEN a post contains 3 media items, THE Media Grid SHALL display them with 1 large item on the left and 2 stacked items on the right
3. WHEN a post contains 4 media items, THE Media Grid SHALL display them in a 2x2 grid layout
4. WHEN a post contains 5 or more media items, THE Media Grid SHALL display the first 4 items in a 2x2 grid with a "+N" overlay on the 4th item indicating additional media
5. THE Media Grid SHALL maintain consistent spacing of 2dp between grid items

### Requirement 3

**User Story:** As a user, I want to see the actual photos from posts instead of placeholders, so that I can view the real content

#### Acceptance Criteria

1. WHEN the Post Repository fetches a post, THE Post Repository SHALL retrieve all media URLs from the Supabase Storage bucket
2. WHEN media URLs are retrieved, THE Post System SHALL construct full public URLs using the Supabase project reference and bucket name
3. THE Post System SHALL load images using the constructed URLs with proper authentication headers if required
4. WHEN an image fails to load, THE Post System SHALL retry up to 2 times with exponential backoff
5. THE Post System SHALL display a placeholder only when all retry attempts fail or while the image is loading

### Requirement 4

**User Story:** As a user, I want to see the actual username of the post author instead of their UID, so that I can identify who created the post

#### Acceptance Criteria

1. WHEN the Post Repository fetches a post, THE Post Repository SHALL join with the user profiles table to retrieve the username
2. THE Post Repository SHALL use a SQL query that includes "SELECT profiles.username" in the join clause
3. WHEN a username is null or empty, THE Post System SHALL display "Unknown User" as a fallback
4. THE Post System SHALL cache user profile data for 5 minutes to reduce database queries
5. THE Post System SHALL display the username in the post card header within 500ms of post rendering

### Requirement 5

**User Story:** As a user, I want to react to posts successfully, so that I can express my feelings about content

#### Acceptance Criteria

1. WHEN a user taps a reaction button, THE Reaction System SHALL verify the user is authenticated before proceeding
2. THE Reaction System SHALL insert a new row in the post_reactions table with user_id, post_id, and reaction_type
3. WHEN a reaction insert fails due to RLS policies, THE Reaction System SHALL display a specific error message indicating permission issues
4. WHEN a reaction insert fails due to network issues, THE Reaction System SHALL retry up to 2 times
5. THE Reaction System SHALL update the reaction count in the UI immediately upon successful insertion

### Requirement 6

**User Story:** As a user, I want to tap on a media item in the grid to view it in full screen, so that I can see details clearly

#### Acceptance Criteria

1. WHEN a user taps any media item in the Media Grid, THE Post System SHALL open a full-screen media viewer
2. THE Post System SHALL pass all media URLs from the post to the full-screen viewer
3. THE Post System SHALL open the viewer at the index of the tapped media item
4. THE Post System SHALL support swipe gestures in the full-screen viewer to navigate between media items
5. THE Post System SHALL display media position indicator (e.g., "2 of 5") in the full-screen viewer

### Requirement 7

**User Story:** As a developer, I want the post data fetching to include all necessary joins, so that the UI has complete data without additional queries

#### Acceptance Criteria

1. THE Post Repository SHALL use a single query with joins to fetch posts, user profiles, and media attachments
2. THE Post Repository SHALL include the following fields in the query: post.*, profiles.username, profiles.avatar_url, media.url, media.type
3. THE Post Repository SHALL order media attachments by their created_at timestamp or position field
4. THE Post Repository SHALL handle null values gracefully for optional fields like avatar_url
5. THE Post Repository SHALL use Supabase's select() method with proper relationship syntax for nested data

### Requirement 8

**User Story:** As a developer, I want proper error handling for Supabase operations, so that users see meaningful error messages

#### Acceptance Criteria

1. WHEN a Supabase query fails, THE Post Repository SHALL log the error with full details including error code and message
2. THE Post Repository SHALL map Supabase error codes to user-friendly messages
3. WHEN RLS policies block an operation, THE Post System SHALL display "Permission denied" with guidance to check account status
4. WHEN network errors occur, THE Post System SHALL display "Connection failed" with a retry button
5. THE Post System SHALL display error messages using Snackbar or Toast with appropriate duration

### Requirement 9

**User Story:** As a user, I want the media grid to handle different aspect ratios gracefully, so that photos look good regardless of their dimensions

#### Acceptance Criteria

1. THE Media Grid SHALL use centerCrop scaling for all media items to fill the grid cells
2. THE Media Grid SHALL maintain a 1:1 aspect ratio for grid cells in multi-item layouts
3. WHEN a post contains a single media item, THE Media Grid SHALL display it with its original aspect ratio up to a maximum height of 400dp
4. THE Media Grid SHALL apply rounded corners of 8dp to all media items
5. THE Media Grid SHALL display a play icon overlay on video thumbnails

### Requirement 10

**User Story:** As a developer, I want to verify RLS policies are correctly configured, so that data access is secure and functional

#### Acceptance Criteria

1. THE Post Repository SHALL test that authenticated users can read posts from users they follow or public posts
2. THE Post Repository SHALL test that authenticated users can insert reactions on posts they can view
3. THE Post Repository SHALL test that users can read media attachments for posts they can access
4. THE Post Repository SHALL test that users can read profile information for post authors
5. WHEN RLS policies are misconfigured, THE Post Repository SHALL provide clear error messages indicating which table and operation failed
