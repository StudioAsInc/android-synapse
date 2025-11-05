# Implementation Plan

- [x] 1. Set up Supabase Storage infrastructure





  - Create `chat-media` storage bucket in Supabase
  - Configure bucket to be private (not public)
  - Set up RLS policies for authenticated user access
  - Create folder structure template (chatId/YYYY/MM/DD)
  - Test bucket access with authenticated requests
  - _Requirements: 10.1, 10.3, 10.4_

- [x] 2. Implement SupabaseStorageService for file operations




  - [x] 2.1 Create SupabaseStorageService class


    - Implement `uploadFile()` with progress callbacks
    - Implement `downloadFile()` for retrieving media
    - Implement `deleteFile()` for cleanup
    - Implement `getPublicUrl()` for generating access URLs
    - Implement `generateStoragePath()` with date-based organization
    - _Requirements: 10.1, 10.2, 10.3_

  - [x] 2.2 Add error handling and retry logic

    - Handle network errors with exponential backoff
    - Handle storage quota errors
    - Handle authentication errors
    - Implement upload retry mechanism (max 3 attempts)
    - _Requirements: 8.5_

- [x] 3. Implement ImageCompressor for image optimization







  - [x] 3.1 Create ImageCompressor class




    - Implement `compress()` to resize to max 1920x1080
    - Maintain aspect ratio during compression
    - Target file size of 2MB maximum
    - Use quality 85% as baseline
    - Preserve EXIF orientation data
    - _Requirements: 1.2, 1.3_

  - [x] 3.2 Add iterative compression algorithm


    - Implement `compressIteratively()` to meet size targets
    - Calculate optimal quality based on file size ratio
    - Implement `calculateInSampleSize()` for efficient decoding
    - Handle out-of-memory errors gracefully
    - _Requirements: 1.3_

- [x] 4. Implement ThumbnailGenerator for preview images




  - [x] 4.1 Create ThumbnailGenerator class


    - Implement `generateImageThumbnail()` for 200x200 thumbnails
    - Implement `generateVideoThumbnail()` using MediaMetadataRetriever
    - Implement `extractVideoFrame()` at specific timestamps
    - Use center crop scaling for thumbnails
    - _Requirements: 1.4, 4.3_

  - [x] 4.2 Optimize thumbnail generation performance


    - Use BitmapFactory.Options for efficient decoding
    - Implement thumbnail caching to avoid regeneration
    - Handle corrupted media files gracefully
    - _Requirements: 9.1_

- [x] 5. Implement MediaCache for local file caching




  - [x] 5.1 Create MediaCache class with LRU eviction


    - Implement `put()` to store files in cache directory
    - Implement `get()` to retrieve cached files
    - Implement `remove()` for manual deletion
    - Implement `clear()` to wipe entire cache
    - Set max cache size to 500MB
    - _Requirements: 9.2_

  - [x] 5.2 Add cache expiration and cleanup


    - Implement `evictExpired()` to remove files older than 7 days
    - Implement `evictLRU()` when cache size exceeds limit
    - Schedule periodic cleanup (daily)
    - Track file access times for LRU
    - _Requirements: 9.2_

- [x] 6. Implement MediaUploadManager for upload orchestration





  - [x] 6.1 Create MediaUploadManager class


    - Implement `uploadImage()` with compression and thumbnail generation
    - Implement `uploadVideo()` with thumbnail generation
    - Implement `uploadAudio()` with metadata extraction
    - Implement `uploadDocument()` with validation
    - Generate unique UUIDs for file names
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 4.1, 4.2, 4.3, 6.1, 7.1, 7.2, 10.2_

  - [x] 6.2 Add concurrent upload management


    - Implement upload queue with Channel
    - Limit concurrent uploads to 3 maximum
    - Implement `uploadMultiple()` returning Flow<UploadProgress>
    - Track individual upload progress for each file
    - _Requirements: 2.1, 2.3, 2.4_

  - [x] 6.3 Add upload cancellation support


    - Implement `cancelUpload()` to stop ongoing uploads
    - Clean up partial uploads from storage
    - Update UI state to reflect cancellation
    - _Requirements: 2.5, 12.5_

- [x] 7. Implement MediaDownloadManager for download handling








  - [x] 7.1 Create MediaDownloadManager class



    - Implement `downloadMedia()` with caching
    - Implement `downloadThumbnail()` for preview images
    - Implement `getCachedMedia()` to check cache first
    - Limit concurrent downloads to 5 maximum
    - _Requirements: 9.1, 9.2, 9.5_

  - [x] 7.2 Add preloading for galleries


    - Implement `preloadMedia()` for adjacent images
    - Preload next 3 images when viewing gallery
    - Use background coroutines for preloading
    - Cancel preloading when user navigates away
    - _Requirements: 9.4_

- [x] 8. Implement MediaValidator for file validation





  - Create MediaValidator class
  - Define allowed MIME types for each media category
  - Implement `validateFile()` to check type and size
  - Validate image files (max 2MB after compression)
  - Validate video files (max 100MB)
  - Validate audio files (max 20MB)
  - Validate document files (max 50MB)
  - Return descriptive error messages for validation failures
  - _Requirements: 1.3, 4.2, 4.5, 6.2, 7.2_

- [x] 9. Update data models for media attachments




  - [x] 9.1 Enhance ChatAttachment interface


    - Add `width: Int?` field for images/videos
    - Add `height: Int?` field for images/videos
    - Add `duration: Long?` field for videos/audio (milliseconds)
    - Add `mimeType: String?` field
    - Update ChatAttachmentImpl data class
    - _Requirements: 4.4, 6.3_

  - [x] 9.2 Create UploadProgress data class

    - Define fields: uploadId, fileName, progress, bytesUploaded, totalBytes, state, error
    - Create UploadState enum: QUEUED, COMPRESSING, UPLOADING, COMPLETED, FAILED, CANCELLED
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [x] 9.3 Create MediaUploadResult data class

    - Define fields: url, thumbnailUrl, fileName, fileSize, mimeType, width, height, duration
    - _Requirements: 1.4, 4.3_

  - [x] 9.4 Create MediaMetadata data class

    - Define fields: fileName, fileSize, mimeType, width, height, duration, createdAt
    - _Requirements: 3.4_
-

- [x] 10. Update SupabaseChatService for attachment messages





  - [x] 10.1 Enhance sendMessage() for attachments

    - Accept `attachments: List<ChatAttachment>?` parameter
    - Serialize attachments as JSONB array
    - Set message_type to MessageType.ATTACHMENT when attachments present
    - Store attachment metadata in database
    - _Requirements: 1.5, 2.1_


  - [x] 10.2 Add deleteMessageWithMedia() method

    - Delete message from database
    - Delete associated media files from storage
    - Delete thumbnails from storage
    - Handle cases where files already deleted
    - _Requirements: 10.5_

- [x] 11. Create MediaPickerBottomSheet for file selection










  - [x] 11.1 Create MediaPickerBottomSheet UI


    - Create bottom sheet layout with media type options
    - Add "Photos" option with gallery icon
    - Add "Videos" option with video icon
    - Add "Audio" option with audio icon
    - Add "Documents" option with document icon
    - Apply Material Design styling
    - _Requirements: 1.1_

  - [x] 11.2 Implement media picker launchers


    - Implement `showImagePicker()` with multi-select (max 10)
    - Implement `showVideoPicker()` with single select
    - Implement `showAudioPicker()` with single select
    - Implement `showDocumentPicker()` with single select
    - Handle permission requests for storage access
    - _Requirements: 1.1, 2.1, 4.1, 6.1, 7.1_


  - [x] 11.3 Add MediaPickerListener interface

    - Define `onImagesSelected(uris: List<Uri>)` callback
    - Define `onVideoSelected(uri: Uri)` callback
    - Define `onAudioSelected(uri: Uri)` callback
    - Define `onDocumentSelected(uri: Uri)` callback
    - _Requirements: 1.1_

- [x] 12. Create MediaPreviewScreen for pre-send preview










  - [x] 12.1 Create MediaPreviewScreen layout


    - Create RecyclerView with GridLayoutManager (2 columns)
    - Add caption EditText at bottom
    - Add send button (FAB)
    - Add file size and upload time estimate display
    - Show individual thumbnails for each selected file
    - _Requirements: 2.2, 11.1, 11.3, 11.4_



  - [x] 12.2 Implement preview functionality
    - Implement `setupMediaGrid()` to display selected files
    - Implement `setupCaptionInput()` for text caption
    - Implement `calculateTotalSize()` for all files
    - Implement `estimateUploadTime()` based on file sizes
    - Allow removing individual files from selection


    - Allow adding more files (up to limit)
    - _Requirements: 11.2, 11.3, 11.4_

  - [x] 12.3 Implement send action
    - Validate all files before sending
    - Show warning for large files (>50MB)
    - Trigger upload via ViewModel
    - Navigate back to chat on send
    - _Requirements: 4.5, 11.5_
-


- [x] 13. Create ImageGalleryActivity for full-screen viewing







  - [x] 13.1 Create ImageGalleryActivity layout

    - Create ViewPager2 for image swiping
    - Add PhotoView for pinch-to-zoom (max 3x)
    - Add toolbar with image metadata (name, size)
    - Add download button in toolbar
    - Add close button
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [x] 13.2 Implement gallery functionality


    - Implement `setupViewPager()` with image adapter
    - Implement `setupZoomGestures()` using PhotoView library
    - Implement `preloadAdjacentImages()` for smooth swiping
    - Implement `downloadImage()` to save to device
    - Handle swipe gestures for navigation
    - _Requirements: 3.1, 3.2, 3.3, 3.5_

  - [x] 13.3 Add progressive image loading

    - Load low-resolution thumbnail first
    - Load high-resolution image progressively
    - Show loading indicator during download
    - Handle download failures gracefully
    - _Requirements: 9.3_


- [x] 14. Create VideoPlayerView for inline playback



  - [x] 14.1 Create VideoPlayerView custom widget


    - Extend FrameLayout with ExoPlayer integration
    - Add PlayerView for video rendering
    - Implement `setVideoUrl()` to load video
    - Implement `play()`, `pause()`, `seekTo()` controls
    - Implement `release()` for cleanup
    - _Requirements: 5.1, 5.2_

  - [x] 14.2 Add video controls and features


    - Display standard controls (play/pause, seek bar, volume)
    - Add full-screen toggle button
    - Show video duration on thumbnail
    - Pause other media when video plays
    - Resume from last position on return
    - _Requirements: 5.2, 5.3, 5.4, 5.5_

  - [x] 14.3 Optimize video playback


    - Use adaptive streaming for large videos
    - Cache video segments for offline playback
    - Handle playback errors with retry
    - Show buffering indicator
    - _Requirements: 5.1_

- [x] 15. Create AudioPlayerView for audio playback






  - [x] 15.1 Create AudioPlayerView custom widget

    - Create layout with play/pause button, seek bar, duration
    - Integrate ExoPlayer for audio playback
    - Implement `setAudioUrl()` to load audio
    - Display audio file name and duration
    - _Requirements: 6.3, 6.4_



  - [ ] 15.2 Add waveform visualization
    - Generate waveform data for audio files <5 minutes
    - Display waveform visualization during playback
    - Highlight current playback position
    - Allow seeking by tapping waveform
    - _Requirements: 6.5_

- [x] 16. Update chat message layouts for attachments





  - [x] 16.1 Create attachment bubble layouts


    - Create `chat_bubble_image.xml` for image attachments
    - Create `chat_bubble_video.xml` for video attachments
    - Create `chat_bubble_audio.xml` for audio attachments
    - Create `chat_bubble_document.xml` for document attachments
    - Support multiple attachments in grid layout (2 columns)
    - _Requirements: 2.2_

  - [x] 16.2 Add attachment click handlers


    - Open ImageGalleryActivity on image tap
    - Start video playback on video tap
    - Start audio playback on audio tap
    - Open document with system viewer on document tap
    - _Requirements: 3.1, 5.1, 6.4, 7.5_

- [x] 17. Update ChatViewModel for media operations





  - [x] 17.1 Add upload state management


    - Add `_uploadProgress` MutableStateFlow<Map<String, UploadProgress>>
    - Expose `uploadProgress` StateFlow for UI observation
    - Implement `uploadImages()` for multiple images
    - Implement `uploadVideo()` for single video
    - Implement `uploadAudio()` for audio files
    - Implement `uploadDocument()` for documents
    - _Requirements: 1.5, 2.3, 2.4, 8.1, 8.2_

  - [x] 17.2 Integrate MediaUploadManager

    - Initialize MediaUploadManager in ViewModel
    - Collect upload progress Flow and update state
    - Handle upload completion and create messages
    - Handle upload failures and show errors
    - Implement `cancelUpload()` for user cancellation
    - _Requirements: 8.3, 8.4, 8.5_

  - [x] 17.3 Add attachment message creation

    - Implement `sendMessageWithAttachment()` helper
    - Create ChatAttachment objects from upload results
    - Set message_type to ATTACHMENT_MESSAGE
    - Include optional caption text
    - Update message list with new attachment message
    - _Requirements: 11.5_

- [x] 18. Update chat adapter for attachment rendering







  - [x] 18.1 Create attachment ViewHolders



    - Create ImageAttachmentViewHolder
    - Create VideoAttachmentViewHolder
    - Create AudioAttachmentViewHolder
    - Create DocumentAttachmentViewHolder
    - Handle multiple attachments in grid layout
    - _Requirements: 2.2_

  - [x] 18.2 Implement attachment loading


    - Use Glide to load image thumbnails
    - Load video thumbnails with duration overlay
    - Display audio file info with waveform
    - Display document icon with file info
    - Show loading placeholders during download
    - _Requirements: 9.1_

  - [x] 18.3 Add upload progress UI


    - Show progress bar overlay during upload
    - Display upload percentage text
    - Show estimated time remaining for large files
    - Show success checkmark on completion
    - Show error icon with retry button on failure
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 19. Implement background upload service

  - [ ] 19.1 Create MediaUploadWorker using WorkManager
    - Create Worker class for background uploads
    - Accept upload parameters (URIs, chatId, caption)
    - Perform uploads in background thread
    - Update notification with progress
    - _Requirements: 12.1, 12.2_

  - [ ] 19.2 Add upload notifications
    - Show persistent notification during upload
    - Update notification progress bar
    - Show success notification on completion
    - Show error notification with retry action on failure
    - Allow cancellation from notification
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

- [ ] 20. Add storage management and cleanup


  - [ ] 20.1 Implement orphaned file cleanup
    - Create scheduled job to find orphaned files
    - Delete files not referenced in any message
    - Delete files from deleted messages
    - Run cleanup weekly
    - _Requirements: 10.5_

  - [ ] 20.2 Add storage usage tracking
    - Calculate total storage used per user
    - Display storage usage in settings
    - Warn users approaching storage limits
    - Provide option to clear cached media
    - _Requirements: 10.3_

- [ ] 21. Add permissions handling


  - Create PermissionsManager class
  - Request READ_EXTERNAL_STORAGE permission for media access
  - Request WRITE_EXTERNAL_STORAGE for downloads
  - Request CAMERA permission for camera capture (future)
  - Handle permission denial gracefully
  - Show rationale dialogs for denied permissions
  - _Requirements: 1.1_

- [ ] 22. Implement error handling and user feedback


  - [ ] 22.1 Add error dialogs for upload failures
    - Show dialog for file too large errors
    - Show dialog for unsupported format errors
    - Show dialog for network errors
    - Show dialog for storage quota errors
    - Provide retry and cancel options
    - _Requirements: 8.5_

  - [ ] 22.2 Add warning dialogs for large files
    - Show warning when video >50MB selected
    - Display estimated upload time
    - Allow user to proceed or cancel
    - _Requirements: 4.5_

- [ ] 23. Add performance monitoring
  - Track upload success/failure rates
  - Measure average upload times by file size
  - Monitor compression ratios
  - Track cache hit rates
  - Log errors for debugging
  - _Requirements: 10.1_

- [ ] 24. Integration and end-to-end testing
  - [ ] 24.1 Test image upload flow
    - Test single image upload with compression
    - Test multiple image upload (10 images)
    - Test thumbnail generation
    - Test image viewing in gallery
    - Test image download
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5_

  - [ ] 24.2 Test video upload and playback
    - Test video upload with thumbnail
    - Test video playback inline
    - Test full-screen video
    - Test video controls
    - Test large video warning
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 5.2, 5.3, 5.4, 5.5_

  - [ ] 24.3 Test audio and document handling
    - Test audio upload and playback
    - Test waveform visualization
    - Test document upload
    - Test document opening
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5_

  - [ ] 24.4 Test upload progress and errors
    - Test progress tracking for large files
    - Test upload cancellation
    - Test retry on failure
    - Test background uploads
    - Test notifications
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 12.1, 12.2, 12.3, 12.4, 12.5_

  - [ ] 24.5 Test caching and performance
    - Test media caching
    - Test cache expiration
    - Test preloading
    - Test progressive loading
    - Test concurrent download limits
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

  - [ ] 24.6 Test security and permissions
    - Test RLS policies (users can only access their chat media)
    - Test file validation
    - Test storage permissions
    - Test authenticated uploads
    - _Requirements: 10.1, 10.2, 10.3, 10.4_
