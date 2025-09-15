package com.synapse.social.studioasinc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Asynchronous upload service with progress notifications for attachments
 */
public class AsyncUploadService {
    private static final String TAG = "AsyncUploadService";
    private static final String CHANNEL_ID = "upload_progress";
    private static final int NOTIFICATION_ID_BASE = 1000;
    
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Map<String, Integer> uploadNotificationIds = new HashMap<>();
    private static final Map<String, String> uploadFileNames = new HashMap<>();
    
    public interface UploadProgressListener {
        void onProgress(String filePath, int percent);
        void onSuccess(String filePath, String url, String publicId);
        void onFailure(String filePath, String error);
    }
    
    /**
     * Start asynchronous upload with progress notification
     */
    public static void uploadWithNotification(Context context, String filePath, String fileName, UploadProgressListener listener) {
        if (context == null || filePath == null || fileName == null) {
            Log.e(TAG, "Invalid parameters for upload");
            return;
        }
        
        // Create notification channel for Android O+
        createNotificationChannel(context);
        
        // Generate unique notification ID for this upload
        int notificationId = NOTIFICATION_ID_BASE + uploadNotificationIds.size();
        uploadNotificationIds.put(filePath, notificationId);
        uploadFileNames.put(filePath, fileName);
        
        // Show initial notification
        showUploadNotification(context, notificationId, fileName, 0, "Starting upload...");
        
        // Start upload in background thread
        executor.execute(() -> {
            try {
                UploadFiles.uploadFile(filePath, fileName, new UploadFiles.UploadCallback() {
                    @Override
                    public void onProgress(int percent) {
                        // Update notification and notify listener
                        showUploadNotification(context, notificationId, fileName, percent, "Uploading...");
                        if (listener != null) {
                            listener.onProgress(filePath, percent);
                        }
                    }
                    
                    @Override
                    public void onSuccess(String url, String publicId) {
                        // Show success notification
                        showSuccessNotification(context, notificationId, fileName);
                        
                        // Clean up
                        uploadNotificationIds.remove(filePath);
                        uploadFileNames.remove(filePath);
                        
                        // Notify listener
                        if (listener != null) {
                            listener.onSuccess(filePath, url, publicId);
                        }
                    }
                    
                    @Override
                    public void onFailure(String error) {
                        // Show failure notification
                        showFailureNotification(context, notificationId, fileName, error);
                        
                        // Clean up
                        uploadNotificationIds.remove(filePath);
                        uploadFileNames.remove(filePath);
                        
                        // Notify listener
                        if (listener != null) {
                            listener.onFailure(filePath, error);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Upload execution error: " + e.getMessage());
                showFailureNotification(context, notificationId, fileName, "Upload failed: " + e.getMessage());
                uploadNotificationIds.remove(filePath);
                uploadFileNames.remove(filePath);
                if (listener != null) {
                    listener.onFailure(filePath, "Upload execution error: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Cancel upload and remove notification
     */
    public static void cancelUpload(Context context, String filePath) {
        if (context == null || filePath == null) return;
        
        // Cancel the upload
        UploadFiles.cancelUpload(filePath);
        
        // Remove notification
        Integer notificationId = uploadNotificationIds.remove(filePath);
        if (notificationId != null) {
            NotificationManagerCompat.from(context).cancel(notificationId);
        }
        uploadFileNames.remove(filePath);
    }
    
    /**
     * Create notification channel for Android O+
     */
    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Upload Progress",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows progress for file uploads");
            channel.setShowBadge(false);
            channel.setSound(null, null);
            channel.enableVibration(false);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Show upload progress notification
     */
    private static void showUploadNotification(Context context, int notificationId, String fileName, int progress, String status) {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_upload)
                .setContentTitle("Uploading: " + fileName)
                .setContentText(status)
                .setProgress(100, progress, false)
                .setOngoing(false) // Make cancelable
                .setAutoCancel(true) // Auto cancel when tapped
                .setPriority(NotificationCompat.PRIORITY_LOW);
            
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Error showing upload notification: " + e.getMessage());
        }
    }
    
    /**
     * Show upload success notification
     */
    private static void showSuccessNotification(Context context, int notificationId, String fileName) {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_check_circle)
                .setContentTitle("Upload Complete")
                .setContentText(fileName + " uploaded successfully")
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
            
            // Auto-dismiss success notification after 3 seconds
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    NotificationManagerCompat.from(context).cancel(notificationId);
                } catch (Exception e) {
                    Log.e(TAG, "Error auto-dismissing success notification: " + e.getMessage());
                }
            }, 3000);
        } catch (Exception e) {
            Log.e(TAG, "Error showing success notification: " + e.getMessage());
        }
    }
    
    /**
     * Show upload failure notification
     */
    private static void showFailureNotification(Context context, int notificationId, String fileName, String error) {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_error)
                .setContentTitle("Upload Failed")
                .setContentText(fileName + " failed to upload")
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
            
            // Auto-dismiss failure notification after 5 seconds
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    NotificationManagerCompat.from(context).cancel(notificationId);
                } catch (Exception e) {
                    Log.e(TAG, "Error auto-dismissing failure notification: " + e.getMessage());
                }
            }, 5000);
        } catch (Exception e) {
            Log.e(TAG, "Error showing failure notification: " + e.getMessage());
        }
    }
    
    /**
     * Get current upload count
     */
    public static int getActiveUploadCount() {
        return uploadNotificationIds.size();
    }
    
    /**
     * Cancel all uploads and clear notifications
     */
    public static void cancelAllUploads(Context context) {
        if (context == null) return;
        
        for (String filePath : uploadNotificationIds.keySet()) {
            cancelUpload(context, filePath);
        }
    }
}