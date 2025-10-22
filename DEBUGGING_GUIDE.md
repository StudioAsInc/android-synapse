# Debugging Guide for Android Synapse Issues

## Recent Fixes Applied

### 1. Account Creation "Failed" Issue
**Problem**: Sign-up showing failure even when account is created
**Root Cause**: Supabase sign-up succeeds but user session might not be immediately available when email verification is required
**Fix Applied**: 
- Modified `SupabaseAuthenticationService.signUp()` to handle cases where user is created but not immediately accessible
- Added fallback user creation for pending verification cases
- Enhanced logging to track the sign-up flow

### 2. Profile Creation "Data Format Error"
**Problem**: Serialization issues when inserting user profile
**Root Cause**: Improper handling of null values and data types in database insertion
**Fix Applied**:
- Improved `SupabaseDatabaseService.insert()` with better type handling
- Added comprehensive logging for database operations
- Enhanced null value handling in profile data conversion

### 3. Image Upload "Failed to Upload Image"
**Problem**: Image upload failing without clear error messages
**Root Cause**: Missing error handling for various upload failure scenarios
**Fix Applied**:
- Enhanced `SupabaseStorageService.uploadImage()` with detailed error handling
- Added proper MIME type detection and validation
- Improved logging for upload process tracking

## Debugging Steps

### Check Logs
After applying these fixes, you can monitor the following logs:

1. **Sign-up Process**:
   ```
   AuthActivity: Starting sign up for email: [email]
   SupabaseAuth: Auth Step: Starting sign up for email: [email]
   SupabaseAuth: Sign up request completed successfully
   AuthActivity: Sign up successful: needsVerification=true/false
   ```

2. **Profile Creation**:
   ```
   CompleteProfile: Inserting user profile: [data]
   SupabaseDB: Inserting data into table 'users': [data]
   SupabaseDB: Cleaned insert data: [cleaned_data]
   SupabaseDB: Data inserted successfully into table 'users'
   ```

3. **Image Upload**:
   ```
   CompleteProfile: Starting image upload for URI: [uri]
   SupabaseStorage: Starting image upload for URI: [uri]
   SupabaseStorage: Image MIME type: [type]
   SupabaseStorage: Generated filename: [filename]
   SupabaseStorage: Image data size: [size] bytes
   SupabaseStorage: File uploaded successfully: [filename]
   SupabaseStorage: Upload successful, public URL: [url]
   ```

### Common Issues and Solutions

#### 1. Account Creation Still Shows "Failed"
**Check**: Look for these log entries:
- `AuthActivity: Sign up failed: [error message]`
- Check if the error message contains specific Supabase errors

**Possible Causes**:
- Supabase configuration issues (URL/API key)
- Email verification settings in Supabase dashboard
- Network connectivity issues

**Solution**: 
- Verify Supabase credentials in `gradle.properties`
- Check Supabase Auth settings for email confirmation requirements

#### 2. Profile Creation "Data Format Error"
**Check**: Look for these log entries:
- `SupabaseDB: Database insertion failed`
- Check the specific error message in logs

**Possible Causes**:
- Database table schema mismatch
- Missing required columns in `users` table
- Data type mismatches

**Solution**: Ensure your Supabase `users` table has these columns:
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
    created_at TIMESTAMP DEFAULT NOW()
);
```

#### 3. Image Upload Still Failing
**Check**: Look for these log entries:
- `SupabaseStorage: Failed to access 'avatars' bucket`
- `SupabaseStorage: Upload failed: [error]`

**Possible Causes**:
- Missing `avatars` bucket in Supabase Storage
- Incorrect storage permissions
- File size or type restrictions

**Solution**: 
1. Create `avatars` bucket in Supabase Storage dashboard
2. Set bucket to public or configure proper RLS policies
3. Check file size limits (default is usually 50MB)

### Testing Checklist

1. **Account Creation**:
   - [ ] Try creating account with new email
   - [ ] Check Supabase Auth dashboard for new user
   - [ ] Verify success message appears
   - [ ] Check if email verification email is sent

2. **Profile Completion**:
   - [ ] Try completing profile without image
   - [ ] Try completing profile with image
   - [ ] Check Supabase database for new user record
   - [ ] Verify all fields are saved correctly

3. **Error Scenarios**:
   - [ ] Try duplicate username
   - [ ] Try with network disconnected
   - [ ] Try with invalid image file

### Additional Debugging

If issues persist, add this to your `AuthActivity.onCreate()` for more detailed logging:

```kotlin
// Enable debug logging
if (BuildConfig.DEBUG) {
    android.util.Log.d("AuthActivity", "Debug mode enabled")
    android.util.Log.d("AuthActivity", "Supabase URL: ${BuildConfig.SUPABASE_URL}")
    android.util.Log.d("AuthActivity", "Supabase configured: ${SupabaseClient.isConfigured()}")
}
```

### Expected Behavior After Fixes

1. **Sign-up**: Should show "✅ Account created successfully!" and navigate to email verification
2. **Profile**: Should complete without "Data format error" and show "Profile created successfully!"
3. **Image Upload**: Should work without "Failed to upload image" error
4. **Logs**: Should show detailed progress through each step

If you're still experiencing issues after these fixes, please share the specific log output from the Android Studio Logcat for the failing operations.