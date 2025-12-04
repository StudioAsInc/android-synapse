package com.synapse.social.studioasinc.presentation.editprofile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.ui.settings.SettingsColors
import com.synapse.social.studioasinc.ui.settings.SettingsShapes

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ProfileImageSection(
    coverUrl: String?,
    avatarUrl: String?,
    onCoverClick: () -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SettingsColors.cardBackgroundElevated,
        tonalElevation = 2.dp
    ) {
        // Container with padding for avatar overlap
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Cover Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(onClick = onCoverClick)
            ) {
                if (coverUrl != null) {
                    GlideImage(
                        model = coverUrl,
                        contentDescription = "Cover photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder
                     GlideImage(
                        model = R.drawable.user_null_cover_photo,
                        contentDescription = "Cover photo placeholder",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = "Edit cover photo",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Avatar Image
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 48.dp) // Overlap logic: The box is aligned bottom of parent (which includes cover only if we don't pad).
                    // Actually, the specs say: "Centered, overlapping cover by 48dp".
                    // If Cover is 200dp, and Avatar is 96dp.
                    // We need the avatar to be partially on cover and partially below.
                    // If I put it in the same Box, Alignment.BottomCenter will put it at bottom of the Box (which is likely expanding to fit content if not constrained).
                    // The Cover Box is 200dp height.
                    // If I align Avatar to BottomCenter of the *container* box, I need to make sure the container box has enough height.
                    // The container box height is determined by its children.
                    // Better approach: Column? No, they overlap.
                    // Box logic:
                    // Cover is at top. Avatar is aligned relative to Cover bottom edge.
                    // I can use a Column with negative offset, or Box with alignment.
            ) {
                 // Let's rethink layout.
                 // Box (Container)
                 //   - Cover (200dp)
                 //   - Avatar (96dp) aligned to Alignment.TopCenter with offset (200dp - 48dp = 152dp)?
                 //   - Or use a Column and negative padding?
                 // The Box contains both.
                 // The Cover is 200dp.
                 // The Avatar needs to be at 200 - 48 = 152dp from top.
            }
        }

        // Correct implementation:
        // Use a Box for the whole card content.
        // Height of the Box will be max(Cover + Avatar/2 + Padding).

        Box(
             modifier = Modifier.fillMaxWidth().padding(bottom = 56.dp) // 48dp overlap + extra padding?
             // Specs: "Padding: 24dp bottom padding for avatar overlap" - This probably means the card padding.
             // Let's just use a Column logic where the Avatar overlaps the Cover.
             // But they are in a Box.
        ) {
             // Cover
             Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(onClick = onCoverClick)
            ) {
                 if (coverUrl != null) {
                    GlideImage(
                        model = coverUrl,
                        contentDescription = "Cover photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                     GlideImage(
                        model = R.drawable.user_null_cover_photo,
                        contentDescription = "Cover photo placeholder",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit), // Assuming ic_edit exists
                        contentDescription = "Edit cover photo",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp) // Specs said 48dp touch target, maybe icon size smaller
                    )
                }
            }

            // Avatar
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 152.dp) // 200 - 48
                    .clickable(onClick = onAvatarClick)
            ) {
                 Surface(
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(4.dp, MaterialTheme.colorScheme.surface),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxSize()
                 ) {
                     if (avatarUrl != null) {
                        GlideImage(
                            model = avatarUrl,
                            contentDescription = "Profile photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                     } else {
                         // Placeholder
                         Box(
                             modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
                             contentAlignment = Alignment.Center
                         ) {
                             Icon(
                                 painter = painterResource(R.drawable.ic_person), // Assuming this exists or R.drawable.avatar
                                 contentDescription = null,
                                 tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                 modifier = Modifier.size(48.dp)
                             )
                         }
                     }
                 }

                 // Edit badge
                 Box(
                     modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                     contentAlignment = Alignment.Center
                 ) {
                     Icon(
                         painter = painterResource(R.drawable.ic_edit),
                         contentDescription = "Edit avatar",
                         tint = MaterialTheme.colorScheme.onPrimary,
                         modifier = Modifier.size(16.dp)
                     )
                 }
            }
        }
    }
}
