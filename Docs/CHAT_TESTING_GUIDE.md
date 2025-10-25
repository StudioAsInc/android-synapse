# Chat Feature Testing Guide

## Quick Test Scenarios

### Scenario 1: First Time Chat
**Goal**: Start a new conversation with a user

1. Launch the app and log in
2. Tap the Search icon (bottom navigation, magnifying glass)
3. Type a username in the search bar
4. Tap on a user from the search results
5. This opens their profile
6. From profile, you can message them (if message button exists)
   - OR go back to Search → Tap Inbox → Tap compose → Search user → Tap user

**Expected Result**:
- ChatActivity opens
- Empty chat screen appears
- You can type and send messages
- Messages appear immediately after sending

### Scenario 2: View Chat List
**Goal**: See all your active conversations

1. Launch the app and log in
2. Tap the Inbox icon (bottom navigation, chat bubble)
3. View the "Chats" tab (should be default)

**Expected Result**:
- List of all your chats appears
- Each chat shows:
  - User's avatar
  - Username
  - Last message preview
  - Time of last message
- If no chats exist, shows "No chats yet" message

### Scenario 3: Continue Existing Chat
**Goal**: Open and continue a previous conversation

1. Go to Inbox
2. Tap on any chat from the list

**Expected Result**:
- ChatActivity opens
- All previous messages load
- Messages are in chronological order (oldest first)
- You can scroll through message history
- You can send new messages

### Scenario 4: Send Multiple Messages
**Goal**: Test message sending and display

1. Open any chat
2. Type "Hello" and send
3. Type "How are you?" and send
4. Type "Testing the chat feature" and send

**Expected Result**:
- All three messages appear in the chat
- Messages are aligned correctly (your messages on right)
- Timestamps show for each message
- Messages are saved to database

### Scenario 5: Return to Chat
**Goal**: Verify messages persist

1. Send some messages in a chat
2. Press back button to return to Inbox
3. Open the same chat again

**Expected Result**:
- All previously sent messages are still there
- Messages load from database
- No messages are lost

## Database Verification

You can verify messages are being saved by checking the Supabase dashboard:

1. Go to Supabase Dashboard
2. Navigate to Table Editor
3. Check the `messages` table for new entries
4. Check the `chats` table for chat records
5. Check the `chat_participants` table for participant records

## Common Issues & Solutions

### Issue: "Chat not initialized" error
**Solution**: Make sure you're passing either `chatId` or `uid` in the intent when opening ChatActivity

### Issue: Messages not appearing
**Solution**: 
- Check internet connection
- Verify Supabase credentials in BuildConfig
- Check Logcat for error messages

### Issue: Empty chat list
**Solution**:
- Start a new chat first from Search
- Check if user is logged in
- Verify `chat_participants` table has entries

### Issue: Can't send messages
**Solution**:
- Check if message input field is enabled
- Verify send button is clickable
- Check Logcat for network errors

## Performance Testing

### Test 1: Load Time
- Open a chat with 50+ messages
- Measure time to load and display
- Should be < 2 seconds

### Test 2: Scroll Performance
- Scroll through a long chat history
- Should be smooth with no lag

### Test 3: Send Speed
- Send a message
- Should appear immediately (< 500ms)
- Database save happens in background

## Edge Cases to Test

1. **No Internet Connection**
   - Try sending a message offline
   - Should show error message

2. **Empty Message**
   - Try sending an empty message
   - Send button should be disabled

3. **Very Long Message**
   - Type a message with 1000+ characters
   - Should send and display correctly

4. **Special Characters**
   - Send messages with emojis: 😀 🎉 ❤️
   - Send messages with special chars: @#$%^&*
   - Should display correctly

5. **Rapid Sending**
   - Send 10 messages quickly
   - All should appear in correct order

## Debugging Tips

### Enable Verbose Logging
Add this to ChatActivity to see detailed logs:
```kotlin
android.util.Log.d("ChatActivity", "Message sent: $messageText")
android.util.Log.d("ChatActivity", "Messages loaded: ${messagesList.size}")
```

### Check Supabase Logs
1. Go to Supabase Dashboard
2. Navigate to Logs
3. Filter by "postgrest" to see database queries
4. Look for INSERT and SELECT operations on messages table

### Use Android Studio Logcat
Filter by:
- `ChatActivity` - for chat-specific logs
- `SupabaseChatService` - for backend operations
- `InboxChatsFragment` - for chat list operations

## Success Criteria

The chat feature is working correctly if:
- ✅ Users can start new chats from Search
- ✅ Messages send and appear immediately
- ✅ Messages persist in database
- ✅ Chat list shows all active chats
- ✅ Previous messages load when reopening chat
- ✅ No crashes or errors during normal use
- ✅ UI is responsive and smooth

## Next Steps After Testing

Once basic chat is working:
1. Add real-time message updates (Supabase Realtime)
2. Implement typing indicators
3. Add message read receipts
4. Support image/video attachments
5. Add push notifications for new messages
