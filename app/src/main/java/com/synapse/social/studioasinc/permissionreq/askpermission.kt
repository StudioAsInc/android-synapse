package com.synapse.social.studioasinc.permissionreq

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.synapse.social.studioasinc.MainActivity
import java.util.ArrayList

class AskPermission(private val activity: Activity) {

    private var permissionsToRequest = ArrayList<String>()
    private var currentPermissionIndex = 0

    companion object {
        const val PERMISSION_REQUEST_CODE = 1000
    }

    fun checkAndRequestPermissions() {
        permissionsToRequest = getNeededPermissions()

        if (permissionsToRequest.isEmpty()) {
            startMainActivity()
            return
        }

        requestNextPermission()
    }

    private fun getNeededPermissions(): ArrayList<String> {
        val neededPermissions = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        return neededPermissions
    }

    private fun requestNextPermission() {
        if (currentPermissionIndex < permissionsToRequest.size) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(permissionsToRequest[currentPermissionIndex]),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // All permissions have been requested
            if (areAllPermissionsGranted()) {
                startMainActivity()
            } else {
                showPermissionDeniedMessage()
            }
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            currentPermissionIndex++
            requestNextPermission()
        }
    }

    fun areAllPermissionsGranted(): Boolean {
        return getNeededPermissions().isEmpty()
    }


    private fun startMainActivity() {
        val intent = Intent(activity, MainActivity::class.java)
        activity.startActivity(intent)
        activity.finish()
    }

    private fun showPermissionDeniedMessage() {
        // You can customize this message
        // SketchwareUtil.showMessage(activity, "App needs permissions to work properly!")
        // For now, let's just finish the activity
        activity.finish()
    }

    fun cleanup() {
        // No-op
    }
}