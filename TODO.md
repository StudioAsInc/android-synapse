# Synapse TODO List

## High Priority Tasks

### 1. FEAT: Implement Offline-First Architecture with Room Database
**Location**: `android/app/src/main/java/com/synapse/social/studioasinc/data/`
**Description**: Add Room database layer for offline caching of posts, comments, and user data
**Tasks**:
- Create Room entities for Post, Comment, User, Chat models
- Implement DAOs for CRUD operations
- Add Repository pattern with offline-first logic (check local DB first, then network)
- Implement sync mechanism to update local cache from Supabase
- Add WorkManager for background sync
**Impact**: Improves app performance and enables offline functionality

### 2. FIX: Memory Leaks in ChatActivity and ProfileActivity
**Location**: `android/app/src/main/java/com/synapse/social/studioasinc/`
**Description**: Fix potential memory leaks from improper lifecycle handling
**Tasks**:
- Audit ChatActivity.kt for lifecycle-aware components
- Fix Realtime subscription cleanup in onDestroy()
- Ensure Glide requests are cleared properly
- Add proper coroutine cancellation in ViewModels
- Implement LeakCanary for detection
**Impact**: Prevents crashes and improves app stability

### 3. FEAT: Implement Pagination for All List Views
**Location**: `android/app/src/main/java/com/synapse/social/studioasinc/data/repository/`
**Description**: Add proper pagination using Paging 3 library
**Tasks**:
- Migrate PostRepository to use Paging 3
- Implement PagingSource for posts, comments, chat messages
- Update adapters to use PagingDataAdapter
- Add RemoteMediator for offline support
- Implement proper loading states (loading, error, empty)
**Impact**: Better performance with large datasets

### 4. DEBUG: Fix Realtime Subscription Memory Issues
**Location**: `android/app/src/main/java/com/synapse/social/studioasinc/chat/service/SupabaseRealtimeService.kt`
**Description**: Investigate and fix Realtime channel subscription leaks
**Tasks**:
- Audit all Realtime channel subscriptions
- Ensure proper unsubscribe in lifecycle methods
- Add connection state monitoring
- Implement reconnection logic with exponential backoff
- Add logging for subscription lifecycle
**Impact**: Prevents memory leaks and improves real-time reliability

### 5. FEAT: Add Comprehensive Error Handling and Retry Logic
**Location**: `android/app/src/main/java/com/synapse/social/studioasinc/data/repository/`
**Description**: Implement robust error handling across all repositories
**Tasks**:
- Create sealed class for Result<T> with Success/Error/Loading states
- Add retry logic with exponential backoff for network calls
- Implement proper error messages for different failure scenarios
- Add network connectivity checks before API calls
- Create centralized error handler utility
**Impact**: Better user experience and app reliability

### 6. FIX: Optimize Image Loading and Caching
**Location**: `android/app/src/main/java/com/synapse/social/studioasinc/`
**Description**: Improve image loading performance and reduce memory usage
**Tasks**:
- Configure Glide with proper disk and memory cache sizes
- Implement image compression before upload
- Add thumbnail generation for large images
- Use appropriate image formats (WebP)
- Implement lazy loading for RecyclerView images
**Impact**: Reduces memory usage and improves scrolling performance

### 7. FEAT: Implement Unit and Integration Tests
**Location**: `android/app/src/test/` and `android/app/src/androidTest/`
**Description**: Add comprehensive test coverage for critical components
**Tasks**:
- Write unit tests for all Repository classes
- Add ViewModel tests with coroutine testing
- Implement UI tests for critical user flows (login, post creation, chat)
- Add integration tests for Supabase interactions
- Set up CI/CD pipeline for automated testing
**Impact**: Ensures code quality and prevents regressions

### 8. DEBUG: Fix Comment Reply Threading Issues
**Location**: `android/app/src/main/java/com/synapse/social/studioasinc/adapters/CommentsAdapter.kt`
**Description**: Fix nested reply display and interaction bugs
**Tasks**:
- Fix reply hierarchy rendering in RecyclerView
- Ensure proper parent-child relationship in database
- Add collapse/expand functionality for reply threads
- Fix reply count updates
- Optimize nested adapter performance
**Impact**: Improves comment system usability

### 9. FEAT: Add Push Notification Handling and Deep Linking
**Location**: `android/app/src/main/java/com/synapse/social/studioasinc/`
**Description**: Implement comprehensive notification system
**Tasks**:
- Set up OneSignal notification handlers
- Implement deep linking for notifications (post, chat, profile)
- Add notification channels for different types
- Implement in-app notification UI
- Add notification preferences in settings
**Impact**: Better user engagement and navigation

### 10. FIX: Refactor Large Activity Classes
**Location**: `android/app/src/main/java/com/synapse/social/studioasinc/`
**Description**: Break down ChatActivity and AuthActivity into smaller components
**Tasks**:
- Extract ChatActivity logic into separate ViewModels
- Create reusable UI components for chat features
- Move business logic from Activities to UseCases
- Implement proper separation of concerns
- Reduce class size to under 500 lines
**Impact**: Improves code maintainability and testability

---

## Notes
- All tasks should follow MVVM architecture pattern
- Use Kotlin coroutines for async operations
- Ensure proper null safety
- Follow Material Design 3 guidelines
- Test on multiple Android versions (API 26-34)
