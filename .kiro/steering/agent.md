---
inclusion: always
---

# Synapse Android Development Guidelines

Synapse is an open-source social media platform built with Kotlin for Android, using Supabase as the backend. Focus on privacy, real-time communication, and lightweight user experience.

## Build Configuration

- Target SDK 32, Min SDK 26, Compile SDK 36
- Use Gradle Kotlin DSL where applicable
- Maintain separate debug and release configurations
- Apply ProGuard rules for release builds
- Supabase credentials managed through BuildConfig (never hardcode)

## Architecture

**Pattern**: MVVM with Repository pattern
- ViewModels manage UI state using StateFlow/LiveData
- Repositories abstract data layer (Supabase, local storage)
- Separate UI logic from business logic


**File Organization**:
- Group by feature, not layer (e.g., `auth/`, `profile/`, `feed/`)
- Standard Android project structure under `app/src/main/`
- Resources named with feature prefix: `activity_profile_edit.xml`, `ic_profile_avatar.png`

## Kotlin Style

- Use data classes for models
- Prefer sealed classes for state management
- Use coroutines for async operations (avoid callbacks)
- Leverage Kotlin extensions and Android KTX
- Null safety: use `?.`, `?:`, and `!!` sparingly with justification

## Android UI

- Use ViewBinding (no findViewById or synthetic imports)
- Follow Material Design 3 guidelines
- Implement proper lifecycle awareness (collect flows in lifecycleScope)
- Use Navigation Component for app navigation
- Handle configuration changes properly

## Supabase Integration

**Client Initialization**:
```kotlin
val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    install(GoTrue)
    install(Postgrest)
    install(Storage)
    install(Realtime)
}
```

**Authentication**:
- Use GoTrue for auth (signInWith, signOut, currentSession)
- Handle session state changes with auth.sessionStatus flow
- Store tokens securely (never in SharedPreferences)
- Implement proper error handling for network failures

**Data Operations**:
- Use Kotlin serialization for JSON parsing
- Implement retry logic for network operations
- Handle Supabase errors gracefully with user-friendly messages
- Use Postgrest for database queries with proper error handling

**Storage**:
- Use Supabase Storage for media uploads
- Implement progress tracking for large uploads
- Handle file compression before upload
- Use signed URLs for private content

## Dependencies

Core libraries in use:
- Supabase BOM (auth, database, storage, realtime)
- AndroidX (lifecycle, navigation, viewmodel)
- Kotlin Coroutines
- Glide (image loading)
- Material Components
- Markwon (markdown rendering)
- Media3 (media playback)

## Security

- Never commit credentials or API keys
- Use BuildConfig for environment-specific values
- Implement proper permission handling (runtime permissions)
- Validate user input before sending to backend
- Use HTTPS for all network requests
- Follow Android security best practices for data storage

## AI Assistant Guidelines

- Provide concise, actionable responses
- Focus on code implementation over explanations
- **Never create .md files without explicit permission**
- When suggesting changes, provide specific file paths and code snippets
- Check for syntax errors before suggesting code
- Consider Android lifecycle and memory leaks in solutions