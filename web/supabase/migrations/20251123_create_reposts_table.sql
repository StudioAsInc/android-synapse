-- Create reposts table
CREATE TABLE IF NOT EXISTS reposts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(user_id, post_id)
);

-- Create index for performance
CREATE INDEX idx_reposts_user_id ON reposts(user_id);
CREATE INDEX idx_reposts_post_id ON reposts(post_id);
CREATE INDEX idx_reposts_created_at ON reposts(created_at DESC);

-- Enable RLS
ALTER TABLE reposts ENABLE ROW LEVEL SECURITY;

-- RLS Policies
CREATE POLICY "Users can view all reposts"
  ON reposts FOR SELECT
  USING (true);

CREATE POLICY "Users can create their own reposts"
  ON reposts FOR INSERT
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own reposts"
  ON reposts FOR DELETE
  USING (auth.uid() = user_id);
