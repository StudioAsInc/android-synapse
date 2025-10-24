# Supabase Configuration Guide

## Quick Setup

1. **Update gradle.properties** with your actual Supabase credentials:

```properties
# Replace these with your actual Supabase project values
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=your-actual-anon-key-here
```

2. **Find your Supabase credentials**:
   - Go to your Supabase project dashboard
   - Navigate to Settings > API
   - Copy the "Project URL" and "anon/public" key

3. **Database Schema**: Make sure your `users` table has these columns:
   - `uid` (text, primary key)
   - `username` (text, unique)
   - `display_name` (text)
   - `email` (text)
   - `bio` (text, nullable)
   - `profile_image_url` (text, nullable)
   - `followers_count` (integer, default 0)
   - `following_count` (integer, default 0)
   - `posts_count` (integer, default 0)
   - `status` (text, default 'offline')
   - `account_type` (text, default 'user')
   - `verify` (boolean, default false)
   - `banned` (boolean, default false)

## Common Issues Fixed

- ✅ Fixed bio field nullability mismatch
- ✅ Simplified UserProfile model to match UserInsert
- ✅ Removed redundant UserInsert usage
- ✅ Improved error handling for serialization

## Test the Fix

After updating your Supabase credentials, the CompleteProfileActivity should work without serialization errors.