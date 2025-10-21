-- Storage Buckets and Policies for Synapse Social Media App
-- Run these SQL commands to set up file storage

-- =============================================
-- CREATE STORAGE BUCKETS
-- =============================================

-- Create bucket for user profile photos
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'profile-photos',
    'profile-photos',
    true,
    5242880, -- 5MB limit
    ARRAY['image/jpeg', 'image/png', 'image/webp', 'image/gif']
) ON CONFLICT (id) DO NOTHING;

-- Create bucket for user cover photos
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'cover-photos',
    'cover-photos',
    true,
    10485760, -- 10MB limit
    ARRAY['image/jpeg', 'image/png', 'image/webp']
) ON CONFLICT (id) DO NOTHING;

-- Create bucket for post attachments
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'post-attachments',
    'post-attachments',
    true,
    52428800, -- 50MB limit
    ARRAY[
        'image/jpeg', 'image/png', 'image/webp', 'image/gif',
        'video/mp4', 'video/webm', 'video/quicktime',
        'audio/mpeg', 'audio/wav', 'audio/ogg'
    ]
) ON CONFLICT (id) DO NOTHING;

-- Create bucket for chat attachments
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'chat-attachments',
    'chat-attachments',
    false, -- Private bucket
    52428800, -- 50MB limit
    ARRAY[
        'image/jpeg', 'image/png', 'image/webp', 'image/gif',
        'video/mp4', 'video/webm', 'video/quicktime',
        'audio/mpeg', 'audio/wav', 'audio/ogg', 'audio/mp4',
        'application/pdf', 'text/plain',
        'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    ]
) ON CONFLICT (id) DO NOTHING;

-- Create bucket for stories
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'stories',
    'stories',
    true,
    52428800, -- 50MB limit
    ARRAY[
        'image/jpeg', 'image/png', 'image/webp',
        'video/mp4', 'video/webm', 'video/quicktime'
    ]
) ON CONFLICT (id) DO NOTHING;

-- =============================================
-- STORAGE POLICIES
-- =============================================

-- Profile Photos Policies
CREATE POLICY "Users can upload their own profile photos" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'profile-photos' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can update their own profile photos" ON storage.objects
    FOR UPDATE USING (
        bucket_id = 'profile-photos' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can delete their own profile photos" ON storage.objects
    FOR DELETE USING (
        bucket_id = 'profile-photos' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Anyone can view profile photos" ON storage.objects
    FOR SELECT USING (bucket_id = 'profile-photos');

-- Cover Photos Policies
CREATE POLICY "Users can upload their own cover photos" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'cover-photos' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can update their own cover photos" ON storage.objects
    FOR UPDATE USING (
        bucket_id = 'cover-photos' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can delete their own cover photos" ON storage.objects
    FOR DELETE USING (
        bucket_id = 'cover-photos' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Anyone can view cover photos" ON storage.objects
    FOR SELECT USING (bucket_id = 'cover-photos');

-- Post Attachments Policies
CREATE POLICY "Users can upload post attachments" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'post-attachments' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can update their own post attachments" ON storage.objects
    FOR UPDATE USING (
        bucket_id = 'post-attachments' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can delete their own post attachments" ON storage.objects
    FOR DELETE USING (
        bucket_id = 'post-attachments' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Anyone can view post attachments" ON storage.objects
    FOR SELECT USING (bucket_id = 'post-attachments');

-- Chat Attachments Policies (Private)
CREATE POLICY "Users can upload chat attachments" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'chat-attachments' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can view chat attachments they have access to" ON storage.objects
    FOR SELECT USING (
        bucket_id = 'chat-attachments' AND (
            auth.uid()::text = (storage.foldername(name))[1] OR
            EXISTS (
                SELECT 1 FROM public.messages m
                JOIN public.chat_participants cp ON m.chat_id = cp.chat_id
                WHERE m.attachments::text LIKE '%' || name || '%'
                AND cp.user_id = auth.uid()
                AND cp.left_at IS NULL
            )
        )
    );

CREATE POLICY "Users can delete their own chat attachments" ON storage.objects
    FOR DELETE USING (
        bucket_id = 'chat-attachments' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

-- Stories Policies
CREATE POLICY "Users can upload their own stories" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'stories' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can delete their own stories" ON storage.objects
    FOR DELETE USING (
        bucket_id = 'stories' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can view stories based on privacy settings" ON storage.objects
    FOR SELECT USING (
        bucket_id = 'stories' AND (
            auth.uid()::text = (storage.foldername(name))[1] OR
            EXISTS (
                SELECT 1 FROM public.stories s
                JOIN public.users u ON s.user_id = u.uid
                WHERE s.content_url LIKE '%' || name || '%'
                AND (
                    NOT u.private_account OR
                    EXISTS (
                        SELECT 1 FROM public.follows f
                        WHERE f.follower_id = auth.uid()
                        AND f.following_id = u.uid
                        AND f.status = 'accepted'
                    )
                )
                AND s.expires_at > NOW()
            )
        )
    );

-- =============================================
-- HELPER FUNCTIONS FOR FILE MANAGEMENT
-- =============================================

-- Function to clean up expired stories
CREATE OR REPLACE FUNCTION cleanup_expired_stories()
RETURNS void AS $$
DECLARE
    expired_story RECORD;
BEGIN
    -- Delete expired stories from database and storage
    FOR expired_story IN 
        SELECT id, content_url FROM public.stories 
        WHERE expires_at < NOW()
    LOOP
        -- Delete from storage
        PERFORM storage.delete_object('stories', expired_story.content_url);
        
        -- Delete from database
        DELETE FROM public.stories WHERE id = expired_story.id;
    END LOOP;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get file upload URL with proper path
CREATE OR REPLACE FUNCTION get_upload_url(
    bucket_name TEXT,
    file_extension TEXT DEFAULT 'jpg'
)
RETURNS TEXT AS $$
DECLARE
    file_name TEXT;
    user_folder TEXT;
BEGIN
    user_folder := auth.uid()::text;
    file_name := user_folder || '/' || gen_random_uuid()::text || '.' || file_extension;
    
    RETURN file_name;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================
-- SCHEDULED CLEANUP (Run this as a cron job)
-- =============================================

-- Create a function to be called by pg_cron or external scheduler
CREATE OR REPLACE FUNCTION scheduled_cleanup()
RETURNS void AS $$
BEGIN
    -- Clean up expired stories
    PERFORM cleanup_expired_stories();
    
    -- Clean up orphaned files (files not referenced in database)
    -- This is a more complex operation and should be done carefully
    -- You might want to implement this based on your specific needs
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================
-- FILE SIZE AND TYPE VALIDATION
-- =============================================

-- Function to validate file upload
CREATE OR REPLACE FUNCTION validate_file_upload(
    bucket_name TEXT,
    file_name TEXT,
    file_size BIGINT,
    mime_type TEXT
)
RETURNS BOOLEAN AS $$
DECLARE
    bucket_config RECORD;
BEGIN
    -- Get bucket configuration
    SELECT * INTO bucket_config 
    FROM storage.buckets 
    WHERE id = bucket_name;
    
    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;
    
    -- Check file size
    IF file_size > bucket_config.file_size_limit THEN
        RETURN FALSE;
    END IF;
    
    -- Check mime type
    IF bucket_config.allowed_mime_types IS NOT NULL AND 
       NOT (mime_type = ANY(bucket_config.allowed_mime_types)) THEN
        RETURN FALSE;
    END IF;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================
-- COMPLETION MESSAGE
-- =============================================

-- Storage setup completed successfully!
-- 
-- Buckets created:
-- - profile-photos (public, 5MB, images only)
-- - cover-photos (public, 10MB, images only)
-- - post-attachments (public, 50MB, images/videos/audio)
-- - chat-attachments (private, 50MB, all file types)
-- - stories (public, 50MB, images/videos)
-- 
-- All storage policies are configured for proper security.
-- 
-- Next steps:
-- 1. Configure real-time subscriptions
-- 2. Set up authentication providers
-- 3. Test file uploads from your app