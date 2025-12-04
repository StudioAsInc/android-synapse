-- Fix increment_post_views function to handle UUID type correctly
-- This migration fixes the "operator does not exist: text = uuid" error

-- Drop the existing function if it exists
DROP FUNCTION IF EXISTS increment_post_views(text);
DROP FUNCTION IF EXISTS increment_post_views(uuid);

-- Create the function with proper UUID type handling
CREATE OR REPLACE FUNCTION increment_post_views(post_id uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    -- Increment the views_count for the specified post
    UPDATE posts
    SET views_count = COALESCE(views_count, 0) + 1,
        updated_at = NOW()
    WHERE id = post_id;
END;
$$;

-- Grant execute permission to authenticated users
GRANT EXECUTE ON FUNCTION increment_post_views(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION increment_post_views(uuid) TO anon;

-- Add comment for documentation
COMMENT ON FUNCTION increment_post_views(uuid) IS 'Increments the view count for a post by 1';
