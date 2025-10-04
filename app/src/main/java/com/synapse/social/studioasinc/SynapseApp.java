package com.synapse.social.studioasinc;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;
import com.onesignal.user.subscriptions.IPushSubscriptionObserver;
import com.onesignal.user.subscriptions.PushSubscriptionChangedState;
import com.synapse.social.studioasinc.backend.IAuthenticationService;
import com.synapse.social.studioasinc.backend.IDatabaseService;
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService;
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService;
import java.util.Calendar;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.LifecycleOwner;
import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.createSupabaseClient;
import io.github.jan.supabase.gotrue.GoTrue;
import io.github.jan.supabase.postgrest.Postgrest;
import io.github.jan.supabase.storage.Storage;

public class SynapseApp extends Application implements DefaultLifecycleObserver {
    
    private static Context mContext;
    private Thread.UncaughtExceptionHandler mExceptionHandler;
    
    private IAuthenticationService authService;
    private IDatabaseService dbService;
    private SupabaseClient supabaseClient;
    
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
        
        // Initialize Supabase client
        supabaseClient = createSupabaseClient(
            "https://apqvyyphlrtmuyjnzmuq.supabase.co",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFwcXZ5eXBobHJ0bXV5am56bXVxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTg3MDUwODcsImV4cCI6MjA3NDI4MTA4N30.On7kjijj7bUg_xzr2HwCTYvLaV-f_1aDYqVTfKai7gc",
            builder -> {
                builder.install(GoTrue.class);
                builder.install(Postgrest.class);
                builder.install(Storage.class);
            }
        );

        // Initialize Supabase services
        authService = new SupabaseAuthenticationService(supabaseClient);
        dbService = new SupabaseDatabaseService(supabaseClient);
        
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

        // Add a subscription observer to get the Player ID and save it to the database
        OneSignal.getUser().getPushSubscription().addObserver(new IPushSubscriptionObserver() {
            @Override
            public void onPushSubscriptionChange(@NonNull PushSubscriptionChangedState state) {
                if (state.getCurrent().getOptedIn()) {
                    String playerId = state.getCurrent().getId();
                    if (authService.getCurrentUser() != null && playerId != null) {
                        String userUid = authService.getCurrentUser().getUid();
                        OneSignalManager.savePlayerIdToRealtimeDatabase(userUid, playerId);
                    }
                }
            }
        });
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (authService.getCurrentUser() != null) {
            PresenceManager.goOnline(authService.getCurrentUser().getUid());
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (authService.getCurrentUser() != null) {
            PresenceManager.goOffline(authService.getCurrentUser().getUid());
        }
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

    @NonNull
    public IAuthenticationService getAuthenticationService() {
        return authService;
    }

    @NonNull
    public IDatabaseService getDatabaseService() {
        return dbService;
    }
    
    public SupabaseClient getSupabaseClient() {
        return supabaseClient;
    }
}