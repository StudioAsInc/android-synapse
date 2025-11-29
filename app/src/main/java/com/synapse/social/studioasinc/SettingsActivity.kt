package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SettingsScreen(
                    onBackClick = { finish() },
                    onAccountClick = { startActivity(Intent(this, ProfileEditActivity::class.java)) },
                    onPrivacyClick = { startActivity(Intent(this, ChatPrivacySettingsActivity::class.java)) },
                    onNotificationsClick = { openNotificationSettings() },
                    onLogoutClick = { performLogout() }
                )
            }
        }
    }

    private fun openNotificationSettings() {
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        })
    }

    private fun performLogout() {
        lifecycleScope.launch {
            authRepository.signOut()
            startActivity(Intent(this@SettingsActivity, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}
