---
inclusion: always
---

# Agent Guidelines

**Code**: Kotlin, View Binding mandatory, null safety (`?.`, `?:`, `!!`), coroutines (`viewModelScope`/`lifecycleScope`), no hardcoded strings/colors/dimensions (use XML resources)

**Architecture**: MVVM, Repository pattern in `data/`, SupabaseClient.kt singleton only, domain for business logic

**Supabase**: Use `SupabaseClient.client` singleton, respect RLS, test with different users, Realtime for live updates

**Docs**: Only in `Docs/` directory, never auto-create without permission

**Testing**: Run `./gradlew build` before commit, test RLS and null handling

**Avoid**: `findViewById()`, new Supabase instances, mixing `adapter/`+`adapters/` (use `adapters/`), docs outside `Docs/`
