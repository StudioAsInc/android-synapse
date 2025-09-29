# Synapse Android - Architecture

This document provides a detailed overview of the Synapse Android application's architecture, components, and design patterns.

## Table of Contents
- [Application Entry Points](#application-entry-points)
- [Feature Modules](#feature-modules)
- [Supporting Components](#supporting-components)
- [AI Integration](#ai-integration)
- [External Dependencies](#external-dependencies)

## Application Entry Points

### Main Application Class
- **`SynapseApp.java`**: The main application class that handles initialization of core services.
  - *Responsibilities*: Firebase, OneSignal, global exception handling, and user presence management.

### Primary Activities
- **`SplashActivity.java`**: The initial splash screen.
- **`MainActivity.java`**: The launcher activity and entry point for user authentication.
- **`HomeActivity.java`**: The main hub for the social feed and navigation after authentication.
- **`AuthActivity.java`**: Handles user authentication, registration, and sign-in flows.
- **`CheckpermissionActivity.java`**: Manages runtime permission requests required by the application.
- **`DebugActivity.java`**: An activity for debugging purposes.

## Feature Modules

### Social Features
- **`ProfileActivity.java`**: Manages user profile display and interactions.
- **`ProfileEditActivity.java`**: Provides functionality for users to edit their profiles.
- **`SearchActivity.java`**: Enables searching for users and content within the platform.
- **`UserFollowsListActivity.java`**: Displays lists of users who are following or are followed by the current user.

### Messaging System
- **`InboxActivity.java`**: The main interface for the user's message inbox.
- **`ChatActivity.java`**: Handles individual and group chat conversations.
- **`ChatAdapter.java`**: An adapter for displaying messages within a chat conversation.
- **`FragInboxChatsActivity.java`**: A fragment that displays the list of active chats.
- **`FragInboxCallsActivity.java`**: A fragment that shows the user's call history.
- **`FragInboxStoriesActivity.java`**: A fragment for displaying stories in the inbox.

### Content Creation
- **`CreateImagePostActivity.java`**: Allows users to create posts with images.
- **`CreateImagePostNextStepActivity.java`**: The second step in the image post publishing workflow.
- **`CreateLineVideoActivity.java`**: Enables the creation of short video posts.
- **`CreateLineVideoNextStepActivity.java`**: The second step in the video post publishing workflow.
- **`LineVideoPlayerActivity.java`**: An activity for playing back video content.

### Settings & Configuration
- **`SettingsActivity.java`**: Provides access to global application settings.
- **`SelectRegionActivity.java`**: Allows users to select their geographical region.
- **`BgWallpapersActivity.java`**: Enables users to customize their background wallpapers.
- **`ChatsettingsActivity.java`**: Contains settings specific to chat functionalities.
- **`DisappearingMessageSettingsActivity.java`**: Manages settings for message expiration.

### Lab (Experimental Features)
- **`LabActivity.java`**: Main entry point for experimental features.
- **`LabDashboardActivity.java`**: Dashboard for lab features.
- **`LabFeaturesActivity.java`**: Lists available lab features.
- **`LabSettingsActivity.java`**: Settings for experimental features.
- **`LabVerifyActivity.java`**: Verification for lab features.

## Supporting Components

### UI Components
- **`PostCommentsBottomSheetDialog.java`**: A bottom sheet dialog for displaying and adding comments to posts.
- **`PostMoreBottomSheetDialog.java`**: A bottom sheet menu for additional post actions.
- **`ContentDisplayBottomSheetDialogFragment.java`**: A bottom sheet fragment for displaying content summaries.
- **`animations/FadePageTransformer.java`**: A `ViewPager2` transformer for fade animations.
- **`styling/Styling.java`**: A class for managing UI styles.
- **`widget/CompassView.java`**: A custom view for displaying a compass.
- **`widget/RadialProgress.java`**: A custom view for displaying a radial progress indicator.
- **`FadeEditText.java`**: A custom `EditText` with fade-in/out text effects.

### Utility Classes
- **`FileUtil.java`**: A utility class for file operations and management.
- **`StorageUtil.java`**: Provides utilities for data storage.
- **`SketchwareUtil.java`**: Contains UI utility functions.
- **`RequestNetwork.java`**: A wrapper for making network requests.
- **`RequestNetworkController.java`**: Manages and controls HTTP requests.
- **`UploadFiles.java`**: Handles the functionality for uploading files.
- **`ImageUploader.java`**: A specialized class for handling image uploads.
- **`audio/AudioPlayer.java`**: A class for playing audio files.

### Specialized Components
- **`PresenceManager.kt`**: A Kotlin class for managing user online status.
- **`NotificationHelper.kt`**: A Kotlin class for handling push notifications.
- **`OneSignalManager.kt`**: A Kotlin class for integrating with the OneSignal service.

## AI Integration
- **`AI/Gemini.java`**: Integrates with Google's Gemini AI for content generation and other AI-powered features.

## External Dependencies
A comprehensive list of external dependencies can be found in the `app/build.gradle` file. Key dependencies include:
- **Firebase**: For authentication, database, storage, and analytics.
- **Glide**: For image loading and caching.
- **Lottie**: For rendering complex animations.
- **OneSignal**: For push notification services.
- **Markwon**: For rendering Markdown text.
- **ExoPlayer**: For video playback.