-- Migration: Add message actions support (edit, forward, delete, AI summary)
-- Date: 2024-11-04
-- Description: Extends messages table and creates supporting tables for message actions

-- ============================================================================
-- 1. Extend messages table with new columns
-- ============================================================================

-- Add edit_history column to track message edits
ALTER TABLE messages 
ADD COLUMN IF NOT EXISTS edit_history JSONB DEFAULT '[]'::jsonb;

-- Add forwarded_from_message_id to track message forwarding
ALTER TABLE messages 
ADD COLUMN IF NOT EXISTS forwarded_from_message_id TEXT;

-- Add forwarded_from_chat_id to track original chat of forwarded message
ALTER TABLE messages 
ADD COLUMN IF NOT EXISTS forwarded_from_chat_id TEXT;

-- Add delete_for_everyone flag to distinguish deletion types
ALTER TABLE messages 
ADD COLUMN IF NOT EXISTS delete_for_everyone BOOLEAN DEFAULT FALSE;

-- Add comments for documentation
COMMENT ON COLUMN messages.edit_history IS 'JSONB array storing edit history with timestamps and previous content';
COMMENT ON COLUMN messages.forwarded_from_message_id IS 'Reference to original message ID if this message was forwarded';
COMMENT ON COLUMN messages.forwarded_from_chat_id IS 'Reference to original chat ID if this message was forwarded';
COMMENT ON COLUMN messages.delete_for_everyone IS 'TRUE if message was deleted for all users, FALSE if deleted locally only';

-- ============================================================================
-- 2. Create message_forwards table
-- ============================================================================

CREATE TABLE IF NOT EXISTS message_forwards (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    original_message_id TEXT NOT NULL,
    original_chat_id TEXT NOT NULL,
    forwarded_message_id TEXT NOT NULL,
    forwarded_chat_id TEXT NOT NULL,
    forwarded_by TEXT NOT NULL,
    forwarded_at BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Foreign key constraints
    CONSTRAINT fk_original_message 
        FOREIGN KEY (original_message_id) 
        REFERENCES messages(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_forwarded_message 
        FOREIGN KEY (forwarded_message_id) 
        REFERENCES messages(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_original_chat 
        FOREIGN KEY (original_chat_id) 
        REFERENCES chats(chat_id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_forwarded_chat 
        FOREIGN KEY (forwarded_chat_id) 
        REFERENCES chats(chat_id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_forwarded_by_user 
        FOREIGN KEY (forwarded_by) 
        REFERENCES users(uid) 
        ON DELETE CASCADE,
    
    -- Unique constraint to prevent duplicate forwards to same chat
    CONSTRAINT unique_forward_per_chat 
        UNIQUE (original_message_id, forwarded_chat_id)
);

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_message_forwards_original_message 
    ON message_forwards(original_message_id);

CREATE INDEX IF NOT EXISTS idx_message_forwards_forwarded_message 
    ON message_forwards(forwarded_message_id);

CREATE INDEX IF NOT EXISTS idx_message_forwards_forwarded_by 
    ON message_forwards(forwarded_by);

-- Add table comment
COMMENT ON TABLE message_forwards IS 'Tracks message forwarding relationships between chats';

-- ============================================================================
-- 3. Create ai_summaries table
-- ============================================================================

CREATE TABLE IF NOT EXISTS ai_summaries (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    message_id TEXT NOT NULL,
    summary_text TEXT NOT NULL,
    generated_at BIGINT NOT NULL,
    generated_by TEXT NOT NULL,
    model_version TEXT DEFAULT 'gemini-pro',
    character_count INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Foreign key constraints
    CONSTRAINT fk_message 
        FOREIGN KEY (message_id) 
        REFERENCES messages(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_generated_by_user 
        FOREIGN KEY (generated_by) 
        REFERENCES users(uid) 
        ON DELETE CASCADE,
    
    -- Unique constraint to ensure one summary per message
    CONSTRAINT unique_summary_per_message 
        UNIQUE (message_id)
);

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_ai_summaries_message 
    ON ai_summaries(message_id);

CREATE INDEX IF NOT EXISTS idx_ai_summaries_generated_by 
    ON ai_summaries(generated_by);

-- Add table comment
COMMENT ON TABLE ai_summaries IS 'Stores AI-generated summaries for messages using Gemini API';

-- ============================================================================
-- 4. Row Level Security (RLS) Policies
-- ============================================================================

-- Enable RLS on new tables
ALTER TABLE message_forwards ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_summaries ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- 4.1 Messages Table RLS Policies
-- ============================================================================

-- Policy: Users can edit their own messages within 48 hours
DROP POLICY IF EXISTS "Users can edit own messages" ON messages;
CREATE POLICY "Users can edit own messages"
ON messages
FOR UPDATE
USING (
    sender_id = auth.uid()::text 
    AND created_at > (EXTRACT(EPOCH FROM NOW()) * 1000 - 172800000)::bigint
)
WITH CHECK (
    sender_id = auth.uid()::text 
    AND created_at > (EXTRACT(EPOCH FROM NOW()) * 1000 - 172800000)::bigint
);

-- Policy: Users can delete their own messages
DROP POLICY IF EXISTS "Users can delete own messages" ON messages;
CREATE POLICY "Users can delete own messages"
ON messages
FOR UPDATE
USING (sender_id = auth.uid()::text)
WITH CHECK (sender_id = auth.uid()::text);

-- Policy: Users can view messages in their chats
DROP POLICY IF EXISTS "Users can view messages in their chats" ON messages;
CREATE POLICY "Users can view messages in their chats"
ON messages
FOR SELECT
USING (
    chat_id IN (
        SELECT chat_id 
        FROM chat_participants 
        WHERE user_id = auth.uid()::text
    )
);

-- ============================================================================
-- 4.2 Message Forwards Table RLS Policies
-- ============================================================================

-- Policy: Users can forward messages from chats they participate in
DROP POLICY IF EXISTS "Users can forward messages" ON message_forwards;
CREATE POLICY "Users can forward messages"
ON message_forwards
FOR INSERT
WITH CHECK (
    forwarded_by = auth.uid()::text
    AND original_chat_id IN (
        SELECT chat_id 
        FROM chat_participants 
        WHERE user_id = auth.uid()::text
    )
    AND forwarded_chat_id IN (
        SELECT chat_id 
        FROM chat_participants 
        WHERE user_id = auth.uid()::text
    )
);

-- Policy: Users can view forward history for messages in their chats
DROP POLICY IF EXISTS "Users can view forward history" ON message_forwards;
CREATE POLICY "Users can view forward history"
ON message_forwards
FOR SELECT
USING (
    forwarded_by = auth.uid()::text
    OR original_chat_id IN (
        SELECT chat_id 
        FROM chat_participants 
        WHERE user_id = auth.uid()::text
    )
    OR forwarded_chat_id IN (
        SELECT chat_id 
        FROM chat_participants 
        WHERE user_id = auth.uid()::text
    )
);

-- ============================================================================
-- 4.3 AI Summaries Table RLS Policies
-- ============================================================================

-- Policy: Users can create summaries for messages in their chats
DROP POLICY IF EXISTS "Users can create summaries" ON ai_summaries;
CREATE POLICY "Users can create summaries"
ON ai_summaries
FOR INSERT
WITH CHECK (
    generated_by = auth.uid()::text
    AND message_id IN (
        SELECT id 
        FROM messages 
        WHERE chat_id IN (
            SELECT chat_id 
            FROM chat_participants 
            WHERE user_id = auth.uid()::text
        )
    )
);

-- Policy: Users can view summaries for messages in their chats
DROP POLICY IF EXISTS "Users can view summaries" ON ai_summaries;
CREATE POLICY "Users can view summaries"
ON ai_summaries
FOR SELECT
USING (
    message_id IN (
        SELECT id 
        FROM messages 
        WHERE chat_id IN (
            SELECT chat_id 
            FROM chat_participants 
            WHERE user_id = auth.uid()::text
        )
    )
);

-- ============================================================================
-- Migration Complete
-- ============================================================================

-- Log migration completion
DO $$
BEGIN
    RAISE NOTICE 'Migration completed: Message actions schema created successfully';
END $$;
