# Select Region Screen Migration - PRD

## Overview
Migrate the SelectRegionActivity from XML-based Activity to Jetpack Compose with Material 3 Expressive components to achieve UI consistency across the app.

## Problem Statement
- Current SelectRegionActivity uses deprecated findViewById pattern
- Custom graphics helpers and hardcoded colors violate Material Design
- PNG icons in app bar instead of Material Icons
- Inconsistent with Compose-based EditProfileScreen
- Poor UX with basic RecyclerView implementation

## Goals
- Achieve UI design consistency with Material 3
- Follow modern Android development standards
- Improve user experience with better search and selection
- Maintain MVVM architecture pattern

## Technical Specifications

### Component Structure
```
ui/settings/
├── SelectRegionScreen.kt       (Compose UI)
└── SelectRegionViewModel.kt    (if needed for state)
```

### UI Components
- **TopAppBar**: Material 3 with back navigation icon
- **SearchBar**: Material 3 SearchBar component with real-time filtering
- **LazyColumn**: Scrollable region list
- **ListItem**: Material 3 list items with checkmark for selected region

### Features
1. **Search Functionality**
   - Real-time filtering as user types
   - Case-insensitive search
   - Clear search capability

2. **Region Selection**
   - Visual indicator (checkmark) for current region
   - Touch feedback on selection
   - Return selected region to EditProfileScreen

3. **Navigation**
   - Back button returns to EditProfileScreen
   - Result handling for selected region

### Data
- 195 countries/regions (existing list from SelectRegionActivity)
- Current region passed as parameter
- Selected region returned as result

## Design Requirements
- Use MaterialTheme colors (no hardcoded values)
- Material Icons only (no custom PNGs)
- Proper elevation and spacing per Material 3 guidelines
- Responsive touch feedback
- Accessibility support

## Implementation Steps
1. Create SelectRegionScreen.kt with Compose UI
2. Implement search state management
3. Add region list with LazyColumn
4. Update EditProfileScreen navigation
5. Test region selection flow
6. Remove old Activity and XML files

## Success Criteria
- ✓ No XML layouts or findViewById
- ✓ Material 3 components throughout
- ✓ Consistent with app's Compose screens
- ✓ Search works smoothly
- ✓ Region selection returns correct result
- ✓ Follows MVVM architecture

## Files to Modify
- Create: `app/src/main/java/com/synapse/social/studioasinc/ui/settings/SelectRegionScreen.kt`
- Update: `app/src/main/java/com/synapse/social/studioasinc/ui/settings/EditProfileScreen.kt`
- Delete: `app/src/main/java/com/synapse/social/studioasinc/SelectRegionActivity.kt`
- Delete: `app/src/main/res/layout/activity_select_region.xml`

## Non-Goals
- Adding new regions or country data
- Implementing region-based features
- Changing region data structure
- Adding region flags or icons
