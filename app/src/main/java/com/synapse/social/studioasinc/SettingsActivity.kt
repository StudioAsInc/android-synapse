package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.databinding.ActivitySettingsBinding
import kotlinx.coroutines.launch

/**
 * Central settings hub for app preferences and navigation to sub-settings.
 * Uses ViewBinding for type-safe view access.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSettingsItems()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSettingsItems() {
        // Account
        setupSettingRow(
            binding.settingAccount.root,
            R.drawable.ic_person,
            getString(R.string.settings_account),
            getString(R.string.settings_account_subtitle)
        ) {
            startActivity(Intent(this, ProfileEditActivity::class.java))
        }

        // Privacy
        setupSettingRow(
            binding.settingPrivacy.root,
            R.drawable.ic_shield_lock,
            getString(R.string.settings_privacy),
            getString(R.string.settings_privacy_subtitle)
        ) {
            startActivity(Intent(this, ChatPrivacySettingsActivity::class.java))
        }

        // Notifications
        setupSettingRow(
            binding.settingNotifications.root,
            R.drawable.ic_notifications,
            getString(R.string.settings_notifications),
            getString(R.string.settings_notifications_subtitle)
        ) {
            openNotificationSettings()
        }

        // Logout
        setupSettingRow(
            binding.settingLogout.root,
            R.drawable.ic_logout,
            getString(R.string.settings_logout),
            null,
            showChevron = false
        ) {
            performLogout()
        }
    }

    private fun setupSettingRow(
        view: View,
        iconRes: Int,
        title: String,
        subtitle: String?,
        showChevron: Boolean = true,
        onClick: () -> Unit
    ) {
        view.findViewById<ImageView>(R.id.icon_start).setImageResource(iconRes)
        view.findViewById<TextView>(R.id.title).text = title
        
        val subtitleView = view.findViewById<TextView>(R.id.subtitle)
        if (subtitle != null) {
            subtitleView.text = subtitle
            subtitleView.visibility = View.VISIBLE
        } else {
            subtitleView.visibility = View.GONE
        }
        
        view.findViewById<ImageView>(R.id.icon_end).visibility = 
            if (showChevron) View.VISIBLE else View.GONE
        
        view.setOnClickListener { onClick() }
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    private fun performLogout() {
        lifecycleScope.launch {
            authRepository.signOut()
            val intent = Intent(this@SettingsActivity, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
