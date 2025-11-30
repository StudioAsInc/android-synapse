# Implementation Plan

**Status**: Specifications 100% complete. All implementation details documented.

**Progress**: 14/19 phases complete (73% implementation, 100% specification)

**Remaining Implementation Work**:
- Task 12: Realtime subscriptions (fully specified)
- Task 14.3-14.4: Poll layouts (fully specified)
- Task 15.7: Poll UI logic (fully specified)
- Task 17: Bottom sheets and dialogs (fully specified)
- Task 18.3: Hashtag/mention navigation (fully specified)

All remaining tasks have complete implementation details in design.md including:
- Code snippets with full method signatures
- XML layout structures
- Animation specifications
- Navigation patterns

---

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

- [x] 8. Implement text parsing utilities
  - [x] 8.1 Create HashtagParser utility
    - Extract hashtags from text content using regex ✓
    - Return list of hashtag strings without # prefix ✓
    - _Requirements: 9.1_
  - [x] 8.2 Create MentionParser utility
    - Extract mentions from text content using regex ✓
    - Return list of username strings without @ prefix ✓
    - _Requirements: 9.3_

- [x] 9. Implement ReportRepository
  - [x] 9.1 Create ReportRepository
    - Implement `createReport(postId, reason, description)` inserting into post_reports ✓
    - _Requirements: 10.3_

- [x] 10. Checkpoint (SKIPPED - tests excluded)

- [x] 11. Implement PostDetailViewModel
  - [x] 11.1 Create PostDetailViewModel with state management
    - Implement StateFlows for postState, commentsState ✓
    - Implement loadPost(postId) combining repository calls ✓
    - Implement loadComments(postId) with pagination ✓
    - _Requirements: 1.1, 4.1_
  - [x] 11.2 Implement reaction actions in ViewModel
    - Implement toggleReaction(reactionType) calling ReactionRepository ✓
    - Implement toggleCommentReaction(commentId, reactionType) ✓
    - Update local state optimistically ✓
    - _Requirements: 3.2, 6.2_
  - [x] 11.3 Implement comment actions in ViewModel
    - Implement addComment(content, parentCommentId) ✓
    - Implement deleteComment(commentId) ✓
    - Implement editComment(commentId, content) ✓
    - _Requirements: 4.3, 5.4_
  - [x] 11.4 Implement poll, bookmark, reshare actions
    - Implement votePoll(optionIndex) ✓
    - Implement toggleBookmark() ✓
    - Implement createReshare(commentary) ✓
    - _Requirements: 7.3, 8.1, 8.5_
  - [x] 11.5 Implement report action
    - Implement reportPost(reason) ✓
    - _Requirements: 10.3_

- [ ] 12. Implement Supabase Realtime subscriptions
  - [ ] 12.1 Add real-time comment subscription
    - Subscribe to comments table changes for post_id using `SupabaseClient.client.realtime.channel()`
    - Emit CommentEvent.Added, Updated, Deleted via callbackFlow
    - Handle INSERT, UPDATE, DELETE PostgresActions with filters
    - _Requirements: 11.1, 11.3_
    - _Implementation: See design.md "Realtime Subscriptions Implementation" section_
  - [ ] 12.2 Add real-time reaction subscription
    - Subscribe to reactions table changes for post_id
    - Update reaction counts in real-time via observePostReactions Flow
    - _Requirements: 11.2_
    - _Implementation: See design.md "ReactionRepository Realtime" section_
  - [ ] 12.3 Implement subscription cleanup
    - Unsubscribe from all channels in ViewModel.onCleared()
    - Cancel all realtime jobs
    - _Requirements: 11.4_
    - _Implementation: See design.md "ViewModel Realtime Integration" section_

- [x] 13. Checkpoint (SKIPPED - tests excluded)

- [x] 14. Create XML layouts with Material 3 design
  - [x] 14.1 Create activity_post_detail.xml ✓
    - CoordinatorLayout with CollapsingToolbarLayout for media ✓
    - NestedScrollView for post content ✓
    - RecyclerView for comments ✓
    - Bottom input bar for comment entry ✓
    - Material 3 cards, buttons, and typography ✓
    - _Requirements: 1.1, 1.2, 4.1_
  - [x] 14.2 Create item_comment_detail.xml ✓
    - CircleImageView for avatar ✓
    - TextViews for username, content, timestamp ✓
    - Reaction summary display ✓
    - Reply button and replies count ✓
    - Indentation support for nested replies ✓
    - _Requirements: 4.2, 5.1, 5.5_
  - [ ] 14.3 Create layout_poll.xml
    - MaterialCardView with 12dp corner radius
    - Poll question TextView with titleMedium appearance
    - RecyclerView for poll options
    - Footer with vote count and end time TextViews
    - _Requirements: 7.1, 7.2, 7.4_
    - _Implementation: See design.md "layout_poll.xml Structure" section_
  - [ ] 14.4 Create item_poll_option.xml
    - FrameLayout with progress background View
    - MaterialCardView with RadioButton, option text, and percentage
    - 8dp corner radius with outline stroke
    - _Implementation: See design.md "item_poll_option.xml Structure" section_
  - [x] 14.4 Create bottom_sheet_reaction_picker.xml ✓
    - Horizontal layout with 6 reaction options ✓
    - Material 3 expressive animations ✓
    - _Requirements: 3.1, 6.1_

- [x] 15. Implement PostDetailActivity
  - [x] 15.1 Create PostDetailActivity with View Binding ✓
    - Initialize binding and ViewModel ✓
    - Set up toolbar with back navigation ✓
    - Observe ViewModel state flows ✓
    - _Requirements: 1.1_
  - [x] 15.2 Implement post content display ✓
    - Display post text with hashtag/mention highlighting ✓
    - Display author info with badges ✓
    - Display location if present ✓
    - Display YouTube embed if present ✓
    - _Requirements: 1.1, 1.3, 1.4, 2.1, 2.2, 2.3, 9.1, 9.3_
  - [x] 15.3 Implement media gallery ✓
    - ViewPager2 for multiple media items ✓
    - Image loading with Glide ✓
    - Video player integration ✓
    - Page indicator for multiple items ✓
    - _Requirements: 1.2_
  - [x] 15.4 Implement reaction bar ✓
    - Display reaction summary with counts ✓
    - Long-press to show reaction picker ✓
    - Tap to toggle like reaction ✓
    - _Requirements: 3.1, 3.2, 3.5_
  - [x] 15.5 Implement comments section ✓
    - Set up CommentsAdapter with click listeners ✓
    - Handle reply expansion ✓
    - Handle comment long-press for reactions ✓
    - _Requirements: 4.1, 5.1, 5.2, 6.1_
  - [x] 15.6 Implement comment input ✓
    - EditText with send button ✓
    - Reply mode indicator ✓
    - Media attachment option ✓
    - _Requirements: 4.3, 5.3_
  - [ ] 15.7 Implement poll UI
    - Create PollAdapter with PollOptionResult binding
    - Display poll question, options, vote count, end time
    - Handle vote selection with onOptionClick callback
    - Show results with animated progress bars after voting
    - Disable voting if poll ended (check pollEndTime)
    - Format duration for "Ends in" display
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
    - _Implementation: See design.md "Poll UI Implementation" section_
  - [x] 15.8 Implement action buttons ✓
    - Bookmark toggle button ✓
    - Share button with intent ✓
    - Reshare button with dialog ✓
    - More options menu with report ✓
    - _Requirements: 8.1, 8.3, 8.4, 10.1, 10.2_

- [x] 16. Implement adapters
  - [x] 16.1 Update CommentsAdapter for new features ✓
    - Support CommentWithUser model ✓
    - Display reaction summary ✓
    - Handle reply expansion ✓
    - Support comment reactions ✓
    - _Requirements: 4.2, 5.1, 6.2_
  - [x] 16.2 Create MediaGalleryAdapter ✓
    - Support images and videos ✓
    - Lazy loading with placeholders ✓
    - Full-screen tap handler ✓
    - _Requirements: 1.2_

- [ ] 17. Implement bottom sheets and dialogs
  - [ ] 17.1 Create ReactionPickerBottomSheet
    - Extend BottomSheetDialogFragment with bottom_sheet_reaction_picker.xml binding
    - Display 6 reaction options (👍❤️😂😮😢😠) in horizontal layout
    - Animate entry with scale animation (0→1, 200ms, OvershootInterpolator)
    - Invoke onReactionSelected callback on tap and dismiss
    - _Requirements: 3.1, 6.1_
    - _Implementation: See design.md "ReactionPickerBottomSheet" section_
  - [ ] 17.2 Create ReshareDialog
    - Extend DialogFragment with MaterialAlertDialogBuilder
    - Bind dialog_reshare.xml with TextInputEditText for optional commentary
    - Max length 500 characters, minLines 3
    - Positive button invokes onReshareConfirmed with commentary text
    - _Requirements: 8.4_
    - _Implementation: See design.md "ReshareDialog" section_
  - [ ] 17.3 Create ReportDialog
    - Extend DialogFragment with MaterialAlertDialogBuilder
    - Bind dialog_report.xml with Spinner for reason selection
    - Reasons: spam, harassment, hate_speech, violence, misinformation, inappropriate_content, other
    - TextInputEditText for optional description (max 1000 chars)
    - Positive button invokes onReportSubmitted with reason and description
    - _Requirements: 10.2_
    - _Implementation: See design.md "ReportDialog" section_
  - [ ] 17.4 Create dialog_reshare.xml layout
    - LinearLayout with TextInputLayout (OutlinedBox style, 16dp corner radius)
    - TextInputEditText with reshare_hint, maxLength 500, minLines 3
    - _Implementation: See design.md "dialog_reshare.xml Structure" section_
  - [ ] 17.5 Create dialog_report.xml layout
    - LinearLayout with reason label TextView
    - Spinner for reason selection with ArrayAdapter
    - TextInputLayout with description TextInputEditText (maxLength 1000, minLines 3)
    - _Implementation: See design.md "dialog_report.xml Structure" section_

- [x] 18. Final integration and navigation
  - [x] 18.1 Add PostDetailActivity to AndroidManifest ✓
    - Configure activity theme ✓
    - Add intent filters if needed ✓
  - [x] 18.2 Implement navigation from feed ✓
    - Add click handler in PostsAdapter ✓
    - Pass post ID via Intent extra ✓
  - [ ] 18.3 Implement hashtag/mention navigation
    - Create TextLinkifier utility with HASHTAG_PATTERN and MENTION_PATTERN regex
    - Apply ClickableSpan to hashtags and mentions in post/comment text
    - Navigate to SearchActivity with "#hashtag" query for hashtag taps
    - Navigate to ProfileActivity with username for mention taps
    - Use primary color for links, no underline
    - _Requirements: 9.2, 9.4_
    - _Implementation: See design.md "Hashtag and Mention Navigation" section_

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
