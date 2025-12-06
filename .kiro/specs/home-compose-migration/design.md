# Home, Fragments, Posts & Custom Views - Compose Design

## Architecture Overview

### Component Hierarchy
```
HomeScreen (Composable)
├── TopAppBar
│   ├── AppTitle
│   ├── SearchIcon
│   └── NotificationIcon
├── NavigationBar
│   ├── HomeTab
│   ├── ReelsTab
│   └── NotificationsTab
└── Content (NavHost)
    ├── FeedScreen
    │   ├── PullRefreshIndicator
    │   └── LazyColumn
    │       └── PostCard (repeated)
    ├── ReelsScreen
    │   └── VerticalPager
    │       └── ReelItem (repeated)
    └── NotificationsScreen
        └── LazyColumn
            └── NotificationItem (repeated)
```

## Core Composables

### 1. HomeScreen

**Purpose**: Main container with navigation

**State**:
```kotlin
data class HomeUiState(
    val selectedTab: HomeTab = HomeTab.FEED,
    val unreadNotifications: Int = 0,
    val isSearchVisible: Boolean = false
)

enum class HomeTab { FEED, REELS, NOTIFICATIONS }
```

**Signature**:
```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPost: (String) -> Unit
)
```

**Layout**:
- Scaffold with TopAppBar and BottomNavigation
- NavHost for tab content
- Handle back press for nested navigation

### 2. FeedScreen

**Purpose**: Display scrollable post feed

**State**:
```kotlin
data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true
)
```

**Signature**:
```kotlin
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = hiltViewModel(),
    onPostClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onCommentClick: (String) -> Unit
)
```

**Features**:
- Pull-to-refresh
- Infinite scroll with pagination
- Loading shimmer
- Empty state
- Error state

### 3. PostCard

**Purpose**: Reusable post display component

**State**:
```kotlin
data class PostCardState(
    val post: Post,
    val isLiked: Boolean,
    val likeCount: Int,
    val commentCount: Int,
    val isBookmarked: Boolean,
    val reactions: Map<String, Int>,
    val isFollowing: Boolean
)
```

**Signature**:
```kotlin
@Composable
fun PostCard(
    state: PostCardState,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onUserClick: () -> Unit,
    onPostClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Sections**:
1. **Header**: User info, timestamp, options
2. **Content**: Text, media, polls
3. **Interactions**: Like, comment, share buttons
4. **Footer**: Reaction summary, comment preview

### 4. PostHeader

**Purpose**: User info section of post

**Signature**:
```kotlin
@Composable
fun PostHeader(
    user: User,
    timestamp: String,
    onUserClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Components**:
- CircularAvatar (48dp)
- Username with badges
- Timestamp
- Options menu button

### 5. PostContent

**Purpose**: Display post content (text, media, polls)

**Signature**:
```kotlin
@Composable
fun PostContent(
    content: PostContent,
    onMediaClick: (Int) -> Unit,
    onPollVote: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

**Variants**:
- Text only
- Single image
- Multiple images (grid)
- Video
- Poll
- Mixed content

### 6. PostInteractionBar

**Purpose**: Like, comment, share buttons

**Signature**:
```kotlin
@Composable
fun PostInteractionBar(
    isLiked: Boolean,
    likeCount: Int,
    commentCount: Int,
    shareCount: Int,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onReactionClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Layout**:
- Row with equal spacing
- Icon + count for each action
- Ripple effects
- Animated like button

### 7. ReelsScreen

**Purpose**: Vertical video feed

**State**:
```kotlin
data class ReelsUiState(
    val reels: List<Reel> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = true,
    val isLoading: Boolean = false
)
```

**Signature**:
```kotlin
@Composable
fun ReelsScreen(
    viewModel: ReelsViewModel = hiltViewModel(),
    onUserClick: (String) -> Unit,
    onCommentClick: (String) -> Unit
)
```

**Features**:
- VerticalPager for swipe navigation
- Auto-play current video
- Pause on tap
- Overlay controls
- Like/comment buttons

### 8. ReelItem

**Purpose**: Single reel display

**Signature**:
```kotlin
@Composable
fun ReelItem(
    reel: Reel,
    isActive: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Layout**:
- Full screen video player
- Gradient overlay at bottom
- User info overlay
- Interaction buttons (right side)
- Double-tap to like

### 9. NotificationsScreen

**Purpose**: Display user notifications

**State**:
```kotlin
data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val unreadCount: Int = 0
)
```

**Signature**:
```kotlin
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = hiltViewModel(),
    onNotificationClick: (Notification) -> Unit,
    onUserClick: (String) -> Unit
)
```

**Features**:
- Pull-to-refresh
- Mark as read on view
- Different notification types
- Navigate to content

### 10. NotificationItem

**Purpose**: Single notification display

**Signature**:
```kotlin
@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Types**:
- Like notification
- Comment notification
- Follow notification
- Mention notification
- System notification

## Custom Views Migration

### MediaViewer
**From**: Custom View
**To**: Composable with Coil

```kotlin
@Composable
fun MediaViewer(
    media: List<Media>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
)
```

### ReactionPicker
**From**: BottomSheet with RecyclerView
**To**: ModalBottomSheet with LazyRow

```kotlin
@Composable
fun ReactionPicker(
    onReactionSelected: (String) -> Unit,
    onDismiss: () -> Unit
)
```

### CommentSection
**From**: Fragment with RecyclerView
**To**: Composable with LazyColumn

```kotlin
@Composable
fun CommentSection(
    postId: String,
    onUserClick: (String) -> Unit,
    onReplyClick: (Comment) -> Unit
)
```

## State Management

### ViewModel Pattern
```kotlin
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    
    fun loadPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            postRepository.getPosts()
                .onSuccess { posts ->
                    _uiState.update { 
                        it.copy(posts = posts, isLoading = false) 
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(error = error.message, isLoading = false) 
                    }
                }
        }
    }
    
    fun likePost(postId: String) {
        viewModelScope.launch {
            postRepository.likePost(postId)
        }
    }
}
```

### State Hoisting
- Keep state in ViewModel
- Pass state down as parameters
- Pass events up as lambdas
- Use remember for local UI state

## Navigation

### NavGraph
```kotlin
@Composable
fun HomeNavGraph(
    navController: NavHostController,
    startDestination: String = "feed"
) {
    NavHost(navController, startDestination) {
        composable("feed") {
            FeedScreen(
                onPostClick = { navController.navigate("post/$it") },
                onUserClick = { navController.navigate("profile/$it") }
            )
        }
        composable("reels") {
            ReelsScreen(
                onUserClick = { navController.navigate("profile/$it") }
            )
        }
        composable("notifications") {
            NotificationsScreen(
                onNotificationClick = { /* handle */ }
            )
        }
    }
}
```

## Theming

### Material 3 Theme
```kotlin
@Composable
fun SynapseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF6750A4),
            onPrimary = Color.White,
            surface = Color(0xFF1C1B1F),
            onSurface = Color(0xFFE6E1E5)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6750A4),
            onPrimary = Color.White,
            surface = Color(0xFFFFFBFE),
            onSurface = Color(0xFF1C1B1F)
        )
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SynapseTypography,
        content = content
    )
}
```

## Performance Optimizations

### 1. Lazy Loading
```kotlin
LazyColumn {
    items(
        items = posts,
        key = { it.id }
    ) { post ->
        PostCard(
            state = rememberPostCardState(post),
            // ...
        )
    }
}
```

### 2. Image Loading
```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .crossfade(true)
        .placeholder(R.drawable.placeholder)
        .build(),
    contentDescription = null,
    modifier = Modifier.fillMaxWidth()
)
```

### 3. Video Playback
```kotlin
@Composable
fun VideoPlayer(
    videoUrl: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
        }
    }
    
    DisposableEffect(isPlaying) {
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
        onDispose { exoPlayer.release() }
    }
    
    AndroidView(
        factory = { PlayerView(context).apply { player = exoPlayer } },
        modifier = modifier
    )
}
```

### 4. Stable Keys
```kotlin
@Immutable
data class Post(
    val id: String,
    val content: String,
    // ...
)
```

## Animations

### Like Animation
```kotlin
val scale by animateFloatAsState(
    targetValue = if (isLiked) 1.2f else 1f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)
```

### Scroll Animations
```kotlin
val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
        TopAppBar(
            title = { Text("Synapse") },
            scrollBehavior = scrollBehavior
        )
    }
)
```

## Accessibility

### Content Descriptions
```kotlin
IconButton(
    onClick = onLikeClick,
    modifier = Modifier.semantics {
        contentDescription = if (isLiked) {
            "Unlike post"
        } else {
            "Like post"
        }
    }
) {
    Icon(
        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        contentDescription = null
    )
}
```

### Semantic Structure
```kotlin
Column(
    modifier = Modifier.semantics(mergeDescendants = true) {
        heading()
    }
) {
    Text("Post by $username")
}
```

## Testing Strategy

### Preview
```kotlin
@Preview(showBackground = true)
@Composable
fun PostCardPreview() {
    SynapseTheme {
        PostCard(
            state = PostCardState(
                post = samplePost,
                isLiked = false,
                likeCount = 42,
                commentCount = 5
            ),
            onLikeClick = {},
            onCommentClick = {},
            onShareClick = {},
            onUserClick = {},
            onPostClick = {},
            onReactionClick = {},
            onOptionsClick = {}
        )
    }
}
```

### UI Tests
```kotlin
@Test
fun postCard_likeButton_togglesState() {
    composeTestRule.setContent {
        PostCard(/* ... */)
    }
    
    composeTestRule
        .onNodeWithContentDescription("Like post")
        .performClick()
    
    composeTestRule
        .onNodeWithContentDescription("Unlike post")
        .assertExists()
}
```

## Migration Path

### Step 1: Create Composables
- Build PostCard composable
- Build FeedScreen composable
- Test in isolation

### Step 2: Integrate with Activity
- Replace Fragment with ComposeView
- Connect ViewModel
- Test navigation

### Step 3: Full Migration
- Remove XML layouts
- Remove Fragment classes
- Update navigation

### Step 4: Optimize
- Add animations
- Improve performance
- Enhance accessibility
