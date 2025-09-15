package com.synapse.social.studioasinc.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Helper class for managing notification permissions in Android 13+ (API 33+)
 */
object NotificationPermissionHelper {
    
    private const val TAG = "NotificationPermissionHelper"
    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    
    /**
     * Checks if the app has notification permission.
     * For Android 13+ (API 33+), this checks the POST_NOTIFICATIONS permission.
     * For older versions, this always returns true as notifications are enabled by default.
     *
     * @param context The context to check permissions in
     * @return true if notifications are allowed, false otherwise
     */
    @JvmStatic
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 12 and below, notifications are enabled by default
            true
        }
    }
    
    /**
     * Requests notification permission if needed.
     * Only requests permission on Android 13+ (API 33+).
     *
     * @param activity The activity to request permissions from
     * @return true if permission was already granted or not needed, false if permission was requested
     */
    @JvmStatic
    fun requestNotificationPermissionIfNeeded(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                Log.i(TAG, "Requesting POST_NOTIFICATIONS permission")
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
                return false
            }
        }
        return true
    }
    
    /**
     * Handles the result of a permission request.
     * Call this from your Activity's onRequestPermissionsResult method.
     *
     * @param requestCode The request code from onRequestPermissionsResult
     * @param permissions The permissions array from onRequestPermissionsResult
     * @param grantResults The grant results array from onRequestPermissionsResult
     * @param callback Callback to be called with the result
     */
    @JvmStatic
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        callback: (Boolean) -> Unit
    ) {
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() && 
                         grantResults[0] == PackageManager.PERMISSION_GRANTED
            
            if (granted) {
                Log.i(TAG, "POST_NOTIFICATIONS permission granted")
            } else {
                Log.w(TAG, "POST_NOTIFICATIONS permission denied")
            }
            
            callback(granted)
        }
    }
    
    /**
     * Checks if the app should show a rationale for requesting notification permission.
     *
     * @param activity The activity to check
     * @return true if rationale should be shown, false otherwise
     */
    fun shouldShowNotificationPermissionRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            false
        }
    }
    
    /**
     * Opens the app's notification settings page.
     *
     * @param context The context to use for opening settings
     */
    fun openNotificationSettings(context: Context) {
        try {
            val intent = android.content.Intent().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                } else {
                    action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                }
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open notification settings", e)
        }
    }
}