# Implementation Plan

- [x] 1. Set up data models and repository interfaces

  - [x] 1.1 Create PostDetail and related data models
    - Create `PostDetail`, `CommentWithUser`, `CommentReaction`, `PollOptionResult` data classes in `model/`
    - Add serialization annotations for Supabase compatibility
    - _Requirements: 1.1, 1.2, 1.4, 2.1, 4.2, 7.1_

  - [x] 1.2 ~~Write property test for data model serialization round-trip~~ (SKIPPED - tests excluded)

  - [x] 1.3 Create state classes for UI
    - Create `PostDetailState`, `CommentsState`, `CommentEvent` sealed classes
    - _Requirements: 1.1, 4.1_

- [x] 2. Implement PostDetailRepository

  - [x] 2.1 Create PostDetailRepository with post fetching
    - Implement `getPostWithDetails(postId)` with user join query
    - Implement `incrementViewCount(postId)`
    - Handle media_items JSONB parsing
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3_

  - [x] 2.2-2.5 ~~Write property tests~~ (SKIPPED - tests excluded)

- [x] 3. Implement ReactionRepository

  - [x] 3.1 Create ReactionRepository for post reactions
    - Implement `togglePostReaction(postId, reactionType)` using reactions table
    - Implement `getPostReactionSummary(postId)` for aggregated counts
    - Implement `getUserPostReaction(postId)` for current user's reaction
    - _Requirements: 3.2, 3.3, 3.4, 3.5_

  - [x] 3.2-3.3 ~~Write property tests~~ (SKIPPED - tests excluded)

  - [x] 3.4 Implement comment reactions
    - Implement `toggleCommentReaction(commentId, reactionType)` using comment_reactions table ✓
    - Implement `getCommentReactionSummary(commentId)` ✓
    - Implement `getUserCommentReaction(commentId)` ✓
    - _Requirements: 6.2, 6.3, 6.4_

  - [x] 3.5 ~~Write property test~~ (SKIPPED - tests excluded)

- [x] 4. Implement CommentRepository

  - [x] 4.1 Create CommentRepository with comment fetching
    - Implement `getComments(postId, limit, offset)` with user join and sorting
    - Implement `getReplies(commentId)` for nested comments
    - Parse parent_comment_id for reply hierarchy
    - _Requirements: 4.1, 4.2, 5.1_

  - [x] 4.2-4.3 ~~Write property tests~~ (SKIPPED - tests excluded)

  - [x] 4.4 Implement comment CRUD operations
    - Implement `createComment(postId, content, mediaUrl, parentCommentId)` ✓
    - Implement `deleteComment(commentId)` with soft delete ✓
    - Implement `editComment(commentId, content)` ✓
    - _Requirements: 4.3, 4.5, 4.6, 5.4_

  - [x] 4.5-4.8 ~~Write property tests~~ (SKIPPED - tests excluded)

- [x] 5. Checkpoint (SKIPPED - tests excluded)

- [x] 6. Implement PollRepository
  - [x] 6.1 Create PollRepository for poll operations
    - Implement `getUserVote(postId)` to check existing vote in poll_votes ✓
    - Implement `submitVote(postId, optionIndex)` with poll_end_time validation ✓
    - Implement `getPollResults(postId)` to calculate vote percentages ✓
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 7. Implement BookmarkRepository and ReshareRepository
  - [x] 7.1 Create BookmarkRepository
    - Implement `isBookmarked(postId)` checking favorites table ✓
    - Implement `toggleBookmark(postId, collectionId)` for add/remove ✓
    - _Requirements: 8.1, 8.2_
  - [x] 7.2 Create ReshareRepository
    - Implement `createReshare(postId, commentary)` inserting into reshares table ✓
    - Implement `hasReshared(postId)` to check existing reshare ✓
    - Update post reshares_count on creation ✓
    - _Requirements: 8.5_

- [ ] 8. Implement text parsing utilities
  - [ ] 8.1 Create HashtagParser utility
    - Extract hashtags from text content using regex
    - Return list of hashtag strings without # prefix
    - _Requirements: 9.1_
  - [ ] 8.2 Create MentionParser utility
    - Extract mentions from text content using regex
    - Return list of username strings without @ prefix
    - _Requirements: 9.3_

- [ ] 9. Implement ReportRepository
  - [ ] 9.1 Create ReportRepository
    - Implement `createReport(postId, reason, description)` inserting into post_reports
    - _Requirements: 10.3_

- [x] 10. Checkpoint (SKIPPED - tests excluded)

- [ ] 11. Implement PostDetailViewModel
  - [ ] 11.1 Create PostDetailViewModel with state management
    - Implement StateFlows for postState, commentsState
    - Implement loadPost(postId) combining repository calls
    - Implement loadComments(postId) with pagination
    - _Requirements: 1.1, 4.1_
  - [ ] 11.2 Implement reaction actions in ViewModel
    - Implement toggleReaction(reactionType) calling ReactionRepository
    - Implement toggleCommentReaction(commentId, reactionType)
    - Update local state optimistically
    - _Requirements: 3.2, 6.2_
  - [ ] 11.3 Implement comment actions in ViewModel
    - Implement addComment(content, parentCommentId)
    - Implement deleteComment(commentId)
    - Implement editComment(commentId, content)
    - _Requirements: 4.3, 5.4_
  - [ ] 11.4 Implement poll, bookmark, reshare actions
    - Implement votePoll(optionIndex)
    - Implement toggleBookmark()
    - Implement createReshare(commentary)
    - _Requirements: 7.3, 8.1, 8.5_
  - [ ] 11.5 Implement report action
    - Implement reportPost(reason)
    - _Requirements: 10.3_

- [ ] 12. Implement Supabase Realtime subscriptions
  - [ ] 12.1 Add real-time comment subscription
    - Subscribe to comments table changes for post_id
    - Emit CommentEvent.Added, Updated, Deleted
    - _Requirements: 11.1, 11.3_
  - [ ] 12.2 Add real-time reaction subscription
    - Subscribe to reactions table changes for post_id
    - Update reaction counts in real-time
    - _Requirements: 11.2_
  - [ ] 12.3 Implement subscription cleanup
    - Unsubscribe from all channels in onCleared()
    - _Requirements: 11.4_

- [x] 13. Checkpoint (SKIPPED - tests excluded)

- [ ] 14. Create XML layouts with Material 3 design
  - [ ] 14.1 Create activity_post_detail.xml
    - CoordinatorLayout with CollapsingToolbarLayout for media
    - NestedScrollView for post content
    - RecyclerView for comments
    - Bottom input bar for comment entry
    - Material 3 cards, buttons, and typography
    - _Requirements: 1.1, 1.2, 4.1_
  - [ ] 14.2 Create item_comment.xml
    - CircleImageView for avatar
    - TextViews for username, content, timestamp
    - Reaction summary display
    - Reply button and replies count
    - Indentation support for nested replies
    - _Requirements: 4.2, 5.1, 5.5_
  - [ ] 14.3 Create layout_poll.xml
    - Poll question TextView
    - RadioGroup/CheckBox group for options
    - Progress bars for results display
    - End time indicator
    - _Requirements: 7.1, 7.2, 7.4_
  - [ ] 14.4 Create bottom_sheet_reaction_picker.xml
    - Horizontal layout with 6 reaction options
    - Material 3 expressive animations
    - _Requirements: 3.1, 6.1_

- [ ] 15. Implement PostDetailActivity
  - [ ] 15.1 Create PostDetailActivity with View Binding
    - Initialize binding and ViewModel
    - Set up toolbar with back navigation
    - Observe ViewModel state flows
    - _Requirements: 1.1_
  - [ ] 15.2 Implement post content display
    - Display post text with hashtag/mention highlighting
    - Display author info with badges
    - Display location if present
    - Display YouTube embed if present
    - _Requirements: 1.1, 1.3, 1.4, 2.1, 2.2, 2.3, 9.1, 9.3_
  - [ ] 15.3 Implement media gallery
    - ViewPager2 for multiple media items
    - Image loading with Glide
    - Video player integration
    - Page indicator for multiple items
    - _Requirements: 1.2_
  - [ ] 15.4 Implement reaction bar
    - Display reaction summary with counts
    - Long-press to show reaction picker
    - Tap to toggle like reaction
    - _Requirements: 3.1, 3.2, 3.5_
  - [ ] 15.5 Implement comments section
    - Set up CommentsAdapter with click listeners
    - Handle reply expansion
    - Handle comment long-press for reactions
    - _Requirements: 4.1, 5.1, 5.2, 6.1_
  - [ ] 15.6 Implement comment input
    - EditText with send button
    - Reply mode indicator
    - Media attachment option
    - _Requirements: 4.3, 5.3_
  - [ ] 15.7 Implement poll UI
    - Display poll options
    - Handle vote selection
    - Show results after voting
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
  - [ ] 15.8 Implement action buttons
    - Bookmark toggle button
    - Share button with intent
    - Reshare button with dialog
    - More options menu with report
    - _Requirements: 8.1, 8.3, 8.4, 10.1, 10.2_

- [ ] 16. Implement adapters
  - [ ] 16.1 Update CommentsAdapter for new features
    - Support CommentWithUser model
    - Display reaction summary
    - Handle reply expansion
    - Support comment reactions
    - _Requirements: 4.2, 5.1, 6.2_
  - [ ] 16.2 Create MediaGalleryAdapter
    - Support images and videos
    - Lazy loading with placeholders
    - Full-screen tap handler
    - _Requirements: 1.2_

- [ ] 17. Implement bottom sheets and dialogs
  - [ ] 17.1 Create ReactionPickerBottomSheet
    - Display 6 reaction options with animations
    - Handle selection callback
    - Support both post and comment reactions
    - _Requirements: 3.1, 6.1_
  - [ ] 17.2 Create ReshareDialog
    - Optional commentary input
    - Confirm/cancel buttons
    - _Requirements: 8.4_
  - [ ] 17.3 Create ReportDialog
    - Reason selection list
    - Optional description input
    - Submit confirmation
    - _Requirements: 10.2_

- [ ] 18. Final integration and navigation
  - [ ] 18.1 Add PostDetailActivity to AndroidManifest
    - Configure activity theme
    - Add intent filters if needed
  - [ ] 18.2 Implement navigation from feed
    - Add click handler in PostsAdapter
    - Pass post ID via Intent extra
  - [ ] 18.3 Implement hashtag/mention navigation
    - Navigate to search results for hashtag tap
    - Navigate to profile for mention tap
    - _Requirements: 9.2, 9.4_

- [x] 19. Final Checkpoint (SKIPPED - tests excluded)

---

## Supabase Schema Reference

The following tables are used by this feature (verified against production schema):

### Core Tables
- `posts` - Post content with media_items JSONB, poll fields, location fields
- `users` - Author info with verify, account_premium, profile_image_url
- `comments` - Comments with parent_comment_id for nesting, user_id FK to users.uid
- `reactions` - Post reactions with reaction_type (like, love, haha, wow, sad, angry)
- `comment_reactions` - Comment reactions with same reaction types

### Supporting Tables
- `favorites` - Bookmarked posts with optional collection_id
- `bookmark_collections` - User bookmark collections
- `reshares` - Post reshares with optional reshare_text
- `poll_votes` - Poll votes with option_index
- `post_reports` - Post reports with reason and description
- `hashtags` - Hashtag registry with usage_count
- `post_hashtags` - Post-hashtag junction table
- `mentions` - User mentions in posts/comments

### Key Relationships
- `posts.author_uid` → `users.uid`
- `comments.user_id` → `users.uid`
- `comments.post_id` → `posts.id`
- `comments.parent_comment_id` → `comments.id` (self-reference for replies)
- `reactions.post_id` → `posts.id`
- `comment_reactions.comment_id` → `comments.id`
- `favorites.post_id` → `posts.id`
- `reshares.post_id` → `posts.id`
- `poll_votes.post_id` → `posts.id`
