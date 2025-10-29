# Project Structure

## Root Directory

```
android-synapse/
├── app/                    # Main application module
├── assets/                 # App icons and branding assets
├── Docs/                   # ALL project documentation (mandatory location)
├── .github/                # GitHub workflows and templates
├── .kiro/                  # Kiro AI assistant configuration
├── gradle/                 # Gradle wrapper files
├── build.gradle            # Root build configuration
├── settings.gradle         # Project settings
└── gradle.properties       # Build and Supabase configuration
```

## App Module Structure

```
app/src/main/
├── java/com/synapse/social/studioasinc/
│   ├── adapter/           # RecyclerView adapters (legacy)
│   ├── adapters/          # RecyclerView adapters (newer)
│   ├── backend/           # Supabase service layer
│   ├── chat/              # Chat feature components
│   ├── data/              # Repositories & data sources
│   ├── domain/            # Business logic & use cases
│   ├── fragments/         # Fragment components
│   ├── home/              # Home feed features
│   ├── model/models/      # Data models & DTOs
│   ├── presentation/      # ViewModels
│   ├── util/              # Utilities & extensions
│   ├── widget/            # Custom views
│   ├── *Activity.kt       # Activity classes (root level)
│   ├── SupabaseClient.kt  # Supabase singleton client
│   └── SynapseApp.kt      # Application class
└── res/                   # Resources (layouts, drawables, strings, etc.)
```

## Architecture Layers

### UI Layer
- **Activities**: Root-level `*Activity.kt` files (e.g., `HomeActivity.kt`, `ChatActivity.kt`)
- **Fragments**: Located in `fragments/` directory
- **Adapters**: RecyclerView adapters in `adapter/` and `adapters/`
- **Custom Views**: Located in `widget/` directory
- **ViewBinding**: Mandatory for all UI code

### ViewModel Layer
- **Location**: `presentation/` directory
- **Pattern**: ViewModels expose StateFlow/LiveData for reactive UI updates
- **Scope**: Use `viewModelScope` for coroutines

### Repository Layer
- **Location**: `data/` directory
- **Pattern**: Repositories abstract data sources (Supabase, local storage)
- **Methods**: Use suspend functions for async operations

### Domain Layer
- **Location**: `domain/` directory
- **Purpose**: Business logic and use cases

### Backend Layer
- **Location**: `backend/` directory
- **Purpose**: Supabase service wrappers and API abstractions

## Key Files

- **SupabaseClient.kt**: Singleton providing access to Supabase services (Auth, Postgrest, Realtime, Storage)
- **SynapseApp.kt / SynapseApplication.kt**: Application initialization
- **BuildConfig**: Auto-generated configuration from gradle.properties

## Documentation Standards

**CRITICAL**: All documentation MUST be placed in `Docs/` directory:
- ✅ `Docs/[filename].md`
- ❌ NOT in root, app/docs/, .github/docs/, or any other location

Accepted documentation files:
- `Docs/Source Map.md`
- `Docs/API Documentation.md`
- `Docs/Architecture.md`
- `Docs/Setup Guide.md`
- `Docs/Contributing.md`
- `Docs/Changelog.md`
- `Docs/Troubleshooting.md`

## Package Naming

Base package: `com.synapse.social.studioasinc`

Feature-based organization with some legacy flat structure at root level.
