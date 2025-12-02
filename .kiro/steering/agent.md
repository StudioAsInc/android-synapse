---
inclusion: always
---

# Agent Guidelines

**Code**: Kotlin, View Binding, null safety (`?.`, `?:`, `!!`), coroutines (`viewModelScope`/`lifecycleScope`), XML resources only

**Architecture**: MVVM, Repository in `data/`, SupabaseClient.kt singleton, domain for business logic

**Supabase**: `SupabaseClient.client` singleton, respect RLS, test multi-user, Realtime for live updates

**Testing**: `./gradlew build` before commit, test RLS and nulls

**Avoid**: `findViewById()`, new Supabase instances, `adapter/`+`adapters/` mix (use `adapters/`)

## Docs

**All docs in `Docs/` only**: Source Map, API Documentation, Architecture, Setup Guide, Contributing, Changelog, Troubleshooting

**Rules**: No docs in root/`app/docs/`/`.github/docs/`, use Markdown + TOC, paths from project root
