# Synapse - Project Structure

## Root Directory
- `app/` - Main Android application module
- `Docs/` - Project documentation (Architecture, Contributing, etc.)
- `gradle/` - Gradle wrapper files
- `.kiro/` - Kiro IDE configuration and steering rules

## Source Code Organization
**Base Package**: `com.synapse.social.studioasinc`

### Core Architecture Layers
- `data/` - Data layer (repositories, data sources)
- `domain/` - Business logic and use cases  
- `presentation/` - UI layer (activities, fragments, ViewModels)
- `di/` - Dependency injection setup

### Feature Modules
- `chat/` - Messaging and real-time communication
- `home/` - Main social feed and navigation
- `fragments/` - Reusable UI fragments
- `lab/` - Experimental features
- `AI/` - Gemini AI integration

### Supporting Components
- `adapter/` - RecyclerView adapters
- `model/` & `models/` - Data models and DTOs
- `repository/` - Data access layer
- `util/` - Utility classes and helpers
- `widget/` - Custom UI components
- `styling/` - UI styling and themes
- `config/` - App configuration
- `backend/` - Supabase integration

### Key Files
- `SynapseApp.java` - Application class
- `MainActivity.kt` - Main entry point
- `SupabaseClient.kt` - Supabase configuration
- `AuthActivity.kt` - Authentication flow

## Naming Conventions
- **Activities**: `*Activity.kt/java`
- **Fragments**: `*Fragment.kt/java` 
- **ViewModels**: `*ViewModel.kt`
- **Adapters**: `*Adapter.kt/java`
- **Repositories**: `*Repository.kt`
- **Services**: `*Service.kt`

## File Status
- `.disabled` suffix indicates temporarily disabled files during migration
- Kotlin files preferred for new development
- Java files being gradually migrated to Kotlin

## Resource Organization
- `res/layout/` - XML layouts
- `res/values/` - Strings, colors, dimensions
- `res/drawable/` - Images and vector drawables
- `assets/` - Raw assets and files