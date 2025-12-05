# Profile Compose Architecture

## Overview
The Profile screen has been migrated to Jetpack Compose following MVVM architecture with clean separation of concerns.

## Architecture Layers

### 1. Presentation Layer (`ui/profile/`)
- **ProfileScreen.kt**: Main composable with state management
- **ProfileViewModel.kt**: State holder with business logic
- **Components**: Reusable UI components (header, stats, tabs, etc.)

### 2. Domain Layer (`domain/usecase/profile/`)
- **GetProfileUseCase**: Fetch user profile data
- **UpdateProfileUseCase**: Update profile information
- **FollowUserUseCase**: Follow/unfollow operations
- **GetProfileContentUseCase**: Fetch posts, photos, reels

### 3. Data Layer (`data/repository/`)
- **ProfileRepository**: Interface defining data operations
- **ProfileRepositoryImpl**: Supabase integration with caching and retry logic

## Data Flow
```
User Action → ProfileScreen → ProfileViewModel → UseCase → Repository → Supabase
                    ↓
              State Update
                    ↓
              UI Recomposition
```

## State Management
- **ProfileUiState**: Sealed class (Loading, Success, Error, Empty)
- **ProfileScreenState**: Data class holding all screen state
- **StateFlow**: Reactive state updates with `collectAsState()`

## Key Features
- Pull-to-refresh
- Infinite scroll pagination
- Bottom sheet modals
- View As mode
- Memory and network optimization
- Accessibility support

## Performance Optimizations
- Image caching (25% memory, 50MB disk)
- Request caching (1-minute TTL)
- Post limit (50 max cached)
- Retry with exponential backoff
- Memory pressure handling

## Testing
- Unit tests for ViewModels and UseCases
- UI tests for components
- Accessibility compliance tests

## Dependencies
- Jetpack Compose BOM
- Material 3
- Coil for image loading
- Supabase Kotlin SDK
