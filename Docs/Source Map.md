# Synapse Android - Source Map

## Project Overview

**Synapse** is a next-generation open-source social platform for Android, developed by StudioAs Inc. The project combines speed, privacy, and customization with real-time communication features, zero ads, and a non-profit model.

### Project Statistics
> **⚠️ Note**: These statistics are current as of the last documentation update. For current values, check the build.gradle files directly.

- **Language Distribution**: Primarily Java with Kotlin components (exact distribution may vary)
- **Package**: `com.synapse.social.studioasinc`
- **Version**: 1.0.0-alpha06 (versionCode: 14) *- Check app/build.gradle for current version*
- **Min SDK**: 26, Target SDK: 32, Compile SDK: 36 *- Check app/build.gradle for current SDK levels*

## Project Structure

```
synapse-android/
├── app/                              # Main Android application module
│   ├── src/main/                     # Main source directory
│   │   ├── java/com/synapse/social/studioasinc/  # Core application code
│   │   ├── res/                      # Android resources
│   │   ├── assets/                   # Application assets
│   │   └── AndroidManifest.xml       # App configuration and permissions
│   ├── libs/                         # Local AAR dependencies
│   ├── build.gradle                  # App-level build configuration
│   └── proguard-rules.pro           # Code obfuscation rules
├── gradle/                           # Gradle wrapper files
├── .github/                          # GitHub Actions and templates
│   ├── workflows/                    # CI/CD pipelines
│   └── ISSUE_TEMPLATE/              # Issue templates
├── Docs/                            # Project documentation
├── build.gradle                     # Project-level build configuration
├── settings.gradle                  # Gradle settings
├── gradle.properties               # Build properties
├── worker.js                       # Cloudflare Worker for server-side notifications
└── README.md                       # Project overview
```

## Core Architecture

### Application Entry Points

#### Main Application Class
- **`SynapseApp.java`** - Application class handling:
  - Firebase initialization
  - Notification bootstrap (see `NotificationConfig.kt`)
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
- **`FragInboxChatsActivity.java`** - Chat list fragment
- **`FragInboxCallsActivity.java`** - Call history fragment
- **`FragInboxStoriesActivity.java`** - Stories inbox fragment

#### Content Creation
- **`CreateImagePostActivity.java`** - Image post creation
- **`CreateImagePostNextStepActivity.java`** - Post publishing workflow
- **`CreateLineVideoActivity.java`** - Short video creation
- **`CreateLineVideoNextStepActivity.java`** - Video publishing workflow
- **`LineVideoPlayerActivity.java`** - Video playback interface

#### Settings & Configuration
- **`SettingsActivity.java`** - App settings
- **`SelectRegionActivity.java`** - Region selection
- **`BgWallpapersActivity.java`** - Background wallpaper selection
- **`ChatsettingsActivity.java`** - Chat-specific settings
- **`DisappearingMessageSettingsActivity.java`** - Message expiration settings

### Supporting Components

#### UI Components
- **`PostCommentsBottomSheetDialog.java`** - Comment interface
- **`PostMoreBottomSheetDialog.java`** - Post action menu
- **`UserProfileMoreBottomSheet.java`** - Profile action menu
- **`ContentDisplayBottomSheetDialogFragment.java`** - Content summary display

#### Utility Classes
- **`FileUtil.java`** - File operations and management
- **`StorageUtil.java`** - Data storage utilities
- **`SketchwareUtil.java`** - UI utility functions
- **`RequestNetwork.java`** - Network request wrapper
- **`RequestNetworkController.java`** - HTTP request management
- **`UploadFiles.java`** - File upload functionality
- **`ImageUploader.java`** - Image upload handling

#### Specialized Components
- **`PresenceManager.kt`** (Kotlin) - User online status management
- **`NotificationConfig.kt`** (Kotlin) - Centralized notification settings, feature flags, and system toggle
- **`NotificationHelper.kt`** (Kotlin) - Unified notification sender (client-side OneSignal REST or server-side Cloudflare Worker) with fallback
- **`RadialProgress.java`** - Custom progress indicator
- **`FadeEditText.java`** - Custom text input with fade effects

### AI Integration
- **`AI/Gemini.java`** - Google Gemini AI integration for content generation and assistance

### Package Organization

```
com.synapse.social.studioasinc/
├── [root]                           # Main activities and core classes
├── AI/                              # Artificial Intelligence components
├── animations/                      # Animation utilities
├── audio/                          # Audio processing components
├── lab/                            # Experimental features
├── permissionreq/                  # Permission request utilities
├── styling/                        # UI styling components
└── widget/                         # Custom widget implementations
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
- **OneSignal (client-side REST)** - Primary push service when enabled
- **Cloudflare Worker (server-side)** - Alternate push path and fallback
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
- `firebase-presence.js` - Firebase presence integration
- `presence-integration-example.html` - Web presence example

## CI/CD Pipeline

### GitHub Actions Workflow
- **Build Process**: Automated APK compilation
- **Testing**: Code quality checks
- **Distribution**: Telegram notification system
- **Artifacts**: APK delivery with commit tracking

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
- **End-to-end encryption (E2EE)** - Identity keypair + session key derivation; message encryption with AES-GCM
- **Signed APKs** - Release build verification

## Project Roadmap Features

### Current Features
- Real-time messaging with typing indicators
- End-to-end encrypted chats (see `crypto/E2EEHelper.java`)
- Image and video post creation
- User profiles and following system
- Push notifications via configurable system (`NotificationConfig.kt`)
- AI-powered content assistance
- Markdown support in messages

### Upcoming Features
- Peer-to-peer video/audio calling
- Enhanced story features
- Advanced content discovery
- Community groups
- Enhanced AI integration

---

## Documentation Maintenance

> **🔄 Keep Documentation Current**: This source map contains references to specific files, versions, and statistics that may change over time. 

### Regular Updates Needed:
- **Version numbers** in Project Statistics section
- **SDK levels** and **Gradle plugin versions**
- **Dependency versions** as they are updated
- **File lists** when new activities/components are added
- **Language distribution** statistics

### Quick Verification Commands:
```bash
# Check current versions
grep "versionName\|versionCode" app/build.gradle
grep "minSdk\|targetSdk\|compileSdk" app/build.gradle

# Count source files
find app/src/main/java -name "*.java" | wc -l
find app/src/main/java -name "*.kt" | wc -l
```

### Recommended Review Schedule:
- **After major releases** - Update version information
- **After dependency updates** - Update library versions
- **Monthly** - Verify file lists and statistics
- **When adding new features** - Update architecture descriptions

---

*This source map provides a comprehensive overview of the Synapse Android project structure. For specific implementation details, refer to individual source files and their inline documentation.*