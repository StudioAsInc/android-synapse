# Supabase + MCP Quick Start Checklist

## ✅ Completed
- [x] MCP Supabase server enabled
- [x] Database schema ready (`supabase-database-schema.sql`)
- [x] RLS policies ready (`supabase-rls-policies.sql`)
- [x] Storage setup ready (`supabase-storage-setup.sql`)
- [x] Real-time setup ready (`supabase-realtime-setup.sql`)

## 🔄 Next Steps (Do These Now)

### 1. Create Supabase Project
- [ ] Go to https://supabase.com/dashboard
- [ ] Sign up/login
- [ ] Click "New Project"
- [ ] Name: `synapse-social-app`
- [ ] Choose region closest to you
- [ ] Set strong database password
- [ ] Wait for project creation (2-3 minutes)

### 2. Get Credentials
- [ ] Go to Settings → API in your Supabase dashboard
- [ ] Copy Project URL (looks like: `https://abc123.supabase.co`)
- [ ] Copy Anon/Public Key (starts with `eyJ...`)

### 3. Update gradle.properties
Replace these lines in `gradle.properties`:
```properties
SUPABASE_URL=https://your-actual-project-id.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.your-actual-key
```

### 4. Test Connection
- [ ] Run: `./gradlew clean`
- [ ] Run: `./gradlew build`
- [ ] Should build without Supabase connection errors

### 5. Set Up Database (In Supabase SQL Editor)
- [ ] Run `supabase-database-schema.sql`
- [ ] Run `supabase-rls-policies.sql`
- [ ] Run `supabase-storage-setup.sql`
- [ ] Run `supabase-realtime-setup.sql`

### 6. Test MCP Integration
Once you have real credentials, you can test MCP by asking Kiro to:
- Query your users table
- Check database connection
- List your storage buckets
- Monitor real-time events

## 🎯 Success Indicators
- ✅ Android app builds without errors
- ✅ MCP can connect to your Supabase project
- ✅ Database tables are created
- ✅ Storage buckets are set up
- ✅ Real-time subscriptions work

## 🆘 Need Help?
If you get stuck:
1. Check the MCP logs in Kiro
2. Verify your Supabase project is active
3. Double-check your credentials
4. Make sure you're using the correct project URL format