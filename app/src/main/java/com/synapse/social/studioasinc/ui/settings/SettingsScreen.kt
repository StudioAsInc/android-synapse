package com.synapse.social.studioasinc.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onAccountClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            SettingRow(
                icon = R.drawable.ic_person,
                title = stringResource(R.string.settings_account),
                subtitle = stringResource(R.string.settings_account_subtitle),
                onClick = onAccountClick
            )
            SettingRow(
                icon = R.drawable.ic_shield_lock,
                title = stringResource(R.string.settings_privacy),
                subtitle = stringResource(R.string.settings_privacy_subtitle),
                onClick = onPrivacyClick
            )
            SettingRow(
                icon = R.drawable.ic_notifications,
                title = stringResource(R.string.settings_notifications),
                subtitle = stringResource(R.string.settings_notifications_subtitle),
                onClick = onNotificationsClick
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingRow(
                icon = R.drawable.ic_logout,
                title = stringResource(R.string.settings_logout),
                subtitle = null,
                showChevron = false,
                onClick = onLogoutClick
            )
        }
    }
}

@Composable
private fun SettingRow(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String?,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showChevron) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
