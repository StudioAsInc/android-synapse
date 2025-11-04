# Requirements Document

## Introduction

This document specifies the requirements for implementing a comprehensive message actions system in the Synapse chat application. When a user long-presses a message bubble, a bottom sheet will appear with fully functional actions including Reply, Forward, Delete, Edit, and AI Summary. All features must be production-ready with complete backend integration, not placeholders or mocks.

## Glossary

- **Chat System**: The messaging interface component of the Synapse application
- **Message Bubble**: The visual container displaying individual chat messages
- **Bottom Sheet**: A Material Design component that slides up from the bottom of the screen to display contextual actions
- **Message Actions**: User-initiated operations performed on existing messages (reply, forward, delete, edit, summarize)
- **Long Press**: A touch gesture where the user presses and holds on a UI element for approximately 500ms
- **Reply Action**: Creating a new message that references and quotes a previous message
- **Forward Action**: Sending a copy of an existing message to one or more different conversations
- **Delete Action**: Removing a message from the conversation with options for local-only or server-side deletion
- **Edit Action**: Modifying the content of a previously sent message with edit history tracking
- **AI Summary Action**: Generating a concise summary of a message using Gemini AI API
- **Supabase Database**: The PostgreSQL database backend accessed via Supabase Postgrest
- **Gemini API**: Google's generative AI API for text summarization and analysis
- **Message Metadata**: Additional information about a message including edit history, deletion status, and reply references

## Requirements

### Requirement 1

**User Story:** As a user, I want to long-press on any message bubble to see available actions, so that I can quickly perform operations on messages without navigating through menus.

#### Acceptance Criteria

1. WHEN a user long-presses on a Message Bubble for 500ms or more, THE Chat System SHALL display a Bottom Sheet with available Message Actions
2. THE Chat System SHALL display the Bottom Sheet with a smooth slide-up animation within 200ms of gesture detection
3. THE Chat System SHALL include a haptic feedback vibration when the Bottom Sheet appears to confirm gesture recognition
4. WHEN the Bottom Sheet is displayed, THE Chat System SHALL dim the background content to focus attention on the action menu
5. WHEN a user taps outside the Bottom Sheet or presses the back button, THE Chat System SHALL dismiss the Bottom Sheet with a slide-down animation

### Requirement 2

**User Story:** As a user, I want to reply to specific messages, so that I can maintain context in conversations and respond to particular points.

#### Acceptance Criteria

1. WHEN a user selects the Reply Action from the Bottom Sheet, THE Chat System SHALL display the referenced message in a reply preview above the message input field
2. THE Chat System SHALL include the original message text and sender information in the reply preview with a maximum of 3 lines of text
3. WHEN a user sends a message with an active reply preview, THE Chat System SHALL store the replied message ID in the Supabase Database with the new message
4. THE Chat System SHALL display replied messages with a visual indicator showing the quoted message content within the message bubble
5. WHEN a user taps on a reply indicator in a message, THE Chat System SHALL scroll to and highlight the original referenced message
6. THE Chat System SHALL allow users to cancel the reply by tapping a close button in the reply preview

### Requirement 3

**User Story:** As a user, I want to forward messages to other conversations, so that I can share information across different chats without retyping.

#### Acceptance Criteria

1. WHEN a user selects the Forward Action from the Bottom Sheet, THE Chat System SHALL display a conversation selection dialog showing all available conversations
2. THE Chat System SHALL allow users to select one or multiple conversations as forward destinations
3. THE Chat System SHALL display a search field in the conversation selection dialog to filter conversations by name
4. WHEN a user confirms forward destinations, THE Chat System SHALL send a copy of the message to each selected conversation via the Supabase Database
5. THE Chat System SHALL preserve message content, media attachments, and formatting when forwarding messages
6. THE Chat System SHALL display a "Forwarded" label on forwarded messages to indicate their origin
7. THE Chat System SHALL show a success confirmation with the number of conversations the message was forwarded to

### Requirement 4

**User Story:** As a user, I want to delete messages I've sent, so that I can remove mistakes, outdated information, or unwanted content from conversations.

#### Acceptance Criteria

1. WHERE the message was sent by the current user, THE Chat System SHALL display the Delete Action in the Bottom Sheet
2. WHEN a user selects the Delete Action, THE Chat System SHALL display a confirmation dialog with two options: "Delete for me" and "Delete for everyone"
3. WHEN a user selects "Delete for me", THE Chat System SHALL mark the message as deleted locally without removing it from the Supabase Database
4. WHEN a user selects "Delete for everyone", THE Chat System SHALL update the message deletion status in the Supabase Database and notify all conversation participants
5. THE Chat System SHALL display deleted messages as "This message was deleted" placeholder text with italic styling
6. THE Chat System SHALL remove media attachments from deleted messages and prevent access to attachment URLs
7. WHEN a message deletion fails due to network issues, THE Chat System SHALL display an error message and allow retry

### Requirement 5

**User Story:** As a user, I want to edit messages I've sent, so that I can correct typos, clarify information, or update content without sending a new message.

#### Acceptance Criteria

1. WHERE the message was sent by the current user AND the message is a text message, THE Chat System SHALL display the Edit Action in the Bottom Sheet
2. WHEN a user selects the Edit Action, THE Chat System SHALL populate the message input field with the current message text and display an "Editing" indicator
3. WHEN a user submits an edited message, THE Chat System SHALL update the message content in the Supabase Database with the new text
4. THE Chat System SHALL append an "(edited)" label to edited messages with a timestamp of the last edit
5. THE Chat System SHALL store edit history in the Supabase Database including original content and edit timestamps
6. WHEN a user taps on the "(edited)" label, THE Chat System SHALL display a dialog showing the edit history with timestamps
7. THE Chat System SHALL allow users to cancel editing by tapping a cancel button in the editing indicator
8. THE Chat System SHALL prevent editing of messages older than 48 hours to maintain conversation integrity

### Requirement 6

**User Story:** As a user, I want to generate AI summaries of long messages, so that I can quickly understand the key points without reading the entire message.

#### Acceptance Criteria

1. WHERE a message contains more than 100 characters, THE Chat System SHALL display the AI Summary Action in the Bottom Sheet
2. WHEN a user selects the AI Summary Action, THE Chat System SHALL display a loading indicator while generating the summary
3. THE Chat System SHALL send the message text to the Gemini API with a summarization prompt requesting concise key points
4. WHEN the Gemini API returns a summary, THE Chat System SHALL display the summary in a dialog with the original message for comparison
5. THE Chat System SHALL cache generated summaries in local storage to avoid redundant API calls for the same message
6. WHEN the Gemini API request fails, THE Chat System SHALL display an error message "Unable to generate summary" and log the error details
7. THE Chat System SHALL include a character count and estimated reading time in the summary dialog
8. THE Chat System SHALL allow users to copy the summary text to clipboard via a copy button
9. THE Chat System SHALL respect API rate limits and display appropriate messages when limits are reached

### Requirement 7

**User Story:** As a user, I want the message actions to adapt based on message type and ownership, so that I only see relevant and permitted actions for each message.

#### Acceptance Criteria

1. WHERE a message was sent by another user, THE Chat System SHALL NOT display Edit or Delete for Everyone actions
2. WHERE a message is a media-only message without text, THE Chat System SHALL NOT display the Edit Action
3. WHERE a message is a system message or notification, THE Chat System SHALL NOT display any Message Actions
4. WHERE a message has already been deleted, THE Chat System SHALL NOT display the Bottom Sheet on long press
5. THE Chat System SHALL display all applicable actions in a consistent order: Reply, Forward, Edit, Delete, AI Summary
6. THE Chat System SHALL use appropriate icons and labels for each action to ensure clarity

### Requirement 8

**User Story:** As a developer, I want all message actions to integrate with the existing Supabase backend, so that data is properly synchronized across devices and users.

#### Acceptance Criteria

1. THE Chat System SHALL use Supabase Postgrest to perform all database operations for message actions
2. THE Chat System SHALL implement Row Level Security policies to ensure users can only edit or delete their own messages
3. THE Chat System SHALL use Supabase Realtime to broadcast message updates (edits, deletions) to all conversation participants
4. WHEN a message is edited or deleted, THE Chat System SHALL update the message in real-time for all users viewing the conversation
5. THE Chat System SHALL handle network failures gracefully with retry logic and offline queueing for pending actions
6. THE Chat System SHALL validate all message action requests on the server side to prevent unauthorized operations
