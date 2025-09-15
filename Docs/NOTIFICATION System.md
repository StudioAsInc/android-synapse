# Notification System

This document describes the current notification architecture and how to configure and use it.

## Overview

Synapse supports two delivery paths, selectable at runtime via `NotificationConfig.kt`:
- Client-side: OneSignal REST API
- Server-side: Cloudflare Worker endpoint (`worker.js`)

Fallback is enabled so if the active path fails, the helper will attempt the alternate path when `ENABLE_FALLBACK_MECHANISMS` is true.

## Key Components

- `NotificationConfig.kt`
  - `USE_CLIENT_SIDE_NOTIFICATIONS`: Toggle between OneSignal (true) and Worker (false)
  - Credentials and titles: `ONESIGNAL_APP_ID`, `ONESIGNAL_REST_API_KEY`, `NOTIFICATION_TITLE`, `NOTIFICATION_SUBTITLE`
  - Channel and priority: `NOTIFICATION_CHANNEL_ID`, `NOTIFICATION_PRIORITY`
  - Feature flags: `ENABLE_DEEP_LINKING`, `ENABLE_SMART_SUPPRESSION`, `ENABLE_FALLBACK_MECHANISMS`
  - Server endpoint: `WORKER_URL`

- `NotificationHelper.kt`
  - `sendNotification(recipientUid, senderUid, message, notificationType, data?)`: Main API
  - Resolves OneSignal Player ID for `recipientUid`
  - Routes to client-side or server-side senders per `NotificationConfig`
  - Persists notification entry under `skyline/notifications/{recipientUid}`

## Sending Flow

1) Chat or feature triggers a notification, e.g. `ChatActivity` on message send:
   - Builds optional `data` map (e.g. `chatId` for deep linking)
   - Calls `NotificationHelper.sendNotification(...)`

2) Player ID lookup:
   - `skyline/users/{recipientUid}/oneSignalPlayerId` is read
   - If absent, sending is skipped

3) Delivery path selection:
   - If `USE_CLIENT_SIDE_NOTIFICATIONS` is true → OneSignal REST
   - Else → Cloudflare Worker (`WORKER_URL`)

4) Fallback (optional):
   - On error, the helper attempts the alternate path when enabled

5) Storage:
   - A `model.Notification` entry is written to `skyline/notifications/{recipientUid}`

## Deep Linking

When `ENABLE_DEEP_LINKING` is true, `data` payload includes fields such as:
- `sender_uid`: The UID of the sender (when available)
- `type`: Notification type (e.g. `chat_message`)
- Additional keys (e.g. `chatId`) for in-app navigation

The app-side click handler should read these keys to navigate to the correct screen.

## Notification Types and Titles

`NotificationConfig.kt` maps types to human-friendly titles via `getTitleForNotificationType(type)` and provides a default title.

Common values used across the app:
- `chat_message` (via `NotificationHelper`)
- Post-related types such as `NEW_POST`, `NEW_COMMENT`, `NEW_REPLY`, etc.

## OneSignal Channel ID

`android_channel_id` is added only when `NOTIFICATION_CHANNEL_ID` appears to be a valid UUID. If not, it is omitted to avoid OneSignal API errors.

## Presence Smart Suppression

The previous presence-based suppression has been removed. Notifications are sent without checking live presence, though `RECENT_ACTIVITY_THRESHOLD` remains available in `NotificationConfig` for future use.

## Server-Side Worker

For server-side delivery, `worker.js` receives JSON:
```
{
  "recipientUserId": "<OneSignal Player ID>",
  "notificationMessage": "<text>"
}
```
Extend as needed to handle various `notificationType` and `data` payloads.

## Configuration Checklist

- Set valid `ONESIGNAL_APP_ID` and `ONESIGNAL_REST_API_KEY`
- Ensure users store `oneSignalPlayerId` under `skyline/users/{uid}`
- Set `USE_CLIENT_SIDE_NOTIFICATIONS` appropriately
- Verify `WORKER_URL` for server-side path
- Optionally enable `ENABLE_FALLBACK_MECHANISMS` and `ENABLE_DEEP_LINKING`