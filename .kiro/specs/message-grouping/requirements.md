# Requirements Document

## Introduction

This document specifies the requirements for implementing message grouping in the chat interface. Message grouping is a visual enhancement pattern commonly found in modern messaging applications where consecutive messages from the same sender are visually grouped together. This improves readability, reduces visual clutter, and provides a more polished user experience by adjusting corner radii of message bubbles and consolidating timestamps for messages sent in quick succession.

## Glossary

- **Chat System**: The messaging interface component of the Synapse application that displays conversations between users
- **Message Bubble**: The visual container (LinearLayout with background drawable) that wraps message content
- **Message Group**: A sequence of two or more consecutive messages from the same sender
- **Corner Radius**: The rounded corner styling applied to message bubble backgrounds
- **Timestamp**: The time display (TextView with id "date") showing when a message was sent
- **Sender ID**: The unique identifier (sender_id or uid field) of the user who sent a message
- **Time Threshold**: The maximum time interval (in milliseconds) between messages to be considered part of the same group
- **ChatAdapter**: The RecyclerView adapter class responsible for rendering chat messages
- **Message Position**: The location of a message within a group (first, middle, last, or single)

## Requirements

### Requirement 1

**User Story:** As a user viewing a conversation, I want consecutive messages from the same sender to be visually grouped together, so that I can easily distinguish between different conversation turns and the interface feels less cluttered.

#### Acceptance Criteria

1. WHEN the Chat System displays two or more consecutive messages with the same sender_id, THE Chat System SHALL apply grouped message styling to those messages
2. WHEN the Chat System displays a message that is not part of a group, THE Chat System SHALL apply single message styling with all corners rounded
3. WHEN the sender_id changes between consecutive messages, THE Chat System SHALL terminate the current message group and start a new group
4. WHERE a message is deleted or marked as deleted, THE Chat System SHALL exclude that message from grouping calculations
5. WHEN the Chat System calculates message grouping, THE Chat System SHALL process messages in chronological order based on their position in the data list

### Requirement 2

**User Story:** As a user viewing grouped messages, I want the message bubbles to have appropriate corner radii based on their position in the group, so that the visual grouping is clear and aesthetically pleasing.

#### Acceptance Criteria

1. WHEN a message is the first message in a group, THE Chat System SHALL apply rounded corners to the top two corners and square corners to the bottom two corners of the Message Bubble
2. WHEN a message is in the middle of a group, THE Chat System SHALL apply square corners to all four corners of the Message Bubble
3. WHEN a message is the last message in a group, THE Chat System SHALL apply square corners to the top two corners and rounded corners to the bottom two corners of the Message Bubble
4. WHEN a message is a single message (not part of a group), THE Chat System SHALL apply rounded corners to all four corners of the Message Bubble
5. WHERE a message is sent by the current user, THE Chat System SHALL apply outgoing message corner radius styling
6. WHERE a message is received from another user, THE Chat System SHALL apply incoming message corner radius styling

### Requirement 3

**User Story:** As a user viewing grouped messages, I want timestamps to be shown only on the last message of each group when messages are sent within a short time period, so that the interface is cleaner and less repetitive.

#### Acceptance Criteria

1. WHEN messages in a group are sent within 60 seconds of each other, THE Chat System SHALL hide the Timestamp on all messages except the last message in the group
2. WHEN the time difference between consecutive messages in a group exceeds 60 seconds, THE Chat System SHALL display the Timestamp on both messages
3. WHEN a message is a single message (not part of a group), THE Chat System SHALL always display the Timestamp
4. WHEN the Chat System displays a Timestamp, THE Chat System SHALL use the existing formatMessageTime function to format the time value
5. WHERE a message is the last message in a group, THE Chat System SHALL always display the Timestamp regardless of time difference

### Requirement 4

**User Story:** As a user viewing a conversation with multiple message types, I want all message types (text, media, video, voice, link preview) to support message grouping, so that the grouping behavior is consistent across the entire conversation.

#### Acceptance Criteria

1. WHEN the Chat System renders text messages (VIEW_TYPE_TEXT), THE Chat System SHALL apply message grouping logic
2. WHEN the Chat System renders media messages (VIEW_TYPE_MEDIA_GRID), THE Chat System SHALL apply message grouping logic
3. WHEN the Chat System renders video messages (VIEW_TYPE_VIDEO), THE Chat System SHALL apply message grouping logic
4. WHEN the Chat System renders voice messages (VIEW_TYPE_VOICE_MESSAGE), THE Chat System SHALL apply message grouping logic
5. WHEN the Chat System renders link preview messages (VIEW_TYPE_LINK_PREVIEW), THE Chat System SHALL apply message grouping logic
6. WHERE a message has VIEW_TYPE_TYPING, VIEW_TYPE_ERROR, or VIEW_TYPE_LOADING_MORE, THE Chat System SHALL exclude that message from grouping calculations

### Requirement 5

**User Story:** As a user in a group chat, I want message grouping to work correctly with sender usernames, so that I can see who sent each group of messages without redundant username displays.

#### Acceptance Criteria

1. WHEN the Chat System displays the first message in a group in a group chat, THE Chat System SHALL display the sender username (senderUsername TextView)
2. WHEN the Chat System displays middle or last messages in a group in a group chat, THE Chat System SHALL hide the sender username
3. WHEN the Chat System displays a single message in a group chat, THE Chat System SHALL display the sender username
4. WHERE the message is from the current user, THE Chat System SHALL hide the sender username regardless of grouping
5. WHERE the conversation is not a group chat, THE Chat System SHALL hide the sender username for all messages

### Requirement 6

**User Story:** As a developer maintaining the chat system, I want the message grouping logic to be implemented efficiently, so that scrolling performance remains smooth even with large conversation histories.

#### Acceptance Criteria

1. WHEN the Chat System calculates message grouping, THE Chat System SHALL determine grouping position during the onBindViewHolder method
2. WHEN the Chat System applies corner radius styling, THE Chat System SHALL use existing drawable resources with appropriate background resource assignment
3. WHEN the Chat System updates message grouping, THE Chat System SHALL not create new drawable resources at runtime
4. WHEN the Chat System processes a message for grouping, THE Chat System SHALL complete the grouping calculation in less than 5 milliseconds per message
5. WHERE the Chat System needs to access previous or next message data, THE Chat System SHALL access the data ArrayList directly by index position
