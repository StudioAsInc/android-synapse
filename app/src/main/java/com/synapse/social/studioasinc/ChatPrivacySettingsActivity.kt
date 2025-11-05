package com.synapse.social.studioasinc

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.materialswitch.MaterialSwitch
import com.synapse.social.studioasinc.chat.service.PreferencesManager
import kotlinx.coroutines.launch

/**
 * Activity for managing chat privacy settings.
 * Allows users to control read receipts and typing indicators.
 * 
 * Requirements: 5.1, 5.5
 */
class ChatPrivacySettingsActivity : AppCompatActivity() {
    
    private lateinit var backButton: ImageView
    private lateinit var titleText: TextView
    private lateinit var readReceiptsSwitch: MaterialSwitch
    private lateinit var typingIndicatorsSwitch: MaterialSwitch
    private lateinit var readReceiptsDescription: TextView
    private lateinit var typingIndicatorsDescription: TextView
    
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_privacy_settings)
        
        initializeViews()
        initializePreferencesManager()
        setupClickListeners()
        loadCurrentSettings()
        observePreferenceChanges()
    }
    
    private fun initializeViews() {
        backButton = findViewById(R.id.back_button)
        titleText = findViewById(R.id.title_text)
        readReceiptsSwitch = findViewById(R.id.read_receipts_switch)
        typingIndicatorsSwitch = findViewById(R.id.typing_indicators_switch)
        readReceiptsDescription = findViewById(R.id.read_receipts_description)
        typingIndicatorsDescription = findViewById(R.id.typing_indicators_description)
        
        // Set title
        titleText.text = "Chat Privacy"
    }
    
    private fun initializePreferencesManager() {
        preferencesManager = PreferencesManager(this)
    }
    
    private fun setupClickListeners() {
        // Back button
        backButton.setOnClickListener {
            finish()
        }
        
        // Read receipts switch
        readReceiptsSwitch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferencesManager.setReadReceiptsEnabled(isChecked)
            }
        }
        
        // Typing indicators switch
        typingIndicatorsSwitch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferencesManager.setTypingIndicatorsEnabled(isChecked)
            }
        }
    }
    
    private fun loadCurrentSettings() {
        lifecycleScope.launch {
            val preferences = preferencesManager.getChatPreferences()
            
            // Update switches with current settings
            readReceiptsSwitch.isChecked = preferences.sendReadReceipts
            typingIndicatorsSwitch.isChecked = preferences.showTypingIndicators
        }
    }
    
    /**
     * Observe preference changes and update UI reactively.
     * This ensures the UI stays in sync with preference changes.
     */
    private fun observePreferenceChanges() {
        lifecycleScope.launch {
            preferencesManager.getChatPreferencesFlow().collect { preferences ->
                // Update switches if they don't match current preferences
                if (readReceiptsSwitch.isChecked != preferences.sendReadReceipts) {
                    readReceiptsSwitch.isChecked = preferences.sendReadReceipts
                }
                if (typingIndicatorsSwitch.isChecked != preferences.showTypingIndicators) {
                    typingIndicatorsSwitch.isChecked = preferences.showTypingIndicators
                }
            }
        }
    }
}