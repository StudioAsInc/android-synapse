-- Row Level Security (RLS) Policies for Synapse Social Media App
-- Run these SQL commands AFTER creating the database schema

-- =============================================
-- ENABLE RLS ON ALL TABLES
-- =============================================

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chats ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.message_reactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.hashtags ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.stories ENABLE ROW LEVEL SECURITY;

-- =============================================
-- USERS TABLE POLICIES
-- =============================================

-- Users can read all public profiles
CREATE POLICY "Users can view public profiles" ON public.users
    FOR SELECT USING (
        NOT private_account OR 
        uid = auth.uid() OR
        EXISTS (
            SELECT 1 FROM public.follows 
            WHERE follower_id = auth.uid() 
            AND following_id = uid 
            AND status = 'accepted'
        )
    );

-- Users can update their own profile
CREATE POLICY "Users can update own profile" ON public.users
    FOR UPDATE USING (uid = auth.uid());

-- Users can insert their own profile (during registration)
CREATE POLICY "Users can insert own profile" ON public.users
    FOR INSERT WITH CHECK (uid = auth.uid());

-- =============================================
-- POSTS TABLE POLICIES
-- =============================================

-- Users can view posts based on visibility and follow status
CREATE POLICY "Users can view posts" ON public.posts
    FOR SELECT USING (
        visibility = 'public' OR
        user_id = auth.uid() OR
        (visibility = 'followers' AND EXISTS (
            SELECT 1 FROM public.follows 
            WHERE follower_id = auth.uid() 
            AND following_id = user_id 
            AND status = 'accepted'
        ))
    );

-- Users can create their own posts
CREATE POLICY "Users can create posts" ON public.posts
    FOR INSERT WITH CHECK (user_id = auth.uid());

-- Users can update their own posts
CREATE POLICY "Users can update own posts" ON public.posts
    FOR UPDATE USING (user_id = auth.uid());

-- Users can delete their own posts
CREATE POLICY "Users can delete own posts" ON public.posts
    FOR DELETE USING (user_id = auth.uid());

-- =============================================
-- COMMENTS TABLE POLICIES
-- =============================================

-- Users can view comments on posts they can see
CREATE POLICY "Users can view comments" ON public.comments
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.posts 
            WHERE id = post_id AND (
                visibility = 'public' OR
                user_id = auth.uid() OR
                (visibility = 'followers' AND EXISTS (
                    SELECT 1 FROM public.follows 
                    WHERE follower_id = auth.uid() 
                    AND following_id = posts.user_id 
                    AND status = 'accepted'
                ))
            )
        )
    );

-- Users can create comments on posts they can see
CREATE POLICY "Users can create comments" ON public.comments
    FOR INSERT WITH CHECK (
        user_id = auth.uid() AND
        EXISTS (
            SELECT 1 FROM public.posts 
            WHERE id = post_id AND (
                visibility = 'public' OR
                user_id = auth.uid() OR
                (visibility = 'followers' AND EXISTS (
                    SELECT 1 FROM public.follows 
                    WHERE follower_id = auth.uid() 
                    AND following_id = posts.user_id 
                    AND status = 'accepted'
                ))
            )
        )
    );

-- Users can update their own comments
CREATE POLICY "Users can update own comments" ON public.comments
    FOR UPDATE USING (user_id = auth.uid());

-- Users can delete their own comments
CREATE POLICY "Users can delete own comments" ON public.comments
    FOR DELETE USING (user_id = auth.uid());

-- =============================================
-- LIKES TABLE POLICIES
-- =============================================

-- Users can view likes on content they can see
CREATE POLICY "Users can view likes" ON public.likes
    FOR SELECT USING (
        user_id = auth.uid() OR
        (post_id IS NOT NULL AND EXISTS (
            SELECT 1 FROM public.posts 
            WHERE id = post_id AND (
                visibility = 'public' OR
                user_id = auth.uid() OR
                (visibility = 'followers' AND EXISTS (
                    SELECT 1 FROM public.follows 
                    WHERE follower_id = auth.uid() 
                    AND following_id = posts.user_id 
                    AND status = 'accepted'
                ))
            )
        )) OR
        (comment_id IS NOT NULL AND EXISTS (
            SELECT 1 FROM public.comments c
            JOIN public.posts p ON c.post_id = p.id
            WHERE c.id = comment_id AND (
                p.visibility = 'public' OR
                p.user_id = auth.uid() OR
                (p.visibility = 'followers' AND EXISTS (
                    SELECT 1 FROM public.follows 
                    WHERE follower_id = auth.uid() 
                    AND following_id = p.user_id 
                    AND status = 'accepted'
                ))
            )
        ))
    );

-- Users can create likes
CREATE POLICY "Users can create likes" ON public.likes
    FOR INSERT WITH CHECK (user_id = auth.uid());

-- Users can delete their own likes
CREATE POLICY "Users can delete own likes" ON public.likes
    FOR DELETE USING (user_id = auth.uid());

-- =============================================
-- FOLLOWS TABLE POLICIES
-- =============================================

-- Users can view follows involving them or public follows
CREATE POLICY "Users can view follows" ON public.follows
    FOR SELECT USING (
        follower_id = auth.uid() OR 
        following_id = auth.uid() OR
        status = 'accepted'
    );

-- Users can create follow requests
CREATE POLICY "Users can create follows" ON public.follows
    FOR INSERT WITH CHECK (
        follower_id = auth.uid() AND 
        follower_id != following_id
    );

-- Users can update follows they're involved in (accept/reject requests)
CREATE POLICY "Users can update follows" ON public.follows
    FOR UPDATE USING (
        follower_id = auth.uid() OR 
        following_id = auth.uid()
    );

-- Users can delete follows they're involved in
CREATE POLICY "Users can delete follows" ON public.follows
    FOR DELETE USING (
        follower_id = auth.uid() OR 
        following_id = auth.uid()
    );

-- =============================================
-- CHATS TABLE POLICIES
-- =============================================

-- Users can view chats they participate in
CREATE POLICY "Users can view chats" ON public.chats
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.chat_participants 
            WHERE chat_id = id 
            AND user_id = auth.uid() 
            AND left_at IS NULL
        )
    );

-- Users can create chats
CREATE POLICY "Users can create chats" ON public.chats
    FOR INSERT WITH CHECK (created_by = auth.uid());

-- Users can update chats they created or are admins of
CREATE POLICY "Users can update chats" ON public.chats
    FOR UPDATE USING (
        created_by = auth.uid() OR
        EXISTS (
            SELECT 1 FROM public.chat_participants 
            WHERE chat_id = id 
            AND user_id = auth.uid() 
            AND role = 'admin'
            AND left_at IS NULL
        )
    );

-- =============================================
-- CHAT PARTICIPANTS TABLE POLICIES
-- =============================================

-- Users can view participants of chats they're in
CREATE POLICY "Users can view chat participants" ON public.chat_participants
    FOR SELECT USING (
        user_id = auth.uid() OR
        EXISTS (
            SELECT 1 FROM public.chat_participants cp2
            WHERE cp2.chat_id = chat_id 
            AND cp2.user_id = auth.uid() 
            AND cp2.left_at IS NULL
        )
    );

-- Chat creators and admins can add participants
CREATE POLICY "Admins can manage participants" ON public.chat_participants
    FOR INSERT WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.chats c
            WHERE c.id = chat_id AND (
                c.created_by = auth.uid() OR
                EXISTS (
                    SELECT 1 FROM public.chat_participants cp
                    WHERE cp.chat_id = chat_id 
                    AND cp.user_id = auth.uid() 
                    AND cp.role = 'admin'
                    AND cp.left_at IS NULL
                )
            )
        )
    );

-- Users can leave chats (update their own participation)
CREATE POLICY "Users can leave chats" ON public.chat_participants
    FOR UPDATE USING (
        user_id = auth.uid() OR
        EXISTS (
            SELECT 1 FROM public.chats c
            WHERE c.id = chat_id AND (
                c.created_by = auth.uid() OR
                EXISTS (
                    SELECT 1 FROM public.chat_participants cp
                    WHERE cp.chat_id = chat_id 
                    AND cp.user_id = auth.uid() 
                    AND cp.role = 'admin'
                    AND cp.left_at IS NULL
                )
            )
        )
    );

-- =============================================
-- MESSAGES TABLE POLICIES
-- =============================================

-- Users can view messages in chats they participate in
CREATE POLICY "Users can view messages" ON public.messages
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.chat_participants 
            WHERE chat_id = messages.chat_id 
            AND user_id = auth.uid() 
            AND (left_at IS NULL OR left_at > messages.created_at)
        )
    );

-- Users can send messages to chats they participate in
CREATE POLICY "Users can send messages" ON public.messages
    FOR INSERT WITH CHECK (
        sender_id = auth.uid() AND
        EXISTS (
            SELECT 1 FROM public.chat_participants 
            WHERE chat_id = messages.chat_id 
            AND user_id = auth.uid() 
            AND left_at IS NULL
        )
    );

-- Users can update their own messages
CREATE POLICY "Users can update own messages" ON public.messages
    FOR UPDATE USING (sender_id = auth.uid());

-- Users can delete their own messages
CREATE POLICY "Users can delete own messages" ON public.messages
    FOR DELETE USING (sender_id = auth.uid());

-- =============================================
-- MESSAGE REACTIONS TABLE POLICIES
-- =============================================

-- Users can view reactions on messages they can see
CREATE POLICY "Users can view message reactions" ON public.message_reactions
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.messages m
            JOIN public.chat_participants cp ON m.chat_id = cp.chat_id
            WHERE m.id = message_id 
            AND cp.user_id = auth.uid() 
            AND (cp.left_at IS NULL OR cp.left_at > m.created_at)
        )
    );

-- Users can add reactions to messages they can see
CREATE POLICY "Users can add reactions" ON public.message_reactions
    FOR INSERT WITH CHECK (
        user_id = auth.uid() AND
        EXISTS (
            SELECT 1 FROM public.messages m
            JOIN public.chat_participants cp ON m.chat_id = cp.chat_id
            WHERE m.id = message_id 
            AND cp.user_id = auth.uid() 
            AND cp.left_at IS NULL
        )
    );

-- Users can remove their own reactions
CREATE POLICY "Users can remove own reactions" ON public.message_reactions
    FOR DELETE USING (user_id = auth.uid());

-- =============================================
-- NOTIFICATIONS TABLE POLICIES
-- =============================================

-- Users can only view their own notifications
CREATE POLICY "Users can view own notifications" ON public.notifications
    FOR SELECT USING (user_id = auth.uid());

-- System can create notifications (handled by triggers/functions)
CREATE POLICY "System can create notifications" ON public.notifications
    FOR INSERT WITH CHECK (true);

-- Users can update their own notifications (mark as read)
CREATE POLICY "Users can update own notifications" ON public.notifications
    FOR UPDATE USING (user_id = auth.uid());

-- Users can delete their own notifications
CREATE POLICY "Users can delete own notifications" ON public.notifications
    FOR DELETE USING (user_id = auth.uid());

-- =============================================
-- HASHTAGS TABLE POLICIES
-- =============================================

-- Everyone can view hashtags
CREATE POLICY "Everyone can view hashtags" ON public.hashtags
    FOR SELECT USING (true);

-- System can manage hashtags (handled by triggers/functions)
CREATE POLICY "System can manage hashtags" ON public.hashtags
    FOR ALL USING (true);

-- =============================================
-- STORIES TABLE POLICIES
-- =============================================

-- Users can view stories from people they follow or their own
CREATE POLICY "Users can view stories" ON public.stories
    FOR SELECT USING (
        user_id = auth.uid() OR
        EXISTS (
            SELECT 1 FROM public.follows 
            WHERE follower_id = auth.uid() 
            AND following_id = user_id 
            AND status = 'accepted'
        ) OR
        NOT EXISTS (
            SELECT 1 FROM public.users 
            WHERE uid = user_id 
            AND private_account = true
        )
    );

-- Users can create their own stories
CREATE POLICY "Users can create stories" ON public.stories
    FOR INSERT WITH CHECK (user_id = auth.uid());

-- Users can delete their own stories
CREATE POLICY "Users can delete own stories" ON public.stories
    FOR DELETE USING (user_id = auth.uid());

-- =============================================
-- HELPER FUNCTIONS FOR POLICIES
-- =============================================

-- Function to check if user can see another user's content
CREATE OR REPLACE FUNCTION can_view_user_content(target_user_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN (
        target_user_id = auth.uid() OR
        NOT EXISTS (
            SELECT 1 FROM public.users 
            WHERE uid = target_user_id 
            AND private_account = true
        ) OR
        EXISTS (
            SELECT 1 FROM public.follows 
            WHERE follower_id = auth.uid() 
            AND following_id = target_user_id 
            AND status = 'accepted'
        )
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================
-- COMPLETION MESSAGE
-- =============================================

-- RLS Policies setup completed successfully!
-- Your database is now secure with proper row-level security.
-- 
-- Next steps:
-- 1. Set up authentication in your Supabase dashboard
-- 2. Configure storage buckets for media files
-- 3. Set up real-time subscriptions
-- 4. Test the policies with your application