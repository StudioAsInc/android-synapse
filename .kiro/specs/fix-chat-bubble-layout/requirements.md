# Requirements Document

## Introduction

This document outlines the requirements for fixing the chat bubble layout issue where message bubbles are appearing on the wrong side (not aligned based on sender) and spanning full width instead of wrapping content appropriately.

## Glossary

- **Chat Bubble**: The visual container displaying a message in the chat interface
- **Message Alignment**: The horizontal positioning of chat bubbles (left for received messages, right for sent messages)
- **Bubble Width**: The horizontal space occupied by a chat bubble, which should wrap to content with a maximum width constraint
- **ChatAdapter**: The RecyclerView adapter responsible for binding message data to chat bubble views
- **Message Layout**: The FrameLayout container that holds the message bubble and controls its alignment

## Requirements

### Requirement 1: Correct Message Alignment

**User Story:** As a user, I want my sent messages to appear on the right side and received messages on the left side, so that I can easily distinguish between my messages and others' messages.

#### Acceptance Criteria

1. WHEN the Chat System renders a message sent by the current user, THE Chat System SHALL position the message bubble aligned to the right edge of the screen
2. WHEN the Chat System renders a message received from another user, THE Chat System SHALL position the message bubble aligned to the left edge of the screen
3. WHEN the Chat System determines message alignment, THE Chat System SHALL compare the message sender_id with the current user's ID
4. WHEN the Chat System applies alignment to a message bubble, THE Chat System SHALL set the FrameLayout.LayoutParams gravity property to Gravity.END for sent messages
5. WHEN the Chat System applies alignment to a message bubble, THE Chat System SHALL set the FrameLayout.LayoutParams gravity property to Gravity.START for received messages

### Requirement 2: Proper Bubble Width Constraint

**User Story:** As a user, I want message bubbles to wrap their content width (up to a maximum), so that short messages don't unnecessarily span the full screen width.

#### Acceptance Criteria

1. WHEN the Chat System renders a message bubble, THE Chat System SHALL set the bubble container width to wrap_content
2. WHEN the Chat System renders a message bubble, THE Chat System SHALL enforce a maximum width constraint defined by the message_bubble_max_width dimension resource
3. WHEN the Chat System renders a message bubble with short text content, THE Chat System SHALL display the bubble with width matching the text content plus padding
4. WHEN the Chat System renders a message bubble with long text content exceeding maximum width, THE Chat System SHALL wrap the text within the maximum width constraint
5. WHEN the Chat System renders the inner LinearLayout within the message_layout FrameLayout, THE Chat System SHALL set its width to wrap_content

### Requirement 3: Consistent Layout Across Message Types

**User Story:** As a user, I want all message types (text, media, links, etc.) to have consistent alignment and width behavior, so that the chat interface looks uniform.

#### Acceptance Criteria

1. WHEN the Chat System renders any message type (text, media, video, link preview, voice, error), THE Chat System SHALL apply the same alignment logic based on sender
2. WHEN the Chat System renders any message type, THE Chat System SHALL apply the same width constraints
3. WHEN the Chat System binds common message properties, THE Chat System SHALL correctly identify the inner LinearLayout as the direct child of the message_layout FrameLayout
4. WHEN the Chat System applies layout parameters to the inner LinearLayout, THE Chat System SHALL cast the layout parameters to FrameLayout.LayoutParams
5. WHEN the Chat System updates layout parameters, THE Chat System SHALL call setLayoutParams to apply the changes
