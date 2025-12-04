package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopAppBar(
    username: String,
    scrollProgress: Float,
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (scrollProgress > 0.5f) {
            MaterialTheme.colorScheme.surface
        } else {
            Color.Transparent
        },
        label = "backgroundColor"
    )

    val elevation by animateDpAsState(
        targetValue = if (scrollProgress > 0.5f) 4.dp else 0.dp,
        label = "elevation"
    )

    TopAppBar(
        title = {
            if (scrollProgress > 0.5f) {
                Text(text = "@$username")
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor
        ),
        modifier = modifier
    )
}

@Preview
@Composable
private fun ProfileTopAppBarPreview() {
    MaterialTheme {
        ProfileTopAppBar(
            username = "johndoe",
            scrollProgress = 0.7f,
            onBackClick = {},
            onMoreClick = {}
        )
    }
}
