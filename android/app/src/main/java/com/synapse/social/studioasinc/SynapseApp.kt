package com.synapse.social.studioasinc

import android.app.Application
import android.app.Notification
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
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.user.subscriptions.IPushSubscriptionObserver
import com.onesignal.user.subscriptions.PushSubscriptionChangedState
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

class SynapseApp : Application(), DefaultLifecycleObserver {
    
    private lateinit var exceptionHandler: Thread.UncaughtExceptionHandler
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        private lateinit var context: Context
        
        @JvmStatic
        lateinit var mAuth: SupabaseAuthenticationService
            private set
        
        @JvmStatic
        val mCalendar: Calendar = Calendar.getInstance()
        
        @JvmStatic
        fun getContext(): Context = context
    }
    
    override fun onCreate() {
        super<Application>.onCreate()
        context = this
        exceptionHandler = Thread.getDefaultUncaughtExceptionHandler() 
            ?: Thread.UncaughtExceptionHandler { _, _ -> }
        
        // Initialize Supabase client (already initialized in SupabaseClient singleton)
        
        // Create notification channels
        createNotificationChannels()
        
        mAuth = SupabaseAuthenticationService()
        
        // Set up global exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val intent = Intent(context, DebugActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("error", Log.getStackTraceString(throwable))
            }
            context.startActivity(intent)
            exceptionHandler.uncaughtException(thread, throwable)
        }
        
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        // Initialize OneSignal
        initializeOneSignal()
    }
    
    private fun initializeOneSignal() {
        val oneSignalAppId = "044e1911-6911-4871-95f9-d60003002fe2"
        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        OneSignal.initWithContext(this, oneSignalAppId)
        
        // Prompt for push notifications using Kotlin coroutine
        applicationScope.launch {
            try {
                val granted = OneSignal.Notifications.requestPermission(true)
                Log.i("OneSignal", "Notification permission granted: $granted")
            } catch (e: Exception) {
                Log.e("OneSignal", "Error requesting notification permission", e)
            }
        }
        
        // Add a subscription observer to get the Player ID and save it to Supabase
        OneSignal.User.pushSubscription.addObserver(object : IPushSubscriptionObserver {
            override fun onPushSubscriptionChange(state: PushSubscriptionChangedState) {
                if (state.current.optedIn) {
                    val playerId = state.current.id
                    val userUid = mAuth.getCurrentUserId()
                    if (userUid != null && playerId != null) {
                        OneSignalManager.savePlayerIdToSupabase(userUid, playerId)
                    }
                }
            }
        })
    }
    
    override fun onStart(owner: LifecycleOwner) {
        mAuth.getCurrentUserId()?.let { userUid ->
            PresenceManager.goOnline(userUid)
        }
    }
    
    override fun onStop(owner: LifecycleOwner) {
        mAuth.getCurrentUserId()?.let { userUid ->
            PresenceManager.goOffline(userUid)
        }
    }
    
    private fun createNotificationChannels() {
        // Create notification channels for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            // Messages channel
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
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            
            // General notifications channel
            val generalChannel = NotificationChannel(
                "general",
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
                enableLights(false)
                enableVibration(false)
            }
            
            // Create the channels
            notificationManager?.createNotificationChannel(messagesChannel)
            notificationManager?.createNotificationChannel(generalChannel)
        }
    }
}
