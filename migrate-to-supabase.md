# Migration to Supabase

This document tracks the progress of migrating the Synapse Android application from Firebase to Supabase.

## Done

- [x] **Dependencies:**
    - [x] Removed Firebase dependencies from `app/build.gradle` and root `build.gradle`.
    - [x] Added Supabase dependencies for `auth`, `postgrest`, `realtime`, and `storage` to `app/build.gradle`.
    - [x] Added the Kotlin serialization plugin to `app/build.gradle`.
- [x] **Backend Abstraction Layer:**
    - [x] Created `IAuthenticationService` and `IDatabaseService` interfaces to abstract backend operations.
    - [x] Implemented `SupabaseAuthService` which implements `IAuthenticationService` using the Supabase Kotlin client.
    - [x] Implemented `SupabaseDatabaseService` which implements `IDatabaseService` using the Supabase Kotlin client.
    - [x] Created `IUser`, `IAuthResult`, `IDataSnapshot`, `IDatabaseError`, `IQuery`, `IDatabaseReference`, `IRealtimeListener`, and `IRealtimeChannel` interfaces to support the abstraction layer.
- [x] **AuthActivity:**
    - [x] Refactored `AuthActivity` to use `IAuthenticationService` and `IDatabaseService` instead of directly using Firebase.
- [x] **ChatMessageManager:**
    - [x] Refactored `ChatMessageManager` to a class that accepts `IDatabaseService` and `IAuthenticationService` as dependencies.
    - [x] Replaced all Firebase calls with the corresponding service methods.
- [x] **DatabaseHelper:**
    - [x] Refactored `DatabaseHelper` to use `IDatabaseService` and `IAuthenticationService`.
    - [x] Replaced all `FirebaseDatabase` and `FirebaseAuth` calls with the corresponding service methods.
    - [x] Replaced `ChildEventListener` with `IRealtimeListener` to listen for realtime database changes.
- [x] **MessageSendingHandler:**
    - [x] Refactored `MessageSendingHandler` to use `IAuthenticationService` and `IDatabaseService`.
    - [x] Replaced all `FirebaseAuth` and `FirebaseDatabase` calls with the corresponding service methods.
- [x] **MessageInteractionHandler:**
    - [x] Refactored `MessageInteractionHandler` to use `IAuthenticationService` and `IDatabaseService`.
    - [x] Replaced all `FirebaseAuth` and `FirebaseDatabase` calls with the corresponding service methods.
- [x] **AiFeatureHandler:**
    - [x] Refactored `AiFeatureHandler` to use `IAuthenticationService`.
    - [x] Replaced all `FirebaseAuth` calls with the corresponding service methods.
- [x] **AttachmentHandler:**
    - [x] Refactored `AttachmentHandler` to use `IAuthenticationService`.
    - [x] Replaced all `FirebaseAuth` calls with the corresponding service methods.
- [x] **ChatKeyboardHandler:**
    - [x] Refactored `ChatKeyboardHandler` to use `IAuthenticationService` and `IDatabaseService`.
    - [x] Replaced all `FirebaseAuth` and `FirebaseDatabase` calls with the corresponding service methods.
- [x] **GroupDetailsLoader:**
    - [x] Refactored `GroupDetailsLoader` to use `IDatabaseService`.
    - [x] Replaced all `FirebaseDatabase` calls with the corresponding service methods.
- [x] **UserBlockService:**
    - [x] Refactored `UserBlockService` to use `IAuthenticationService` and `IDatabaseService`.
    - [x] Replaced all `FirebaseAuth` and `FirebaseDatabase` calls with the corresponding service methods.
- [x] **ChatActivity:**
    - [x] Replaced all `ChatMessageManager.INSTANCE` calls with an instance of the `ChatMessageManager` class.
    - [x] Removed all direct Firebase dependencies.

## TODO

- [ ] **Configuration:**
    - [ ] Remove the `google-services.json` file.
    - [ ] Add Supabase URL and Key to a configuration file.
- [ ] **Testing:**
    - [ ] Thoroughly test the application to ensure that all features work as expected with Supabase.
    - [ ] Fix the Java version issue to be able to build the project.