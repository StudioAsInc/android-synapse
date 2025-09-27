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

import io.github.jan_tennert.supabase.SupabaseClient;
import com.synapse.social.studioasinc.util.UserUtils; // Import UserUtils
import com.synapse.social.studioasinc.util.NotificationUtils; // Import NotificationUtils

public class SynapseApp extends Application implements DefaultLifecycleObserver {
    
    private static Context mContext;
    private Thread.UncaughtExceptionHandler mExceptionHandler;

    private SupabaseClient supabase;

    // Removed static Calendar mCalendar as it's no longer used after Firebase removal
    
    public static Context getContext() {
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        this.mExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        this.mCalendar = Calendar.getInstance();

        // Initialize Supabase
        supabase = new SupabaseClient(
                "YOUR_SUPABASE_URL", // Replace with your Supabase URL
                "YOUR_SUPABASE_ANON_KEY"  // Replace with your Supabase anon key
        );
        // Initialize UserUtils with the Supabase client
        UserUtils.setSupabaseClient(supabase);
        // Initialize NotificationUtils with the Supabase client
        NotificationUtils.setSupabaseClient(supabase);
        
        // Create notification channels
        createNotificationChannels();

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

        // Add a subscription observer to get the Player ID and save it to Firestore
        OneSignal.getUser().getPushSubscription().addObserver(new IPushSubscriptionObserver() {
            @Override
            public void onPushSubscriptionChange(@NonNull PushSubscriptionChangedState state) {
                if (state.getCurrent().getOptedIn()) {
                    String playerId = state.getCurrent().getId();
                    // TODO: Save this playerId to Supabase for the current user if needed for custom targeting
                }
            }
        });
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
    }
    
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
    
}