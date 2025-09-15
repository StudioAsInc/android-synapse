package com.synapse.social.studioasinc.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Simple storage helper to replace any keystore-based storage
 * This stores data in plain SharedPreferences without encryption
 */
public class SimpleStorageHelper {
    
    private static final String PREFS_NAME = "simple_storage_prefs";
    private static final String TAG = "SimpleStorageHelper";
    
    // Prevent instantiation
    private SimpleStorageHelper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Store a string value
     * @param context Application context
     * @param key Key to store under
     * @param value Value to store
     */
    public static void storeString(Context context, String key, String value) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(key, value).apply();
            Log.d(TAG, "Stored string for key: " + key);
        } catch (Exception e) {
            Log.e(TAG, "Error storing string for key: " + key, e);
        }
    }
    
    /**
     * Retrieve a string value
     * @param context Application context
     * @param key Key to retrieve
     * @param defaultValue Default value if key not found
     * @return Stored value or default value
     */
    public static String getString(Context context, String key, String defaultValue) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String value = prefs.getString(key, defaultValue);
            Log.d(TAG, "Retrieved string for key: " + key + " = " + value);
            return value;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving string for key: " + key, e);
            return defaultValue;
        }
    }
    
    /**
     * Store a boolean value
     * @param context Application context
     * @param key Key to store under
     * @param value Value to store
     */
    public static void storeBoolean(Context context, String key, boolean value) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(key, value).apply();
            Log.d(TAG, "Stored boolean for key: " + key + " = " + value);
        } catch (Exception e) {
            Log.e(TAG, "Error storing boolean for key: " + key, e);
        }
    }
    
    /**
     * Retrieve a boolean value
     * @param context Application context
     * @param key Key to retrieve
     * @param defaultValue Default value if key not found
     * @return Stored value or default value
     */
    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean value = prefs.getBoolean(key, defaultValue);
            Log.d(TAG, "Retrieved boolean for key: " + key + " = " + value);
            return value;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving boolean for key: " + key, e);
            return defaultValue;
        }
    }
    
    /**
     * Store an integer value
     * @param context Application context
     * @param key Key to store under
     * @param value Value to store
     */
    public static void storeInt(Context context, String key, int value) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putInt(key, value).apply();
            Log.d(TAG, "Stored int for key: " + key + " = " + value);
        } catch (Exception e) {
            Log.e(TAG, "Error storing int for key: " + key, e);
        }
    }
    
    /**
     * Retrieve an integer value
     * @param context Application context
     * @param key Key to retrieve
     * @param defaultValue Default value if key not found
     * @return Stored value or default value
     */
    public static int getInt(Context context, String key, int defaultValue) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            int value = prefs.getInt(key, defaultValue);
            Log.d(TAG, "Retrieved int for key: " + key + " = " + value);
            return value;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving int for key: " + key, e);
            return defaultValue;
        }
    }
    
    /**
     * Remove a key from storage
     * @param context Application context
     * @param key Key to remove
     */
    public static void remove(Context context, String key) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().remove(key).apply();
            Log.d(TAG, "Removed key: " + key);
        } catch (Exception e) {
            Log.e(TAG, "Error removing key: " + key, e);
        }
    }
    
    /**
     * Clear all stored data
     * @param context Application context
     */
    public static void clearAll(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
            Log.d(TAG, "Cleared all stored data");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing all data", e);
        }
    }
    
    /**
     * Check if a key exists
     * @param context Application context
     * @param key Key to check
     * @return true if key exists
     */
    public static boolean contains(Context context, String key) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean exists = prefs.contains(key);
            Log.d(TAG, "Key " + key + " exists: " + exists);
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "Error checking if key exists: " + key, e);
            return false;
        }
    }
}