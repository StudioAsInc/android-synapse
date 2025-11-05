# Requirements Document

## Introduction

This feature adds comprehensive media attachment support to the Synapse chat system, enabling users to send and receive images, videos, audio files, and documents. The implementation includes media upload to Supabase Storage, thumbnail generation, progress tracking, and an optimized viewing experience with image galleries and video playback.

## Glossary

- **Chat System**: The Synapse real-time messaging module that handles direct messages between users
- **Media Attachment**: A file (image, video, audio, or document) attached to a chat message
- **Supabase Storage**: The S3-compatible object storage service for media files
- **Thumbnail**: A small preview image generated for media attachments
- **Upload Progress**: Real-time feedback showing the percentage of file upload completion
- **Media Gallery**: A full-screen viewer for browsing images and videos
- **Attachment Message**: A chat message containing one or more media attachments
- **Storage Bucket**: A container in Supabase Storage for organizing uploaded files
- **Compression**: Reducing file size while maintaining acceptable quality

## Requirements

### Requirement 1

**User Story:** As a chat user, I want to attach images to my messages, so that I can share visual content with my contacts

#### Acceptance Criteria

1. WHEN a user taps the attachment button, THE Chat System SHALL display an image picker within 500 milliseconds
2. WHEN a user selects an image, THE Chat System SHALL compress the image to a maximum of 1920x1080 resolution while maintaining aspect ratio
3. WHEN an image is compressed, THE Chat System SHALL reduce file size to a maximum of 2MB without visible quality loss
4. THE Chat System SHALL generate a thumbnail with 200x200 pixel dimensions for each image
5. WHEN an image upload starts, THE Chat System SHALL display a progress indicator showing percentage completion

### Requirement 2

**User Story:** As a chat user, I want to send multiple images at once, so that I can share photo albums or related pictures efficiently

#### Acceptance Criteria

1. THE Chat System SHALL allow selection of up to 10 images in a single message
2. WHEN multiple images are selected, THE Chat System SHALL display a preview grid with 2 columns
3. WHEN uploading multiple images, THE Chat System SHALL upload files concurrently with a maximum of 3 simultaneous uploads
4. THE Chat System SHALL display individual progress indicators for each image in the upload queue
5. WHEN any image fails to upload, THE Chat System SHALL allow retry for that specific image without affecting others

### Requirement 3

**User Story:** As a chat user, I want to view images in full screen, so that I can see details clearly

#### Acceptance Criteria

1. WHEN a user taps an image attachment, THE Chat System SHALL open a full-screen image viewer within 300 milliseconds
2. THE Chat System SHALL support pinch-to-zoom gestures with a maximum zoom level of 3x
3. THE Chat System SHALL support swipe gestures to navigate between multiple images in a message
4. THE Chat System SHALL display image metadata including file name and size in the viewer
5. THE Chat System SHALL provide a download button to save images to device storage

### Requirement 4

**User Story:** As a chat user, I want to send video files, so that I can share recorded moments with my contacts

#### Acceptance Criteria

1. THE Chat System SHALL support video uploads with formats: MP4, MOV, AVI, MKV
2. THE Chat System SHALL limit video file size to a maximum of 100MB per file
3. WHEN a video is selected, THE Chat System SHALL generate a thumbnail from the first frame
4. THE Chat System SHALL display video duration on the thumbnail overlay
5. WHEN a video upload exceeds 50MB, THE Chat System SHALL show a warning with estimated upload time

### Requirement 5

**User Story:** As a chat user, I want to play videos inline, so that I can watch content without leaving the chat

#### Acceptance Criteria

1. WHEN a user taps a video attachment, THE Chat System SHALL start playback within 1 second
2. THE Chat System SHALL display standard video controls including play/pause, seek bar, and volume
3. THE Chat System SHALL support full-screen video playback
4. WHEN a video is playing, THE Chat System SHALL pause other media in the chat
5. THE Chat System SHALL resume video from the last position if playback was interrupted

### Requirement 6

**User Story:** As a chat user, I want to send audio files and voice messages, so that I can share sound recordings

#### Acceptance Criteria

1. THE Chat System SHALL support audio uploads with formats: MP3, WAV, M4A, OGG
2. THE Chat System SHALL limit audio file size to a maximum of 20MB per file
3. WHEN an audio file is attached, THE Chat System SHALL display audio duration and file name
4. THE Chat System SHALL provide inline audio playback with play/pause and seek controls
5. THE Chat System SHALL display a waveform visualization for audio files under 5 minutes

### Requirement 7

**User Story:** As a chat user, I want to send document files, so that I can share PDFs, text files, and other documents

#### Acceptance Criteria

1. THE Chat System SHALL support document uploads with formats: PDF, DOC, DOCX, TXT, XLS, XLSX, PPT, PPTX
2. THE Chat System SHALL limit document file size to a maximum of 50MB per file
3. WHEN a document is attached, THE Chat System SHALL display file name, type, and size
4. THE Chat System SHALL provide a download button for documents
5. WHEN a user taps a document, THE Chat System SHALL open it with the appropriate system viewer

### Requirement 8

**User Story:** As a chat user, I want to see upload progress, so that I know how long the upload will take

#### Acceptance Criteria

1. WHEN a file upload starts, THE Chat System SHALL display a progress bar showing percentage completion
2. THE Chat System SHALL update progress at intervals not exceeding 500 milliseconds
3. THE Chat System SHALL display estimated time remaining for uploads exceeding 5 seconds
4. WHEN an upload completes, THE Chat System SHALL show a success indicator for 1 second before hiding the progress UI
5. WHEN an upload fails, THE Chat System SHALL display an error message with a retry button

### Requirement 9

**User Story:** As a chat user, I want attachments to load quickly, so that I can view content without delays

#### Acceptance Criteria

1. THE Chat System SHALL load thumbnails within 500 milliseconds of message display
2. THE Chat System SHALL cache downloaded media files for 7 days
3. THE Chat System SHALL use progressive loading for images, showing low-resolution first then high-resolution
4. THE Chat System SHALL preload the next 3 images when viewing a gallery
5. THE Chat System SHALL limit concurrent media downloads to 5 files to prevent network congestion

### Requirement 10

**User Story:** As a developer, I want media uploads to be secure and efficient, so that user data is protected and storage costs are minimized

#### Acceptance Criteria

1. THE Chat System SHALL upload files to Supabase Storage with authenticated user credentials
2. THE Chat System SHALL generate unique file names using UUID to prevent collisions
3. THE Chat System SHALL organize files in storage buckets by chat_id and date (YYYY/MM/DD structure)
4. THE Chat System SHALL apply Row Level Security policies to ensure users can only access media from their chats
5. THE Chat System SHALL delete media files from storage when messages are deleted by all participants

### Requirement 11

**User Story:** As a chat user, I want to preview attachments before sending, so that I can verify the correct files are selected

#### Acceptance Criteria

1. WHEN a user selects media files, THE Chat System SHALL display a preview screen before sending
2. THE Chat System SHALL allow users to add or remove files from the preview screen
3. THE Chat System SHALL allow users to add a text caption to accompany media attachments
4. THE Chat System SHALL display total file size and estimated upload time on the preview screen
5. WHEN a user confirms, THE Chat System SHALL send the message with all attachments

### Requirement 12

**User Story:** As a chat user, I want to receive notifications about attachment uploads, so that I can continue using the app while files upload

#### Acceptance Criteria

1. WHEN a large file upload is in progress, THE Chat System SHALL display a persistent notification showing progress
2. THE Chat System SHALL allow users to navigate away from the chat while uploads continue in the background
3. WHEN an upload completes, THE Chat System SHALL update the notification with a success message
4. WHEN an upload fails, THE Chat System SHALL display a notification with a retry action
5. THE Chat System SHALL cancel ongoing uploads when the user explicitly cancels the message
