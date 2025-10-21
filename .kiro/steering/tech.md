# Synapse - Technology Stack

## Platform & Language
- **Android Native** - Kotlin/Java
- **Target SDK**: 32, **Compile SDK**: 36
- **Min SDK**: 26 (Android 8.0+)
- **Java Version**: 17

## Backend & Database
- **Supabase** - PostgreSQL database, authentication, real-time subscriptions
- **Migration Status**: Recently migrated from Firebase to Supabase (100% complete)

## Key Dependencies
- **Supabase BOM**: 2.6.0 (postgrest-kt, gotrue-kt, realtime-kt)
- **Kotlin Coroutines**: 1.10.2 (async operations)
- **Kotlinx Serialization**: 1.6.3 (JSON handling)
- **AndroidX**: Latest stable versions
- **Glide**: 5.0.0-rc01 (image loading)
- **OneSignal**: 5.1.6+ (push notifications)
- **Markwon**: 4.6.2 (markdown rendering)
- **Media3**: 1.3.1 (video playback)
- **Google Gemini AI**: 1.12.0 (AI features)

## Architecture Patterns
- **Clean Architecture** - Domain, Data, Presentation layers
- **MVVM** - ViewModels with LiveData
- **Repository Pattern** - Data abstraction
- **Dependency Injection** - Manual DI setup

## Build System
- **Gradle** with Kotlin DSL support
- **Android Gradle Plugin**: 8.12.0
- **Kotlin**: 2.2.0

## Common Commands
```bash
# Clean build
./gradlew clean build

# Debug build
./gradlew assembleDebug

# Release build  
./gradlew assembleRelease

# Install debug APK
./gradlew installDebug
```

## Configuration
- Supabase credentials in `gradle.properties`
- Build variants: debug (with .debug suffix) and release
- ProGuard disabled for easier debugging