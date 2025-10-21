# Complete Supabase Setup Guide for Synapse Social Media App

Since the Supabase MCP server isn't available, I'll guide you through setting up Supabase manually with all the necessary components for your social media app.

## Step 1: Create Supabase Project

1. Go to [https://supabase.com](https://supabase.com)
2. Sign up/Sign in to your account
3. Click "New Project"
4. Choose your organization (or create one)
5. Fill in project details:
   - **Name**: `synapse-social-app`
   - **Database Password**: Choose a strong password (save it!)
   - **Region**: Choose closest to your users (e.g., US East, Europe, etc.)
6. Click "Create new project"
7. Wait for the project to be created (2-3 minutes)

## Step 2: Get Your Credentials

Once your project is ready:

1. Go to **Settings** → **API**
2. Copy these values:
   - **Project URL** (e.g., `https://your-project-id.supabase.co`)
   - **Anon/Public Key** (starts with `eyJ...`)
   - **Service Role Key** (for admin operations, keep secret!)

## Step 3: Update Your App Configuration

Update your `gradle.properties` file with the actual credentials:

```properties
# Replace with your actual Supabase credentials
SUPABASE_URL=https://your-actual-project-id.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.your-actual-anon-key
```

## Step 4: Database Schema Setup

I'll provide you with SQL scripts to create all the necessary tables and policies.

## Next Steps

After you create the project and get the credentials:
1. Update your `gradle.properties` with the real values
2. I'll provide you with the complete database schema
3. We'll set up authentication policies
4. Configure storage for media files
5. Set up real-time subscriptions

Let me know when you have the project created and the credentials!