-- Migration: Optimize database queries for typing indicators and read receipts
-- Requirements: 6.5
-- Date: 2024-11-05

-- ============================================================================
-- PERFORMANCE INDEXES
-- ============================================================================

-- Index for efficient read receipt queries on (chat_id, message_state)
-- This supports queries like: SELECT * FROM messages WHERE chat_id = ? AND message_state = ?
CREATE INDEX IF NOT EXISTS idx_messages_chat_state 
ON messages(chat_id, message_state);

-- Index for efficient message state updates by message ID
-- This supports batch updates: UPDATE messages SET message_state = ? WHERE id IN (...)
CREATE INDEX IF NOT EXISTS idx_messages_id_state 
ON messages(id, message_state);

-- Index for efficient queries on delivered_at and read_at timestamps
-- This supports queries for message delivery and read analytics
CREATE INDEX IF NOT EXISTS idx_messages_timestamps 
ON messages(chat_id, delivered_at, read_at) 
WHERE delivered_at IS NOT NULL OR read_at IS NOT NULL;

-- Composite index for efficient read receipt batch queries
-- This supports queries: SELECT * FROM messages WHERE chat_id = ? AND receiver_id = ? AND message_state != 'read'
CREATE INDEX IF NOT EXISTS idx_messages_chat_receiver_state 
ON messages(chat_id, receiver_id, message_state);

-- ============================================================================
-- TYPING STATUS TABLE OPTIMIZATION
-- ============================================================================

-- Create typing_status table if it doesn't exist (for persistence)
CREATE TABLE IF NOT EXISTS typing_status (
    chat_id UUID NOT NULL,
    user_id UUID NOT NULL,
    is_typing BOOLEAN NOT NULL DEFAULT false,
    timestamp BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (chat_id, user_id)
);

-- Index for timestamp-based cleanup queries
CREATE INDEX IF NOT EXISTS idx_typing_status_timestamp 
ON typing_status(timestamp);

-- Index for chat-based queries
CREATE INDEX IF NOT EXISTS idx_typing_status_chat 
ON typing_status(chat_id) 
WHERE is_typing = true;

-- ============================================================================
-- TTL AND CLEANUP FUNCTIONS
-- ============================================================================

-- Function to clean up old typing status records (older than 1 hour)
CREATE OR REPLACE FUNCTION cleanup_old_typing_status()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
    cutoff_timestamp BIGINT;
BEGIN
    -- Calculate cutoff timestamp (1 hour ago)
    cutoff_timestamp := (EXTRACT(EPOCH FROM NOW()) - 3600) * 1000;
    
    -- Delete old records
    DELETE FROM typing_status 
    WHERE timestamp < cutoff_timestamp;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    -- Log the cleanup
    RAISE NOTICE 'Cleaned up % old typing status records', deleted_count;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to automatically set typing to false for stale records (older than 10 seconds)
CREATE OR REPLACE FUNCTION cleanup_stale_typing_status()
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
    cutoff_timestamp BIGINT;
BEGIN
    -- Calculate cutoff timestamp (10 seconds ago)
    cutoff_timestamp := (EXTRACT(EPOCH FROM NOW()) - 10) * 1000;
    
    -- Update stale typing records to false
    UPDATE typing_status 
    SET is_typing = false, 
        timestamp = EXTRACT(EPOCH FROM NOW()) * 1000
    WHERE is_typing = true 
    AND timestamp < cutoff_timestamp;
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    
    -- Log the cleanup
    IF updated_count > 0 THEN
        RAISE NOTICE 'Set % stale typing indicators to false', updated_count;
    END IF;
    
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- BATCH UPDATE FUNCTIONS
-- ============================================================================

-- Function for efficient batch message state updates
CREATE OR REPLACE FUNCTION batch_update_message_state(
    message_ids UUID[],
    new_state TEXT,
    user_id UUID DEFAULT NULL
)
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
    current_timestamp BIGINT;
BEGIN
    current_timestamp := EXTRACT(EPOCH FROM NOW()) * 1000;
    
    -- Update message states with appropriate timestamps
    UPDATE messages 
    SET 
        message_state = new_state,
        delivered_at = CASE 
            WHEN new_state = 'delivered' AND delivered_at IS NULL 
            THEN current_timestamp 
            ELSE delivered_at 
        END,
        read_at = CASE 
            WHEN new_state = 'read' AND read_at IS NULL 
            THEN current_timestamp 
            ELSE read_at 
        END
    WHERE id = ANY(message_ids)
    AND (user_id IS NULL OR receiver_id = user_id);
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- Function for efficient typing status upsert
CREATE OR REPLACE FUNCTION upsert_typing_status(
    p_chat_id UUID,
    p_user_id UUID,
    p_is_typing BOOLEAN
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO typing_status (chat_id, user_id, is_typing, timestamp)
    VALUES (p_chat_id, p_user_id, p_is_typing, EXTRACT(EPOCH FROM NOW()) * 1000)
    ON CONFLICT (chat_id, user_id)
    DO UPDATE SET 
        is_typing = EXCLUDED.is_typing,
        timestamp = EXCLUDED.timestamp;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- ROW LEVEL SECURITY POLICIES
-- ============================================================================

-- Enable RLS on typing_status table
ALTER TABLE typing_status ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only see typing status for chats they participate in
CREATE POLICY "Users can view typing status in their chats"
ON typing_status FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM chat_rooms
        WHERE id = chat_id
        AND (sender_id = auth.uid() OR receiver_id = auth.uid())
    )
);

-- Policy: Users can only update typing status for themselves
CREATE POLICY "Users can update their own typing status"
ON typing_status FOR ALL
USING (user_id = auth.uid())
WITH CHECK (user_id = auth.uid());

-- ============================================================================
-- PERFORMANCE MONITORING VIEWS
-- ============================================================================

-- View for monitoring message state distribution
CREATE OR REPLACE VIEW message_state_stats AS
SELECT 
    chat_id,
    message_state,
    COUNT(*) as count,
    AVG(CASE 
        WHEN delivered_at IS NOT NULL AND push_date IS NOT NULL 
        THEN delivered_at - push_date 
        ELSE NULL 
    END) as avg_delivery_time_ms,
    AVG(CASE 
        WHEN read_at IS NOT NULL AND delivered_at IS NOT NULL 
        THEN read_at - delivered_at 
        ELSE NULL 
    END) as avg_read_time_ms
FROM messages
WHERE push_date IS NOT NULL
GROUP BY chat_id, message_state;

-- View for monitoring typing activity
CREATE OR REPLACE VIEW typing_activity_stats AS
SELECT 
    chat_id,
    COUNT(*) as total_typing_events,
    COUNT(CASE WHEN is_typing THEN 1 END) as active_typing_count,
    MAX(timestamp) as last_activity_timestamp,
    AVG(timestamp) as avg_timestamp
FROM typing_status
GROUP BY chat_id;

-- ============================================================================
-- CLEANUP SCHEDULE (Optional - requires pg_cron extension)
-- ============================================================================

-- Note: These scheduled jobs require the pg_cron extension to be enabled
-- Uncomment and run these if pg_cron is available in your Supabase instance

-- Schedule cleanup of old typing status every 15 minutes
-- SELECT cron.schedule('cleanup-typing-status', '*/15 * * * *', 'SELECT cleanup_old_typing_status();');

-- Schedule cleanup of stale typing indicators every minute
-- SELECT cron.schedule('cleanup-stale-typing', '* * * * *', 'SELECT cleanup_stale_typing_status();');

-- ============================================================================
-- PERFORMANCE ANALYSIS QUERIES
-- ============================================================================

-- Query to analyze index usage (run periodically to monitor performance)
/*
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan as index_scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes 
WHERE tablename IN ('messages', 'typing_status')
ORDER BY idx_scan DESC;
*/

-- Query to analyze table sizes and growth
/*
SELECT 
    tablename,
    pg_size_pretty(pg_total_relation_size(tablename::regclass)) as total_size,
    pg_size_pretty(pg_relation_size(tablename::regclass)) as table_size,
    pg_size_pretty(pg_total_relation_size(tablename::regclass) - pg_relation_size(tablename::regclass)) as index_size
FROM (VALUES ('messages'), ('typing_status')) AS t(tablename);
*/

-- ============================================================================
-- MIGRATION COMPLETION
-- ============================================================================

-- Log successful migration
DO $$
BEGIN
    RAISE NOTICE 'Migration 002_optimize_typing_read_receipts_performance completed successfully';
    RAISE NOTICE 'Created indexes: idx_messages_chat_state, idx_messages_id_state, idx_messages_timestamps, idx_messages_chat_receiver_state';
    RAISE NOTICE 'Created typing_status table with indexes and cleanup functions';
    RAISE NOTICE 'Created batch update functions for improved performance';
    RAISE NOTICE 'Created monitoring views for performance analysis';
END $$;