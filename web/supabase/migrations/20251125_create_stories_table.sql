-- Create stories table
CREATE TABLE IF NOT EXISTS stories (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(uid) ON DELETE CASCADE,
  media_url TEXT NOT NULL,
  thumbnail_url TEXT,
  media_type VARCHAR(20) NOT NULL CHECK (media_type IN ('image', 'video')),
  duration INTEGER DEFAULT 5,
  caption TEXT,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  expires_at TIMESTAMPTZ DEFAULT (NOW() + INTERVAL '24 hours'),
  view_count INTEGER DEFAULT 0
);

-- Create story_views table to track who viewed each story
CREATE TABLE IF NOT EXISTS story_views (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  story_id UUID NOT NULL REFERENCES stories(id) ON DELETE CASCADE,
  viewer_id UUID NOT NULL REFERENCES users(uid) ON DELETE CASCADE,
  viewed_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(story_id, viewer_id)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_stories_user_id ON stories(user_id);
CREATE INDEX IF NOT EXISTS idx_stories_created_at ON stories(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_stories_expires_at ON stories(expires_at);
CREATE INDEX IF NOT EXISTS idx_stories_active ON stories(is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_story_views_story_id ON story_views(story_id);
CREATE INDEX IF NOT EXISTS idx_story_views_viewer_id ON story_views(viewer_id);

-- Enable RLS
ALTER TABLE stories ENABLE ROW LEVEL SECURITY;
ALTER TABLE story_views ENABLE ROW LEVEL SECURITY;

-- RLS Policies for stories
CREATE POLICY "Users can view active stories from followed users"
  ON stories FOR SELECT
  USING (
    is_active = true 
    AND expires_at > NOW()
    AND (
      user_id = auth.uid()
      OR user_id IN (
        SELECT following_id FROM follows WHERE follower_id = auth.uid()
      )
    )
  );

CREATE POLICY "Users can insert their own stories"
  ON stories FOR INSERT
  WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can update their own stories"
  ON stories FOR UPDATE
  USING (user_id = auth.uid());

CREATE POLICY "Users can delete their own stories"
  ON stories FOR DELETE
  USING (user_id = auth.uid());

-- RLS Policies for story_views
CREATE POLICY "Users can view their own story views"
  ON story_views FOR SELECT
  USING (viewer_id = auth.uid() OR story_id IN (SELECT id FROM stories WHERE user_id = auth.uid()));

CREATE POLICY "Users can insert their own story views"
  ON story_views FOR INSERT
  WITH CHECK (viewer_id = auth.uid());

-- Function to automatically deactivate expired stories
CREATE OR REPLACE FUNCTION deactivate_expired_stories()
RETURNS void AS $$
BEGIN
  UPDATE stories
  SET is_active = false
  WHERE is_active = true
    AND expires_at <= NOW();
END;
$$ LANGUAGE plpgsql;

-- Create a scheduled job to run every hour (requires pg_cron extension)
-- Note: This requires pg_cron extension to be enabled
-- Alternatively, handle expiration in application logic
