# Profile Compose - Deferred Features

## Overview
Features marked as TODO in the codebase that are intentionally deferred to future iterations.

## Deferred to Post-Launch

### 1. Copy Link to Clipboard
**Location**: ProfileScreen.kt - Share sheet  
**Reason**: Requires clipboard manager integration  
**Priority**: P2  
**Estimated**: 30 minutes

### 2. User Search Dialog (View As)
**Location**: ProfileScreen.kt - View As sheet  
**Reason**: Requires search UI component  
**Priority**: P2  
**Estimated**: 2 hours

### 3. Story Integration
**Location**: ProfileScreen.kt - Profile header  
**Features**:
- Story ring indicator
- Story creation
- Story viewing
**Reason**: Stories feature not yet implemented  
**Priority**: P1  
**Estimated**: 8-10 hours

### 4. Full Screen Image Viewer
**Location**: ProfileScreen.kt - Profile image click  
**Reason**: Requires image viewer component  
**Priority**: P2  
**Estimated**: 3 hours

### 5. Photo Grid Viewer
**Location**: ProfileScreen.kt - Photo tab  
**Reason**: Requires photo viewer with swipe  
**Priority**: P2  
**Estimated**: 4 hours

### 6. Edit Profile Details Navigation
**Location**: ProfileScreen.kt - About tab  
**Reason**: Edit profile screen not migrated  
**Priority**: P1  
**Estimated**: 6 hours

### 7. Following List Loading
**Location**: ProfileScreen.kt - About tab  
**Reason**: Requires following list component  
**Priority**: P2  
**Estimated**: 2 hours

### 8. Comment Navigation
**Location**: ProfileScreen.kt - Post items  
**Reason**: Comments screen not migrated  
**Priority**: P1  
**Estimated**: 4 hours

### 9. Post Sharing
**Location**: ProfileScreen.kt - Post items  
**Reason**: Requires share sheet for posts  
**Priority**: P2  
**Estimated**: 2 hours

## Implementation Notes

All deferred features have placeholder callbacks that:
- Accept the necessary parameters
- Do nothing (empty lambda)
- Are clearly marked with TODO comments

This allows the UI to be fully functional while features are implemented incrementally.

## Priority Definitions

- **P0**: Critical for launch (none in deferred list)
- **P1**: Important, implement within 1-2 sprints
- **P2**: Nice to have, implement as capacity allows

## Total Estimated Time
~31.5 hours for all deferred features

## Recommendation

Implement in this order:
1. Story integration (P1, high user value)
2. Edit profile navigation (P1, core functionality)
3. Comment navigation (P1, engagement)
4. Photo viewer (P2, user experience)
5. Full screen image viewer (P2, user experience)
6. Post sharing (P2, engagement)
7. Following list (P2, social features)
8. User search (P2, View As enhancement)
9. Copy link (P2, convenience)
