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
import java.util.Calendar;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.LifecycleOwner;
import com.synapse.social.studioasinc.util.UpdateManager;
import android.os.Bundle;
import android.app.Activity;
import java.lang.ref.WeakReference;

public class SynapseApp extends Application implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    
    private static Context mContext;
    private Thread.UncaughtExceptionHandler mExceptionHandler;
    private WeakReference<Activity> currentActivity;
    
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
        
        // Aggressively disable all Firebase crypto and encrypted storage
        System.setProperty("firebase.auth.disable.encrypted.storage", "true");
        System.setProperty("firebase.storage.disable.encryption", "true");
        System.setProperty("firebase.database.disable.encryption", "true");
        System.setProperty("firebase.firestore.disable.encryption", "true");
        System.setProperty("firebase.disable.keystore", "true");
        System.setProperty("firebase.disable.crypto", "true");
        
        // Clear any existing crypto preferences
        try {
            getSharedPreferences("firebear_storage_crypto", Context.MODE_PRIVATE).edit().clear().apply();
            getSharedPreferences("com.google.firebase.auth", Context.MODE_PRIVATE).edit().clear().apply();
        } catch (Exception e) {
            Log.w("SynapseApp", "Could not clear crypto prefs", e);
        }
        
        // Initialize Firebase with disabled encrypted storage to prevent keystore failures
        FirebaseConfig.initializeFirebase(this);
        FirebaseApp.initializeApp(this);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        FirebaseConfig.forceRegularStorage(this);
        
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
        
        // Initialize OneSignal
        final String ONESIGNAL_APP_ID = "044e1911-6911-4871-95f9-d60003002fe2";
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);

        // OneSignal user state observer removed due to API compatibility issues

        // The IPushSubscriptionObserver has been removed.
        // User identification is now handled by calling OneSignalManager.loginUser(uid)
        // from AuthActivity and CompleteProfileActivity.

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        currentActivity = new WeakReference<>(activity);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        currentActivity = new WeakReference<>(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        currentActivity.clear();
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getDisplayName() != null && !mAuth.getCurrentUser().getDisplayName().isEmpty()) {
            PresenceManager.goOnline(mAuth.getCurrentUser().getUid());
        }
        Activity activity = currentActivity != null ? currentActivity.get() : null;
        if (activity instanceof MainActivity) {
            UpdateManager updateManager = new UpdateManager(activity, new Runnable() {
                @Override
                public void run() {
                    ((MainActivity) activity).proceedToAuthCheck();
                }
            });
            updateManager.checkForUpdate();
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getDisplayName() != null && !mAuth.getCurrentUser().getDisplayName().isEmpty()) {
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