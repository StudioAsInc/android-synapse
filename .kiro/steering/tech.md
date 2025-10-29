# Technology Stack

## Build System

- **Build Tool**: Gradle 8.12.0 with Kotlin DSL
- **Kotlin**: 2.2.0
- **JDK**: 17
- **Min SDK**: 26
- **Target SDK**: 32
- **Compile SDK**: 36

## Common Commands

```bash
# Build the project
./gradlew build

# Run debug build
./gradlew assembleDebug

# Run release build
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

## Backend & Networking

- **Supabase BOM 2.6.0**: Backend-as-a-service (database, auth, storage, realtime)
  - GoTrue: Authentication (email, OAuth)
  - Postgrest: PostgreSQL database access
  - Storage: Media file storage with S3 compatibility
  - Realtime: Live updates and subscriptions
- **Ktor 2.3.12**: HTTP client for Supabase communication
- **Kotlinx Serialization 1.6.3**: JSON serialization for API data
- **OkHttp 5.1.0**: HTTP client for networking
- **Gson 2.13.1**: JSON parsing

## UI & Design

- **Material Design 1.14.0-alpha06**: Material Design 3 components
- **ViewBinding**: Type-safe view access (mandatory, no findViewById)
- **AndroidX Core KTX 1.17.0-rc01**: Kotlin extensions
- **Glide 5.0.0-rc01**: Image loading and caching
- **Lottie 6.6.0**: Animated vector graphics
- **CircleImageView 3.1.0**: Circular avatars
- **PhotoView 2.3.0**: Pinch-to-zoom image viewing

## Architecture & Lifecycle

- **Lifecycle 2.9.2**: Lifecycle-aware components and ViewModels
- **Navigation 2.9.3**: Fragment navigation and deep linking
- **Work Manager 2.10.3**: Background task scheduling
- **Paging 3.3.6**: Efficient data pagination

## Media & Content

- **Media3 1.3.1**: Modern media playback (replaces ExoPlayer)
- **Markwon 4.6.2**: Markdown rendering with GitHub Flavored Markdown
  - Includes tables, task lists, strikethrough, LaTeX, HTML support

## Async Operations

- **Kotlin Coroutines 1.10.2**: Async/await pattern
- **StateFlow/LiveData**: Reactive UI updates
- **viewModelScope/lifecycleScope**: Lifecycle-aware coroutine scopes

## Configuration

Supabase credentials are configured via `gradle.properties` or environment variables:
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_SYNAPSE_S3_ENDPOINT_URL`
- `SUPABASE_SYNAPSE_S3_ENDPOINT_REGION`
- `SUPABASE_SYNAPSE_S3_ACCESS_KEY_ID`
- `SUPABASE_SYNAPSE_S3_ACCESS_KEY`

Authentication behavior can be configured with properties like `auth.require.email.verification`, `auth.enable.debug.logging`, etc.

## Key Libraries to Know

- **Supabase Client**: Singleton in `SupabaseClient.kt` provides access to all Supabase services
- **OneSignal 5.1.x**: Push notifications (planned feature)
- **Multidex**: Enabled for large dependency count
