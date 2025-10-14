
package com.synapse.social.studioasinc

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import java.util.Calendar

class SynapseApp : Application(), DefaultLifecycleObserver {

    private var mExceptionHandler: Thread.UncaughtExceptionHandler? = null

    companion object {
        lateinit var mContext: Context
        lateinit var mCalendar: Calendar
    }

    override fun onCreate() {
        super.onCreate()
        mContext = this
        mExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        mCalendar = Calendar.getInstance()

        createNotificationChannels()

        Thread.setDefaultUncaughtExceptionHandler { mThread, mThrowable ->
            val mIntent = Intent(mContext, DebugActivity::class.java)
            mIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            mIntent.putExtra("error", Log.getStackTraceString(mThrowable))
            mContext.startActivity(mIntent)
            mExceptionHandler?.uncaughtException(mThread, mThrowable)
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        val ONESIGNAL_APP_ID = "044e1911-6911-4871-95f9-d60003002fe2"
        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)

        ProcessLifecycleOwner.get().lifecycleScope.launch {
            OneSignal.Notifications.requestPermission(true)
        }

        OneSignal.Notifications.addClickListener(NotificationClickHandler())
    }

    override fun onStart(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            val user = Supabase.client.auth.currentUserOrNull()
            if (user != null) {
                PresenceManager.goOnline(user.id)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            val user = Supabase.client.auth.currentUserOrNull()
            if (user != null) {
                PresenceManager.goOffline(user.id)
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val messagesChannel = NotificationChannel(
                "messages",
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chat message notifications"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationManager.IMPORTANCE_PRIVATE
            }

            val generalChannel = NotificationChannel(
                "general",
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
                enableLights(false)
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }
}
