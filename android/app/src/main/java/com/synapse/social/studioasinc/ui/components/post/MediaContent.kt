package com.synapse.social.studioasinc.ui.components.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun MediaContent(
    mediaUrls: List<String>,
    isVideo: Boolean,
    onMediaClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (mediaUrls.isEmpty()) return

    if (mediaUrls.size == 1) {
        val url = mediaUrls.first()
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { onMediaClick(0) }
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Post Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (isVideo) 16f / 9f else 4f / 3f) // Adjust aspect ratio as needed
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            if (isVideo) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Play Video",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    } else {
        // Handle grid layout for multiple images
        // For simplicity in this task, we can show a pager or just the first image with a count
        // Implementing a simple grid for now or just a pager
         // NOTE: A more complex grid layout can be implemented here.
        val url = mediaUrls.first()
        Box(
             modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { onMediaClick(0) }
        ) {
             AsyncImage(
                model = url,
                contentDescription = "Post Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            if (mediaUrls.size > 1) {
                // Show "+N" overlay
                // This is a placeholder for the multi-image requirement
            }
        }
    }
}
