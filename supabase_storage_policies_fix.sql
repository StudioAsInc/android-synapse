-- =====================================================
-- Supabase Storage Policies Fix
-- Run this if you get "policy already exists" errors
-- =====================================================

-- This script safely creates storage policies only if they don't exist

-- Profile Photos Policies
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE schemaname = 'storage' 
        AND tablename = 'objects' 
        AND policyname = 'Users can upload their own profile photos'
    ) THEN
        CREATE POLICY "Users can upload their own profile photos" ON storage.objects
            FOR INSERT WITH CHECK (
                bucket_id = 'avatars' AND
                auth.uid()::text = (storage.foldername(name))[1]
            );
    END IF;
END $$;

DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE schemaname = 'storage' 
        AND tablename = 'objects' 
        AND policyname = 'Anyone can view profile photos'
    ) THEN
        CREATE POLICY "Anyone can view profile photos" ON storage.objects
            FOR SELECT USING (bucket_id = 'avatars');
    END IF;
END $$;

-- Media Files Policies
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE schemaname = 'storage' 
        AND tablename = 'objects' 
        AND policyname = 'Users can upload their own media'
    ) THEN
        CREATE POLICY "Users can upload their own media" ON storage.objects
            FOR INSERT WITH CHECK (
                bucket_id = 'media' AND
                auth.uid()::text = (storage.foldername(name))[1]
            );
    END IF;
END $$;

DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE schemaname = 'storage' 
        AND tablename = 'objects' 
        AND policyname = 'Anyone can view public media'
    ) THEN
        CREATE POLICY "Anyone can view public media" ON storage.objects
            FOR SELECT USING (bucket_id = 'media');
    END IF;
END $$;

-- Success message
SELECT 'Storage policies created successfully!' as result;