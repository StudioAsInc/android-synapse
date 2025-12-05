---
inclusion: always
---

# Agent Guidelines

## Code Style & Language

- **Language**: Kotlin only
- **UI Binding**: View Binding (no `findViewById()`)
- **Null Safety**: Use safe calls (`?.`), Elvis operator (`?:`), and non-null assertion (`!!`) appropriately
- **Async**: Coroutines with `viewModelScope` (ViewModels) or `lifecycleScope` (Activities/Fragments)
- **Resources**: XML layouts and resources only (no Jetpack Compose in this project)

## Architecture

- **Pattern**: MVVM (Model-View-ViewModel)
- **Repository Layer**: All data access logic in `data/` package
- **Domain Layer**: Business logic in `domain/` package when needed
- **ViewModels**: Handle UI logic and state, expose LiveData/StateFlow to Views
- **Adapters**: Use `adapters/` package consistently (avoid mixing `adapter/` and `adapters/`)

## Supabase Backend

- **Client**: Use `SupabaseClient.client` singleton exclusively (never create new instances)
- **Security**: Always respect Row Level Security (RLS) policies
- **Testing**: Test multi-user scenarios and RLS enforcement
- **Real-time**: Use Supabase Realtime for live data updates
- **MCP**: Consider using Supabase MCP tools for backend configuration and queries

## Testing & Quality

- **Build**: Run `./gradlew build` before committing changes
- **Null Safety**: Test all nullable paths and edge cases
- **RLS**: Verify Row Level Security works correctly for different user roles

## Documentation

- **Location**: All documentation MUST be in `Docs/` directory only
- **Format**: Markdown with table of contents for longer documents
- **Paths**: Use paths relative to project root
- **Prohibited**: No docs in root, `app/docs/`, or `.github/docs/`
- **Types**: Source maps, API docs, architecture guides, setup instructions, changelogs, troubleshooting

## File Creation Rules

- **Documentation**: Do NOT create markdown or text documentation files without explicit user permission
- **Code Files**: Create code files as needed to fulfill requirements
- **Existing Docs**: Update existing documentation when changes warrant it