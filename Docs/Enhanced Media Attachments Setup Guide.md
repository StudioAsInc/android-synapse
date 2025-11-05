# Enhanced Media Attachments Setup Guide

This guide explains how to set up the Supabase Storage infrastructure for the Enhanced Media Attachments feature.

## Overview

The Enhanced Media Attachments feature requires:
- A private `chat-media` storage bucket in Supabase
- Row Level Security (RLS) policies for secure access
- Organized folder structure for efficient storage
- Proper authentication and access controls

## Setup Steps

### 1. Create Storage Bucket

1. Open your Supabase Dashboard
2. Navigate to **Storage** section
3. Click **New Bucket**
4. Configure the bucket:
   - **Name**: `chat-media`
   - **Public**: `false` (private bucket)
   - **File size limit**: `100MB` (recommended)
   - **Allowed MIME types**: Configure as needed (see below)

### 2. Configure MIME Types (Optional)

If you want to restrict file types, configure these MIME types in the bucket settings:

**Images:**
- `image/jpeg`
- `image/png`
- `image/gif`
- `image/webp`

**Videos:**
- `video/mp4`
- `video/quicktime`
- `video/x-msvideo`
- `video/webm`

**Audio:**
- `audio/mpeg`
- `audio/wav`
- `audio/mp4`
- `audio/ogg`

**Documents:**
- `application/pdf`
- `application/msword`
- `application/vnd.openxmlformats-officedocument.wordprocessingml.document`
- `text/plain`

### 3. Set Up RLS Policies

1. Go to **SQL Editor** in your Supabase Dashboard
2. Run the SQL script from `Docs/supabase-storage-setup.sql`
3. This will create the necessary RLS policies to ensure users can only access media from chats they participate in

### 4. Verify Setup

The app includes a built-in test utility to verify the storage infrastructure:

```kotlin
// In your activity or service
val storageService = SupabaseStorageService()
val testResult = storageService.testStorageInfrastructure(context)

testResult.fold(
    onSuccess = { results ->
        Log.i("StorageSetup", "Test Results:\n$results")
    },
    onFailure = { error ->
        Log.e("StorageSetup", "Setup verification failed", error)
    }
)
```

## Folder Structure

The system organizes files using this structure:
```
chat-media/
├── {chat_id}/
│   ├── 2025/
│   │   ├── 01/
│   │   │   ├── 15/
│   │   │   │   ├── {uuid}_original.jpg
│   │   │   │   ├── {uuid}_thumb.jpg
│   │   │   │   └── {uuid}_video.mp4
```

This structure provides:
- **Organization**: Files grouped by chat and date
- **Scalability**: Prevents too many files in a single folder
- **Uniqueness**: UUID prefixes prevent filename collisions
- **Security**: RLS policies restrict access by chat participation

## Security Features

### Row Level Security (RLS)

The RLS policies ensure:
- Users can only upload to chats they participate in
- Users can only download media from their chats
- Users can only delete files from their chats
- Unauthorized access attempts are blocked

### Authentication

All storage operations require:
- Valid Supabase authentication
- Active user session
- Participation in the relevant chat

### File Validation

The system validates:
- File size limits (configurable per type)
- MIME type restrictions
- User permissions
- Chat participation

## Troubleshooting

### Common Issues

1. **"Bucket not found" error**
   - Ensure the `chat-media` bucket is created in Supabase Dashboard
   - Check bucket name spelling (case-sensitive)

2. **"Access denied" error**
   - Verify RLS policies are applied correctly
   - Check user authentication status
   - Ensure user is a participant in the chat

3. **"File too large" error**
   - Check bucket file size limits
   - Verify file compression is working
   - Adjust size limits if needed

4. **Upload failures**
   - Check network connectivity
   - Verify Supabase credentials
   - Check storage quota limits

### Testing Commands

Run these SQL queries in Supabase SQL Editor to verify setup:

```sql
-- Check if policies are created
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual 
FROM pg_policies 
WHERE tablename = 'objects' AND schemaname = 'storage'
AND policyname LIKE '%chat%';

-- Verify chat_participants table exists
SELECT COUNT(*) FROM chat_participants;

-- Test folder structure parsing
SELECT (storage.foldername('test_chat_id/2025/01/15/file.jpg'))[1] as chat_id;
```

## Performance Considerations

### Caching Strategy

The system implements:
- Local file caching (500MB limit)
- 7-day cache expiration
- LRU eviction policy
- Progressive image loading

### Upload Optimization

Features include:
- Concurrent uploads (max 3)
- Progress tracking
- Automatic retry on failure
- Background upload support

### Download Optimization

Optimizations include:
- Preloading for galleries
- Thumbnail generation
- Concurrent downloads (max 5)
- Cache-first strategy

## Monitoring

### Metrics to Track

- Upload success/failure rates
- Average upload times by file size
- Storage usage per user
- Cache hit rates
- RLS policy violations

### Logs to Monitor

- Authentication failures
- Upload/download errors
- RLS policy denials
- Storage quota warnings

## Maintenance

### Regular Tasks

1. **Cleanup orphaned files** (weekly)
   - Files not referenced in any message
   - Files from deleted messages

2. **Monitor storage usage**
   - Track per-user storage consumption
   - Alert on approaching limits

3. **Cache maintenance**
   - Clear expired cache entries
   - Monitor cache performance

4. **Security audits**
   - Review RLS policy effectiveness
   - Check for unauthorized access attempts

## Next Steps

After completing the setup:

1. Test the infrastructure using the built-in test utility
2. Proceed with implementing the media upload/download components
3. Set up monitoring and alerting
4. Configure backup and disaster recovery procedures

For implementation details, see the main design document and task list.