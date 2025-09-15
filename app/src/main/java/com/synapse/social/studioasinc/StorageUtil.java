/*
 * MIT License
 *
 * Copyright (c) 2025 StudioAs Inc. & Synapse
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
 
package com.synapse.social.studioasinc;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StorageUtil {

    private static final String TAG = "StorageUtil";
    private static final int BUFFER_SIZE = 8192;

    public static void pickSingleFile(Activity activity, String mimeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void pickMultipleFiles(Activity activity, String mimeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(Intent.createChooser(intent, "Select Files"), requestCode);
    }

    public static void pickDirectory(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        activity.startActivityForResult(intent, requestCode);
    }
    
    public static void createFile(Activity activity, String mimeType, String defaultName, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, defaultName);
        activity.startActivityForResult(intent, requestCode);
    }

    public static Uri saveImageToGallery(Context context, Bitmap bitmap, String fileName, String subFolder, Bitmap.CompressFormat format) {
        String mimeType = format == Bitmap.CompressFormat.PNG ? "image/png" : "image/jpeg";
        String extension = format == Bitmap.CompressFormat.PNG ? ".png" : ".jpg";
        String finalFileName = fileName + extension;

        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, finalFileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String relativePath = Environment.DIRECTORY_PICTURES;
            if (subFolder != null && !subFolder.isEmpty()) {
                relativePath += File.separator + subFolder;
            }
            values.put(MediaStore.Images.Media.RELATIVE_PATH, relativePath);
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Uri itemUri = resolver.insert(collectionUri, values);

        if (itemUri == null) {
            Log.e(TAG, "Failed to create new MediaStore entry for image.");
            return null;
        }

        try (OutputStream os = resolver.openOutputStream(itemUri)) {
            if (os == null) throw new IOException("Failed to get output stream for URI: " + itemUri);
            bitmap.compress(format, 95, os);
        } catch (IOException e) {
            Log.e(TAG, "Failed to save bitmap.", e);
            if (itemUri != null) {
                resolver.delete(itemUri, null, null);
            }
            return null;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(itemUri, values, null, null);
        }

        return itemUri;
    }
    
    public static Uri saveVideoToGallery(Context context, File videoFile, String fileName, String subFolder) {
        String mimeType = "video/mp4";
        String finalFileName = fileName.endsWith(".mp4") ? fileName : fileName + ".mp4";

        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, finalFileName);
        values.put(MediaStore.Video.Media.MIME_TYPE, mimeType);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String relativePath = Environment.DIRECTORY_MOVIES;
            if (subFolder != null && !subFolder.isEmpty()) {
                relativePath += File.separator + subFolder;
            }
            values.put(MediaStore.Video.Media.RELATIVE_PATH, relativePath);
            values.put(MediaStore.Video.Media.IS_PENDING, 1);
        }

        Uri collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Uri itemUri = resolver.insert(collectionUri, values);

        if (itemUri == null) {
            Log.e(TAG, "Failed to create new MediaStore entry for video.");
            return null;
        }

        try (OutputStream os = resolver.openOutputStream(itemUri);
             InputStream is = new java.io.FileInputStream(videoFile)) {
            if (os == null) throw new IOException("Failed to get output stream.");
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to save video.", e);
            if (itemUri != null) {
                resolver.delete(itemUri, null, null);
            }
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear();
            values.put(MediaStore.Video.Media.IS_PENDING, 0);
            resolver.update(itemUri, values, null, null);
        }

        return itemUri;
    }

    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to get file name for URI: " + uri, e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }
    
    public static String getPathFromUri(final Context context, final Uri uri) {
        if (uri == null) {
            return null;
        }

        String path = null;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    path = Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                final String id = DocumentsContract.getDocumentId(uri);
                if (id != null && id.startsWith("raw:")) {
                    path = id.substring(4);
                } else {
                    try {
                        final Uri contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
                        path = getDataColumn(context, contentUri, null, null);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Could not parse download ID: " + id, e);
                        path = null;
                    }
                }
            } else if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                path = getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        
        if (path == null) {
            if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
                path = getDataColumn(context, uri, null, null);
            } else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
                path = uri.getPath();
            }
        }

        if (path == null) {
            path = copyToCache(context, uri);
        }

        return path;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        final String column = MediaStore.MediaColumns.DATA;
        final String[] projection = {column};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            Log.w(TAG, "getDataColumn failed for URI: " + uri, e);
        }
        return null;
    }
    
    private static String copyToCache(Context context, Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return null;
            
            String fileName = getFileName(context, uri);
            if (fileName == null || fileName.isEmpty()) {
                fileName = "temp_file_" + System.currentTimeMillis();
            }

            File cacheFile = new File(context.getCacheDir(), fileName);
            try (OutputStream outputStream = new FileOutputStream(cacheFile)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            return cacheFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy URI to cache: " + uri, e);
            return null;
        }
    }

    public static Bitmap decodeSampledBitmapFromUri(Context context, Uri uri, int reqWidth, int reqHeight) throws IOException {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            
            options.inJustDecodeBounds = false;
            try (InputStream is2 = context.getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(is2, null, options);
            }
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}