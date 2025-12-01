# Database Structure

This document outlines the database structure for the Synapse Social application, inferred from the Kotlin data models and Supabase client usage.

## Overview

The application uses Supabase (PostgreSQL) as its backend. Key entities include Users, Posts, Comments, Chats, Messages, and Stories.

## Tables

### `users`

Stores user profile information.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | UUID/Text | Primary Key. |
| `uid` | Text | Unique User ID (likely links to `auth.users`). |
| `email` | Text | User's email address. |
| `username` | Text | Unique username. |
| `nickname` | Text | User's nickname. |
| `display_name` | Text | Display name shown in UI. |
| `biography` | Text | Short biography. |
| `bio` | Text | Another bio field (legacy?). |
| `avatar` | Text | URL or path to avatar image. |
| `profile_image_url` | Text | URL to profile image. |
| `avatar_history_type` | Text | Type of avatar history (default: 'local'). |
| `profile_cover_image` | Text | URL to cover image. |
| `account_premium` | Boolean | Premium status flag. |
| `user_level_xp` | Integer | User experience points (default: 500). |
| `verify` | Boolean | Verification status. |
| `account_type` | Text | Type of account (default: 'user'). |
| `gender` | Text | User gender (default: 'hidden'). |
| `banned` | Boolean | Ban status. |
| `status` | Text | Online status (e.g., 'offline'). |
| `join_date` | Timestamp | Date user joined. |
| `one_signal_player_id` | Text | OneSignal ID for push notifications. |
| `last_seen` | Timestamp | Last active timestamp. |
| `chatting_with` | Text | UID of user currently chatting with. |
| `created_at` | Timestamp | Record creation timestamp. |
| `updated_at` | Timestamp | Record update timestamp. |
| `followers_count` | Integer | Cached count of followers. |
| `following_count` | Integer | Cached count of following. |
| `posts_count` | Integer | Cached count of posts. |

### `posts`

Stores user posts.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | UUID/Text | Primary Key. |
| `key` | Text | Unique key for the post. |
| `author_uid` | Text | Foreign Key to `users.uid`. |
| `post_text` | Text | Text content of the post. |
| `post_image` | Text | URL to main post image. |
| `post_type` | Text | Type of post (e.g., 'IMAGE', 'VIDEO', 'TEXT'). |
| `publish_date` | Timestamp | Date published. |
| `timestamp` | BigInt | Epoch timestamp. |
| `likes_count` | Integer | Count of likes. |
| `comments_count` | Integer | Count of comments. |
| `views_count` | Integer | Count of views. |
| `reshares_count` | Integer | Count of reshares. |
| `post_hide_views_count` | Text/Bool | Setting to hide view count. |
| `post_hide_like_count` | Text/Bool | Setting to hide like count. |
| `post_hide_comments_count` | Text/Bool | Setting to hide comment count. |
| `post_disable_comments` | Text/Bool | Setting to disable comments. |
| `post_visibility` | Text | Visibility setting. |
| `media_items` | JSONB | Array of media items (images/videos). |
| `is_encrypted` | Boolean | Encryption flag. |
| `nonce` | Text | Encryption nonce. |
| `encryption_key_id` | Text | Key ID for encryption. |
| `is_deleted` | Boolean | Soft delete flag. |
| `is_edited` | Boolean | Edit flag. |
| `edited_at` | Timestamp | Edit timestamp. |
| `deleted_at` | Timestamp | Delete timestamp. |
| `has_poll` | Boolean | Flag if post contains a poll. |
| `poll_question` | Text | Poll question text. |
| `poll_options` | JSONB | Array of poll options. |
| `poll_end_time` | Timestamp | Poll expiration time. |
| `poll_allow_multiple` | Boolean | Poll configuration. |
| `has_location` | Boolean | Flag if post has location. |
| `location_name` | Text | Name of the location. |
| `location_address` | Text | Address of the location. |
| `location_latitude` | Double | Latitude. |
| `location_longitude` | Double | Longitude. |
| `location_place_id` | Text | Place ID. |
| `youtube_url` | Text | Embedded YouTube URL. |

### `comments`

Stores comments on posts.

| Column | Type | Description |
| :--- | :--- | :--- |
| `key` | Text | Primary Key (Comment ID). |
| `uid` | Text | Foreign Key to `users.uid` (Author). |
| `comment` | Text | Comment content. |
| `push_time` | Timestamp | Creation timestamp. |
| `post_key` | Text | Foreign Key to `posts.key` (or `id`). |
| `reply_comment_key` | Text | Self-reference Foreign Key (for nested replies). |
| `is_pinned` | Boolean | Pin status. |
| `pinned_at` | Timestamp | Pin timestamp. |
| `pinned_by` | Text | User who pinned the comment. |
| `edited_at` | Timestamp | Edit timestamp. |
| `report_count` | Integer | Count of reports. |

### `chats`

Stores chat conversations (direct messages and groups).

| Column | Type | Description |
| :--- | :--- | :--- |
| `chat_id` | Text | Primary Key (or Unique Key). Often constructed as `dm_{uid1}_{uid2}` for DMs. |
| `id` | Text | UUID (likely internal PK). |
| `name` | Text | Chat name (for groups). |
| `created_by` | Text | Foreign Key to `users.uid`. |
| `created_at` | BigInt/TS | Creation timestamp. |
| `updated_at` | BigInt/TS | Update timestamp. |
| `is_group` | Boolean | Group chat flag. |
| `is_active` | Boolean | Active status. |
| `participant_count` | Integer | Count of participants. |
| `last_message` | Text | Preview of the last message. |
| `last_message_time` | BigInt/TS | Timestamp of the last message. |
| `last_message_sender` | Text | UID of the last message sender. |

### `chat_participants`

Stores participants in a chat.

| Column | Type | Description |
| :--- | :--- | :--- |
| `chat_id` | Text | Foreign Key to `chats.chat_id`. |
| `user_id` | Text | Foreign Key to `users.uid`. |
| `role` | Text | Role in chat (e.g., 'member'). |
| `is_admin` | Boolean | Admin status. |
| `can_send_messages` | Boolean | Permission to send messages. |
| `joined_at` | BigInt/TS | Timestamp when user joined. |

### `messages`

Stores messages within chats.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | Text | Primary Key. |
| `chat_id` | Text | Foreign Key to `chats.id`. |
| `sender_id` | Text | Foreign Key to `users.uid`. |
| `content` | Text | Message text content. |
| `message_type` | Text | Type (e.g., 'text', 'image', 'video', 'audio', 'file'). |
| `media_url` | Text | URL for media messages. |
| `created_at` | BigInt/TS | Creation timestamp. |
| `updated_at` | BigInt/TS | Update timestamp. |
| `is_deleted` | Boolean | Soft delete flag. |
| `is_edited` | Boolean | Edit flag. |
| `reply_to_id` | Text | Self-reference Foreign Key (reply). |
| `edit_history` | JSONB | History of edits. |
| `forwarded_from_message_id`| Text | ID of original message if forwarded. |
| `forwarded_from_chat_id` | Text | ID of original chat if forwarded. |
| `delete_for_everyone` | Boolean | Delete type flag. |
| `attachments` | JSONB | Array of attachments. |

### `follows`

Stores follow relationships between users.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | Text | Primary Key. |
| `follower_id` | Text | Foreign Key to `users.uid`. |
| `following_id` | Text | Foreign Key to `users.uid`. |
| `created_at` | Timestamp | Creation timestamp. |

### `stories`

Stores ephemeral user stories.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | Text | Primary Key. |
| `user_id` | Text | Foreign Key to `users.uid`. |
| `image_url` | Text | Story image URL. |
| `video_url` | Text | Story video URL. |
| `content` | Text | Story text content. |
| `created_at` | Timestamp | Creation timestamp. |
| `expires_at` | Timestamp | Expiration timestamp. |

## Relationships inferred

-   **Users <-> Posts**: One-to-Many (`users.uid` -> `posts.author_uid`)
-   **Users <-> Comments**: One-to-Many (`users.uid` -> `comments.uid`)
-   **Posts <-> Comments**: One-to-Many (`posts.key` -> `comments.post_key`)
-   **Users <-> Follows**: Many-to-Many (via `follows` table with `follower_id` and `following_id`)
-   **Users <-> Chats**: Many-to-Many (via `chat_participants` table).
-   **Chats <-> Messages**: One-to-Many (`chats.chat_id` -> `messages.chat_id`)
-   **Users <-> Stories**: One-to-Many (`users.uid` -> `stories.user_id`)

## JSONB Usage

The database makes use of `JSONB` columns for storing structured data that doesn't need its own table, such as:
-   `posts.media_items`: Storing multiple media files for a single post.
-   `posts.poll_options`: Storing options for a poll.
-   `messages.attachments`: Storing file attachments.
-   `messages.edit_history`: Storing version history of messages.

## Security & Access Control

-   The application uses Row Level Security (RLS) policies (implied by Supabase best practices, though policies are not visible in the code models).
-   `users.uid` maps to the Supabase Auth user ID.

---
*Note: This structure is inferred from the Kotlin data models located in `app/src/main/java/com/synapse/social/studioasinc/model/`. Actual database schema might vary slightly.*
