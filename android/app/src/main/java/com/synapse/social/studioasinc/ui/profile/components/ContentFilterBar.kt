package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.profile.ProfileContentFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentFilterBar(
    selectedFilter: ProfileContentFilter,
    onFilterSelected: (ProfileContentFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfileContentFilter.values().forEach { filter ->
                FilterChipItem(
                    label = filter.name,
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        label = "chipColor"
    )

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = containerColor
        )
    )
}

@Preview
@Composable
private fun ContentFilterBarPreview() {
    MaterialTheme {
        ContentFilterBar(
            selectedFilter = ProfileContentFilter.POSTS,
            onFilterSelected = {}
        )
    }
}
