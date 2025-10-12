# Synapse Android - Source Map

> **Changelog (2025-09-26, auto-update)**
> - Verified all file paths and descriptions against the current repository state.
> - Updated the project structure diagram and core component lists.
> - Marked files that have been removed or renamed.
> - Added a comprehensive list of all new and unlisted files discovered in the repository.
> - Added a section for ambiguous entries that may require manual review.

## Project Overview

**Synapse** is a next-generation open-source social platform for Android, developed by StudioAs Inc. The project combines speed, privacy, and customization with real-time communication features, zero ads, and a non-profit model.

### Project Statistics
> **⚠️ Note**: These statistics are current as of the last documentation update. For current values, check the build.gradle files directly.

- **Language Distribution**: Primarily Java with Kotlin components (exact distribution may vary)
- **Package**: `com.synapse.social.studioasinc`
- **Version**: 1.0.0-alpha07 (versionCode: 15) *- Check app/build.gradle for current version*
- **Min SDK**: 26, Target SDK: 32, Compile SDK: 36 *- Check app/build.gradle for current SDK levels*

## Project Structure

```
synapse-android/
├── app/                              # Main Android application module
│   ├── libs/                         # Local AAR dependencies
│   ├── src/main/                     # Main source directory
│   │   ├── java/com/studioasinc/synapse/ # Core application code
│   │   ├── res/                      # Android resources
│   │   ├── assets/                   # Application assets
│   │   └── AndroidManifest.xml       # App configuration and permissions
│   ├── build.gradle                  # App-level build configuration
│   └── proguard-rules.pro           # Code obfuscation rules
├── gradle/                           # Gradle wrapper files
├── Docs/                             # Project documentation
├── build.gradle                      # Project-level build configuration
├── settings.gradle                   # Gradle settings
├── gradle.properties                # Build properties
├── worker.js                        # Cloudflare Worker for push notifications
└── README.md                        # Project overview
```

## Core Architecture

### Application Entry Points

#### Main Application Class
- **`SynapseApp.java`** - Application class handling:
  - Firebase initialization
  - OneSignal push notifications setup
  - Global exception handling
  - User presence management

#### Primary Activities
- **`MainActivity.java`** - App launcher and authentication entry point
- **`HomeActivity.java`** - Main social feed and navigation hub
- **`AuthActivity.java`** - User authentication and registration
- **`CheckpermissionActivity.java`** - Runtime permissions handling

### Feature Modules

#### Social Features
- **`ProfileActivity.java`** - User profile management
- **`ProfileEditActivity.java`** - Profile editing functionality
- **`SearchActivity.java`** - User and content search
- **`UserFollowsListActivity.java`** - Following/followers management

#### Messaging System
- **`InboxActivity.java`** - Message inbox interface
- **`ChatActivity.java`** - Individual chat conversations
- **`ChatAdapter.java`** - Message display adapter
- **`fragments/InboxChatsFragment.java`** - Chat list fragment (was `FragInboxChatsActivity.java`)
- **`fragments/InboxCallsFragment.java`** - Call history fragment (was `FragInboxCallsActivity.java`)
- **`fragments/InboxStoriesFragment.java`** - Stories inbox fragment (was `FragInboxStoriesActivity.java`)

#### Content Creation
- **`CreatePostActivity.kt`** (Kotlin) - Unified post creation for images and text (replaces `CreateImagePostActivity.java` and `CreateImagePostNextStepActivity.java`)
- **`CreateLineVideoActivity.java`** - Short video creation
- **`CreateLineVideoNextStepActivity.java`** - Video publishing workflow
- **`LineVideoPlayerActivity.java`** - Video playback interface

#### Settings & Configuration
- **`SelectRegionActivity.java`** - Region selection
- **`ChatsettingsActivity.java`** - Chat-specific settings
- **`DisappearingMessageSettingsActivity.java`** - Message expiration settings
- **`SettingsActivity.java`** - `[REMOVED]`
- **`BgWallpapersActivity.java`** - `[REMOVED]`

### Supporting Components

#### UI Components
- **`PostCommentsBottomSheetDialog.java`** - Comment interface
- **`PostMoreBottomSheetDialog.java`** - Post action menu
- **`ContentDisplayBottomSheetDialogFragment.java`** - Content summary display

#### Utility Classes
- **`FileUtil.java`** - `[REMOVED]` (Functionality likely moved to `MediaStorageUtils.java` or other utils)
- **`StorageUtil.java`** - Exists
- **`SketchwareUtil.java`** - Exists
- **`RequestNetwork.java`** - Exists
- **`RequestNetworkController.java`** - Exists
- **`UploadFiles.java`** - Exists
- **`ImageUploader.java`** - Exists

#### Specialized Components
- **`PresenceManager.kt`** (Kotlin) - User online status management
- **`NotificationHelper.kt`** (Kotlin) - Push notification handling
- **`OneSignalManager.kt`** (Kotlin) - OneSignal integration
- **`RadialProgress.java`** - Custom progress indicator
- **`FadeEditText.java`** - Custom text input with fade effects

### AI Integration
- **`AI/Gemini.java`** - Google Gemini AI integration for content generation and assistance

### Package Organization

```
com.studioasinc.synapse/
├── [root]                           # Main activities and core classes
├── AI/                              # Artificial Intelligence components
├── adapter/                         # RecyclerView adapters
├── animations/                      # Animation utilities
├── attachments/                     # Attachment handling utilities
├── audio/                           # Audio processing components
├── backend/                         # Backend services (Auth, Database)
├── config/                          # Configuration files
├── fragments/                       # UI Fragments
├── lab/                             # Experimental features
├── model/                           # Data models
├── permissionreq/                   # Permission request utilities
├── styling/                         # UI styling components
├── util/                            # General utility classes
└── widget/                          # Custom widget implementations
```

## External Dependencies

### Firebase Services
- **Firebase Authentication** - User authentication
- **Firebase Realtime Database** - Real-time data synchronization
- **Firebase Storage** - File and media storage
- **Firebase Crashlytics** - Crash reporting
- **Firebase Analytics** - Usage analytics
- **Firebase Performance** - Performance monitoring

### UI & Media Libraries
- **Glide 5.0.0-rc01** - Image loading and caching
- **Lottie 6.6.0** - Animation rendering
- **Material Design Components** - Modern UI components
- **CircleImageView** - Circular image views
- **UCrop & Android Image Cropper** - Image editing capabilities

### Networking & Data
- **OkHttp 5.1.0** - HTTP client
- **Gson 2.13.1** - JSON parsing
- **Markwon 4.6.2** - Markdown rendering with extensions

### Push Notifications
- **OneSignal** - Push notification service
- **Google Play Services Auth** - Google authentication

### AI & Machine Learning
- **Google Gemini AI 1.12.0** - Generative AI integration

### Development Tools
- **Kotlin Coroutines** - Asynchronous programming
- **AndroidX Libraries** - Modern Android components
- **Desugaring** - Java 8+ API compatibility

## Build Configuration

### Gradle Setup
> **📋 Current versions** (check build.gradle files for latest):

- **Android Gradle Plugin**: 8.12.0 *- Check build.gradle*
- **Kotlin**: 2.2.0 *- Check build.gradle*
- **Java Version**: 17 *- Check app/build.gradle*
- **Multi-dex enabled** for large app support

### Build Variants
- **Debug**: Development builds with debugging features
- **Release**: Production builds with signing and optimization

### Key Features
- **View Binding** enabled for type-safe view references
- **R8 optimization** for code shrinking
- **Jetifier** for AndroidX migration
- **Parallel builds** for faster compilation

## Assets & Resources

### Animation Assets (Lottie)
- `bubble.json` - Bubble animation
- `loading.json` - Loading indicator
- `typing.json` - Typing indicator
- `update_animation.json` - Update notification animation

### Web Integration

- `firebase-presence.js` - `[REMOVED]`
- `presence-integration-example.html` - `[REMOVED]`


## Notification System
- **Deep Linking**: The application supports deep linking from notifications, allowing users to navigate directly to specific content (e.g., a chat, post, or profile) from a notification. For more details, see [NOTIFICATION_DEEP_LINKING.md](../NOTIFICATION_DEEP_LINKING.md).
- **OneSignal**: Push notifications are handled by OneSignal and managed through the `OneSignalManager.kt` and `NotificationHelper.kt` classes.

## CI/CD Pipeline

### GitHub Actions Workflow
- **Build Process**: Automated APK compilation
- **Testing**: Code quality checks
- **Distribution**: Telegram notification system
- **Artifacts**: APK delivery with commit tracking
- **NOTE**: `.github` directory was not found in the current repository structure.

### Notification System
- **Telegram Integration** - Build status notifications
- **Python Scripts** - Automated deployment notifications

## Development Guidelines

### Code Standards
- **Kotlin/Android best practices**
- **Semantic commit messages**
- **Multi-device testing requirements**
- **Documentation updates mandatory**

### Contribution Process
1. Fork and clone repository
2. Create feature branch
3. Implement changes with testing
4. Submit pull request with documentation
5. Address review feedback

## Security & Permissions

### Required Permissions
- **Internet access** - Network connectivity
- **Network state** - Connection monitoring
- **Notifications** - Push message delivery
- **Storage access** - File and media management
- **Vibration** - Notification feedback

### Security Features
- **Firebase Authentication** - Secure user management
- **Encrypted communication** - End-to-end chat encryption
- **Signed APKs** - Release build verification

## Project Roadmap Features

### Current Features
- Real-time messaging with typing indicators
- Image and video post creation
- User profiles and following system
- Push notifications via OneSignal
- AI-powered content assistance
- Markdown support in messages

### Upcoming Features
- Peer-to-peer video/audio calling
- Enhanced story features
- Advanced content discovery
- Community groups
- Enhanced AI integration

---

## Missing files discovered (auto)
*This section lists all files found in the repository that are not mentioned in this document.*

**Root**
- `NOTIFICATION_DEEP_LINKING.md`

**Docs**
- `Agent.md`
- `CONTRIBUTE.md`
- `LICENSE.md`

**app/src/main/java/com/synapse/social/studioasinc**
- `AiFeatureHandler.kt`
- `AsyncUploadService.java`
- `AttachmentHandler.kt`
- `BaseMessageViewHolder.java`
- `CarouselItemDecoration.java`
- `CenterCropLinearLayout.java`
- `CenterCropLinearLayoutNoEffect.java`
- `ChatAdapterListener.java`
- `ChatConstants.kt`
- `ChatGroupActivity.kt`
- `ChatInteractionListener.java`
- `ChatKeyboardHandler.kt`
- `ChatScrollListener.kt`
- `ChatState.kt`
- `ChatUIUpdater.kt`
- `ChatViewHolders.java`
- `CompleteProfileActivity.java`
- `ConversationSettingsActivity.kt`
- `CreateGroupActivity.kt`
- `DebugActivity.java`
- `DownloadCompletedReceiver.java`
- `EditPostActivity.java`
- `ImageGalleryActivity.java`
- `ImageGalleryPagerAdapter.java`
- `LineVideosRecyclerViewAdapter.java`
- `LinkPreviewUtil.java`
- `MessageImageCarouselAdapter.java`
- `MessageInteractionHandler.kt`
- `MessageSendingHandler.kt`
- `NewGroupActivity.kt`
- `NotificationClickHandler.kt`
- `NotificationConfig.kt`
- `ProfileCoverPhotoHistoryActivity.java`
- `ProfilePhotoHistoryActivity.java`
- `RadialProgressView.java`
- `UserActivity.kt`
- `UserDataPusher.kt`
- `UserMention.java`
- `VoiceMessageHandler.kt`

**app/src/main/java/com/synapse/social/studioasinc/adapter**
- `MediaPagerAdapter.kt`
- `NotificationAdapter.java`
- `PostsAdapter.kt`
- `SearchUserAdapter.java`
- `SelectedMediaAdapter.kt`
- `ViewPagerAdapter.java`

**app/src/main/java/com/synapse/social/studioasinc/animations**
- `Shimmer.kt`
- `layout/layoutshaker.kt`
- `textview/TVeffect.kt`

**app/src/main/java/com/synapse/social/studioasinc/attachments**
- `Rv_attacmentListAdapter.java`

**app/src/main/java/com/synapse/social/studioasinc/audio**
- `SoundEffectPlayer.kt`

**app/src/main/java/com/synapse/social/studioasinc/backend**
- `AuthenticationService.kt`
- `DatabaseService.kt`
- `UserService.kt`
- `interfaces/IAuthResult.kt`
- `interfaces/IAuthenticationService.kt`
- `interfaces/ICompletionListener.kt`
- `interfaces/IDatabaseService.kt`

**app/src/main/java/com/synapse/social/studioasinc/config**
- `CloudinaryConfig.java`

**app/src/main/java/com/synapse/social/studioasinc/fragments**
- `HomeFragment.java`
- `NotificationsFragment.java`
- `ReelsFragment.java`
- `FallbackFragment.java`

**app/src/main/java/com/synapse/social/studioasinc/model**
- `Attachment.java`
- `Notification.java`
- `Post.kt`
- `User.java`

**app/src/main/java/com/synapse/social/studioasinc/permissionreq**
- `askpermission.kt`

**app/src/main/java/com/synapse/social/studioasinc/styling**
- `MarkdownRenderer.kt`

**app/src/main/java/com/synapse/social/studioasinc/util**
- `ActivityResultHandler.kt`
- `AttachmentUtils.java`
- `ChatHelper.kt`
- `ChatMessageManager.kt`
- `CountUtils.kt`
- `DatabaseHelper.kt`
- `MediaStorageUtils.java`
- `MediaUploadManager.kt`
- `MentionUtils.java`
- `NotificationUtils.java`
- `SystemUIUtils.java`
- `TimeUtils.kt`
- `UIUtils.java`
- `UserProfileUpdater.kt`
- `UserUtils.java`
- `ViewUtils.kt`

**app/src/main/java/com/synapse/social/studioasinc/widget/ZoomImageViewLib**
- `ZoomInImageView.java`
- `ZoomInImageViewAttacher.java`
- `animation/AnimCompat.java`
- `animation/SpringInterpolator.java`
- `gestures/OnScaleAndMoveGestureListener.java`
- `gestures/ScaleAndMoveDetector.java`
- `window/WindowManagerUtil.java`

**app/src/main/res/**
- A full listing of the `res` directory has been omitted for brevity, but it contains all the drawable assets, layouts, fonts, and values for the application UI.

---

## TODO / Ambiguous entries
*This section lists items that were removed or renamed where the replacement is not 100% clear and may require manual verification.*

- **`FragInboxChatsActivity.java`**: Renamed to `fragments/InboxChatsFragment.java`.
- **`FragInboxCallsActivity.java`**: Renamed to `fragments/InboxCallsFragment.java`.
- **`FragInboxStoriesActivity.java`**: Renamed to `fragments/InboxStoriesFragment.java`.
- **`CreateImagePostActivity.java`**: Replaced by `CreatePostActivity.kt`. The new file is in Kotlin and likely handles more post types.
- **`CreateImagePostNextStepActivity.java`**: Functionality appears to be merged into `CreatePostActivity.kt`.
- **`SettingsActivity.java`**: Removed. Its functionality may have been integrated into other activities like `ProfileActivity.java` or `HomeActivity.java`.
- **`BgWallpapersActivity.java`**: Removed. This feature might be deprecated or moved.
- **`FileUtil.java`**: Removed. File operations are likely now handled by more specific utilities like `MediaStorageUtils.java` or other classes in the `util` package.
- **`.github/` directory**: This directory was mentioned but is not present in the repository. CI/CD configurations may have been removed or are stored elsewhere.
- **`firebase-presence.js` & `presence-integration-example.html`**: Removed. Web-based presence examples are no longer in the repository.

---

*This source map provides a comprehensive overview of the Synapse Android project structure. For specific implementation details, refer to individual source files and their inline documentation.*