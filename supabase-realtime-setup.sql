-- Real-time Subscriptions Setup for Synapse Social Media App
-- Run these SQL commands to enable real-time features

-- =============================================
-- ENABLE REAL-TIME ON TABLES
-- =============================================

-- Enable real-time for messages (chat functionality)
ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;

-- Enable real-time for message reactions
ALTER PUBLICATION supabase_realtime ADD TABLE public.message_reactions;

-- Enable real-time for chat participants (user join/leave events)
ALTER PUBLICATION supabase_realtime ADD TABLE public.chat_participants;

-- Enable real-time for user status updates
ALTER PUBLICATION supabase_realtime ADD TABLE public.users;

-- Enable real-time for notifications
ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;

-- Enable real-time for new posts (optional - for live feed)
ALTER PUBLICATION supabase_realtime ADD TABLE public.posts;

-- Enable real-time for likes (optional - for live like counts)
ALTER PUBLICATION supabase_realtime ADD TABLE public.likes;

-- Enable real-time for comments (optional - for live comments)
ALTER PUBLICATION supabase_realtime ADD TABLE public.comments;

-- Enable real-time for follows (for live follow notifications)
ALTER PUBLICATION supabase_realtime ADD TABLE public.follows;

-- =============================================
-- REAL-TIME HELPER FUNCTIONS
-- =============================================

-- Function to update user online status
CREATE OR REPLACE FUNCTION update_user_status(
    user_status TEXT DEFAULT 'online'
)
RETURNS void AS $$
BEGIN
    UPDATE public.users 
    SET 
        status = user_status,
        last_seen = NOW()
    WHERE uid = auth.uid();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get online users in a chat
CREATE OR REPLACE FUNCTION get_online_chat_users(chat_uuid UUID)
RETURNS TABLE(
    user_id UUID,
    username TEXT,
    display_name TEXT,
    profile_photo_url TEXT,
    status TEXT,
    last_seen TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        u.uid,
        u.username,
        u.display_name,
        u.profile_photo_url,
        u.status,
        u.last_seen
    FROM public.users u
    JOIN public.chat_participants cp ON u.uid = cp.user_id
    WHERE cp.chat_id = chat_uuid
    AND cp.left_at IS NULL
    AND u.status IN ('online', 'away')
    ORDER BY u.last_seen DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to mark messages as read
CREATE OR REPLACE FUNCTION mark_messages_as_read(
    chat_uuid UUID,
    last_read_message_id UUID DEFAULT NULL
)
RETURNS void AS $$
BEGIN
    -- This would typically update a separate read_receipts table
    -- For now, we'll just update the user's last_seen in the chat
    UPDATE public.chat_participants
    SET updated_at = NOW()
    WHERE chat_id = chat_uuid 
    AND user_id = auth.uid();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================
-- NOTIFICATION TRIGGERS FOR REAL-TIME EVENTS
-- =============================================

-- Function to create notification for new message
CREATE OR REPLACE FUNCTION notify_new_message()
RETURNS TRIGGER AS $$
DECLARE
    recipient RECORD;
    sender_info RECORD;
    chat_info RECORD;
BEGIN
    -- Get sender information
    SELECT username, display_name INTO sender_info
    FROM public.users WHERE uid = NEW.sender_id;
    
    -- Get chat information
    SELECT type, name INTO chat_info
    FROM public.chats WHERE id = NEW.chat_id;
    
    -- Create notifications for all chat participants except sender
    FOR recipient IN 
        SELECT cp.user_id, u.username
        FROM public.chat_participants cp
        JOIN public.users u ON cp.user_id = u.uid
        WHERE cp.chat_id = NEW.chat_id 
        AND cp.user_id != NEW.sender_id
        AND cp.left_at IS NULL
    LOOP
        INSERT INTO public.notifications (
            user_id,
            type,
            title,
            body,
            data
        ) VALUES (
            recipient.user_id,
            'message',
            CASE 
                WHEN chat_info.type = 'group' THEN 
                    COALESCE(chat_info.name, 'Group Chat')
                ELSE 
                    COALESCE(sender_info.display_name, sender_info.username)
            END,
            CASE 
                WHEN NEW.message_type = 'text' THEN 
                    COALESCE(NEW.content, 'Sent a message')
                WHEN NEW.message_type = 'image' THEN 
                    'Sent an image'
                WHEN NEW.message_type = 'video' THEN 
                    'Sent a video'
                WHEN NEW.message_type = 'audio' THEN 
                    'Sent an audio message'
                ELSE 
                    'Sent a file'
            END,
            jsonb_build_object(
                'chat_id', NEW.chat_id,
                'message_id', NEW.id,
                'sender_id', NEW.sender_id,
                'sender_username', sender_info.username
            )
        );
    END LOOP;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER notify_new_message_trigger
    AFTER INSERT ON public.messages
    FOR EACH ROW EXECUTE FUNCTION notify_new_message();

-- Function to create notification for new like
CREATE OR REPLACE FUNCTION notify_new_like()
RETURNS TRIGGER AS $$
DECLARE
    content_owner_id UUID;
    liker_info RECORD;
    content_type TEXT;
    content_preview TEXT;
BEGIN
    -- Get liker information
    SELECT username, display_name INTO liker_info
    FROM public.users WHERE uid = NEW.user_id;
    
    IF NEW.post_id IS NOT NULL THEN
        -- Like on post
        SELECT user_id, LEFT(content, 50) INTO content_owner_id, content_preview
        FROM public.posts WHERE id = NEW.post_id;
        content_type := 'post';
    ELSIF NEW.comment_id IS NOT NULL THEN
        -- Like on comment
        SELECT user_id, LEFT(content, 50) INTO content_owner_id, content_preview
        FROM public.comments WHERE id = NEW.comment_id;
        content_type := 'comment';
    END IF;
    
    -- Don't notify if user liked their own content
    IF content_owner_id != NEW.user_id THEN
        INSERT INTO public.notifications (
            user_id,
            type,
            title,
            body,
            data
        ) VALUES (
            content_owner_id,
            'like',
            COALESCE(liker_info.display_name, liker_info.username),
            'liked your ' || content_type,
            jsonb_build_object(
                'liker_id', NEW.user_id,
                'liker_username', liker_info.username,
                content_type || '_id', COALESCE(NEW.post_id, NEW.comment_id),
                'content_preview', content_preview
            )
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER notify_new_like_trigger
    AFTER INSERT ON public.likes
    FOR EACH ROW EXECUTE FUNCTION notify_new_like();

-- Function to create notification for new comment
CREATE OR REPLACE FUNCTION notify_new_comment()
RETURNS TRIGGER AS $$
DECLARE
    post_owner_id UUID;
    commenter_info RECORD;
    post_preview TEXT;
BEGIN
    -- Get commenter information
    SELECT username, display_name INTO commenter_info
    FROM public.users WHERE uid = NEW.user_id;
    
    -- Get post owner and preview
    SELECT user_id, LEFT(content, 50) INTO post_owner_id, post_preview
    FROM public.posts WHERE id = NEW.post_id;
    
    -- Don't notify if user commented on their own post
    IF post_owner_id != NEW.user_id THEN
        INSERT INTO public.notifications (
            user_id,
            type,
            title,
            body,
            data
        ) VALUES (
            post_owner_id,
            'comment',
            COALESCE(commenter_info.display_name, commenter_info.username),
            'commented on your post',
            jsonb_build_object(
                'commenter_id', NEW.user_id,
                'commenter_username', commenter_info.username,
                'post_id', NEW.post_id,
                'comment_id', NEW.id,
                'post_preview', post_preview,
                'comment_preview', LEFT(NEW.content, 50)
            )
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER notify_new_comment_trigger
    AFTER INSERT ON public.comments
    FOR EACH ROW EXECUTE FUNCTION notify_new_comment();

-- Function to create notification for new follow
CREATE OR REPLACE FUNCTION notify_new_follow()
RETURNS TRIGGER AS $$
DECLARE
    follower_info RECORD;
BEGIN
    -- Only notify for new follows, not updates
    IF TG_OP = 'INSERT' THEN
        -- Get follower information
        SELECT username, display_name INTO follower_info
        FROM public.users WHERE uid = NEW.follower_id;
        
        INSERT INTO public.notifications (
            user_id,
            type,
            title,
            body,
            data
        ) VALUES (
            NEW.following_id,
            'follow',
            COALESCE(follower_info.display_name, follower_info.username),
            CASE 
                WHEN NEW.status = 'pending' THEN 'requested to follow you'
                ELSE 'started following you'
            END,
            jsonb_build_object(
                'follower_id', NEW.follower_id,
                'follower_username', follower_info.username,
                'status', NEW.status
            )
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER notify_new_follow_trigger
    AFTER INSERT ON public.follows
    FOR EACH ROW EXECUTE FUNCTION notify_new_follow();

-- =============================================
-- TYPING INDICATORS (Optional)
-- =============================================

-- Table for typing indicators (temporary data)
CREATE TABLE IF NOT EXISTS public.typing_indicators (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    chat_id UUID NOT NULL REFERENCES public.chats(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(chat_id, user_id)
);

-- Enable real-time for typing indicators
ALTER PUBLICATION supabase_realtime ADD TABLE public.typing_indicators;

-- Function to set typing indicator
CREATE OR REPLACE FUNCTION set_typing_indicator(chat_uuid UUID)
RETURNS void AS $$
BEGIN
    INSERT INTO public.typing_indicators (chat_id, user_id)
    VALUES (chat_uuid, auth.uid())
    ON CONFLICT (chat_id, user_id) 
    DO UPDATE SET created_at = NOW();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to remove typing indicator
CREATE OR REPLACE FUNCTION remove_typing_indicator(chat_uuid UUID)
RETURNS void AS $$
BEGIN
    DELETE FROM public.typing_indicators 
    WHERE chat_id = chat_uuid AND user_id = auth.uid();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to clean up old typing indicators (run periodically)
CREATE OR REPLACE FUNCTION cleanup_typing_indicators()
RETURNS void AS $$
BEGIN
    DELETE FROM public.typing_indicators 
    WHERE created_at < NOW() - INTERVAL '10 seconds';
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================
-- REAL-TIME POLICIES FOR TYPING INDICATORS
-- =============================================

ALTER TABLE public.typing_indicators ENABLE ROW LEVEL SECURITY;

-- Users can view typing indicators in chats they participate in
CREATE POLICY "Users can view typing indicators" ON public.typing_indicators
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.chat_participants 
            WHERE chat_id = typing_indicators.chat_id 
            AND user_id = auth.uid() 
            AND left_at IS NULL
        )
    );

-- Users can set their own typing indicators
CREATE POLICY "Users can set typing indicators" ON public.typing_indicators
    FOR INSERT WITH CHECK (
        user_id = auth.uid() AND
        EXISTS (
            SELECT 1 FROM public.chat_participants 
            WHERE chat_id = typing_indicators.chat_id 
            AND user_id = auth.uid() 
            AND left_at IS NULL
        )
    );

-- Users can update their own typing indicators
CREATE POLICY "Users can update own typing indicators" ON public.typing_indicators
    FOR UPDATE USING (user_id = auth.uid());

-- Users can remove their own typing indicators
CREATE POLICY "Users can remove own typing indicators" ON public.typing_indicators
    FOR DELETE USING (user_id = auth.uid());

-- =============================================
-- COMPLETION MESSAGE
-- =============================================

-- Real-time setup completed successfully!
-- 
-- Enabled real-time subscriptions for:
-- - messages (chat functionality)
-- - message_reactions (emoji reactions)
-- - chat_participants (join/leave events)
-- - users (status updates)
-- - notifications (live notifications)
-- - posts (live feed updates)
-- - likes (live like counts)
-- - comments (live comments)
-- - follows (follow notifications)
-- - typing_indicators (typing status)
-- 
-- Notification triggers created for:
-- - New messages
-- - New likes
-- - New comments
-- - New follows
-- 
-- Helper functions available:
-- - update_user_status()
-- - get_online_chat_users()
-- - mark_messages_as_read()
-- - set_typing_indicator()
-- - remove_typing_indicator()
-- 
-- Your real-time features are now ready!