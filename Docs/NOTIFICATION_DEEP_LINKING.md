# Notification Deep Linking Implementation

This document describes the implementation of notification click handling for in-app navigation, addressing GitHub issue #290.

## Overview

The notification system now supports deep linking that allows users to be directed to the relevant content within the app when they tap on notifications.

## Implementation Details

### 1. Notification Click Handler (`NotificationClickHandler.kt`)

A new class that implements `INotificationClickListener` to handle notification clicks:

- **Chat Notifications**: Opens the specific chat with the sender
- **Post Notifications**: Opens the user's profile or specific post
- **Comment/Reply Notifications**: Opens the post with comments highlighted
- **Like Notifications**: Opens the relevant post or comment
- **Default**: Opens the home activity

### 2. Deep Link URLs

The system generates deep link URLs based on notification type:

- `synapse://chat?uid=<sender_uid>&chatId=<chat_id>` - For chat messages
- `synapse://profile?uid=<user_uid>&postId=<post_id>` - For profile/post related notifications
- `synapse://home?postId=<post_id>&commentId=<comment_id>` - For comment/reply notifications
- `synapse://home` - Default fallback

### 3. AndroidManifest.xml Updates

Added intent filters for deep linking to key activities:

- **HomeActivity**: `synapse://home`
- **ChatActivity**: `synapse://chat`
- **ProfileActivity**: `synapse://profile`

All activities are configured with `android:launchMode="singleTop"` to prevent duplicate instances.

### 4. OneSignal Integration

Updated `SynapseApp.java` to register the notification click handler:

```java
OneSignal.getNotifications().addClickListener(new NotificationClickHandler());
```

### 5. Enhanced Notification Data

Updated `NotificationHelper.kt` to include:

- Proper deep link URLs in notification payload
- Enhanced data structure for better navigation
- Support for different notification types

## Notification Types Supported

1. **Chat Messages** (`chat_message`)
   - Data: `sender_uid`, `chat_id`
   - Action: Opens ChatActivity with specific user/chat

2. **New Posts** (`NEW_POST`)
   - Data: `sender_uid`, `postId`
   - Action: Opens ProfileActivity with specific post

3. **Comments** (`NEW_COMMENT`)
   - Data: `postId`, `commentId`
   - Action: Opens HomeActivity with post highlighted

4. **Replies** (`NEW_REPLY`)
   - Data: `postId`, `commentId`
   - Action: Opens HomeActivity with comment thread

5. **Post Likes** (`NEW_LIKE_POST`)
   - Data: `postId`
   - Action: Opens HomeActivity with specific post

6. **Comment Likes** (`NEW_LIKE_COMMENT`)
   - Data: `postId`, `commentId`
   - Action: Opens HomeActivity with specific comment

## Testing

To test the deep linking functionality:

1. Send a notification with the appropriate data structure
2. Tap the notification
3. Verify the app opens to the correct screen with the right content

## Configuration

The deep linking feature is controlled by `NotificationConfig.ENABLE_DEEP_LINKING` (currently set to `true`).

## Future Enhancements

- Add support for more notification types as needed
- Implement specific post viewer activity for better UX
- Add analytics to track deep link usage
- Support for external deep links (web to app)