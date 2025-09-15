package com.synapse.social.studioasinc;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;
import com.onesignal.user.subscriptions.IPushSubscriptionObserver;
import com.onesignal.user.subscriptions.PushSubscriptionChangedState;
import java.util.Calendar;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.LifecycleOwner;

public class SynapseApp extends Application implements DefaultLifecycleObserver {
    
    private static Context mContext;
    private Thread.UncaughtExceptionHandler mExceptionHandler;
    
    public static FirebaseAuth mAuth;
    
    public static DatabaseReference getCheckUserReference;
    public static DatabaseReference setUserStatusRef;
    public static DatabaseReference setUserStatusReference;
    
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
        
        // Initialize Firebase with disk persistence
        FirebaseApp.initializeApp(this);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        
        // Create notification channels
        createNotificationChannels();
        
        this.mAuth = FirebaseAuth.getInstance();
        this.getCheckUserReference = FirebaseDatabase.getInstance().getReference("skyline/users");
        this.setUserStatusRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        
        // Keep users data synced for offline use
        getCheckUserReference.keepSynced(true);
        
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
        OneSignal.getNotifications().requestPermission(true);

        // Set up notification click handler for in-app navigation
        OneSignal.getNotifications().addClickListener(new NotificationClickHandler());

        // Add a subscription observer to get the Player ID and save it to Firestore
        OneSignal.getUser().getPushSubscription().addObserver(new IPushSubscriptionObserver() {
            @Override
            public void onPushSubscriptionChange(@NonNull PushSubscriptionChangedState state) {
                if (state.getCurrent().getOptedIn()) {
                    String playerId = state.getCurrent().getId();
                    if (mAuth.getCurrentUser() != null && playerId != null) {
                        String userUid = mAuth.getCurrentUser().getUid();
                        OneSignalManager.savePlayerIdToRealtimeDatabase(userUid, playerId);
                    }
                }
            }
        });
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mAuth.getCurrentUser() != null) {
            PresenceManager.goOnline(mAuth.getCurrentUser().getUid());
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mAuth.getCurrentUser() != null) {
            PresenceManager.goOffline(mAuth.getCurrentUser().getUid());
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
    
    /**
     * Enable offline persistence for any database reference
     * @param ref DatabaseReference to enable offline sync for
     */
    public static void enableOfflineSync(DatabaseReference ref) {
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        ref.keepSynced(true);
    }
}