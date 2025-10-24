# Database Setup Guide

## Quick Fix for "Name", "..." Placeholder Issues

The app is showing placeholders because:
1. **Supabase is not configured** - Update `gradle.properties` with real credentials
2. **Database tables don't exist** - Create the required tables
3. **No data in database** - Tables are empty

## Step 1: Configure Supabase

Update `gradle.properties`:
```properties
SUPABASE_URL=https://your-actual-project-id.supabase.co
SUPABASE_ANON_KEY=your-actual-anon-key-here
```

## Step 2: Create Database Tables

Run these SQL commands in your Supabase SQL editor:

### Users Table
```sql
CREATE TABLE users (
    uid TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    display_name TEXT NOT NULL,
    email TEXT NOT NULL,
    bio TEXT,
    profile_image_url TEXT,
    followers_count INTEGER DEFAULT 0,
    following_count INTEGER DEFAULT 0,
    posts_count INTEGER DEFAULT 0,
    status TEXT DEFAULT 'offline',
    account_type TEXT DEFAULT 'user',
    verify BOOLEAN DEFAULT false,
    banned BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### Posts Table
```sql
CREATE TABLE posts (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    key TEXT,
    "authorUid" TEXT NOT NULL REFERENCES users(uid),
    "postText" TEXT,
    "postImage" TEXT,
    "postType" TEXT DEFAULT 'TEXT',
    "postHideViewsCount" TEXT DEFAULT 'false',
    "postHideLikeCount" TEXT DEFAULT 'false',
    "postHideCommentsCount" TEXT DEFAULT 'false',
    "postDisableComments" TEXT DEFAULT 'false',
    "postVisibility" TEXT DEFAULT 'public',
    "publishDate" TEXT,
    timestamp BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    "likesCount" INTEGER DEFAULT 0,
    "commentsCount" INTEGER DEFAULT 0,
    "viewsCount" INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);
```

## Step 3: Insert Test Data

### Test User
```sql
INSERT INTO users (uid, username, display_name, email, bio) VALUES 
('test-user-123', 'testuser', 'Test User', 'test@example.com', 'This is a test user');
```

### Test Post
```sql
INSERT INTO posts (id, key, "authorUid", "postText", "postType") VALUES 
('test-post-123', 'post_test_123', 'test-user-123', 'Hello, this is my first post!', 'TEXT');
```

## Step 4: Enable Row Level Security (RLS)

```sql
-- Enable RLS on tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;

-- Allow public read access (adjust as needed)
CREATE POLICY "Allow public read access on users" ON users FOR SELECT USING (true);
CREATE POLICY "Allow public read access on posts" ON posts FOR SELECT USING (true);

-- Allow authenticated users to insert/update their own data
CREATE POLICY "Users can insert their own profile" ON users FOR INSERT WITH CHECK (auth.uid()::text = uid);
CREATE POLICY "Users can update their own profile" ON users FOR UPDATE USING (auth.uid()::text = uid);
CREATE POLICY "Users can insert their own posts" ON posts FOR INSERT WITH CHECK (auth.uid()::text = "authorUid");
```

## Step 5: Test the Connection

The app includes `SupabaseConfigTest.testConfiguration()` to verify your setup.

## Common Issues

1. **"Class 'Any' not found"** - Kotlin serialization issue, fixed in the code
2. **Empty data** - Database tables are empty, add test data
3. **Connection failed** - Wrong Supabase credentials
4. **Permission denied** - RLS policies not configured

After following these steps, the app should show real data instead of placeholders.