# Requirements Document

## Introduction

This feature adds real-time typing indicators and read receipts to the Synapse chat system. Typing indicators show when a user is actively composing a message, while read receipts track and display when messages have been delivered and read by recipients. These features enhance user engagement and provide transparency in conversations.

## Glossary

- **Chat System**: The Synapse real-time messaging module that handles direct messages between users
- **Typing Indicator**: A visual UI element that displays when a remote user is actively typing a message
- **Read Receipt**: A status indicator showing whether a message has been delivered to and read by the recipient
- **Supabase Realtime**: The WebSocket-based real-time communication service used for live updates
- **Message State**: The current status of a message (sending, sent, delivered, read, failed)
- **Typing Event**: A real-time broadcast indicating a user is typing in a specific chat
- **Read Event**: A real-time broadcast indicating a user has read messages in a specific chat
- **Debounce**: A technique to limit the frequency of event emissions during rapid user input

## Requirements

### Requirement 1

**User Story:** As a chat user, I want to see when the other person is typing, so that I know they are actively engaged in the conversation

#### Acceptance Criteria

1. WHEN a user types in the message input field, THE Chat System SHALL broadcast a typing event to the chat room within 500 milliseconds
2. WHEN a typing event is received from a remote user, THE Chat System SHALL display a typing indicator below the last message within 200 milliseconds
3. WHILE a user continues typing, THE Chat System SHALL send typing events at intervals not exceeding 3 seconds
4. WHEN a user stops typing for 3 seconds, THE Chat System SHALL broadcast a typing-stopped event
5. WHEN a user sends a message, THE Chat System SHALL immediately broadcast a typing-stopped event and hide the typing indicator

### Requirement 2

**User Story:** As a chat user, I want the typing indicator to be visually clear and non-intrusive, so that it enhances rather than distracts from the conversation

#### Acceptance Criteria

1. THE Chat System SHALL display the typing indicator as an animated three-dot pattern with a fade-in animation lasting 200 milliseconds
2. THE Chat System SHALL position the typing indicator at the bottom of the message list with 8dp vertical spacing
3. WHEN multiple users are typing in a group chat, THE Chat System SHALL display "User1, User2 are typing..." with a maximum of 2 names shown
4. THE Chat System SHALL automatically hide the typing indicator after 5 seconds if no typing-stopped event is received
5. THE Chat System SHALL use the Material Design color scheme with primary color for the typing animation

### Requirement 3

**User Story:** As a message sender, I want to know when my messages have been delivered and read, so that I can confirm the recipient has seen my message

#### Acceptance Criteria

1. WHEN a message is successfully sent to Supabase, THE Chat System SHALL update the message state to "sent" and display a single checkmark icon
2. WHEN a recipient's device receives a message, THE Chat System SHALL update the message state to "delivered" and display a double checkmark icon
3. WHEN a recipient views a message in the chat screen, THE Chat System SHALL update the message state to "read" and display a blue double checkmark icon
4. THE Chat System SHALL display read receipt icons with 12dp size aligned to the right of the message timestamp
5. WHEN a message fails to send, THE Chat System SHALL display an error icon and provide a retry option

### Requirement 4

**User Story:** As a chat user, I want read receipts to update in real-time, so that I see accurate message status without refreshing

#### Acceptance Criteria

1. WHEN a user opens a chat screen, THE Chat System SHALL mark all unread messages as read within 1 second
2. WHEN messages are marked as read, THE Chat System SHALL broadcast a read event to the Supabase Realtime channel
3. WHEN a read event is received, THE Chat System SHALL update all affected message states to "read" within 500 milliseconds
4. THE Chat System SHALL batch read receipts for multiple messages into a single database update operation
5. WHEN the app is in the background, THE Chat System SHALL defer read receipt updates until the chat screen is visible

### Requirement 5

**User Story:** As a privacy-conscious user, I want the option to disable read receipts, so that I can read messages without notifying the sender

#### Acceptance Criteria

1. THE Chat System SHALL provide a settings toggle labeled "Send Read Receipts" with a default value of enabled
2. WHEN read receipts are disabled, THE Chat System SHALL not broadcast read events to other users
3. WHEN read receipts are disabled, THE Chat System SHALL still receive and display read receipts from other users
4. THE Chat System SHALL persist the read receipt preference in local storage
5. WHEN the read receipt setting changes, THE Chat System SHALL apply the new setting to all future messages immediately

### Requirement 6

**User Story:** As a developer, I want typing indicators and read receipts to be efficient and scalable, so that they don't impact app performance or backend costs

#### Acceptance Criteria

1. THE Chat System SHALL debounce typing events to send at most one event per 500 milliseconds during active typing
2. THE Chat System SHALL use Supabase Realtime presence or broadcast features rather than database polling
3. THE Chat System SHALL limit typing indicator subscriptions to active chat rooms only
4. THE Chat System SHALL unsubscribe from typing events when a chat screen is closed or backgrounded
5. THE Chat System SHALL batch read receipt database updates to execute at most one update per second per chat
