# Source Map

This document provides an overview of the source code structure of the Synapse Android application.

## Java/Kotlin Files

### Activities
- **/app/src/main/java/com/synapse/social/studioasinc/CreateGroupActivity.kt**: Handles the creation of new chat groups.
- **/app/src/main/java/com/synapse/social/studioasinc/ProfileCoverPhotoHistoryActivity.java**: Displays the history of a user's profile cover photos.
- **/app/src/main/java/com/synapse/social/studioasinc/ChatGroupActivity.kt**: Manages the user interface and logic for group chat conversations.
- **/app/src/main/java/com/synapse/social/studioasinc/ChatActivity.java**: Manages the user interface and logic for one-on-one chat conversations.
- **/app/src/main/java/com/synapse/social/studioasinc/EditPostActivity.java**: Allows users to edit their existing posts.
- **/app/src/main/java/com/synapse/social/studioasinc/InboxActivity.java**: The main activity for the user's inbox, likely displaying a list of conversations.
- **/app/src/main/java/com/synapse/social/studioasinc/MainActivity.java**: The main entry point of the application after authentication.
- **/app/src/main/java/com/synapse/social/studioasinc/ConversationSettingsActivity.kt**: Provides settings for a specific conversation.
- **/app/src/main/java/com/synapse/social/studioasinc/CreatePostActivity.kt**: Allows users to create new posts.
- **/app/src/main/java/com/synapse/social/studioasinc/ProfileEditActivity.java**: Allows users to edit their profile information.
- **/app/src/main/java/com/synapse/social/studioasinc/SearchActivity.java**: Handles the search functionality of the application.
- **/app/src/main/java/com/synapse/social/studioasinc/CheckpermissionActivity.java**: Checks for necessary permissions required by the application.
- **/app/src/main/java/com/synapse/social/studioasinc/CompleteProfileActivity.java**: Guides the user to complete their profile after registration.
- **/app/src/main/java/com/synapse/social/studioasinc/ImageGalleryActivity.java**: Displays a gallery of images.
- **/app/src/main/java/com/synapse/social/studioasinc/ProfileActivity.java**: Displays a user's profile.
- **/app/src/main/java/com/synapse/social/studioasinc/CreateLineVideoActivity.java**: Allows users to create "Line" videos.
- **/app/src/main/java/com/synapse/social/studioasinc/NewGroupActivity.kt**: Likely another activity for creating a new group, possibly a different flow.
- **/app/src/main/java/com/synapse/social/studioasinc/ChatsettingsActivity.java**: Older or alternative version of conversation settings.
- **/app/src/main/java/com/synapse/social/studioasinc/UserActivity.kt**: Displays a user's profile and related information.
- **/app/src/main/java/com/synapse/social/studioasinc/ProfilePhotoHistoryActivity.java**: Displays the history of a user's profile photos.
- **/app/src/main/java/com/synapse/social/studioasinc/HomeActivity.java**: The main home screen of the application.
- **/app/src/main/java/com/synapse/social/studioasinc/SelectRegionActivity.java**: Allows the user to select their region.
- **/app/src/main/java/com/synapse/social/studioasinc/DisappearingMessageSettingsActivity.java**: Manages settings for disappearing messages.
- **/app/src/main/java/com/synapse/social/studioasinc/LineVideoPlayerActivity.java**: Plays "Line" videos.
- **/app/src/main/java/com/synapse/social/studioasinc/DebugActivity.java**: An activity for debugging purposes.
- **/app/src/main/java/com/synapse/social/studioasinc/UserFollowsListActivity.java**: Displays a list of users that a user follows or is followed by.
- **/app/src/main/java/com/synapse/social/studioasinc/CreateLineVideoNextStepActivity.java**: The next step in the "Line" video creation process.

### Fragments
- **/app/src/main/java/com/synapse/social/studioasinc/lab/FallbackFragment.java**: A fallback fragment to be displayed in case of errors.
- **/app/src/main/java/com/synapse/social/studioasinc/InboxCallsFragment.java**: Displays a list of calls in the inbox.
- **/app/src/main/java/com/synapse/social/studioasinc/fragments/HomeFragment.java**: The fragment for the home feed.
- **/app/src/main/java/com/synapse/social/studioasinc/fragments/NotificationsFragment.java**: The fragment for displaying notifications.
- **/app/src/main/java/com/synapse/social/studioasinc/fragments/ReelsFragment.java**: The fragment for displaying short-form videos (reels).
- **/app/src/main/java/com/synapse/social/studioasinc/InboxChatsFragment.java**: Displays a list of chats in the inbox.
- **/app/src/main/java/com/synapse/social/studioasinc/InboxStoriesFragment.java**: Displays a list of stories in the inbox.

### Adapters
- **/app/src/main/java/com/synapse/social/studioasinc/ChatAdapter.java**: Adapter for displaying messages in a chat.
- **/app/src/main/java/com/synapse/social/studioasinc/ImageGalleryPagerAdapter.java**: Pager adapter for the image gallery.
- **/app/src/main/java/com/synapse/social/studioasinc/adapter/PostsAdapter.kt**: Adapter for displaying posts in a feed.
- **/app/src/main/java/com/synapse/social/studioasinc/adapter/ViewPagerAdapter.java**: A general-purpose ViewPager adapter.
- **/app/src/main/java/com/synapse/social/studioasinc/adapter/NotificationAdapter.java**: Adapter for displaying notifications.
- **/app/src/main/java/com/synapse/social/studioasinc/adapter/MediaPagerAdapter.kt**: Pager adapter for media content.
- **/app/src/main/java/com/synapse/social/studioasinc/adapter/SelectedMediaAdapter.kt**: Adapter for displaying selected media.
- **/app/src/main/java/com/synapse/social/studioasinc/adapter/SearchUserAdapter.java**: Adapter for displaying user search results.
- **/app/src/main/java/com/synapse/social/studioasinc/MessageImageCarouselAdapter.java**: Carousel adapter for images within a message.
- **/app/src/main/java/com/synapse/social/studioasinc/attachments/Rv_attacmentListAdapter.java**: RecyclerView adapter for attachments.
- **/app/src/main/java/com/synapse/social/studioasinc/LineVideosRecyclerViewAdapter.java**: RecyclerView adapter for "Line" videos.

### Models
- **/app/src/main/java/com/synapse/social/studioasinc/model/User.java**: Data model for a user.
- **/app/src/main/java/com/synapse/social/studioasinc/model/Notification.java**: Data model for a notification.
- **/app/src/main/java/com/synapse/social/studioasinc/model/Post.kt**: Data model for a post.
- **/app/src/main/java/com/synapse/social/studioasinc/model/Attachment.java**: Data model for an attachment.

### Utilities
- **/app/src/main/java/com/synapse/social/studioasinc/styling/MarkdownRenderer.kt**: Renders Markdown text.
- **/app/src/main/java/com/synapse/social/studioasinc/config/CloudinaryConfig.java**: Configuration for the Cloudinary service.
- **/app/src/main/java/com/synapse/social/studioasinc/ImageUploader.java**: Handles image uploads.
- **/app/src/main/java/com/synapse/social/studioasinc/NotificationConfig.kt**: Configuration for notifications.
- **/app/src/main/java/com/synapse/social/studioasinc/NotificationHelper.kt**: Helper class for creating and managing notifications.
- **/app/src/main/java/com/synapse/social/studioasinc/AI/Gemini.java**: Integration with the Gemini AI model.
- **/app/src/main/java/com/synapse/social/studioasinc/PresenceManager.kt**: Manages user presence (online/offline status).
- **/app/src/main/java/com/synapse/social/studioasinc/UserDataPusher.kt**: Pushes user data to the backend.
- **/app/src/main/java/com/synapse/social/studioasinc/RequestNetworkController.java**: Manages network requests.
- **/app/src/main/java/com/synapse/social/studioasinc/util/CountUtils.kt**: Utility functions for counting.
- **/app/src/main/java/com/synapse/social/studioasinc/util/UIUtils.java**: Utility functions for UI-related tasks.
- **/app/src/main/java/com/synapse/social/studioasinc/util/MediaUploadManager.kt**: Manages media uploads.
- **/app/src/main/java/com/synapse/social/studioasinc/util/NotificationUtils.java**: Utility functions for notifications.
- **/app/src/main/java/com/synapse/social/studioasinc/util/UserProfileUpdater.kt**: Updates user profiles.
- **/app/src/main/java/com/synapse/social/studioasinc/util/ViewUtils.kt**: Utility functions for Views.
- **/app/src/main/java/com/synapse/social/studioasinc/util/UserUtils.java**: Utility functions for user-related tasks.
- **/app/src/main/java/com/synapse/social/studioasinc/util/ChatMessageManager.kt**: Manages chat messages.
- **/app/src/main/java/com/synapse/social/studioasinc/util/MentionUtils.java**: Utility functions for handling user mentions.
- **/app/src/main/java/com/synapse/social/studioasinc/util/TimeUtils.kt**: Utility functions for time and date formatting.
- **/app/src/main/java/com/synapse/social/studioasinc/util/SystemUIUtils.java**: Utility functions for system UI.
- **/app/src/main/java/com/synapse/social/studioasinc/util/MediaStorageUtils.java**: Utility functions for media storage.
- **/app/src/main/java/com/synapse/social/studioasinc/util/AttachmentUtils.java**: Utility functions for attachments.
- **/app/src/main/java/com/synapse/social/studioasinc/StorageUtil.java**: Utility functions for storage.
- **/app/src/main/java/com/synapse/social/studioasinc/LinkPreviewUtil.java**: Utility for generating link previews.
- **/app/src/main/java/com/synapse/social/studioasinc/FileUtil.java**: Utility for file operations.
- **/app/src/main/java/com/synapse/social/studioasinc/SketchwareUtil.java**: Utility class, possibly from a framework or for compatibility.

### Other
- **/app/src/main/java/com/synapse/social/studioasinc/UserMention.java**: Represents a user mention.
- **/app/src/main/java/com/synapse/social/studioasinc/animations/layout/layoutshaker.kt**: Animation for shaking a layout.
- **/app/src/main/java/com/synapse/social/studioasinc/animations/Shimmer.kt**: Shimmer loading animation.
- **/app/src/main/java/com/synapse/social/studioasinc/animations/textview/TVeffect.kt**: TextView animation effect.
- **/app/src/main/java/com/synapse/social/studioasinc/ChatAdapterListener.java**: Listener interface for the ChatAdapter.
- **/app/src/main/java/com/synapse/social/studioasinc/ContentDisplayBottomSheetDialogFragment.java**: Bottom sheet for displaying content.
- **/app/src/main/java/com/synapse/social/studioasinc/PostCommentsBottomSheetDialog.java**: Bottom sheet for post comments.
- **/app/src/main/java/com/synapse/social/studioasinc/CenterCropLinearLayout.java**: A custom LinearLayout with center cropping.
- **/app/src/main/java/com/synapse/social/studioasinc/NotificationClickHandler.kt**: Handles clicks on notifications.
- **/app/src/main/java/com/synapse/social/studioasinc/OneSignalManager.kt**: Manages OneSignal integration.
- **/app/src/main/java/com/synapse/social/studioasinc/CarouselItemDecoration.java**: Item decoration for carousels.
- **/app/src/main/java/com/synapse/social/studioasinc/RequestNetwork.java**: Class for making network requests.
- **/app/src/main/java/com/synapse/social/studioasinc/RadialProgress.java**: A radial progress bar.
- **/app/src/main/java/com/synapse/social/studioasinc/CenterCropLinearLayoutNoEffect.java**: A custom LinearLayout with center cropping and no effect.
- **/app/src/main/java/com/synapse/social/studioasinc/RadialProgressView.java**: A view for displaying radial progress.
- **/app/src/main/java/com/synapse/social/studioasinc/SynapseApp.java**: The main Application class.
- **/app/src/main/java/com/synapse/social/studioasinc/AsyncUploadService.java**: A service for asynchronous uploads.
- **/app/src/main/java/com/synapse/social/studioasinc/FadeEditText.java**: A custom EditText with a fade effect.
- **/app/src/main/java/com/synapse/social/studioasinc/audio/SoundEffectPlayer.kt**: Plays sound effects.
- **/app/src/main/java/com/synapse/social/studioasinc/PostMoreBottomSheetDialog.java**: Bottom sheet for more post options.
- **/app/src/main/java/com/synapse/social/studioasinc/UploadFiles.java**: Handles file uploads.
- **/app/src/main/java/com/synapse/social/studioasinc/permissionreq/askpermission.kt**: Asks for permissions.
- **/app/src/main/java/com/synapse/social/studioasinc/DownloadCompletedReceiver.java**: BroadcastReceiver for download completion.
- **/app/src/main/java/com/synapse/social/studioasinc/ChatViewHolders.java**: ViewHolders for the chat.

## XML Files

The XML files in this project are primarily used for defining UI layouts, styles, and other resources. A detailed breakdown of each XML file is not provided here, but they are located in the `/app/src/main/res/` directory and its subdirectories (`drawable`, `layout`, `menu`, `values`, etc.).