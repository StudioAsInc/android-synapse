# Supabase Database Schema

This document outlines the PostgreSQL schema used in Supabase, which is designed to replace the previous Firebase Realtime Database structure.

## Tables

### `users`

Stores user profile information.

| Column                | Type      | Constraints                               | Description                               |
| --------------------- | --------- | ----------------------------------------- | ----------------------------------------- |
| `uid`                 | `uuid`    | Primary Key, Foreign Key to `auth.users.id` | The user's unique identifier.             |
| `email`               | `text`    |                                           | The user's email address.                 |
| `username`            | `text`    | Unique                                    | The user's unique username.               |
| `nickname`            | `text`    |                                           | The user's display name.                  |
| `biography`           | `text`    |                                           | A short bio about the user.               |
| `avatar_url`          | `text`    |                                           | URL for the user's profile picture.       |
| `profile_cover_image` | `text`    |                                           | URL for the user's cover photo.           |
| `join_date`           | `timestamp` |                                           | The date and time the user joined.        |
| `banned`              | `boolean` | Default: `false`                          | Indicates if the user is banned.          |
| `status`              | `text`    |                                           | The user's online status (e.g., 'online').|
| `account_type`        | `text`    | Default: `'user'`                         | The user's account type (e.g., 'admin').  |

### `posts`

Stores posts created by users.

| Column      | Type      | Constraints                     | Description                            |
| ----------- | --------- | ------------------------------- | -------------------------------------- |
| `key`       | `uuid`    | Primary Key, Default: `uuid_generate_v4()` | The unique identifier for the post.    |
| `uid`       | `uuid`    | Foreign Key to `users.uid`      | The UID of the user who created the post. |
| `post_text` | `text`    |                                 | The content of the post.               |
| `image_url` | `text`    |                                 | URL for an image attached to the post. |
| `timestamp` | `timestamp` | Default: `now()`                | The date and time the post was created.|

### `followers`

A many-to-many relationship table for tracking user follows.

| Column          | Type      | Constraints                           | Description                               |
| --------------- | --------- | ------------------------------------- | ----------------------------------------- |
| `follower_uid`  | `uuid`    | Primary Key, Foreign Key to `users.uid` | The UID of the user who is following.     |
| `following_uid` | `uuid`    | Primary Key, Foreign Key to `users.uid` | The UID of the user who is being followed.|
| `created_at`    | `timestamp` | Default: `now()`                      | The date and time the follow occurred.    |

### `profile_likes`

Tracks likes on user profiles.

| Column     | Type      | Constraints                           | Description                          |
| ---------- | --------- | ------------------------------------- | ------------------------------------ |
| `user_id`  | `uuid`    | Primary Key, Foreign Key to `users.uid` | The UID of the user whose profile was liked. |
| `liker_id` | `uuid`    | Primary Key, Foreign Key to `users.uid` | The UID of the user who liked the profile. |
| `created_at` | `timestamp` | Default: `now()`                      | The date and time the like occurred. |

### `posts_likes`

Tracks likes on individual posts.

| Column     | Type      | Constraints                         | Description                        |
| ---------- | --------- | ----------------------------------- | ---------------------------------- |
| `post_id`  | `uuid`    | Primary Key, Foreign Key to `posts.key` | The ID of the post that was liked. |
| `user_id`  | `uuid`    | Primary Key, Foreign Key to `users.uid` | The UID of the user who liked the post. |
| `created_at` | `timestamp` | Default: `now()`                    | The date and time the like occurred. |

### `favorite_posts`

Tracks posts that users have marked as favorites.

| Column     | Type      | Constraints                         | Description                               |
| ---------- | --------- | ----------------------------------- | ----------------------------------------- |
| `post_id`  | `uuid`    | Primary Key, Foreign Key to `posts.key` | The ID of the post that was favorited.    |
| `user_id`  | `uuid`    | Primary Key, Foreign Key to `users.uid` | The UID of the user who favorited the post. |
| `created_at` | `timestamp` | Default: `now()`                    | The date and time the favorite occurred.  |

### `groups`

Stores information about chat groups.

| Column | Type      | Constraints                     | Description                          |
| ------ | --------- | ------------------------------- | ------------------------------------ |
| `uid`  | `uuid`    | Primary Key, Default: `uuid_generate_v4()` | The unique identifier for the group. |
| `name` | `text`    |                                 | The name of the group.               |
| `icon` | `text`    |                                 | URL for the group's icon.            |

### `group_members`

A many-to-many relationship table for tracking group members.

| Column     | Type   | Constraints                       | Description                       |
| ---------- | ------ | --------------------------------- | --------------------------------- |
| `group_id` | `uuid` | Primary Key, Foreign Key to `groups.uid` | The ID of the group.              |
| `user_id`  | `uuid` | Primary Key, Foreign Key to `users.uid`  | The ID of the user who is a member. |

### `group-chats`

Stores messages sent in groups.

| Column        | Type      | Constraints                     | Description                             |
| ------------- | --------- | ------------------------------- | --------------------------------------- |
| `key`         | `uuid`    | Primary Key, Default: `uuid_generate_v4()` | The unique identifier for the message.  |
| `group_id`    | `uuid`    | Foreign Key to `groups.uid`     | The ID of the group the message is in.  |
| `uid`         | `uuid`    | Foreign Key to `users.uid`      | The UID of the user who sent the message. |
| `message_text`| `text`    |                                 | The content of the message.             |
| `push_date`   | `timestamp` | Default: `now()`                | The date and time the message was sent. |
| `message_state`| `text`    |                                 | The state of the message (e.g., 'sended').|

### `cover-image-history`

Stores a history of users' cover photos.

| Column      | Type      | Constraints                     | Description                               |
| ----------- | --------- | ------------------------------- | ----------------------------------------- |
| `key`       | `uuid`    | Primary Key, Default: `uuid_generate_v4()` | The unique identifier for the history entry.|
| `uid`       | `uuid`    | Foreign Key to `users.uid`      | The UID of the user.                      |
| `image_url` | `text`    |                                 | The URL of the cover image.               |
| `upload_date`| `timestamp` | Default: `now()`                | The date and time the image was uploaded. |
| `type`      | `text`    |                                 | The type of image (e.g., 'url').          |
