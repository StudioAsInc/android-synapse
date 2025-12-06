---
inclusion: always
---

# Agent Guidelines

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

## Documentation

- **Location**: All documentation MUST be in `Docs/` directory only
- **Format**: Markdown
- **Paths**: Use paths relative to project root
- **Prohibited**: No docs in root, `app/docs/`, or `.github/docs/` etc

## File Creation Rules

- **Documentation**: Do NOT create markdown or text documentation files without explicit user permission