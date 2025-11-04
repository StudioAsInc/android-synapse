# Supabase Database Migrations

This directory contains SQL migration files for the Synapse database schema.

## How to Apply Migrations

### Option 1: Using Supabase Dashboard (Recommended)

1. Log in to your Supabase project dashboard at https://app.supabase.com
2. Navigate to the **SQL Editor** section
3. Open the migration file (`20241104_message_actions_schema.sql`)
4. Copy the entire SQL content
5. Paste it into the SQL Editor
6. Click **Run** to execute the migration

### Option 2: Using Supabase CLI

If you have the Supabase CLI installed:

```bash
# Navigate to project root
cd android-synapse

# Apply the migration
supabase db push
```

### Option 3: Using psql (Direct Database Connection)

If you have direct database access:

```bash
psql "postgresql://postgres:[YOUR-PASSWORD]@[YOUR-PROJECT-REF].supabase.co:5432/postgres" \
  -f supabase/migrations/20241104_message_actions_schema.sql
```

## Migration Files

### 20241104_message_actions_schema.sql

**Purpose**: Adds support for message actions (edit, forward, delete, AI summary)

**Changes**:
- Extends `messages` table with:
  - `edit_history` (JSONB) - Stores edit history
  - `forwarded_from_message_id` (TEXT) - Tracks forwarded messages
  - `forwarded_from_chat_id` (TEXT) - Tracks original chat
  - `delete_for_everyone` (BOOLEAN) - Deletion type flag

- Creates `message_forwards` table:
  - Tracks forwarding relationships
  - Links original and forwarded messages
  - Includes foreign key constraints

- Creates `ai_summaries` table:
  - Stores AI-generated summaries
  - Links to messages and users
  - Tracks model version and metadata

- Implements Row Level Security (RLS) policies:
  - Edit messages within 48 hours
  - Delete own messages
  - Forward messages in participant chats
  - View summaries for accessible messages

**Requirements**: Requires existing `messages`, `chats`, `chat_participants`, and `users` tables

## Verifying Migration Success

After applying the migration, verify it was successful:

```sql
-- Check new columns exist
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'messages' 
AND column_name IN ('edit_history', 'forwarded_from_message_id', 'forwarded_from_chat_id', 'delete_for_everyone');

-- Check new tables exist
SELECT table_name 
FROM information_schema.tables 
WHERE table_name IN ('message_forwards', 'ai_summaries');

-- Check RLS policies
SELECT schemaname, tablename, policyname 
FROM pg_policies 
WHERE tablename IN ('messages', 'message_forwards', 'ai_summaries');
```

## Rollback

If you need to rollback this migration:

```sql
-- Drop new tables
DROP TABLE IF EXISTS ai_summaries CASCADE;
DROP TABLE IF EXISTS message_forwards CASCADE;

-- Remove new columns from messages table
ALTER TABLE messages DROP COLUMN IF EXISTS edit_history;
ALTER TABLE messages DROP COLUMN IF EXISTS forwarded_from_message_id;
ALTER TABLE messages DROP COLUMN IF EXISTS forwarded_from_chat_id;
ALTER TABLE messages DROP COLUMN IF EXISTS delete_for_everyone;

-- Drop RLS policies
DROP POLICY IF EXISTS "Users can edit own messages" ON messages;
DROP POLICY IF EXISTS "Users can delete own messages" ON messages;
DROP POLICY IF EXISTS "Users can view messages in their chats" ON messages;
```

## Notes

- Always backup your database before applying migrations
- Test migrations in a development environment first
- RLS policies ensure data security at the database level
- Foreign key constraints maintain referential integrity
