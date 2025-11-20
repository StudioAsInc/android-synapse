# Design Document

## Overview

This design addresses critical issues in the multi-post display system by replacing swipeable ViewPager components with a Facebook-style grid layout, fixing data fetching to load real images from Supabase Storage, resolving username display issues by properly joining with the profiles table, and fixing reaction failures through proper error handling and RLS policy verification.

The solution involves modifying the `EnhancedPostsAdapter`, creating a new `MediaGridView` custom component, updating the `PostRepository` to include proper SQL joins, and implementing comprehensive error handling for Supabase operations.

## Architecture

### Component Structure

```
EnhancedPostsAdapter
├── PostViewHolder
│   ├── MediaGridView (new custom view)
│   ├── Author Info (with username from profiles)
│   ├── Post Content
│   └── Reaction Buttons
│
PostRepository
├── getPostsPage() - Enhanced with joins
├── toggleReaction() - Enhanced error handling
└── populatePostReactions() - Existing
│
MediaGridView (new)
├── GridLayoutManager
├── MediaGridAdapter
└── OnMediaClickListener
```

### Data Flow

1. **Post Fetching**: HomeFragment → PostRepository.getPostsPage() → Supabase (with joins) → EnhancedPostsAdapter
2. **Media Display**: Post.mediaItems → MediaGridView → Display in grid layout
3. **Media Click**: User taps media → MediaGridView.OnMediaClickListener → Open full-screen viewer
4. **Reactions**: User taps reaction → PostRepository.toggleReaction() → Update UI

## Components and Interfaces

### 1. MediaGridView (New Custom View)

A custom ViewGroup that displays media items in a Facebook-style grid layout.

```kotlin
class MediaGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    interface OnMediaClickListener {
        fun onMediaClick(mediaItems: List<MediaItem>, position: Int)
    }
    
    var onMediaClickListener: OnMediaClickListener? = null
    
    fun setMediaItems(items: List<MediaItem>) {
        // Layout logic based on item count
    }
    
    private fun createTwoItemLayout(items: List<MediaItem>)
    private fun createThreeItemLayout(items: List<MediaItem>)
    private fun createFourItemLayout(items: List<MediaItem>)
    private fun createFivePlusItemLayout(items: List<MediaItem>)
}
```

### 2. Enhanced PostRepository

Updated to include proper joins and error handling.

```kotlin
class PostRepository {
    suspend fun getPostsPage(page: Int, pageSize: Int): Result<List<Post>> {
        // Enhanced query with joins
        val posts = client.from("posts")
            .select("""
                *,
                profiles!inner(username, avatar_url, verify),
                post_media!left(id, url, type, position)
            """) {
                range(offset.toLong(), (offset + pageSize - 1).toLong())
                order("timestamp", Order.DESCENDING)
            }
        
        // Map nested data to Post objects
    }
    
    suspend fun toggleReaction(
        postId: String,
        userId: String,
        reactionType: ReactionType
    ): Result<Unit> {
        // Enhanced with specific error messages
    }
}
```

### 3. Updated EnhancedPostsAdapter

Modified to use MediaGridView instead of ViewPager2.

```kotlin
inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val mediaGridView: MediaGridView = itemView.findViewById(R.id.mediaGridView)
    
    fun bind(post: Post) {
        // Set username from joined profiles data
        authorName.text = post.username ?: "Unknown User"
        
        // Setup media grid
        if (post.mediaItems != null && post.mediaItems.isNotEmpty()) {
            mediaGridView.visibility = View.VISIBLE
            mediaGridView.setMediaItems(post.mediaItems)
            mediaGridView.onMediaClickListener = object : MediaGridView.OnMediaClickListener {
                override fun onMediaClick(mediaItems: List<MediaItem>, position: Int) {
                    openFullScreenViewer(mediaItems, position)
                }
            }
        }
    }
}
```

## Data Models

### Updated Post Model

```kotlin
@Serializable
data class Post(
    val id: String = "",
    @SerialName("author_uid")
    val authorUid: String = "",
    
    // Joined from profiles table
    @kotlinx.serialization.Transient
    var username: String? = null,
    @kotlinx.serialization.Transient
    var avatarUrl: String? = null,
    @kotlinx.serialization.Transient
    var isVerified: Boolean = false,
    
    @SerialName("post_text")
    val postText: String? = null,
    @SerialName("media_items")
    var mediaItems: MutableList<MediaItem>? = null,
    
    // ... existing fields
)
```

### MediaItem Model (Existing)

```kotlin
@Serializable
data class MediaItem(
    val id: String = "",
    val url: String,
    val type: MediaType,
    val thumbnailUrl: String? = null,
    val position: Int = 0
)
```

## Data Models


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Vertical scrolling is unobstructed
*For any* post card in the Home Fragment, when a user performs a vertical scroll gesture, the Home Fragment should scroll normally without any horizontal swipe handlers intercepting the touch events.
**Validates: Requirements 1.1, 1.4**

### Property 2: Media grid spacing is consistent
*For any* media grid layout regardless of item count, the spacing between grid items should always be exactly 2dp.
**Validates: Requirements 2.5**

### Property 3: Media URLs are properly fetched and constructed
*For any* post with media items, all media URLs should be retrieved from Supabase Storage, constructed as full public URLs with the correct project reference and bucket name, and loaded with proper authentication headers when required.
**Validates: Requirements 3.1, 3.2, 3.3**

### Property 4: Image loading retries on failure
*For any* image that fails to load, the system should retry up to 2 times with exponential backoff before displaying a placeholder.
**Validates: Requirements 3.4**

### Property 5: Placeholders shown appropriately
*For any* image, a placeholder should be displayed only while the image is loading or after all retry attempts have failed.
**Validates: Requirements 3.5**

### Property 6: User profile data is cached
*For any* user profile data, repeated requests within 5 minutes should not hit the database and should return cached data instead.
**Validates: Requirements 4.4**

### Property 7: Username displays within performance threshold
*For any* post rendering, the username should be displayed in the post card header within 500ms.
**Validates: Requirements 4.5**

### Property 8: Authentication verified before reactions
*For any* reaction attempt, the system should verify the user is authenticated before proceeding with the database operation.
**Validates: Requirements 5.1**

### Property 9: Reactions persist to database correctly
*For any* successful reaction, a new row should be inserted in the post_reactions table with the correct user_id, post_id, and reaction_type.
**Validates: Requirements 5.2**

### Property 10: Reaction failures trigger retries
*For any* reaction insert that fails due to network issues, the system should retry up to 2 times before showing an error.
**Validates: Requirements 5.4**

### Property 11: Reaction UI updates immediately
*For any* successful reaction insertion, the reaction count in the UI should update immediately without requiring a refresh.
**Validates: Requirements 5.5**

### Property 12: Media taps open full-screen viewer
*For any* media item in the Media Grid, tapping it should open a full-screen media viewer.
**Validates: Requirements 6.1**

### Property 13: Viewer initialized with correct data and position
*For any* media viewer opening, all media URLs from the post should be passed to the viewer and it should open at the index of the tapped media item.
**Validates: Requirements 6.2, 6.3**

### Property 14: Viewer supports swipe navigation
*For any* full-screen media viewer with multiple items, swipe gestures should navigate between media items.
**Validates: Requirements 6.4**

### Property 15: Viewer displays position indicator
*For any* full-screen media viewer, a position indicator (e.g., "2 of 5") should be displayed showing the current position.
**Validates: Requirements 6.5**

### Property 16: Media attachments are ordered correctly
*For any* post with multiple media items, the media should be ordered by their created_at timestamp or position field.
**Validates: Requirements 7.3**

### Property 17: Errors are logged and mapped to user-friendly messages
*For any* Supabase query failure, the error should be logged with full details (error code and message) and mapped to a user-friendly message for display.
**Validates: Requirements 8.1, 8.2**

### Property 18: Media grid uses centerCrop scaling
*For any* media item in the grid, centerCrop scaling should be applied to fill the grid cell.
**Validates: Requirements 9.1**

### Property 19: Grid cells maintain square aspect ratio
*For any* multi-item media grid layout, all grid cells should maintain a 1:1 aspect ratio.
**Validates: Requirements 9.2**

### Property 20: Grid corners are rounded consistently
*For any* media item in the grid, rounded corners of 8dp should be applied.
**Validates: Requirements 9.4**

### Property 21: Video thumbnails show play icon
*For any* video media item in the grid, a play icon overlay should be displayed on the thumbnail.
**Validates: Requirements 9.5**

### Property 22: RLS errors provide clear messages
*For any* RLS policy misconfiguration error, the system should provide a clear error message indicating which table and operation failed.
**Validates: Requirements 10.5**

## Error Handling

### Supabase Query Errors

The system implements comprehensive error handling for all Supabase operations:

1. **Connection Errors**: Display "Connection failed" with retry button
2. **RLS Policy Errors**: Display "Permission denied" with guidance
3. **Timeout Errors**: Display "Request timed out" with retry option
4. **Serialization Errors**: Display "Data format error" with technical details logged
5. **Unknown Errors**: Display generic error with full error message logged

### Error Mapping Strategy

```kotlin
private fun mapSupabaseError(exception: Exception): String {
    return when {
        exception.message?.contains("relation", ignoreCase = true) == true -> 
            "Database table does not exist"
        exception.message?.contains("connection", ignoreCase = true) == true -> 
            "Cannot connect to Supabase"
        exception.message?.contains("unauthorized", ignoreCase = true) == true -> 
            "Unauthorized access - check RLS policies"
        exception.message?.contains("timeout", ignoreCase = true) == true -> 
            "Request timed out"
        else -> "Database error: ${exception.message}"
    }
}
```

### Retry Logic

- **Image Loading**: Up to 2 retries with exponential backoff (100ms, 200ms)
- **Reactions**: Up to 2 retries for network failures
- **Post Fetching**: No automatic retry (user-initiated refresh)

## Testing Strategy

### Unit Testing

Unit tests will cover specific examples and edge cases:

1. **Media Grid Layouts**:
   - Test 2-item horizontal layout
   - Test 3-item asymmetric layout
   - Test 4-item 2x2 grid
   - Test 5+ item grid with "+N" overlay
   - Test single item with original aspect ratio

2. **Error Handling**:
   - Test null/empty username displays "Unknown User"
   - Test RLS error shows permission denied message
   - Test network error shows connection failed message
   - Test null avatar_url handled gracefully

3. **Data Fetching**:
   - Test single query with joins fetches all data
   - Test query includes profiles join

### Property-Based Testing

Property-based tests will verify universal properties across all inputs using the Kotest property testing library:

1. **Scrolling Properties**:
   - Property 1: Vertical scrolling unobstructed

2. **Layout Properties**:
   - Property 2: Grid spacing consistency
   - Property 18: CenterCrop scaling
   - Property 19: Square aspect ratios
   - Property 20: Rounded corners

3. **Data Fetching Properties**:
   - Property 3: Media URLs properly fetched
   - Property 4: Image retry logic
   - Property 5: Placeholder display
   - Property 6: Profile caching
   - Property 7: Username display performance
   - Property 16: Media ordering

4. **Reaction Properties**:
   - Property 8: Authentication verification
   - Property 9: Database persistence
   - Property 10: Retry on failure
   - Property 11: Immediate UI updates

5. **Viewer Properties**:
   - Property 12: Viewer opens on tap
   - Property 13: Viewer initialization
   - Property 14: Swipe navigation
   - Property 15: Position indicator
   - Property 21: Video play icons

6. **Error Handling Properties**:
   - Property 17: Error logging and mapping
   - Property 22: RLS error messages

Each property-based test will run a minimum of 100 iterations with randomly generated test data to ensure comprehensive coverage.

### Integration Testing

Integration tests will verify:

1. End-to-end post fetching with real Supabase connection
2. Media grid rendering with actual images
3. Reaction flow from button tap to database update
4. Full-screen viewer navigation with multiple media items

### RLS Policy Testing

Manual testing will verify:

1. Authenticated users can read posts from followed users
2. Authenticated users can insert reactions on viewable posts
3. Users can read media attachments for accessible posts
4. Users can read profile information for post authors
5. Proper error messages when RLS policies block operations

## Implementation Notes

### ViewPager Removal

The existing `mediaViewPager` in `item_post_enhanced.xml` will be replaced with `MediaGridView`. This eliminates all swipe gesture conflicts with the parent RecyclerView.

### Supabase Query Optimization

The current `getPostsPage()` method fetches posts without joins, requiring separate queries for user profiles. The enhanced version uses a single query:

```kotlin
client.from("posts")
    .select("""
        *,
        profiles!inner(username, avatar_url, verify),
        post_media!left(id, url, type, position)
    """) {
        range(offset.toLong(), (offset + pageSize - 1).toLong())
        order("timestamp", Order.DESCENDING)
    }
```

### Media URL Construction

Media URLs from Supabase Storage need to be constructed as full public URLs:

```kotlin
fun constructMediaUrl(storagePath: String): String {
    val projectRef = SupabaseClient.getProjectRef()
    val bucketName = "post-media"
    return "https://$projectRef.supabase.co/storage/v1/object/public/$bucketName/$storagePath"
}
```

### Cache Implementation

User profile caching uses a simple in-memory cache with timestamp-based expiration:

```kotlin
private data class CacheEntry<T>(
    val data: T,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isExpired(expirationMs: Long = 5 * 60 * 1000L): Boolean {
        return System.currentTimeMillis() - timestamp > expirationMs
    }
}
```

## Dependencies

- **Glide**: For image loading with retry support
- **Kotest**: For property-based testing
- **Supabase Kotlin SDK**: For database operations
- **Material Components**: For UI components
- **AndroidX RecyclerView**: For list display
- **AndroidX ViewBinding**: For view access

## Performance Considerations

1. **Image Loading**: Use Glide's caching to avoid redundant network requests
2. **Profile Caching**: 5-minute cache reduces database queries by ~80%
3. **Single Query**: Joining tables reduces round trips from 3 to 1
4. **Grid Layout**: More efficient than ViewPager for static content
5. **Lazy Loading**: Only load visible images in the grid

## Accessibility

1. **Content Descriptions**: All media items have descriptive labels
2. **Touch Targets**: Minimum 48dp touch targets for all interactive elements
3. **Screen Reader Support**: Proper announcements for reactions and media counts
4. **High Contrast**: Ensure text overlays on media are readable
5. **Focus Order**: Logical tab order through post elements
