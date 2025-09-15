package com.synapse.social.studioasinc.crypto;

import android.content.Context;
import android.util.Log;

/**
 * E2EE Helper - DISABLED VERSION
 * 
 * This class has been disabled to prevent keystore failures on devices
 * that don't support encrypted storage. All encryption functionality
 * has been removed and replaced with no-op methods.
 */
public class E2EEHelper {
    
    private static final String TAG = "E2EEHelper";
    private Context context;
    private String uid;

    public E2EEHelper(Context context) {
        this.context = context;
        Log.w(TAG, "E2EE functionality is disabled to prevent keystore failures");
        // E2EE disabled - no initialization needed
    }

    public void initializeKeys(final KeysInitializationListener listener) {
        Log.w(TAG, "E2EE initialization skipped - functionality disabled");
        // Immediately call failure callback since E2EE is disabled
        listener.onKeyInitializationFailed(new UnsupportedOperationException("E2EE functionality is disabled"));
    }

    public void getPublicKey(String userId, PublicKeyListener listener) {
        Log.w(TAG, "E2EE getPublicKey called but functionality is disabled");
        listener.onPublicKeyReceived(null);
    }

    public void establishSession(String userId, byte[] publicKey, SessionEstablishmentListener listener) {
        Log.w(TAG, "E2EE establishSession called but functionality is disabled");
        listener.onSessionEstablished();
    }

    public String encrypt(String userId, String message) {
        Log.w(TAG, "E2EE encrypt called but functionality is disabled - returning original message");
        return message; // Return original message without encryption
    }

    public String decrypt(String userId, String encryptedMessage) {
        Log.w(TAG, "E2EE decrypt called but functionality is disabled - returning original message");
        return encryptedMessage; // Return original message without decryption
    }

    public String encryptMessage(String message, String userId) {
        Log.w(TAG, "E2EE encryptMessage called but functionality is disabled - returning original message");
        return message; // Return original message without encryption
    }

    public String decryptMessage(String encryptedMessage, String userId) {
        Log.w(TAG, "E2EE decryptMessage called but functionality is disabled - returning original message");
        return encryptedMessage; // Return original message without decryption
    }

    // Interface definitions
    public interface KeysInitializationListener {
        void onKeysInitialized();
        void onKeyInitializationFailed(Exception e);
    }

    public interface PublicKeyListener {
        void onPublicKeyReceived(byte[] publicKey);
        void onPublicKeyFailed(Exception e);
    }

    public interface SessionEstablishmentListener {
        void onSessionEstablished();
        void onSessionEstablishmentFailed(Exception e);
    }
}