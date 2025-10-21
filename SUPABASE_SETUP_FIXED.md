# Supabase Setup - Fixed Approach

## ❌ Issue Found
The `mcp-server-supabase` package doesn't exist in the Python package registry. There's no official Supabase MCP server available.

## ✅ Alternative Solution

Instead of relying on MCP for Supabase management, let's set up your backend properly using the standard approach:

### 1. Create Supabase Project Manually

1. **Go to Supabase Dashboard**
   - Visit: https://supabase.com/dashboard
   - Sign up/login to your account
   - Click "New Project"

2. **Project Configuration**
   - **Name**: `synapse-social-app`
   - **Organization**: Choose your organization
   - **Database Password**: Generate a strong password (save it!)
   - **Region**: Choose closest to your location
   - Click "Create new project"

3. **Wait for Setup** (2-3 minutes)

### 2. Get Your Credentials

Once your project is ready:

1. Go to **Settings** → **API**
2. Copy these values:
   - **Project URL**: `https://your-project-ref.supabase.co`
   - **Anon/Public Key**: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`

### 3. Update gradle.properties

Replace the placeholder values:

```properties
# Replace these with your actual Supabase credentials
SUPABASE_URL=https://your-actual-project-ref.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.your-actual-anon-key
```

### 4. Set Up Database Schema

1. In Supabase dashboard, go to **SQL Editor**
2. Create a new query
3. Copy and paste the entire contents of `supabase-database-schema.sql`
4. Click "Run" to execute
5. Verify all tables were created in the **Table Editor**

### 5. Configure Security (RLS)

1. In **SQL Editor**, create another new query
2. Copy and paste the entire contents of `supabase-rls-policies.sql`
3. Click "Run" to execute
4. This sets up Row Level Security for all your tables

### 6. Set Up Storage

1. In **SQL Editor**, create another new query
2. Copy and paste the entire contents of `supabase-storage-setup.sql`
3. Click "Run" to execute
4. Go to **Storage** in the sidebar to verify buckets were created:
   - `profile-photos`
   - `cover-photos`
   - `post-attachments`
   - `chat-attachments`
   - `stories`

### 7. Enable Real-time

1. In **SQL Editor**, create another new query
2. Copy and paste the entire contents of `supabase-realtime-setup.sql`
3. Click "Run" to execute
4. Go to **Database** → **Replication** to verify real-time is enabled

### 8. Test Your Setup

```bash
# Clean and rebuild your Android project
./gradlew clean
./gradlew build
```

If successful, your app should now connect to Supabase without errors!

## 🔧 Managing Supabase Without MCP

Since MCP isn't available for Supabase, you can manage your backend using:

### Supabase Dashboard
- **Database**: View tables, run queries, manage data
- **Authentication**: Set up auth providers, manage users
- **Storage**: Upload files, manage buckets
- **Edge Functions**: Deploy serverless functions
- **Logs**: Monitor database and API logs

### Supabase CLI (Optional)
Install for local development:
```bash
npm install -g supabase
supabase login
supabase init
supabase start
```

### Direct API Testing
Use tools like Postman or curl to test your Supabase API:
```bash
curl -X GET 'https://your-project.supabase.co/rest/v1/users' \
  -H "apikey: your-anon-key" \
  -H "Authorization: Bearer your-anon-key"
```

## 🎯 Next Steps

1. **Complete the manual setup above**
2. **Test your Android app connection**
3. **Set up authentication in your app**
4. **Test database operations**
5. **Implement real-time features**

## 🆘 Troubleshooting

If you encounter issues:

1. **Build Errors**: Check that your credentials are correct in `gradle.properties`
2. **Database Errors**: Verify all SQL scripts ran successfully
3. **Auth Issues**: Check authentication settings in Supabase dashboard
4. **Storage Issues**: Verify storage buckets and policies are set up
5. **Real-time Issues**: Check that real-time is enabled for your tables

Your Supabase backend will work perfectly without MCP - the dashboard and CLI provide all the management tools you need!