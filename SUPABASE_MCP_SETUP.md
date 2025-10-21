# Supabase Backend Setup with MCP

## Step 1: Create Supabase Project

1. Go to [https://supabase.com/dashboard](https://supabase.com/dashboard)
2. Sign in or create an account
3. Click "New Project"
4. Choose your organization
5. Fill in project details:
   - **Name**: `synapse-social-app` (or your preferred name)
   - **Database Password**: Generate a strong password and save it
   - **Region**: Choose closest to your users
6. Click "Create new project"
7. Wait for project initialization (2-3 minutes)

## Step 2: Get Your Credentials

Once your project is ready:

1. Go to **Settings** → **API** in your Supabase dashboard
2. Copy these values:
   - **Project URL**: `https://your-project-ref.supabase.co`
   - **Anon/Public Key**: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`

## Step 3: Update Your Credentials

Replace the values in `gradle.properties`:

```properties
SUPABASE_URL=https://your-actual-project-ref.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.your-actual-anon-key
```

## Step 4: Set Up Database Schema

1. In your Supabase dashboard, go to **SQL Editor**
2. Create a new query
3. Copy and paste the contents of `supabase-database-schema.sql`
4. Run the query to create all tables, indexes, and functions

## Step 5: Configure Row Level Security (RLS)

1. In SQL Editor, run the contents of `supabase-rls-policies.sql`
2. This will set up proper security policies for your app

## Step 6: Set Up Storage

1. In SQL Editor, run the contents of `supabase-storage-setup.sql`
2. This creates storage buckets for profile photos, post attachments, etc.

## Step 7: Configure Real-time

1. In SQL Editor, run the contents of `supabase-realtime-setup.sql`
2. This enables real-time subscriptions for chat and notifications

## Step 8: Test Your Setup

After updating credentials:

```bash
./gradlew clean
./gradlew build
```

Your app should now connect to Supabase successfully!

## MCP Integration

With the Supabase MCP server enabled, you can now:
- Query your database directly from Kiro
- Manage your Supabase project
- Monitor real-time data
- Debug issues more easily

## Next Steps

1. Set up authentication in your Android app
2. Implement user registration/login
3. Test database operations
4. Set up push notifications
5. Configure storage for media uploads

## Troubleshooting

If you encounter issues:
1. Verify your credentials are correct
2. Check that your Supabase project is active
3. Ensure all SQL scripts ran without errors
4. Check the Supabase logs in your dashboard