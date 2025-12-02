# Tech Stack

## Android App
- **Language**: Kotlin with Android KTX
- **Architecture**: MVVM + Repository pattern
- **UI**: ViewBinding, Jetpack Compose, Material Design 3, Navigation Component
- **Async**: Kotlin Coroutines + Flow
- **Image Loading**: Glide
- **Markdown**: Markwon
- **Media**: Media3

## Backend (Supabase)
- **Database**: PostgreSQL via Postgrest
- **Authentication**: GoTrue (email, OAuth)
- **Storage**: Supabase Storage for media
- **Real-time**: Supabase Realtime for live updates

## Build Configuration
- Target SDK: 34
- Min SDK: 26
- Compile SDK: 36
- Build System: Gradle with Kotlin DSL

## Key Libraries

### Backend & Networking
- **Supabase BOM**: Backend-as-a-service platform providing database, auth, storage, and realtime
- **Ktor**: HTTP client for Supabase communication
- **Kotlinx Serialization**: JSON serialization for API data

### UI & Design
- **Material Design**: Modern Material Design 3 components
- **AndroidX Core KTX**: Kotlin extensions for Android framework
- **Glide**: Efficient image loading and caching
- **Lottie**: Animated vector graphics and illustrations
- **CircleImageView**: Circular image views for avatars

### Architecture & Lifecycle
- **Lifecycle**: Lifecycle-aware components and ViewModels
- **Navigation**: Fragment navigation and deep linking
- **Work Manager**: Background task scheduling
- **Paging**: Efficient data pagination for large lists

### Media & Content
- **Media3**: Modern media playback (replaces ExoPlayer)
- **Markwon**: Markdown rendering with GitHub Flavored Markdown support
- **PhotoView**: Pinch-to-zoom image viewing

### Utilities
- **OkHttp**: HTTP client for networking
- **Gson**: JSON parsing and serialization
- **OneSignal**: Push notifications
