package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R

@Composable
fun ProfileHeader(
    profileImageUrl: String?,
    name: String,
    username: String,
    nickname: String?,
    bio: String?,
    isVerified: Boolean,
    hasStory: Boolean,
    postsCount: Int,
    followersCount: Int,
    followingCount: Int,
    isOwnProfile: Boolean,
    onProfileImageClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onAddStoryClick: () -> Unit,
    onMoreClick: () -> Unit,
    onStatsClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var bioExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clickable(onClick = onProfileImageClick)
            ) {
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = stringResource(R.string.profile_image),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .then(
                            if (hasStory) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        ),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                modifier = Modifier.weight(1f).padding(start = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Posts", postsCount) { onStatsClick("posts") }
                StatItem("Followers", followersCount) { onStatsClick("followers") }
                StatItem("Following", followingCount) { onStatsClick("following") }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (isVerified) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp).padding(start = 4.dp)
                )
            }
        }

        Text(
            text = "@$username",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        nickname?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        bio?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (bioExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .animateContentSize()
                    .clickable { bioExpanded = !bioExpanded }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isOwnProfile) {
                Button(
                    onClick = onEditProfileClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit Profile")
                }
                OutlinedButton(
                    onClick = onAddStoryClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Story")
                }
            } else {
                Button(
                    onClick = { /* Follow */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Follow")
                }
                OutlinedButton(
                    onClick = { /* Message */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Message")
                }
            }
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreVert, "More")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, count: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
private fun ProfileHeaderPreview() {
    MaterialTheme {
        ProfileHeader(
            profileImageUrl = null,
            name = "John Doe",
            username = "johndoe",
            nickname = "JD",
            bio = "Software developer | Tech enthusiast | Coffee lover",
            isVerified = true,
            hasStory = true,
            postsCount = 42,
            followersCount = 1234,
            followingCount = 567,
            isOwnProfile = true,
            onProfileImageClick = {},
            onEditProfileClick = {},
            onAddStoryClick = {},
            onMoreClick = {},
            onStatsClick = {}
        )
    }
}
