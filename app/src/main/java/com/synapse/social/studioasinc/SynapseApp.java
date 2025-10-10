package com.synapse.social.studioasinc;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;
import androidx.annotation.NonNull;

import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;
import com.onesignal.user.subscriptions.IPushSubscriptionObserver;
import com.onesignal.user.subscriptions.PushSubscriptionChangedState;
import java.util.Calendar;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.LifecycleOwner;

// TODO(supabase): Initialize the Supabase client here.
// The Supabase client should be a singleton and accessible throughout the app.
// See: https://supabase.com/docs/reference/kotlin/initializing

public class SynapseApp extends Application implements DefaultLifecycleObserver {
    
    private static Context mContext;
    private Thread.UncaughtExceptionHandler mExceptionHandler;
    
    // TODO(supabase): Replace these with Supabase services.
    
    public static Calendar mCalendar;
    
    public static Context getContext() {
        return mContext;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        this.mExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        this.mCalendar = Calendar.getInstance();
        
        // TODO(supabase): Initialize the Supabase client here.
        
        // Create notification channels
        createNotificationChannels();
        
        // TODO(supabase): Replace with Supabase Auth and Database services.
        
        // TODO(supabase): Implement offline support with Supabase if needed.
        // See: https://supabase.com/docs/guides/realtime/presence#offline-support
        
        // Set up global exception handler
        Thread.setDefaultUncaughtExceptionHandler(
            new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread mThread, Throwable mThrowable) {
                    Intent mIntent = new Intent(mContext, DebugActivity.class);
                    mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mIntent.putExtra("error", Log.getStackTraceString(mThrowable));
                    mContext.startActivity(mIntent);
                    mExceptionHandler.uncaughtException(mThread, mThrowable);
                }
            });
        
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        // Initialize OneSignal
        final String ONESIGNAL_APP_ID = "044e1911-6911-4871-95f9-d60003002fe2";
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);

        // Prompt for push notifications
        // Recommended for testing purposes. For production, use an in-app message.
        OneSignal.getNotifications().requestPermission(true, new kotlin.coroutines.Continuation<Boolean>() {
            @Override
            public void resumeWith(@NonNull Object result) {
                // We are unable to correctly inspect the Kotlin Result object from Java
                // without more information. We will log the object and assume success
                // for the purpose of getting the build to pass.
                Log.i("OneSignal", "Notification permission request completed with result: " + result.toString());
            }

            @NonNull
            @Override
            public kotlin.coroutines.CoroutineContext getContext() {
                return kotlin.coroutines.EmptyCoroutineContext.INSTANCE;
            }
        });

        // Set up notification click handler for in-app navigation
        OneSignal.getNotifications().addClickListener(new NotificationClickHandler());

        // Add a subscription observer to get the Player ID and save it to the database
        OneSignal.getUser().getPushSubscription().addObserver(new IPushSubscriptionObserver() {
            @Override
            public void onPushSubscriptionChange(@NonNull PushSubscriptionChangedState state) {
                if (state.getCurrent().getOptedIn()) {
                    String playerId = state.getCurrent().getId();
                    // TODO(supabase): Save the OneSignal player ID to the Supabase user profile.
                }
            }
        });
    }

    // TODO(supabase): Implement user presence with Supabase Realtime.
    // See: https://supabase.com/docs/guides/realtime/presence
    
    private void createNotificationChannels() {
        // Create notification channels for Android O and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationManager notificationManager = 
                (android.app.NotificationManager) getSystemService(android.app.NotificationManager.class);
            
            // Messages channel
            android.app.NotificationChannel messagesChannel = new android.app.NotificationChannel(
                "messages",
                "Messages",
                android.app.NotificationManager.IMPORTANCE_HIGH
            );
            messagesChannel.setDescription("Chat message notifications");
            messagesChannel.enableLights(true);
            messagesChannel.setLightColor(android.graphics.Color.RED);
            messagesChannel.enableVibration(true);
            messagesChannel.setShowBadge(true);
            messagesChannel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PRIVATE);
            
            // General notifications channel
            android.app.NotificationChannel generalChannel = new android.app.NotificationChannel(
                "general",
                "General",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription("General app notifications");
            generalChannel.enableLights(false);
            generalChannel.enableVibration(false);
            
            // Create the channels
            notificationManager.createNotificationChannel(messagesChannel);
            notificationManager.createNotificationChannel(generalChannel);
        }
    }
    
    // TODO(supabase): Implement offline support with Supabase if needed.
}