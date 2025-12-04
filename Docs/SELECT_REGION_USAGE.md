# SelectRegionScreen Usage Guide

## Quick Start

### Basic Usage

```kotlin
SelectRegionScreen(
    currentRegion = "United States",
    onRegionSelected = { region ->
        // Handle region selection
        viewModel.updateRegion(region)
    },
    onBackClick = {
        // Handle back navigation
        navController.popBackStack()
    }
)
```

## Integration Examples

### 1. In Navigation Graph

```kotlin
composable("select_region/{currentRegion}") { backStackEntry ->
    val currentRegion = backStackEntry.arguments?.getString("currentRegion") ?: ""
    SelectRegionScreen(
        currentRegion = currentRegion,
        onRegionSelected = { region ->
            viewModel.onEvent(RegionSelected(region))
            navController.popBackStack()
        },
        onBackClick = {
            navController.popBackStack()
        }
    )
}
```

### 2. From Parent Screen

```kotlin
// In parent screen
SettingsNavigationItem(
    title = "Region",
    subtitle = currentRegion,
    icon = R.drawable.ic_location,
    onClick = {
        navController.navigate("select_region/$currentRegion")
    }
)
```

## Features

### Search
- Click search icon in TopAppBar
- Type to filter regions in real-time
- Case-insensitive matching
- Click back to exit search

### Selection
- Tap any region to select
- Current region shows checkmark icon
- Automatically navigates back on selection

### Navigation
- Back button in TopAppBar
- Calls `onBackClick` callback
- Preserves navigation state

## Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `currentRegion` | `String` | Currently selected region (shows checkmark) |
| `onRegionSelected` | `(String) -> Unit` | Callback when region is selected |
| `onBackClick` | `() -> Unit` | Callback for back navigation |

## Supported Regions

The screen includes 195 countries/regions:
- All UN member states
- Vatican City
- Palestine
- Taiwan

Regions are displayed in alphabetical order.

## Customization

### Theme Colors
The screen uses MaterialTheme colors:
- `surface` - Background
- `onSurface` - Text
- `primary` - Selected region checkmark

### Search Behavior
Search filters by substring match:
```kotlin
allRegions.filter { it.contains(query, ignoreCase = true) }
```

## State Management

The screen manages its own UI state:
- `searchQuery` - Current search text
- `isSearchActive` - Search mode toggle
- `filteredRegions` - Computed from search query

No ViewModel required - uses Compose state primitives.

## Accessibility

- All interactive elements have content descriptions
- Proper touch targets (48dp minimum)
- Screen reader compatible
- Keyboard navigation support

## Performance

- `LazyColumn` for efficient list rendering
- `remember` for computed values
- Minimal recomposition
- Smooth scrolling for 195+ items

## Error Handling

The screen handles edge cases:
- Empty current region (shows no checkmark)
- No search results (shows empty list)
- Null safety throughout

## Testing

```kotlin
@Test
fun testRegionSelection() {
    composeTestRule.setContent {
        SelectRegionScreen(
            currentRegion = "Canada",
            onRegionSelected = { /* verify callback */ },
            onBackClick = { /* verify callback */ }
        )
    }
    
    // Verify current region shows checkmark
    composeTestRule.onNodeWithText("Canada").assertExists()
    
    // Test selection
    composeTestRule.onNodeWithText("Brazil").performClick()
}
```
