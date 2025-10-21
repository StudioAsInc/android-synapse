# Test MCP Connection with Supabase

Once you have your Supabase project set up and credentials configured, you can test the MCP connection using these steps:

## 1. Verify MCP Server Status

Check that the Supabase MCP server is running and connected in Kiro.

## 2. Test Basic Connection

Try these MCP commands in Kiro to test your connection:

```
List all tables in your Supabase database
Query the users table structure
Check if RLS is enabled on tables
```

## 3. Test Database Operations

Once connected, you can:

- Query your database directly from Kiro
- Monitor real-time changes
- Debug database issues
- Manage your Supabase project settings

## 4. Common Issues

If MCP connection fails:

1. **Check credentials**: Ensure SUPABASE_URL and SUPABASE_ANON_KEY are correct
2. **Verify project status**: Make sure your Supabase project is active
3. **Check MCP server**: Restart the MCP server if needed
4. **Network issues**: Ensure you have internet connectivity

## 5. Success Indicators

You'll know MCP is working when:

- ✓ You can query your database from Kiro
- ✓ Real-time updates are visible
- ✓ No connection errors in MCP logs
- ✓ Your Android app connects successfully

## Next Steps After MCP Setup

1. **Authentication**: Set up user registration/login
2. **Database Testing**: Create test users and posts
3. **Real-time Testing**: Test chat functionality
4. **Storage Testing**: Upload profile photos
5. **Push Notifications**: Configure FCM integration