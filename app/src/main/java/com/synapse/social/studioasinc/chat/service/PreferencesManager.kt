package com.synapse.social.studioasinc.chat.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Manages user preferences for chat features.
 * Handles privacy settings for read receipts and typing indicators.
 * 
 * Requirements: 5.1, 5.4
 */
class PreferencesManager(context: Context) {
    
    companion object {
        private const val TAG = "PreferencesManager"
        private const val PREFS_NAME = "synapse_chat_preferences"
        private const val KEY_SEND_READ_RECEIPTS = "send_read_receipts"
        private const val KEY_SHOW_TYPING_INDICATORS = "show_typing_indicators"
        
        // Default values
        private const val DEFAULT_SEND_READ_RECEIPTS = true
        private const val DEFAULT_SHOW_TYPING_INDICATORS = true
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Check if read receipts are enabled.
     * 
     * @return true if user wants to send read receipts, false otherwise
     */
    fun isReadReceiptsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SEND_READ_RECEIPTS, DEFAULT_SEND_READ_RECEIPTS)
    }
    
    /**
     * Set read receipts preference.
     * 
     * @param enabled true to enable sending read receipts, false to disable
     */
    fun setReadReceiptsEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting read receipts enabled: $enabled")
        sharedPreferences.edit()
            .putBoolean(KEY_SEND_READ_RECEIPTS, enabled)
            .apply()
    }
    
    /**
     * Check if typing indicators are enabled.
     * 
     * @return true if user wants to send typing indicators, false otherwise
     */
    fun isTypingIndicatorsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SHOW_TYPING_INDICATORS, DEFAULT_SHOW_TYPING_INDICATORS)
    }
    
    /**
     * Set typing indicators preference.
     * 
     * @param enabled true to enable sending typing indicators, false to disable
     */
    fun setTypingIndicatorsEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting typing indicators enabled: $enabled")
        sharedPreferences.edit()
            .putBoolean(KEY_SHOW_TYPING_INDICATORS, enabled)
            .apply()
    }
    
    /**
     * Get all chat preferences.
     * 
     * @return ChatPreferences object with current settings
     */
    fun getChatPreferences(): ChatPreferences {
        return ChatPreferences(
            sendReadReceipts = isReadReceiptsEnabled(),
            showTypingIndicators = isTypingIndicatorsEnabled()
        )
    }
    
    /**
     * Update all chat preferences at once.
     * 
     * @param preferences ChatPreferences object with new settings
     */
    fun updateChatPreferences(preferences: ChatPreferences) {
        Log.d(TAG, "Updating chat preferences: $preferences")
        sharedPreferences.edit()
            .putBoolean(KEY_SEND_READ_RECEIPTS, preferences.sendReadReceipts)
            .putBoolean(KEY_SHOW_TYPING_INDICATORS, preferences.showTypingIndicators)
            .apply()
    }
    
    /**
     * Reset all preferences to default values.
     */
    fun resetToDefaults() {
        Log.d(TAG, "Resetting preferences to defaults")
        sharedPreferences.edit()
            .putBoolean(KEY_SEND_READ_RECEIPTS, DEFAULT_SEND_READ_RECEIPTS)
            .putBoolean(KEY_SHOW_TYPING_INDICATORS, DEFAULT_SHOW_TYPING_INDICATORS)
            .apply()
    }
}

/**
 * Data class representing chat preferences.
 */
data class ChatPreferences(
    val sendReadReceipts: Boolean = true,
    val showTypingIndicators: Boolean = true
)
