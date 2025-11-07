# Requirements Document

## Introduction

This document outlines the requirements for fixing message grouping and spacing issues in the chat interface. The current implementation has excessive spacing between grouped messages and fails to maintain proper grouping when new messages arrive via realtime synchronization. Additionally, there is a noticeable delay when sending messages.

## Glossary

- **Chat System**: The messaging interface that displays conversations between users
- **Message Grouping**: Visual clustering of consecutive messages from the same sender with reduced spacing and adjusted corner radii
- **Realtime Sync**: Live message updates received through Supabase Realtime subscriptions
- **Optimistic UI**: Immediate UI updates before server confirmation
- **Message Bubble**: The visual container displaying message content
- **Corner Radius**: The rounded corners of message bubbles that change based on grouping position

## Requirements

### Requirement 1

**User Story:** As a user, I want messages from the same sender to appear visually grouped together, so that conversations are easier to follow

#### Acceptance Criteria

1. WHEN consecutive messages are from the same sender, THE Chat System SHALL reduce vertical spacing between them to 2dp
2. WHEN a message is the first in a group, THE Chat System SHALL apply rounded corners only to the top of the bubble
3. WHEN a message is in the middle of a group, THE Chat System SHALL apply square corners to both top and bottom
4. WHEN a message is the last in a group, THE Chat System SHALL apply rounded corners only to the bottom of the bubble
5. WHEN a message is not part of a group, THE Chat System SHALL apply rounded corners to all sides

### Requirement 2

**User Story:** As a user, I want message grouping to remain consistent when new messages arrive, so that the visual layout doesn't break during conversations

#### Acceptance Criteria

1. WHEN a new message arrives via Realtime Sync, THE Chat System SHALL recalculate grouping for affected messages
2. WHEN a new message is added to an existing group, THE Chat System SHALL update corner radii for the previous last message
3. WHEN the Chat System updates message grouping, THE Chat System SHALL notify only the affected message positions
4. WHEN messages are initially loaded, THE Chat System SHALL calculate correct grouping before display
5. WHEN a user sends a message, THE Chat System SHALL apply correct grouping to the new message immediately

### Requirement 3

**User Story:** As a user, I want my sent messages to appear instantly in the chat, so that the conversation feels responsive

#### Acceptance Criteria

1. WHEN a user sends a message, THE Chat System SHALL display the message in the UI within 100 milliseconds
2. WHEN a message is being sent, THE Chat System SHALL clear the input field immediately
3. WHEN a message send operation completes, THE Chat System SHALL update the message status without re-adding it
4. WHEN a message fails to send, THE Chat System SHALL display an error indicator on the existing message
5. WHEN the Chat System adds an optimistic message, THE Chat System SHALL include correct grouping information

### Requirement 4

**User Story:** As a user, I want appropriate spacing between message groups, so that I can distinguish between different parts of the conversation

#### Acceptance Criteria

1. WHEN messages are from different senders, THE Chat System SHALL apply 12dp vertical spacing between them
2. WHEN messages are from the same sender, THE Chat System SHALL apply 2dp vertical spacing between them
3. WHEN a message is a single standalone message, THE Chat System SHALL apply 12dp spacing before and after it
4. WHEN the Chat System calculates spacing, THE Chat System SHALL consider deleted messages as group breakers
5. WHEN the Chat System applies spacing, THE Chat System SHALL use dimension resources for consistency

### Requirement 5

**User Story:** As a user, I want message bubbles to wrap to their content width and scale appropriately on different screen sizes, so that short messages don't span the entire screen width like other modern chat apps

#### Acceptance Criteria

1. WHEN a message is short, THE Chat System SHALL size the bubble to wrap the content width
2. WHEN a message is long, THE Chat System SHALL limit the bubble width to 75% of the available screen width
3. WHEN the Chat System displays a message bubble, THE Chat System SHALL align sent messages to the right edge
4. WHEN the Chat System displays a message bubble, THE Chat System SHALL align received messages to the left edge
5. WHEN the Chat System sizes message bubbles, THE Chat System SHALL calculate maximum width dynamically based on screen width
6. WHEN the Chat System runs on tablets or large screens, THE Chat System SHALL maintain readable message width without spanning excessively
