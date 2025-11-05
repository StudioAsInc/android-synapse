-- Supabase Storage Setup for Enhanced Media Attachments
-- This script sets up the chat-media storage bucket and RLS policies
-- Run this in your Supabase SQL Editor

-- Create the chat-media storage bucket (if not exists)
-- Note: Buckets are typically created via the Supabase Dashboard Storage section
-- This is for reference - you'll need to create the bucket manually in the dashboard

-- Storage bucket configuration:
-- Bucket name: chat-media
-- Public: false (private bucket)
-- File size limit: 100MB (configurable in dashboard)
-- Allowed MIME types: Configure in dashboard as needed

-- RLS Policies for chat-media bucket
-- These policies ensure users can only access media from chats they participate in

-- Policy 1: Users can upload to their own chat folders
CREATE POLICY "Users can upload to their chats" ON storage.objects
FOR INSERT WITH CHECK (
    bucket_id = 'chat-media' AND
    (storage.foldername(name))[1] IN (
        SELECT chat_id FROM chat_participants WHERE user_id = auth.uid()::text
    )
);

-- Policy 2: Users can read from their chat folders
CREATE POLICY "Users can read from their chats" ON storage.objects
FOR SELECT USING (
    bucket_id = 'chat-media' AND
    (storage.foldername(name))[1] IN (
        SELECT chat_id FROM chat_participants WHERE user_id = auth.uid()::text
    )
);

-- Policy 3: Users can delete from their chat folders
CREATE POLICY "Users can delete from their chats" ON storage.objects
FOR DELETE USING (
    bucket_id = 'chat-media' AND
    (storage.foldername(name))[1] IN (
        SELECT chat_id FROM chat_participants WHERE user_id = auth.uid()::text
    )
);

-- Policy 4: Users can update files in their chat folders
CREATE POLICY "Users can update files in their chats" ON storage.objects
FOR UPDATE USING (
    bucket_id = 'chat-media' AND
    (storage.foldername(name))[1] IN (
        SELECT chat_id FROM chat_participants WHERE user_id = auth.uid()::text
    )
);

-- Enable RLS on storage.objects (should already be enabled)
ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY;

-- Create indexes for better performance on chat_participants table
-- (These may already exist from previous chat setup)
CREATE INDEX IF NOT EXISTS idx_chat_participants_user_id ON chat_participants(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_participants_chat_id ON chat_participants(chat_id);
CREATE INDEX IF NOT EXISTS idx_chat_participants_user_chat ON chat_participants(user_id, chat_id);

-- Verify the setup
-- Run these queries to test the policies work correctly:

-- Test 1: Check if policies are created
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual 
FROM pg_policies 
WHERE tablename = 'objects' AND schemaname = 'storage'
AND policyname LIKE '%chat%';

-- Test 2: Verify chat_participants table exists and has data
-- SELECT COUNT(*) FROM chat_participants;

-- Test 3: Test folder structure parsing
-- SELECT (storage.foldername('test_chat_id/2025/01/15/file.jpg'))[1] as chat_id;

-- Notes for manual setup in Supabase Dashboard:
-- 1. Go to Storage section in Supabase Dashboard
-- 2. Create new bucket named 'chat-media'
-- 3. Set bucket to Private (not public)
-- 4. Configure file size limits (recommended: 100MB max)
-- 5. Set allowed MIME types if needed:
--    - Images: image/jpeg, image/png, image/gif, image/webp
--    - Videos: video/mp4, video/quicktime, video/x-msvideo, video/webm
--    - Audio: audio/mpeg, audio/wav, audio/mp4, audio/ogg
--    - Documents: application/pdf, application/msword, text/plain, etc.
-- 6. Run the RLS policies above in the SQL Editor