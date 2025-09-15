package com.synapse.social.studioasinc.util;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Utility class for safe authentication operations
 * Prevents NullPointerException when user is not logged in
 */
public class AuthUtil {
    
    /**
     * Get current user UID safely
     * @return User UID or null if not logged in
     */
    public static String getCurrentUserUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    /**
     * Check if user is logged in
     * @return true if user is logged in, false otherwise
     */
    public static boolean isLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }
    
    /**
     * Get current user safely
     * @return FirebaseUser or null if not logged in
     */
    public static FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }
    
    /**
     * Get current user email safely
     * @return User email or null if not logged in
     */
    public static String getCurrentUserEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getEmail() : null;
    }
    
    /**
     * Get current user display name safely
     * @return User display name or null if not logged in
     */
    public static String getCurrentUserDisplayName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getDisplayName() : null;
    }
}