-- =====================================================
-- Supabase Database Setup for Synapse Social App
-- Complete SQL setup script for production-ready backend
-- =====================================================

-- =====================================================
-- 1. CORE TABLES
-- =====================================================

-- Users Table - Core user information and profiles
CREATE TABLE users (
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
CREATE TABLE chats (
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
CREATE TABLE messages (
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
CREATE TABLE chat_participants (
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
CREATE TABLE posts (
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
CREATE TABLE comments (
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
CREATE TABLE likes (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id TEXT NOT NULL,
    target_id UUID NOT NULL,
    target_type TEXT NOT NULL CHECK (target_type IN ('post', 'comment')),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, target_id, target_type)
);

-- Follows Table - User following relationships
CREATE TABLE follows (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    follower_id TEXT NOT NULL,
    following_id TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(follower_id, following_id),
    CHECK (follower_id != following_id)
);

-- Stories Table - User stories/reels
CREATE TABLE stories (
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
CREATE TABLE story_views (
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
CREATE TABLE notifications (
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
CREATE TABLE push_tokens (
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
CREATE TABLE user_presence (
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
CREATE TABLE user_settings (
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
CREATE TABLE media_files (
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
CREATE TABLE reports (
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
CREATE TABLE blocked_users (
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
CREATE POLICY "Users can view public profiles" ON users
    FOR SELECT USING (
        NOT banned AND (
            auth.uid()::text = uid OR 
            account_type = 'public' OR
            EXISTS (SELECT 1 FROM follows WHERE follower_id = auth.uid()::text AND following_id = uid)
        )
    );

CREATE POLICY "Users can update their own data" ON users
    FOR UPDATE USING (auth.uid()::text = uid);

CREATE POLICY "Users can insert their own data" ON users
    FOR INSERT WITH CHECK (auth.uid()::text = uid);

-- Messages Policies
CREATE POLICY "Users can view messages in their chats" ON messages
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM chat_participants 
            WHERE chat_participants.chat_id = messages.chat_id 
            AND chat_participants.user_id = auth.uid()::text
        )
    );

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

CREATE POLICY "Users can update their own messages" ON messages
    FOR UPDATE USING (auth.uid()::text = sender_id);

-- Chat Participants Policies
CREATE POLICY "Users can view chat participants for their chats" ON chat_participants
    FOR SELECT USING (
        user_id = auth.uid()::text OR
        EXISTS (
            SELECT 1 FROM chat_participants cp2
            WHERE cp2.chat_id = chat_participants.chat_id 
            AND cp2.user_id = auth.uid()::text
        )
    );

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

CREATE POLICY "Users can create their own posts" ON posts
    FOR INSERT WITH CHECK (auth.uid()::text = user_id);

CREATE POLICY "Users can update their own posts" ON posts
    FOR UPDATE USING (auth.uid()::text = user_id);

CREATE POLICY "Users can delete their own posts" ON posts
    FOR DELETE USING (auth.uid()::text = user_id);

-- Comments Policies
CREATE POLICY "Users can view comments on visible posts" ON comments
    FOR SELECT USING (
        NOT is_deleted AND
        EXISTS (
            SELECT 1 FROM posts 
            WHERE posts.id = comments.post_id 
            AND NOT posts.is_deleted
        )
    );

CREATE POLICY "Users can create comments" ON comments
    FOR INSERT WITH CHECK (auth.uid()::text = user_id);

CREATE POLICY "Users can update their own comments" ON comments
    FOR UPDATE USING (auth.uid()::text = user_id);

-- Likes Policies
CREATE POLICY "Users can view likes" ON likes
    FOR SELECT USING (true);

CREATE POLICY "Users can manage their own likes" ON likes
    FOR ALL USING (auth.uid()::text = user_id);

-- Follows Policies
CREATE POLICY "Users can view follows" ON follows
    FOR SELECT USING (true);

CREATE POLICY "Users can manage their own follows" ON follows
    FOR ALL USING (auth.uid()::text = follower_id);

-- Stories Policies
CREATE POLICY "Users can view active stories" ON stories
    FOR SELECT USING (
        is_active AND expires_at > NOW() AND (
            user_id = auth.uid()::text OR
            EXISTS (SELECT 1 FROM follows WHERE follower_id = auth.uid()::text AND following_id = user_id)
        )
    );

CREATE POLICY "Users can manage their own stories" ON stories
    FOR ALL USING (auth.uid()::text = user_id);

-- Notifications Policies
CREATE POLICY "Users can view their own notifications" ON notifications
    FOR SELECT USING (auth.uid()::text = user_id);

CREATE POLICY "Users can update their own notifications" ON notifications
    FOR UPDATE USING (auth.uid()::text = user_id);

-- User Settings Policies
CREATE POLICY "Users can manage their own settings" ON user_settings
    FOR ALL USING (auth.uid()::text = user_id);

-- User Presence Policies
CREATE POLICY "Users can view presence of followed users" ON user_presence
    FOR SELECT USING (
        user_id = auth.uid()::text OR
        EXISTS (SELECT 1 FROM follows WHERE follower_id = auth.uid()::text AND following_id = user_id)
    );

CREATE POLICY "Users can update their own presence" ON user_presence
    FOR ALL USING (auth.uid()::text = user_id);

-- Media Files Policies
CREATE POLICY "Users can view public media or their own" ON media_files
    FOR SELECT USING (
        is_public = true OR auth.uid()::text = user_id
    );

CREATE POLICY "Users can manage their own media" ON media_files
    FOR ALL USING (auth.uid()::text = user_id);

-- Reports Policies
CREATE POLICY "Users can create reports" ON reports
    FOR INSERT WITH CHECK (auth.uid()::text = reporter_id);

CREATE POLICY "Users can view their own reports" ON reports
    FOR SELECT USING (auth.uid()::text = reporter_id);

-- Blocked Users Policies
CREATE POLICY "Users can manage their own blocks" ON blocked_users
    FOR ALL USING (auth.uid()::text = blocker_id);

-- =====================================================
-- 9. PERFORMANCE INDEXES
-- =====================================================

-- Users Table Indexes
CREATE INDEX idx_users_username ON users(username) WHERE username IS NOT NULL;
CREATE INDEX idx_users_uid ON users(uid);
CREATE INDEX idx_users_email ON users(email) WHERE email IS NOT NULL;
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_banned ON users(banned);

-- Messages Table Indexes
CREATE INDEX idx_messages_chat_id ON messages(chat_id);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_messages_created_at ON messages(created_at DESC);
CREATE INDEX idx_messages_chat_created ON messages(chat_id, created_at DESC);
CREATE INDEX idx_messages_reply_to ON messages(reply_to_id) WHERE reply_to_id IS NOT NULL;

-- Chat Participants Indexes
CREATE INDEX idx_chat_participants_chat_id ON chat_participants(chat_id);
CREATE INDEX idx_chat_participants_user_id ON chat_participants(user_id);
CREATE INDEX idx_chat_participants_role ON chat_participants(role);

-- Posts Table Indexes
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX idx_posts_visibility ON posts(visibility);
CREATE INDEX idx_posts_user_created ON posts(user_id, created_at DESC);

-- Comments Table Indexes
CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_parent ON comments(parent_comment_id) WHERE parent_comment_id IS NOT NULL;

-- Likes Table Indexes
CREATE INDEX idx_likes_target ON likes(target_id, target_type);
CREATE INDEX idx_likes_user ON likes(user_id);

-- Follows Table Indexes
CREATE INDEX idx_follows_follower ON follows(follower_id);
CREATE INDEX idx_follows_following ON follows(following_id);

-- Notifications Table Indexes
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_read ON notifications(read);
CREATE INDEX idx_notifications_type ON notifications(type);

-- Stories Table Indexes
CREATE INDEX idx_stories_user_id ON stories(user_id);
CREATE INDEX idx_stories_active ON stories(is_active, expires_at);

-- User Presence Indexes
CREATE INDEX idx_user_presence_user_id ON user_presence(user_id);
CREATE INDEX idx_user_presence_online ON user_presence(is_online);

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
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_chats_updated_at BEFORE UPDATE ON chats
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_posts_updated_at BEFORE UPDATE ON posts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

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
CREATE TRIGGER update_posts_count AFTER INSERT OR DELETE ON posts
    FOR EACH ROW EXECUTE FUNCTION update_user_stats();

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
CREATE TRIGGER update_likes_count AFTER INSERT OR DELETE ON likes
    FOR EACH ROW EXECUTE FUNCTION update_post_stats();

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
-- Profile Photos Policies
CREATE POLICY "Users can upload their own profile photos" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'avatars' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can update their own profile photos" ON storage.objects
    FOR UPDATE USING (
        bucket_id = 'avatars' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can delete their own profile photos" ON storage.objects
    FOR DELETE USING (
        bucket_id = 'avatars' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Anyone can view profile photos" ON storage.objects
    FOR SELECT USING (bucket_id = 'avatars');

-- Media Files Policies
CREATE POLICY "Users can upload their own media" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'media' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can update their own media" ON storage.objects
    FOR UPDATE USING (
        bucket_id = 'media' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can delete their own media" ON storage.objects
    FOR DELETE USING (
        bucket_id = 'media' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Anyone can view public media" ON storage.objects
    FOR SELECT USING (bucket_id = 'media');

-- Post Attachments Policies
CREATE POLICY "Users can upload post attachments" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'posts' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can manage their own post attachments" ON storage.objects
    FOR ALL USING (
        bucket_id = 'posts' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Anyone can view post attachments" ON storage.objects
    FOR SELECT USING (bucket_id = 'posts');

-- Chat Attachments Policies (Private bucket)
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
                SELECT 1 FROM messages m
                JOIN chat_participants cp ON m.chat_id = cp.chat_id
                WHERE m.media_url LIKE '%' || name || '%'
                AND cp.user_id = auth.uid()::text
            )
        )
    );

CREATE POLICY "Users can delete their own chat attachments" ON storage.objects
    FOR DELETE USING (
        bucket_id = 'chat-attachments' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

-- =====================================================
-- 12. SAMPLE DATA (Optional - for testing)
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
-- Next steps:
-- 1. Create storage buckets in Supabase Dashboard
-- 2. Configure authentication providers
-- 3. Update your app's environment variables with:
--    - SUPABASE_URL
--    - SUPABASE_ANON_KEY
-- 4. Test the connection from your Android app