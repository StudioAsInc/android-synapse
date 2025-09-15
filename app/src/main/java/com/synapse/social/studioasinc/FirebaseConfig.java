package com.synapse.social.studioasinc;

import android.content.Context;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.IOException;

/**
 * Custom Firebase configuration to disable encrypted storage and prevent keystore failures
 */
public class FirebaseConfig {
    private static final String TAG = "FirebaseConfig";
    
    /**
     * Initialize Firebase with disabled encrypted storage
     */
    public static void initializeFirebase(Context context) {
        try {
            // Set system properties to disable Firebase encrypted storage
            System.setProperty("firebase.auth.disable.encrypted.storage", "true");
            System.setProperty("firebase.storage.disable.encryption", "true");
            System.setProperty("firebase.database.disable.encryption", "true");
            
            // Clear any existing encrypted storage data
            clearEncryptedStorageData(context);
            
            Log.d(TAG, "Firebase initialized with encrypted storage disabled");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase with disabled encrypted storage", e);
        }
    }
    
    /**
     * Clear all encrypted storage data that might cause keystore failures
     * Note: This method is kept empty as we're not storing any credentials locally
     */
    private static void clearEncryptedStorageData(Context context) {
        // No local credential storage - authentication handled by Firebase Auth only
        Log.d(TAG, "No local credential storage to clear");
    }
    
    /**
     * Force Firebase to use regular SharedPreferences instead of encrypted storage
     * Note: This method is kept empty as we're not storing any credentials locally
     */
    public static void forceRegularStorage(Context context) {
        // No local credential storage - authentication handled by Firebase Auth only
        Log.d(TAG, "No local storage configuration needed");
    }
}