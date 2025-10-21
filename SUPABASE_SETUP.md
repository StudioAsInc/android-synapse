# Supabase Setup Instructions

Your app is currently crashing because Supabase credentials are not configured. Follow these steps to fix it:

## 1. Create a Supabase Project

1. Go to [https://supabase.com](https://supabase.com)
2. Sign up/Sign in to your account
3. Create a new project
4. Wait for the project to be set up

## 2. Get Your Credentials

1. In your Supabase dashboard, go to **Settings** > **API**
2. Copy the following values:
   - **Project URL** (looks like: `https://your-project-id.supabase.co`)
   - **Anon/Public Key** (starts with `eyJ...`)

## 3. Update gradle.properties

Replace the placeholder values in `gradle.properties` with your actual credentials:

```properties
# Replace these with your actual Supabase credentials
SUPABASE_URL=https://your-actual-project-id.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.your-actual-anon-key
```

## 4. Clean and Rebuild

After updating the credentials:

1. Clean the project: `./gradlew clean`
2. Rebuild: `./gradlew build`
3. Run the app

## 5. Database Setup (Optional for now)

The app will work without database tables initially, but you'll eventually need to create:

- `users` table for user profiles
- `posts` table for social posts
- `messages` table for chat functionality
- Other tables as needed

## Security Note

- Never commit your actual Supabase credentials to version control
- The `gradle.properties` file should be in your `.gitignore`
- Use environment variables in production

## Current Status

The app has been updated to handle missing Supabase credentials gracefully and will show an informative error message instead of crashing.