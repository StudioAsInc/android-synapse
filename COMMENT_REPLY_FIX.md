# Comment Reply Fix

## Issue
Comment replies were not showing after clicking "View n replies" button.

## Root Cause
The application was automatically loading all replies for every comment on initial load, but there was no UI control to show/hide them. The replies were being fetched but not displayed to the user.

## Solution

### Changes Made

#### 1. `comment-item.component.ts` - Added Reply Toggle Functionality

**Added new signals:**
- `showReplies = signal(false)` - Controls visibility of replies
- `loadingReplies = signal(false)` - Shows loading state while fetching replies

**Added new methods:**
```typescript
async toggleReplies() {
  this.showReplies.update(v => !v);
  if (this.showReplies() && (!this.comment().replies || this.comment().replies!.length === 0)) {
    await this.loadReplies();
  }
}

async loadReplies() {
  if (this.loadingReplies()) return;
  
  this.loadingReplies.set(true);
  try {
    const replies = await this.commentService.fetchReplies(this.comment().id);
    this.comment().replies = replies;
  } catch (err) {
    console.error('Error loading replies:', err);
  } finally {
    this.loadingReplies.set(false);
  }
}
```

**Updated template:**
- Added "View n replies" button that shows when `replies_count > 0` and `!isReply()`
- Button displays loading state with spinner while fetching
- Shows "Hide replies" button when replies are visible
- Replies only render when `showReplies()` is true

#### 2. `comment.service.ts` - Optimized Initial Load

**Changed:**
- Removed automatic reply loading in `fetchComments()` method
- Replies are now initialized as empty array `[]` instead of being fetched immediately
- Replies are only loaded on-demand when user clicks "View n replies"

**Benefits:**
- Faster initial page load
- Reduced database queries
- Better user experience with explicit control

#### 3. `submitReply()` Method Update

**Changed:**
- Now calls `await this.loadReplies()` after successfully posting a reply
- Ensures newly posted replies are immediately visible

## Testing

To verify the fix works:

1. Navigate to a post with comments that have replies
2. Look for comments showing "View n replies" button
3. Click the button - replies should load and display
4. Click "Hide replies" - replies should hide
5. Post a new reply - it should appear immediately after submission

## Technical Details

- Uses Angular signals for reactive state management
- Implements lazy loading pattern for better performance
- Maintains proper loading states to prevent duplicate requests
- Properly handles errors during reply fetching
