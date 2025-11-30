# Comment Options Implementation

## Overview
This document describes the implementation of comment action options (share, reply, copy, hide, report, pin, delete, edit) for the Synapse Android app.

## Features Implemented

### User Actions (All Users)
- **Reply**: Reply to any comment
- **Copy**: Copy comment text to clipboard
- **Share**: Share comment via Android share sheet
- **Hide**: Hide comment from user's view (personal)
- **Report**: Report comment for moderation (spam, harassment, hate speech, violence, misinformation, other)

### Owner Actions (Comment Author)
- **Edit**: Edit comment content (marks as edited)
- **Delete**: Soft delete comment (marks as deleted, content replaced with "[deleted]")

### Post Author Actions
- **Pin**: Pin a comment to the top of the post (only one pinned comment per post)

## Backend Implementation

### Database Tables

#### Comments Table Updates
Added fields to existing `comments` table:
- `is_pinned` (BOOLEAN): Whether comment is pinned
- `pinned_at` (TIMESTAMP): When comment was pinned
- `pinned_by` (TEXT): User ID who pinned the comment
- `edited_at` (TIMESTAMP): When comment was last edited
- `report_count` (INTEGER): Number of reports on this comment

#### Hidden Comments Table
```sql
CREATE TABLE hidden_comments (
  id UUID PRIMARY KEY,
  comment_id UUID REFERENCES comments(id),
  user_id TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT NOW()
);
```

#### Comment Reports Table
```sql
CREATE TABLE comment_reports (
  id UUID PRIMARY KEY,
  comment_id UUID REFERENCES comments(id),
  reporter_id TEXT NOT NULL,
  reason TEXT CHECK (reason IN ('spam', 'harassment', 'hate_speech', 'violence', 'misinformation', 'other')),
  description TEXT,
  status TEXT DEFAULT 'pending',
  reviewed_by TEXT,
  reviewed_at TIMESTAMP,
  action_taken TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);
```

### RLS Policies

#### Hidden Comments
- Users can hide comments for themselves (INSERT)
- Users can view their own hidden comments (SELECT)
- Users can unhide comments (DELETE)

#### Comment Reports
- Users can report comments (INSERT)
- Users can view their own reports (SELECT)
- Admins can view all reports (SELECT)
- Admins can update report status (UPDATE)

### Triggers
- Auto-increment `report_count` when a new report is created

## Frontend Implementation

### Models

#### Comment.kt
Updated with action fields:
```kotlin
data class Comment(
    // ... existing fields
    val isPinned: Boolean = false,
    val pinnedAt: String? = null,
    val pinnedBy: String? = null,
    val editedAt: String? = null,
    val reportCount: Int = 0
)
```

#### CommentAction.kt (New)
Sealed class for all comment actions:
```kotlin
sealed class CommentAction {
    data class Share(val commentId: String, val content: String, val postId: String)
    data class Reply(val commentId: String, val parentUserId: String)
    data class Copy(val content: String)
    data class Hide(val commentId: String)
    data class Report(val commentId: String, val reason: String, val description: String?)
    data class Pin(val commentId: String, val postId: String)
    data class Delete(val commentId: String)
    data class Edit(val commentId: String, val newContent: String)
}
```

### Repository Methods

#### CommentRepository.kt
Added methods:
- `pinComment(commentId: String, postId: String): Result<Unit>`
- `hideComment(commentId: String): Result<Unit>`
- `reportComment(commentId: String, reason: String, description: String?): Result<Unit>`
- `deleteComment(commentId: String): Result<Unit>` (already existed, enhanced)
- `editComment(commentId: String, content: String): Result<Unit>` (already existed, enhanced)

### UI Components

#### bottom_sheet_comment_options.xml
Material Design 3 bottom sheet with:
- Reply, Copy, Share, Hide, Report buttons (always visible)
- Pin button (visible for post author)
- Edit, Delete buttons (visible for comment owner)
- Divider between common and owner actions

#### CommentOptionsBottomSheet.kt
Bottom sheet dialog that:
- Shows appropriate actions based on user permissions
- Handles clipboard operations
- Shows report reason selection dialog
- Triggers action callbacks

#### CommentDetailAdapter.kt
Updated to:
- Accept `onOptionsClick` callback
- Show options on long-press
- Pass options click to activity

### Activity Integration

#### PostDetailActivity.kt
Added methods:
- `showCommentOptions(comment: CommentWithUser)`: Shows bottom sheet
- `handleCommentAction(action: CommentAction)`: Handles all actions
- `showDeleteCommentDialog(commentId: String)`: Confirmation dialog
- `showEditCommentDialog(commentId: String, currentContent: String)`: Edit dialog

#### PostDetailViewModel.kt
Added methods:
- `pinComment(commentId: String, postId: String)`
- `hideComment(commentId: String)`
- `reportComment(commentId: String, reason: String, description: String?)`

## Permissions & Security

### Action Permissions
- **Reply, Copy, Share, Hide, Report**: All authenticated users
- **Edit, Delete**: Comment owner only
- **Pin**: Post author only

### RLS Enforcement
- Hidden comments are user-specific (RLS ensures users only see their own)
- Reports are visible to reporter and admins only
- Pin action verifies post authorship via RLS

## User Experience

### Long Press Interaction
- Long-press on any comment opens options bottom sheet
- Smooth animation with Material Design 3 styling
- Context-aware options (shows only relevant actions)

### Visual Feedback
- Toast messages for all actions
- Confirmation dialogs for destructive actions (delete)
- Edit dialog with pre-filled content
- Report reason selection with clear categories

### Accessibility
- All buttons have proper content descriptions
- Material Design 3 touch targets (48dp minimum)
- Clear visual hierarchy with dividers

## Testing Checklist

### Backend Testing
- [ ] Verify RLS policies prevent unauthorized access
- [ ] Test report count increment trigger
- [ ] Verify only one comment can be pinned per post
- [ ] Test hidden comments are user-specific

### Frontend Testing
- [ ] Test all actions as comment owner
- [ ] Test all actions as post author
- [ ] Test all actions as regular user
- [ ] Verify permissions are enforced
- [ ] Test edit preserves formatting
- [ ] Test delete shows "[deleted]" text
- [ ] Test pin/unpin toggle
- [ ] Test hide removes comment from view
- [ ] Test report submission
- [ ] Test share intent
- [ ] Test copy to clipboard

### Edge Cases
- [ ] Test actions on deleted comments
- [ ] Test actions on edited comments
- [ ] Test multiple reports on same comment
- [ ] Test pin when another comment is pinned
- [ ] Test offline behavior

## Files Modified

1. `/app/src/main/java/com/synapse/social/studioasinc/model/Comment.kt`
2. `/app/src/main/java/com/synapse/social/studioasinc/model/CommentAction.kt` (new)
3. `/app/src/main/java/com/synapse/social/studioasinc/data/repository/CommentRepository.kt`
4. `/app/src/main/res/layout/bottom_sheet_comment_options.xml` (new)
5. `/app/src/main/java/com/synapse/social/studioasinc/ui/CommentOptionsBottomSheet.kt` (new)
6. `/app/src/main/java/com/synapse/social/studioasinc/adapters/CommentDetailAdapter.kt`
7. `/app/src/main/java/com/synapse/social/studioasinc/PostDetailActivity.kt`
8. `/app/src/main/java/com/synapse/social/studioasinc/PostDetailViewModel.kt`
9. `/app/src/main/res/values/strings.xml`

## Database Migration

Run the following migration on Supabase:
```sql
-- Already applied via Supabase MCP
-- See migration: add_comment_action_tables
```

## Future Enhancements

1. **Batch Actions**: Select multiple comments for bulk hide/report
2. **Comment Moderation**: Admin dashboard for reviewing reports
3. **Edit History**: Track all edits with timestamps
4. **Pin Duration**: Auto-unpin after certain time
5. **Advanced Reporting**: Add screenshots, additional context
6. **Comment Reactions**: Extend beyond like (already partially implemented)
7. **Nested Replies**: Support deeper reply threads
8. **Comment Drafts**: Save unfinished comments

## Notes

- All actions follow MVVM architecture pattern
- Uses Kotlin coroutines for async operations
- Material Design 3 components throughout
- Follows existing code style and conventions
- RLS policies ensure data security
- Minimal code approach as per project guidelines
