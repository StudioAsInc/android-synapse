---
inclusion: always
---

# Agent Guidelines

## Code Style & Conventions

- **Language**: Use Kotlin exclusively for new code. Java-to-Kotlin migration is ongoing.
- **View Binding**: Mandatory for all UI code. Never use `findViewById()`.
- **Null Safety**: Leverage Kotlin's null safety features. Use `?.`, `?:`, and `!!` appropriately.
- **Coroutines**: Use `suspend` functions for async operations. Prefer `viewModelScope` and `lifecycleScope`.
- **Naming**: Use camelCase for variables/functions, PascalCase for classes, UPPER_SNAKE_CASE for constants.

## Architecture Patterns

- **MVVM**: ViewModels expose StateFlow/LiveData, Fragments/Activities observe and update UI.
- **Repository Pattern**: Repositories in `data/` abstract Supabase and local data sources.
- **Single Source of Truth**: SupabaseClient.kt is the singleton for all backend operations.
- **Separation of Concerns**: Keep business logic in domain layer, UI logic in presentation layer.

## Supabase Integration

- **Client Access**: Always use `SupabaseClient.client` singleton, never create new instances.
- **RLS Policies**: All database operations respect Row Level Security. Test with different user contexts.
- **Realtime**: Use Supabase Realtime for live updates (chat, notifications, feed).
- **Storage**: Use Supabase Storage for media uploads with proper bucket policies.

## Documentation Rules

- **Location**: All documentation MUST go in `Docs/` directory only.
- **No Auto-Creation**: Never create markdown documentation files without explicit user permission.
- **Existing Docs**: Reference and update existing docs rather than creating new ones.

## Testing & Validation

- **Build Before Commit**: Run `./gradlew build` to verify compilation.
- **RLS Testing**: Test database operations with different authenticated users.
- **Null Checks**: Verify null handling for all Supabase responses.

## Common Pitfalls

- Don't mix `adapter/` and `adapters/` packages - prefer `adapters/` for new code.
- Don't bypass ViewBinding with direct view references.
- Don't create Supabase client instances - use the singleton.
- Don't place documentation outside `Docs/` directory.
