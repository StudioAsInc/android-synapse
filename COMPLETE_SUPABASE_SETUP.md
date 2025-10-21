# Complete Supabase Setup Guide for Synapse Social Media App

This guide will walk you through setting up your complete Supabase backend for the Synapse social media app.

## 🚀 Quick Start

### Step 1: Create Supabase Project

1. Go to [https://supabase.com](https://supabase.com)
2. Sign up/Sign in to your account
3. Click "New Project"
4. Fill in project details:
   - **Name**: `synapse-social-app`
   - **Database Password**: Choose a strong password (save it!)
   - **Region**: Choose closest to your users
5. Click "Create new project"
6. Wait 2-3 minutes for project creation

### Step 2: Get Your Credentials

Once your project is ready:

1. Go to **Settings** → **API**
2. Copy these values:
   - **Project URL** (e.g., `https://abcdefgh.supabase.co`)
   - **Anon/Public Key** (starts with `eyJ...`)

### Step 3: Update Your App Configuration

Replace the values in your `gradle.properties` file:

```properties
# Replace with your actual Supabase credentials
SUPABASE_URL=https://your-actual-project-id.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.your-actual-anon-key
```

## 📊 Database Setup

### Step 4: Create Database Schema

1. In your Supabase dashboard, go to **SQL Editor**
2. Copy and paste the contents of `supabase-database-schema.sql`
3. Click "Run" to execute the SQL
4. This creates all tables, indexes, triggers, and functions

### Step 5: Set Up Security Policies

1. In the **SQL Editor**, create a new query
2. Copy and paste the contents of `supabase-rls-policies.sql`
3. Click "Run" to execute the SQL
4. This sets up Row Level Security for all tables

### Step 6: Configure Storage

1. In the **SQL Editor**, create a new query
2. Copy and paste the contents of `supabase-storage-setup.sql`
3. Click "Run" to execute the SQL
4. This creates storage buckets and policies for file uploads

### Step 7: Enable Real-time Features

1. In the **SQL Editor**, create a new query
2. Copy and paste the contents of `supabase-realtime-setup.sql`
3. Click "Run" to execute the SQL
4. This enables real-time subscriptions and notifications

## 🔐 Authentication Setup

### Step 8: Configure Authentication

1. Go to **Authentication** → **Settings**
2. Configure the following:

#### Site URL
- Set to your app's URL (for development: `http://localhost:3000`)

#### Email Templates (Optional)
- Customize confirmation and recovery email templates

#### Auth Providers (Optional)
- Enable Google, Facebook, or other OAuth providers if needed

## 🗂️ Database Tables Created

Your database now includes these tables:

- **users** - User profiles and settings
- **posts** - Social media posts with attachments
- **comments** - Comments and replies on posts
- **likes** - Likes on posts and comments
- **follows** - Follow relationships between users
- **chats** - Chat rooms (direct and group)
- **chat_participants** - Users in each chat
- **messages** - Chat messages with attachments
- **message_reactions** - Emoji reactions on messages
- **notifications** - Push notifications and alerts
- **hashtags** - Trending hashtags
- **stories** - Temporary stories (24-hour expiry)
- **typing_indicators** - Real-time typing status

## 📁 Storage Buckets Created

Your storage includes these buckets:

- **profile-photos** (5MB, images only, public)
- **cover-photos** (10MB, images only, public)
- **post-attachments** (50MB, media files, public)
- **chat-attachments** (50MB, all files, private)
- **stories** (50MB, media files, public)

## 🔄 Real-time Features Enabled

Your app now supports:

- **Live Chat** - Real-time messaging
- **Typing Indicators** - See when someone is typing
- **Live Notifications** - Instant push notifications
- **Live Feed Updates** - Real-time post updates
- **Online Status** - See who's online
- **Live Reactions** - Real-time likes and reactions

## 🧪 Testing Your Setup

### Step 9: Test Your App

1. Clean and rebuild your Android project:
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   ```

2. Install and run the app
3. The app should now start without crashing
4. You should see proper authentication flow

### Step 10: Verify Database Connection

You can test the connection by:

1. Going to **Authentication** → **Users** in Supabase
2. Creating a test user manually
3. Checking if your app can authenticate

## 🛠️ Available API Functions

Your database includes these helper functions:

- `update_user_status(status)` - Update online status
- `get_online_chat_users(chat_id)` - Get online users in chat
- `mark_messages_as_read(chat_id)` - Mark messages as read
- `set_typing_indicator(chat_id)` - Set typing status
- `remove_typing_indicator(chat_id)` - Remove typing status
- `cleanup_expired_stories()` - Clean up old stories
- `validate_file_upload()` - Validate file uploads

## 🔧 Maintenance Tasks

### Scheduled Cleanup (Optional)

Set up these periodic tasks:

1. **Clean expired stories** - Run daily
2. **Clean typing indicators** - Run every minute
3. **Clean old notifications** - Run weekly

You can use Supabase Edge Functions or external cron jobs for this.

## 🚨 Security Notes

- All tables have Row Level Security (RLS) enabled
- Users can only access data they're authorized to see
- File uploads are restricted by size and type
- Private chats are properly secured
- All sensitive operations require authentication

## 📱 Next Steps for Your App

Now that your backend is set up, you can:

1. **Implement Authentication** - Sign up, sign in, profile creation
2. **Build Social Features** - Posts, comments, likes, follows
3. **Add Chat Functionality** - Real-time messaging
4. **Implement File Uploads** - Photos, videos, documents
5. **Add Push Notifications** - Using OneSignal or FCM
6. **Build Stories Feature** - Temporary content
7. **Add Search** - Users, posts, hashtags

## 🆘 Troubleshooting

### Common Issues:

1. **App still crashes**: Double-check your credentials in `gradle.properties`
2. **Can't connect**: Verify your Supabase URL and API key
3. **Permission errors**: Check RLS policies are properly set up
4. **File upload fails**: Verify storage buckets and policies

### Getting Help:

- Check Supabase logs in the dashboard
- Use the SQL Editor to test queries
- Check the Network tab in browser dev tools
- Review the authentication logs

## 🎉 Congratulations!

Your Synapse social media app now has a complete, production-ready backend with:

- ✅ User authentication and profiles
- ✅ Social media features (posts, comments, likes)
- ✅ Real-time chat system
- ✅ File storage and media handling
- ✅ Push notifications system
- ✅ Security and privacy controls
- ✅ Scalable database design

Your app is ready for development and testing!