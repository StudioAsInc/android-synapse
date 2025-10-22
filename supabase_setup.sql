-- =====================================================
-- Supabase Database Setup for Synapse Social App
-- Complete SQL setup script for production-ready backend
-- 
-- SAFE TO RUN MULTIPLE TIMES: This script uses IF NOT EXISTS
-- and DROP POLICY IF EXISTS to avoid conflicts with existing data.
-- 
-- IMPORTANT: This script includes fixed RLS policies that allow
-- proper user registration for both web and Android apps.
-- 
-- Last Updated: Fixed user registration RLS policy issue
-- =====================================================

-- =====================================================
-- 1. CORE TABLES
-- =====================================================

-- Users Table - Core user information and profiles
CREATE TABLE IF NOT EXISTS users (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    uid TEXT UNIQUE NOT NULL,
    email TEXT,
    username TEXT UNIQUE,
    nickname TEXT,
    display_name TEXT,
    biography TEXT,
    bio TEXT,
    avatar TEXT,
    profile_image_url TEXT,
    avatar_history_type TEXT DEFAULT 'local',
    profile_cover_image TEXT,
    account_premium BOOLEAN DEFAULT false,
    user_level_xp INTEGER DEFAULT 500,
    verify BOOLEAN DEFAULT false,
    account_type TEXT DEFAULT 'user',
    gender TEXT DEFAULT 'hidden',
    banned BOOLEAN DEFAULT false,
    status TEXT DEFAULT 'offline',
    join_date TIMESTAMP DEFAULT NOW(),
    one_signal_player_id TEXT,
    last_seen TIMESTAMP,
    chatting_with TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    followers_count INTEGER DEFAULT 0,
    following_count INTEGER DEFAULT 0,
    posts_count INTEGER DEFAULT 0
);

-- Chats Table - Chat room information
CREATE TABLE IF NOT EXISTS chats (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    chat_id TEXT UNIQUE NOT NULL,
    is_group BOOLEAN DEFAULT false,
    chat_name TEXT,
    chat_description TEXT,
    chat_avatar TEXT,
    created_by TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    last_message TEXT,
    last_message_time BIGINT,
    last_message_sender TEXT,
    participants_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true
);

-- Messages Table - Chat messages with full features
CREATE TABLE IF NOT EXISTS messages (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    chat_id TEXT NOT NULL,
    sender_id TEXT NOT NULL,
    content TEXT NOT NULL,
    message_type TEXT DEFAULT 'text',
    media_url TEXT,
    media_type TEXT,
    media_size BIGINT,
    media_duration INTEGER,
    created_at BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    updated_at BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    is_deleted BOOLEAN DEFAULT false,
    is_edited BOOLEAN DEFAULT false,
    edit_history JSONB,
    reply_to_id UUID REFERENCES messages(id),
    forwarded_from UUID REFERENCES messages(id),
    delivery_status TEXT DEFAULT 'sent',
    read_by JSONB DEFAULT '[]'::jsonb,
    reactions JSONB DEFAULT '{}'::jsonb
);

-- Chat Participants Table - Who's in which chat
CREATE TABLE IF NOT EXISTS chat_participants (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    chat_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    role TEXT DEFAULT 'member',
    joined_at TIMESTAMP DEFAULT NOW(),
    added_by TEXT,
    is_admin BOOLEAN DEFAULT false,
    can_send_messages BOOLEAN DEFAULT true,
    last_read_message_id UUID,
    last_read_at TIMESTAMP,
    notification_settings JSONB DEFAULT '{"muted": false, "sound": true}'::jsonb,
    UNIQUE(chat_id, user_id)
);

-- =====================================================
-- 2. SOCIAL FEATURES TABLES
-- =====================================================

-- Posts Table - Social media posts
CREATE TABLE IF NOT EXISTS posts (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id TEXT NOT NULL,
    content TEXT,
    media_urls TEXT[],
    media_types TEXT[],
    post_type TEXT DEFAULT 'text',
    visibility TEXT DEFAULT 'public',
    location TEXT,
    tags TEXT[],
    mentions TEXT[],
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    likes_count INTEGER DEFAULT 0,
    comments_count INTEGER DEFAULT 0,
    shares_count INTEGER DEFAULT 0,
    views_count INTEGER DEFAULT 0,
    is_deleted BOOLEAN DEFAULT false,
    is_edited BOOLEAN DEFAULT false,
    edit_history JSONB
);

-- Comments Table - Post comments
CREATE TABLE IF NOT EXISTS comments (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL,
    parent_comment_id UUID REFERENCES comments(id),
    content TEXT NOT NULL,
    media_url TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    likes_count INTEGER DEFAULT 0,
    replies_count INTEGER DEFAULT 0,
    is_deleted BOOLEAN DEFAULT false,
    is_edited BOOLEAN DEFAULT false
);

-- Likes Table - Post and comment likes
CREATE TABLE IF NOT EXISTS likes (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id TEXT NOT NULL,
    target_id UUID NOT NULL,
    target_type TEXT NOT NULL CHECK (target_type IN ('post', 'comment')),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, target_id, target_type)
);

-- Follows Table - User following relationships
CREATE TABLE IF NOT EXISTS follows (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    follower_id TEXT NOT NULL,
    following_id TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(follower_id, following_id),
    CHECK (follower_id != following_id)
);

-- Stories Table - User stories/reels
CREATE TABLE IF NOT EXISTS stories (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id TEXT NOT NULL,
    media_url TEXT NOT NULL,
    media_type TEXT NOT NULL,
    content TEXT,
    duration INTEGER DEFAULT 24,
    created_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP DEFAULT NOW() + INTERVAL '24 hours',
    views_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true
);

-- Story Views Table - Who viewed which story
CREATE TABLE IF NOT EXISTS story_views (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    story_id UUID NOT NULL REFERENCES stories(id) ON DELETE CASCADE,
    viewer_id TEXT NOT NULL,
    viewed_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(story_id, viewer_id)
);

-- =====================================================
-- 3. NOTIFICATION SYSTEM
-- =====================================================

-- Notifications Table - All app notifications
CREATE TABLE IF NOT EXISTS notifications (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id TEXT NOT NULL,
    sender_id TEXT,
    type TEXT NOT NULL,
    title TEXT,
    message TEXT NOT NULL,
    data JSONB,
    read BOOLEAN DEFAULT false,
    action_url TEXT,
    priority TEXT DEFAULT 'normal',
    created_at TIMESTAMP DEFAULT NOW(),
    read_at TIMESTAMP,
    expires_at TIMESTAMP
);

-- Push Notification Tokens - For mobile push notifications
CREATE TABLE IF NOT EXISTS push_tokens (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id TEXT NOT NULL,
    token TEXT NOT NULL,
    platform TEXT NOT NULL CHECK (platform IN ('android', 'ios', 'web')),
    device_id TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, token)
);

-- =====================================================
-- 4. PRESENCE AND STATUS
-- =====================================================

-- User Presence Table - Online status and activity
CREATE TABLE IF NOT EXISTS user_presence (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id TEXT UNIQUE NOT NULL,
    is_online BOOLEAN DEFAULT false,
    last_seen BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    activity_status TEXT DEFAULT 'offline',
    current_chat_id TEXT,
    device_info JSONB,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- User Settings Table - App preferences
CREATE TABLE IF NOT EXISTS user_settings (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id TEXT UNIQUE NOT NULL,
    privacy_settings JSONB DEFAULT '{
        "profile_visibility": "public",
        "message_requests": true,
        "show_online_status": true,
        "show_read_receipts": true
    }'::jsonb,
    notification_settings JSONB DEFAULT '{
        "push_enabled": true,
        "email_enabled": true,
        "message_notifications": true,
        "post_notifications": true,
        "follow_notifications": true
    }'::jsonb,
    app_settings JSONB DEFAULT '{
        "theme": "system",
        "language": "en",
        "auto_download_media": true,
        "compress_images": true
    }'::jsonb,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- =====================================================
-- 5. MEDIA AND FILES
-- =====================================================

-- Media Files Table - Track uploaded media
CREATE TABLE IF NOT EXISTS media_files (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id TEXT NOT NULL,
    file_name TEXT NOT NULL,
    file_path TEXT NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type TEXT NOT NULL,
    file_type TEXT NOT NULL,
    bucket_name TEXT NOT NULL,
    is_public BOOLEAN DEFAULT false,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP
);

-- =====================================================
-- 6. REPORTING AND MODERATION
-- =====================================================

-- Reports Table - User reports for content moderation
CREATE TABLE IF NOT EXISTS reports (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    reporter_id TEXT NOT NULL,
    reported_user_id TEXT,
    target_id UUID,
    target_type TEXT CHECK (target_type IN ('user', 'post', 'comment', 'message', 'chat')),
    reason TEXT NOT NULL,
    description TEXT,
    status TEXT DEFAULT 'pending',
    reviewed_by TEXT,
    reviewed_at TIMESTAMP,
    action_taken TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Blocked Users Table - User blocking functionality
CREATE TABLE IF NOT EXISTS blocked_users (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    blocker_id TEXT NOT NULL,
    blocked_id TEXT NOT NULL,
    reason TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(blocker_id, blocked_id),
    CHECK (blocker_id != blocked_id)
);

-- =====================================================
-- 7. ENABLE ROW LEVEL SECURITY
-- =====================================================

ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE chats ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE stories ENABLE ROW LEVEL SECURITY;
ALTER TABLE story_views ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE push_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_presence ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE media_files ENABLE ROW LEVEL SECURITY;
ALTER TABLE reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE blocked_users ENABLE ROW LEVEL SECURITY;

-- =====================================================
-- 8. ROW LEVEL SECURITY POLICIES
-- =====================================================

-- Users Table Policies
DROP POLICY IF EXISTS "Users can view profiles" ON users;
DROP POLICY IF EXISTS "Users can view public profiles" ON users;
CREATE POLICY "Users can view profiles" ON users
    FOR SELECT USING (
        NOT banned AND (
            auth.uid()::text = uid OR 
            account_type = 'public' OR
            EXISTS (SELECT 1 FROM follows WHERE follower_id = auth.uid()::text AND following_id = uid)
        )
    );

DROP POLICY IF EXISTS "Users can update their own data" ON users;
CREATE POLICY "Users can update their own data" ON users
    FOR UPDATE USING (auth.uid()::text = uid);

-- FIXED: Allow user registration and profile creation
-- This policy allows users to create their profile during signup process
-- More permissive policy to handle signup flow properly
DROP POLICY IF EXISTS "Users can insert their own data" ON users;
DROP POLICY IF EXISTS "Allow user registration" ON users;
CREATE POLICY "Allow user registration" ON users
    FOR INSERT WITH CHECK (
        -- Allow any authenticated user to insert their own profile
        -- This is necessary because during signup, the user is authenticated
        -- but the profile doesn't exist yet
        auth.uid() IS NOT NULL AND auth.uid()::text = uid
    );

-- Messages Policies
DROP POLICY IF EXISTS "Users can view messages in their chats" ON messages;
CREATE POLICY "Users can view messages in their chats" ON messages
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM chat_participants 
            WHERE chat_participants.chat_id = messages.chat_id 
            AND chat_participants.user_id = auth.uid()::text
        )
    );

DROP POLICY IF EXISTS "Users can send messages to their chats" ON messages;
CREATE POLICY "Users can send messages to their chats" ON messages
    FOR INSERT WITH CHECK (
        auth.uid()::text = sender_id AND
        EXISTS (
            SELECT 1 FROM chat_participants 
            WHERE chat_participants.chat_id = messages.chat_id 
            AND chat_participants.user_id = auth.uid()::text
            AND can_send_messages = true
        )
    );

DROP POLICY IF EXISTS "Users can update their own messages" ON messages;
CREATE POLICY "Users can update their own messages" ON messages
    FOR UPDATE USING (auth.uid()::text = sender_id);

-- Chat Participants Policies
DROP POLICY IF EXISTS "Users can view chat participants for their chats" ON chat_participants;
CREATE POLICY "Users can view chat participants for their chats" ON chat_participants
    FOR SELECT USING (
        user_id = auth.uid()::text OR
        EXISTS (
            SELECT 1 FROM chat_participants cp2
            WHERE cp2.chat_id = chat_participants.chat_id 
            AND cp2.user_id = auth.uid()::text
        )
    );

DROP POLICY IF EXISTS "Admins can manage chat participants" ON chat_participants;
CREATE POLICY "Admins can manage chat participants" ON chat_participants
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM chat_participants 
            WHERE chat_id = chat_participants.chat_id 
            AND user_id = auth.uid()::text 
            AND is_admin = true
        )
    );

-- Posts Policies
DROP POLICY IF EXISTS "Users can view public posts" ON posts;
CREATE POLICY "Users can view public posts" ON posts
    FOR SELECT USING (
        NOT is_deleted AND (
            visibility = 'public' OR
            user_id = auth.uid()::text OR
            (visibility = 'followers' AND EXISTS (
                SELECT 1 FROM follows WHERE follower_id = auth.uid()::text AND following_id = user_id
            ))
        )
    );

DROP POLICY IF EXISTS "Users can create their own posts" ON posts;
CREATE POLICY "Users can create their own posts" ON posts
    FOR INSERT WITH CHECK (auth.uid()::text = user_id);

DROP POLICY IF EXISTS "Users can update their own posts" ON posts;
CREATE POLICY "Users can update their own posts" ON posts
    FOR UPDATE USING (auth.uid()::text = user_id);

DROP POLICY IF EXISTS "Users can delete their own posts" ON posts;
CREATE POLICY "Users can delete their own posts" ON posts
    FOR DELETE USING (auth.uid()::text = user_id);

-- Comments Policies
DROP POLICY IF EXISTS "Users can view comments on visible posts" ON comments;
CREATE POLICY "Users can view comments on visible posts" ON comments
    FOR SELECT USING (
        NOT is_deleted AND
        EXISTS (
            SELECT 1 FROM posts 
            WHERE posts.id = comments.post_id 
            AND NOT posts.is_deleted
        )
    );

DROP POLICY IF EXISTS "Users can create comments" ON comments;
CREATE POLICY "Users can create comments" ON comments
    FOR INSERT WITH CHECK (auth.uid()::text = user_id);

DROP POLICY IF EXISTS "Users can update their own comments" ON comments;
CREATE POLICY "Users can update their own comments" ON comments
    FOR UPDATE USING (auth.uid()::text = user_id);

-- Likes Policies
DROP POLICY IF EXISTS "Users can view likes" ON likes;
CREATE POLICY "Users can view likes" ON likes
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users can manage their own likes" ON likes;
CREATE POLICY "Users can manage their own likes" ON likes
    FOR ALL USING (auth.uid()::text = user_id);

-- Follows Policies
DROP POLICY IF EXISTS "Users can view follows" ON follows;
CREATE POLICY "Users can view follows" ON follows
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users can manage their own follows" ON follows;
CREATE POLICY "Users can manage their own follows" ON follows
    FOR ALL USING (auth.uid()::text = follower_id);

-- Stories Policies
DROP POLICY IF EXISTS "Users can view active stories" ON stories;
CREATE POLICY "Users can view active stories" ON stories
    FOR SELECT USING (
        is_active AND expires_at > NOW() AND (
            user_id = auth.uid()::text OR
            EXISTS (SELECT 1 FROM follows WHERE follower_id = auth.uid()::text AND following_id = user_id)
        )
    );

DROP POLICY IF EXISTS "Users can manage their own stories" ON stories;
CREATE POLICY "Users can manage their own stories" ON stories
    FOR ALL USING (auth.uid()::text = user_id);

-- Notifications Policies
DROP POLICY IF EXISTS "Users can view their own notifications" ON notifications;
CREATE POLICY "Users can view their own notifications" ON notifications
    FOR SELECT USING (auth.uid()::text = user_id);

DROP POLICY IF EXISTS "Users can update their own notifications" ON notifications;
CREATE POLICY "Users can update their own notifications" ON notifications
    FOR UPDATE USING (auth.uid()::text = user_id);

-- User Settings Policies
DROP POLICY IF EXISTS "Users can manage their own settings" ON user_settings;
CREATE POLICY "Users can manage their own settings" ON user_settings
    FOR ALL USING (auth.uid()::text = user_id);

-- User Presence Policies
DROP POLICY IF EXISTS "Users can view presence of followed users" ON user_presence;
CREATE POLICY "Users can view presence of followed users" ON user_presence
    FOR SELECT USING (
        user_id = auth.uid()::text OR
        EXISTS (SELECT 1 FROM follows WHERE follower_id = auth.uid()::text AND following_id = user_id)
    );

DROP POLICY IF EXISTS "Users can update their own presence" ON user_presence;
CREATE POLICY "Users can update their own presence" ON user_presence
    FOR ALL USING (auth.uid()::text = user_id);

-- Media Files Policies
DROP POLICY IF EXISTS "Users can view public media or their own" ON media_files;
CREATE POLICY "Users can view public media or their own" ON media_files
    FOR SELECT USING (
        is_public = true OR auth.uid()::text = user_id
    );

DROP POLICY IF EXISTS "Users can manage their own media" ON media_files;
CREATE POLICY "Users can manage their own media" ON media_files
    FOR ALL USING (auth.uid()::text = user_id);

-- Reports Policies
DROP POLICY IF EXISTS "Users can create reports" ON reports;
CREATE POLICY "Users can create reports" ON reports
    FOR INSERT WITH CHECK (auth.uid()::text = reporter_id);

DROP POLICY IF EXISTS "Users can view their own reports" ON reports;
CREATE POLICY "Users can view their own reports" ON reports
    FOR SELECT USING (auth.uid()::text = reporter_id);

-- Blocked Users Policies
DROP POLICY IF EXISTS "Users can manage their own blocks" ON blocked_users;
CREATE POLICY "Users can manage their own blocks" ON blocked_users
    FOR ALL USING (auth.uid()::text = blocker_id);

-- =====================================================
-- 9. PERFORMANCE INDEXES
-- =====================================================

-- Users Table Indexes
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username) WHERE username IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_uid ON users(uid);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email) WHERE email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_banned ON users(banned);

-- Messages Table Indexes
CREATE INDEX IF NOT EXISTS idx_messages_chat_id ON messages(chat_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON messages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_chat_created ON messages(chat_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_reply_to ON messages(reply_to_id) WHERE reply_to_id IS NOT NULL;

-- Chat Participants Indexes
CREATE INDEX IF NOT EXISTS idx_chat_participants_chat_id ON chat_participants(chat_id);
CREATE INDEX IF NOT EXISTS idx_chat_participants_user_id ON chat_participants(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_participants_role ON chat_participants(role);

-- Posts Table Indexes
CREATE INDEX IF NOT EXISTS idx_posts_user_id ON posts(user_id);
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_visibility ON posts(visibility);
CREATE INDEX IF NOT EXISTS idx_posts_user_created ON posts(user_id, created_at DESC);

-- Comments Table Indexes
CREATE INDEX IF NOT EXISTS idx_comments_post_id ON comments(post_id);
CREATE INDEX IF NOT EXISTS idx_comments_user_id ON comments(user_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent ON comments(parent_comment_id) WHERE parent_comment_id IS NOT NULL;

-- Likes Table Indexes
CREATE INDEX IF NOT EXISTS idx_likes_target ON likes(target_id, target_type);
CREATE INDEX IF NOT EXISTS idx_likes_user ON likes(user_id);

-- Follows Table Indexes
CREATE INDEX IF NOT EXISTS idx_follows_follower ON follows(follower_id);
CREATE INDEX IF NOT EXISTS idx_follows_following ON follows(following_id);

-- Notifications Table Indexes
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_read ON notifications(read);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);

-- Stories Table Indexes
CREATE INDEX IF NOT EXISTS idx_stories_user_id ON stories(user_id);
CREATE INDEX IF NOT EXISTS idx_stories_active ON stories(is_active, expires_at);

-- User Presence Indexes
CREATE INDEX IF NOT EXISTS idx_user_presence_user_id ON user_presence(user_id);
CREATE INDEX IF NOT EXISTS idx_user_presence_online ON user_presence(is_online);

-- =====================================================
-- 10. FUNCTIONS AND TRIGGERS
-- =====================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at triggers to relevant tables
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_chats_updated_at ON chats;
CREATE TRIGGER update_chats_updated_at BEFORE UPDATE ON chats
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_posts_updated_at ON posts;
CREATE TRIGGER update_posts_updated_at BEFORE UPDATE ON posts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_user_settings_updated_at ON user_settings;
CREATE TRIGGER update_user_settings_updated_at BEFORE UPDATE ON user_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function to update user stats
CREATE OR REPLACE FUNCTION update_user_stats()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_TABLE_NAME = 'posts' THEN
        IF TG_OP = 'INSERT' THEN
            UPDATE users SET posts_count = posts_count + 1 WHERE uid = NEW.user_id;
        ELSIF TG_OP = 'DELETE' THEN
            UPDATE users SET posts_count = posts_count - 1 WHERE uid = OLD.user_id;
        END IF;
    ELSIF TG_TABLE_NAME = 'follows' THEN
        IF TG_OP = 'INSERT' THEN
            UPDATE users SET followers_count = followers_count + 1 WHERE uid = NEW.following_id;
            UPDATE users SET following_count = following_count + 1 WHERE uid = NEW.follower_id;
        ELSIF TG_OP = 'DELETE' THEN
            UPDATE users SET followers_count = followers_count - 1 WHERE uid = OLD.following_id;
            UPDATE users SET following_count = following_count - 1 WHERE uid = OLD.follower_id;
        END IF;
    END IF;
    
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ language 'plpgsql';

-- Apply user stats triggers
DROP TRIGGER IF EXISTS update_posts_count ON posts;
CREATE TRIGGER update_posts_count AFTER INSERT OR DELETE ON posts
    FOR EACH ROW EXECUTE FUNCTION update_user_stats();

DROP TRIGGER IF EXISTS update_follow_counts ON follows;
CREATE TRIGGER update_follow_counts AFTER INSERT OR DELETE ON follows
    FOR EACH ROW EXECUTE FUNCTION update_user_stats();

-- Function to update post stats
CREATE OR REPLACE FUNCTION update_post_stats()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_TABLE_NAME = 'likes' THEN
        IF NEW.target_type = 'post' THEN
            IF TG_OP = 'INSERT' THEN
                UPDATE posts SET likes_count = likes_count + 1 WHERE id = NEW.target_id;
            ELSIF TG_OP = 'DELETE' THEN
                UPDATE posts SET likes_count = likes_count - 1 WHERE id = OLD.target_id;
            END IF;
        END IF;
    ELSIF TG_TABLE_NAME = 'comments' THEN
        IF TG_OP = 'INSERT' THEN
            UPDATE posts SET comments_count = comments_count + 1 WHERE id = NEW.post_id;
        ELSIF TG_OP = 'DELETE' THEN
            UPDATE posts SET comments_count = comments_count - 1 WHERE id = OLD.post_id;
        END IF;
    END IF;
    
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ language 'plpgsql';

-- Apply post stats triggers
DROP TRIGGER IF EXISTS update_likes_count ON likes;
CREATE TRIGGER update_likes_count AFTER INSERT OR DELETE ON likes
    FOR EACH ROW EXECUTE FUNCTION update_post_stats();

DROP TRIGGER IF EXISTS update_comments_count ON comments;
CREATE TRIGGER update_comments_count AFTER INSERT OR DELETE ON comments
    FOR EACH ROW EXECUTE FUNCTION update_post_stats();

-- =====================================================
-- 11. STORAGE BUCKETS AND POLICIES
-- =====================================================

-- Create storage buckets (run these in Supabase Dashboard or via API)
-- 
-- Bucket: 'avatars' (Public, 5MB, images only)
-- Bucket: 'media' (Public, 50MB, images/videos/audio)  
-- Bucket: 'posts' (Public, 100MB, images/videos)
-- Bucket: 'chat-attachments' (Private, 50MB, all file types)

-- Storage policies for file access control
-- Note: These policies may already exist if you've run this script before
-- If you get "policy already exists" errors, that's normal and can be ignored

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
        AND policyname = 'Users can update their own profile photos'
    ) THEN
        CREATE POLICY "Users can update their own profile photos" ON storage.objects
            FOR UPDATE USING (
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
        AND policyname = 'Users can delete their own profile photos'
    ) THEN
        CREATE POLICY "Users can delete their own profile photos" ON storage.objects
            FOR DELETE USING (
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
        AND policyname = 'Users can update their own media'
    ) THEN
        CREATE POLICY "Users can update their own media" ON storage.objects
            FOR UPDATE USING (
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
        AND policyname = 'Users can delete their own media'
    ) THEN
        CREATE POLICY "Users can delete their own media" ON storage.objects
            FOR DELETE USING (
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

-- Post Attachments Policies
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE schemaname = 'storage' 
        AND tablename = 'objects' 
        AND policyname = 'Users can upload post attachments'
    ) THEN
        CREATE POLICY "Users can upload post attachments" ON storage.objects
            FOR INSERT WITH CHECK (
                bucket_id = 'posts' AND
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
        AND policyname = 'Users can manage their own post attachments'
    ) THEN
        CREATE POLICY "Users can manage their own post attachments" ON storage.objects
            FOR ALL USING (
                bucket_id = 'posts' AND
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
        AND policyname = 'Anyone can view post attachments'
    ) THEN
        CREATE POLICY "Anyone can view post attachments" ON storage.objects
            FOR SELECT USING (bucket_id = 'posts');
    END IF;
END $$;

-- Chat Attachments Policies (Private bucket)
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE schemaname = 'storage' 
        AND tablename = 'objects' 
        AND policyname = 'Users can upload chat attachments'
    ) THEN
        CREATE POLICY "Users can upload chat attachments" ON storage.objects
            FOR INSERT WITH CHECK (
                bucket_id = 'chat-attachments' AND
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
        AND policyname = 'Users can view chat attachments they have access to'
    ) THEN
        CREATE POLICY "Users can view chat attachments they have access to" ON storage.objects
            FOR SELECT USING (
                bucket_id = 'chat-attachments' AND (
                    auth.uid()::text = (storage.foldername(name))[1] OR
                    EXISTS (
                        SELECT 1 FROM messages m
                        JOIN chat_participants cp ON m.chat_id = cp.chat_id
                        WHERE m.media_url LIKE '%' || name || '%'
                        AND cp.user_id = auth.uid()::text
                    )
                )
            );
    END IF;
END $$;

DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE schemaname = 'storage' 
        AND tablename = 'objects' 
        AND policyname = 'Users can delete their own chat attachments'
    ) THEN
        CREATE POLICY "Users can delete their own chat attachments" ON storage.objects
            FOR DELETE USING (
                bucket_id = 'chat-attachments' AND
                auth.uid()::text = (storage.foldername(name))[1]
            );
    END IF;
END $$;

-- =====================================================
-- 12. ENABLE REAL-TIME SUBSCRIPTIONS
-- =====================================================

-- Enable real-time for tables (safe to run multiple times)
DO $$
BEGIN
    -- Enable real-time for messages (CRITICAL for chat functionality)
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE messages;
    EXCEPTION WHEN duplicate_object THEN
        -- Table already added to publication, skip
        NULL;
    END;

    -- Enable real-time for chat participants (user join/leave events)
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE chat_participants;
    EXCEPTION WHEN duplicate_object THEN
        NULL;
    END;

    -- Enable real-time for user presence (online status)
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE user_presence;
    EXCEPTION WHEN duplicate_object THEN
        NULL;
    END;

    -- Enable real-time for notifications (live notifications)
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE notifications;
    EXCEPTION WHEN duplicate_object THEN
        NULL;
    END;

    -- Enable real-time for posts (live feed updates)
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE posts;
    EXCEPTION WHEN duplicate_object THEN
        NULL;
    END;

    -- Enable real-time for likes (live like counts)
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE likes;
    EXCEPTION WHEN duplicate_object THEN
        NULL;
    END;

    -- Enable real-time for comments (live comments)
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE comments;
    EXCEPTION WHEN duplicate_object THEN
        NULL;
    END;

    -- Enable real-time for follows (follow notifications)
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE follows;
    EXCEPTION WHEN duplicate_object THEN
        NULL;
    END;

    -- Enable real-time for stories (story updates)
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE stories;
    EXCEPTION WHEN duplicate_object THEN
        NULL;
    END;
END $$;

-- =====================================================
-- 13. SAMPLE DATA (Optional - for testing)
-- =====================================================

-- Insert sample notification types for reference
INSERT INTO notifications (user_id, type, title, message, data) VALUES
('system', 'welcome', 'Welcome to Synapse Social!', 'Thanks for joining our community.', '{"action": "onboarding"}'),
('system', 'feature', 'New Features Available', 'Check out the latest updates in the app.', '{"version": "1.0.0"}')
ON CONFLICT DO NOTHING;

-- =====================================================
-- SETUP COMPLETE
-- =====================================================

-- Your Supabase database is now ready for the Synapse Social app!
-- 
-- IMPORTANT: This script fixes the RLS policy that was causing:
-- "new row violates row-level security policy for table users"
-- 
-- Next steps:
-- 1. Create storage buckets in Supabase Dashboard
-- 2. Configure authentication providers  
-- 3. Update your app's environment variables with:
--    - SUPABASE_URL
--    - SUPABASE_ANON_KEY
-- 4. Test the connection from your Android app
--
-- To verify the fix worked, you can test with:
-- SELECT * FROM pg_policies WHERE tablename = 'users' AND policyname = 'Allow user registration';



CREATE POLICY "Users can view chats they are part of" ON chats
FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM chat_participants
        WHERE chat_participants.chat_id = chats.chat_id
        AND chat_participants.user_id = auth.uid()::text
    )
);