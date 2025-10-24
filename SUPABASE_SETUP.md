# Supabase Configuration Guide

## Issue: App Stuck at Authentication Screen

If your app is getting stuck at the authentication screen and not proceeding to the main HomeActivity, it's likely because Supabase is not properly configured.

## Solution

### 1. Check Current Configuration

The app is currently configured with placeholder values in `gradle.properties`:

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key-here
```

### 2. Get Your Supabase Credentials

1. Go to [supabase.com](https://supabase.com)
2. Sign in to your account
3. Select your project (or create a new one)
4. Go to Settings → API
5. Copy your:
   - **Project URL** (looks like: `https://abcdefghijklmnop.supabase.co`)
   - **Anon/Public Key** (starts with `eyJ...`)

### 3. Update Configuration

Replace the placeholder values in `gradle.properties`:

```properties
# Replace with your actual Supabase project URL
SUPABASE_URL=https://your-actual-project-id.supabase.co

# Replace with your actual anon key
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 4. Rebuild the App

After updating the configuration:

1. Clean and rebuild the project:
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   ```

2. Install the new APK on your device

### 5. Verify Setup

The app should now:
1. Start with CheckpermissionActivity
2. Move to MainActivity 
3. Check Supabase configuration (should pass now)
4. Check authentication status
5. Either go to HomeActivity (if logged in) or AuthActivity (if not logged in)

## Database Setup

Make sure your Supabase database has the required tables:
- `users` - for user profiles
- `posts` - for user posts
- `follows` - for follow relationships
- `profile_likes` - for profile likes
- `post_likes` - for post likes
- `favorites` - for favorite posts

## Troubleshooting

If you're still having issues:

1. Check the Android logs for error messages
2. Verify your Supabase project is active
3. Ensure your API keys have the correct permissions
4. Check if your Supabase project has the required tables and RLS policies

## Contact

If you need help with the Supabase setup, contact the development team with:
- Your Supabase project URL
- Any error messages from the Android logs
- Screenshots of the issue