# Fix for Issue #217: Media Posting Permission Error

## Problems Addressed

### Problem 1: UI Issue (Screenshot)
- Requires visual inspection of the screenshot to determine the specific issue

### Problem 2: Glide SecurityException
**Error**: `SecurityException: Permission Denial: opening provider com.android.providers.media.MediaDocumentsProvider`

**Root Cause**: The app was trying to access `MediaStore.Images.Media.DATA` column directly, which requires `ACTION_OPEN_DOCUMENT` permission on modern Android versions (API 29+).

**Solution**: Replaced direct file path access with `ContentResolver` API.

### Problem 3: Cannot Post Media
**Root Cause**: Media upload was failing because:
1. File paths were being accessed directly instead of using URIs
2. `MediaUploadManager` was trying to read from `File` objects instead of using `ContentResolver`

**Solution**: Updated the entire media handling pipeline to use URIs and `ContentResolver`.

---

## Changes Made

### 1. FileUtil.kt
**File**: `app/src/main/java/com/synapse/social/studioasinc/util/FileUtil.kt`

**Changes**:
- Removed `MediaStore.Images.Media.DATA` column access
- `convertUriToFilePath()` now returns `Uri.toString()` instead of file path
- `getFileSize()` now uses `OpenableColumns.SIZE` instead of `MediaStore.Images.Media.SIZE`

**Before**:
```kotlin
val columnIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)
if (columnIndex != -1) {
    it.getString(columnIndex)
}
```

**After**:
```kotlin
fun convertUriToFilePath(context: Context, uri: Uri): String? {
    return uri.toString()
}
```

### 2. MediaUploadManager.kt
**File**: `app/src/main/java/com/synapse/social/studioasinc/util/MediaUploadManager.kt`

**Changes**:
- Added `Context` parameter to `uploadMultipleMedia()`
- Replaced `File.readBytes()` with `ContentResolver.openInputStream()`
- Removed `java.io.File` import, added `android.net.Uri` import

**Before**:
```kotlin
suspend fun uploadMultipleMedia(
    mediaItems: List<MediaItem>,
    onProgress: (Float) -> Unit,
    onComplete: (List<MediaItem>) -> Unit,
    onError: (String) -> Unit
) {
    val file = File(mediaItem.url)
    val bytes = file.readBytes()
}
```

**After**:
```kotlin
suspend fun uploadMultipleMedia(
    context: Context,
    mediaItems: List<MediaItem>,
    onProgress: (Float) -> Unit,
    onComplete: (List<MediaItem>) -> Unit,
    onError: (String) -> Unit
) {
    val uri = Uri.parse(mediaItem.url)
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
}
```

### 3. CreatePostActivity.kt
**File**: `app/src/main/java/com/synapse/social/studioasinc/CreatePostActivity.kt`

**Changes**:
- Updated `uploadMediaAndSave()` to pass `Context` to `MediaUploadManager`

**Before**:
```kotlin
MediaUploadManager.uploadMultipleMedia(
    selectedMedia,
    onProgress = { ... },
    onComplete = { ... },
    onError = { ... }
)
```

**After**:
```kotlin
MediaUploadManager.uploadMultipleMedia(
    this@CreatePostActivity,
    selectedMedia,
    onProgress = { ... },
    onComplete = { ... },
    onError = { ... }
)
```

### 4. AndroidManifest.xml
**File**: `app/src/main/AndroidManifest.xml`

**Changes**:
- Added `READ_MEDIA_VISUAL_USER_SELECTED` permission for Android 14+ (API 34+)

**Added**:
```xml
<uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />
```

This permission allows partial media access on Android 14+, where users can select specific photos/videos instead of granting full media library access.

---

## Technical Details

### Why ContentResolver?
Modern Android (API 29+) uses Scoped Storage, which restricts direct file path access. The `ContentResolver` API provides secure access to media files through URIs without requiring broad storage permissions.

### Benefits
1. **Security**: No longer requires `ACTION_OPEN_DOCUMENT` permission
2. **Compatibility**: Works across all Android versions (API 26+)
3. **Privacy**: Respects Android's scoped storage model
4. **Reliability**: Eliminates `SecurityException` errors

### Supabase Sync
The media upload now properly:
1. Reads media from URIs using `ContentResolver`
2. Uploads to Supabase Storage buckets (`post-images`, `post-videos`)
3. Returns public URLs for database storage
4. Maintains sync between Android app and web app

---

## Testing Checklist

- [ ] Test media selection from gallery
- [ ] Test posting single image
- [ ] Test posting multiple images (up to 10)
- [ ] Test posting video
- [ ] Test posting mixed media (images + videos)
- [ ] Verify Glide loads images without SecurityException
- [ ] Verify media appears in Supabase Storage
- [ ] Verify posts sync with web app
- [ ] Test on Android 13 (API 33)
- [ ] Test on Android 14+ (API 34+) with partial media access

---

## Related Files

### Not Modified (but related)
- `app/src/main/java/com/synapse/social/studioasinc/chat/service/MediaUploadManager.kt` - Different class for chat media
- `app/src/main/java/com/synapse/social/studioasinc/model/MediaItem.kt` - Data model (no changes needed)

---

## References

- [Android Scoped Storage](https://developer.android.com/training/data-storage#scoped-storage)
- [ContentResolver Documentation](https://developer.android.com/reference/android/content/ContentResolver)
- [Photo Picker (Android 14+)](https://developer.android.com/training/data-storage/shared/photopicker)
- [Supabase Storage Documentation](https://supabase.com/docs/guides/storage)
