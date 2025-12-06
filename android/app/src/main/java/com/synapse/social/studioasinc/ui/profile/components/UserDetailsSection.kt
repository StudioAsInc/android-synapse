package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class UserDetails(
    val location: String? = null,
    val joinedDate: String? = null,
    val relationshipStatus: String? = null,
    val birthday: String? = null,
    val work: String? = null,
    val education: String? = null,
    val website: String? = null,
    val gender: String? = null,
    val pronouns: String? = null,
    val linkedAccounts: List<LinkedAccount> = emptyList()
)

data class LinkedAccount(
    val platform: String,
    val username: String
)

@Composable
fun UserDetailsSection(
    details: UserDetails,
    isOwnProfile: Boolean,
    onCustomizeClick: () -> Unit,
    onWebsiteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Show Less" else "Show More")
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    details.location?.let {
                        DetailItem(
                            icon = Icons.Default.LocationOn,
                            label = "Location",
                            value = it
                        )
                    }

                    details.joinedDate?.let {
                        DetailItem(
                            icon = Icons.Default.DateRange,
                            label = "Joined",
                            value = it
                        )
                    }

                    details.relationshipStatus?.let {
                        DetailItem(
                            icon = Icons.Default.Favorite,
                            label = "Relationship",
                            value = it,
                            isPrivate = true
                        )
                    }

                    details.birthday?.let {
                        DetailItem(
                            icon = Icons.Default.Cake,
                            label = "Birthday",
                            value = it,
                            isPrivate = true
                        )
                    }

                    details.work?.let {
                        DetailItem(
                            icon = Icons.Default.Work,
                            label = "Work",
                            value = it
                        )
                    }

                    details.education?.let {
                        DetailItem(
                            icon = Icons.Default.School,
                            label = "Education",
                            value = it
                        )
                    }

                    details.website?.let {
                        DetailItem(
                            icon = Icons.Default.Link,
                            label = "Website",
                            value = it,
                            onClick = { onWebsiteClick(it) }
                        )
                    }

                    details.gender?.let {
                        DetailItem(
                            icon = Icons.Default.Person,
                            label = "Gender",
                            value = it
                        )
                    }

                    details.pronouns?.let {
                        DetailItem(
                            icon = Icons.Default.Badge,
                            label = "Pronouns",
                            value = it
                        )
                    }

                    if (details.linkedAccounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Linked Accounts",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        details.linkedAccounts.forEach { account ->
                            DetailItem(
                                icon = Icons.Default.Link,
                                label = account.platform,
                                value = account.username
                            )
                        }
                    }

                    if (isOwnProfile) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onCustomizeClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Customize Details")
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun UserDetailsSectionPreview() {
    MaterialTheme {
        UserDetailsSection(
            details = UserDetails(
                location = "San Francisco, CA",
                joinedDate = "January 2024",
                work = "Software Engineer at Tech Co",
                website = "https://example.com"
            ),
            isOwnProfile = true,
            onCustomizeClick = {},
            onWebsiteClick = {}
        )
    }
}
