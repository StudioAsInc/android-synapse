# Supabase Setup Guide

## Step 1: Create Supabase Project

1. Go to [https://supabase.com](https://supabase.com)
2. Sign up or log in
3. Click "New Project"
4. Choose your organization
5. Enter project details:
   - Name: `synapse-android`
   - Database Password: (generate a strong password)
   - Region: (choose closest to your users)
6. Click "Create new project"

## Step 2: Get Project Credentials

1. In your Supabase dashboard, go to Settings > API
2. Copy the following values:
   - **Project URL**: `https://your-project-ref.supabase.co`
   - **Anon/Public Key**: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`

## Step 3: Update gradle.properties

Replace the placeholder values in `gradle.properties`:

```properties
# Supabase Configuration
SUPABASE_URL=https://your-actual-project-ref.supabase.co
SUPABASE_ANON_KEY=your-actual-anon-key-here
```

## Step 4: Set Up Database Schema

1. In Supabase dashboard, go to SQL Editor
2. Create a new query
3. Copy and paste the following schema:

```sql
-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uid TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE NOT NULL,
    username TEXT UNIQUE NOT NULL,
    nickname TEXT,
    biography TEXT,
    avatar TEXT,
    avatar_history_type TEXT DEFAULT 'local',
    profile_cover_image TEXT,
    account_premium BOOLEAN DEFAULT false,
    user_level_xp INTEGER DEFAULT 500,
    verify BOOLEAN DEFAULT false,
    account_type TEXT DEFAULT 'user' CHECK (account_type IN ('user', 'admin')),
    gender TEXT DEFAULT 'hidden',
    banned BOOLEAN DEFAULT false,
    status TEXT DEFAULT 'offline' CHECK (status IN ('online', 'offline', 'away')),
    join_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    one_signal_player_id TEXT,
    last_seen TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    chatting_with TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Chats table
CREATE TABLE chats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id TEXT UNIQUE NOT NULL,
    participant_1 UUID NOT NULL REFERENCES users(id),
    participant_2 UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(participant_1, participant_2)
);

-- Messages table
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_key TEXT UNIQUE NOT NULL,
    chat_id UUID NOT NULL REFERENCES chats(id),
    sender_id UUID NOT NULL REFERENCES users(id),
    message_text TEXT,
    message_type TEXT DEFAULT 'text' CHECK (message_type IN ('text', 'image', 'voice', 'file', 'ai')),
    attachment_url TEXT,
    attachment_name TEXT,
    voice_duration INTEGER,
    reply_to_message_id UUID REFERENCES messages(id),
    push_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    edited_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Posts table
CREATE TABLE posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id TEXT UNIQUE NOT NULL,
    author_id UUID NOT NULL REFERENCES users(id),
    content TEXT,
    image_url TEXT,
    video_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Inbox table
CREATE TABLE inbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    chat_partner_id UUID REFERENCES users(id),
    group_id UUID,
    last_message_id UUID REFERENCES messages(id),
    unread_count INTEGER DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, chat_partner_id),
    CHECK ((chat_partner_id IS NOT NULL AND group_id IS NULL) OR (chat_partner_id IS NULL AND group_id IS NOT NULL))
);

-- Create indexes for performance
CREATE INDEX idx_users_uid ON users(uid);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_chats_chat_id ON chats(chat_id);
CREATE INDEX idx_messages_chat_id ON messages(chat_id);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_posts_author_id ON posts(author_id);
CREATE INDEX idx_inbox_user_id ON inbox(user_id);
```

4. Click "Run" to execute the schema

## Step 5: Set Up Row Level Security (RLS)

```sql
-- Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE chats ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE inbox ENABLE ROW LEVEL SECURITY;

-- Users can view all profiles but only update their own
CREATE POLICY "Users can view profiles" ON users FOR SELECT USING (true);
CREATE POLICY "Users can update own profile" ON users FOR UPDATE USING (auth.uid()::text = uid);
CREATE POLICY "Users can insert own profile" ON users FOR INSERT WITH CHECK (auth.uid()::text = uid);

-- Chat access policies
CREATE POLICY "Users can view own chats" ON chats FOR SELECT USING (
    participant_1 = (SELECT id FROM users WHERE uid = auth.uid()::text) OR 
    participant_2 = (SELECT id FROM users WHERE uid = auth.uid()::text)
);

-- Message access policies
CREATE POLICY "Users can view messages in their chats" ON messages FOR SELECT USING (
    chat_id IN (
        SELECT id FROM chats WHERE 
        participant_1 = (SELECT id FROM users WHERE uid = auth.uid()::text) OR 
        participant_2 = (SELECT id FROM users WHERE uid = auth.uid()::text)
    )
);

CREATE POLICY "Users can insert messages in their chats" ON messages FOR INSERT WITH CHECK (
    sender_id = (SELECT id FROM users WHERE uid = auth.uid()::text)
);

-- Post policies
CREATE POLICY "Users can view all posts" ON posts FOR SELECT USING (true);
CREATE POLICY "Users can insert own posts" ON posts FOR INSERT WITH CHECK (
    author_id = (SELECT id FROM users WHERE uid = auth.uid()::text)
);
CREATE POLICY "Users can update own posts" ON posts FOR UPDATE USING (
    author_id = (SELECT id FROM users WHERE uid = auth.uid()::text)
);

-- Inbox policies
CREATE POLICY "Users can view own inbox" ON inbox FOR SELECT USING (
    user_id = (SELECT id FROM users WHERE uid = auth.uid()::text)
);
CREATE POLICY "Users can update own inbox" ON inbox FOR ALL USING (
    user_id = (SELECT id FROM users WHERE uid = auth.uid()::text)
);
```

## Step 6: Test the Setup

1. Try building the project: `./gradlew build`
2. If successful, the Supabase client should initialize properly
3. Test authentication in the app

## Step 7: Configure Storage (Optional)

If you need file storage:

1. In Supabase dashboard, go to Storage
2. Create a new bucket called `avatars`
3. Set up policies for file access
4. Update your upload code to use Supabase Storage

## Troubleshooting

### Build Errors
- Ensure your Supabase URL and key are correct
- Check that your internet connection can reach Supabase
- Verify the project is not paused in Supabase dashboard

### Database Errors
- Make sure all tables are created successfully
- Check that RLS policies are properly set up
- Verify user authentication is working

### Connection Issues
- Check your Supabase project status
- Ensure the anon key has proper permissions
- Verify your project URL format

## Next Steps

After completing this setup:
1. Test the authentication flow
2. Migrate one activity at a time
3. Test each feature as you migrate it
4. Remove Firebase compatibility layer once migration is complete

---

**Important**: Keep your Supabase credentials secure and never commit them to version control!