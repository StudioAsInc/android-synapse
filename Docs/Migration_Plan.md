# Migration Plan: Firebase to Supabase

## 1. High-Level Strategy

This migration will be executed in a phased, service-by-service approach to minimize disruption and allow for iterative testing. The order of migration will be:
1.  **Authentication:** Migrate user authentication from Firebase to Supabase, ensuring existing users can still log in.
2.  **Database:** Migrate the application's data from Firebase Realtime Database/Firestore to Supabase's Postgres database.
3.  **Storage:** Migrate file storage from Firebase Storage to Supabase Storage.

### Key Considerations
*   **Data Migration:** User data will be exported from Firebase and imported into Supabase. This will require a script to transform the data to the new schema.
*   **Downtime:** The migration will be designed to have minimal downtime. We will use feature flags to switch between Firebase and Supabase services during the transition.
*   **User Handling:** Existing users will be migrated to Supabase Auth. We will use a "lazy migration" approach, where users are migrated when they next log in.

## 2. Prerequisites & Setup

### Supabase Project Setup
1.  Create a new project on the [Supabase website](https://supabase.com/).
2.  Set up the database schema to match the existing Firebase data structure.
3.  Enable the required authentication providers (email/password, social logins).
4.  Obtain the project URL and anon key from the Supabase project settings.

### Android Dependencies
Add the following dependencies to the `app/build.gradle` file:
```groovy
dependencies {
    // Supabase
    implementation "io.github.jan-tennert.supabase:postgrest-kt:2.0.0"
    implementation "io.github.jan-tennert.supabase:gotrue-kt:2.0.0"
    implementation "io.github.jan-tennert.supabase:storage-kt:2.0.0"

    // Ktor client
    implementation "io.ktor:ktor-client-android:2.3.4"
}
```

## 3. Service-by-Service Migration Guide

### Authentication

#### Conceptual Mapping
*   **Firebase Auth UID** -> **Supabase Auth UID**
*   **Firebase ID Token** -> **Supabase JWT**

#### Side-by-Side Code Examples (Kotlin)
**Sign Up**
```kotlin
// Firebase
auth.createUserWithEmailAndPassword(email, password)
    .addOnCompleteListener { task -> /* ... */ }

// Supabase
viewModelScope.launch {
    supabase.auth.signUpWith(Email) {
        email = "example@example.com"
        password = "example-password"
    }
}
```

**Sign In**
```kotlin
// Firebase
auth.signInWithEmailAndPassword(email, password)
    .addOnCompleteListener { task -> /* ... */ }

// Supabase
viewModelScope.launch {
    supabase.auth.signInWith(Email) {
        email = "example@example.com"
        password = "example-password"
    }
}
```

**Sign Out**
```kotlin
// Firebase
auth.signOut()

// Supabase
viewModelScope.launch {
    supabase.auth.signOut()
}
```

### Database

#### Conceptual Mapping
*   **Firestore Document** -> **Supabase Postgres Row**
*   **Firestore Collection** -> **Supabase Postgres Table**

#### Side-by-Side Code Examples (Kotlin)
**Create**
```kotlin
// Firebase
db.collection("users").document("uid").set(user)

// Supabase
viewModelScope.launch {
    supabase.from("users").insert(user)
}
```

**Read**
```kotlin
// Firebase
db.collection("users").document("uid").get()

// Supabase
viewModelScope.launch {
    supabase.from("users").select {
        filter {
            eq("uid", "uid")
        }
    }
}
```

### Storage

#### Conceptual Mapping
*   **Firebase Storage Reference** -> **Supabase Storage Reference**

#### Side-by-Side Code Examples (Kotlin)
**Upload**
```kotlin
// Firebase
storageRef.child("images/image.jpg").putFile(uri)

// Supabase
viewModelScope.launch {
    supabase.storage.from("images").upload("image.jpg", uri)
}
```

**Download**
```kotlin
// Firebase
storageRef.child("images/image.jpg").downloadUrl

// Supabase
viewModelScope.launch {
    supabase.storage.from("images").downloadPublic("image.jpg")
}
```

## 4. Project Structure & Best Practices

*   **Supabase Client:** Create a singleton object to manage the Supabase client instance.
*   **API Keys:** Store the Supabase URL and anon key in `gradle.properties` and access them via `BuildConfig`.
*   **Repository Pattern:** Use the repository pattern to abstract data sources (Firebase/Supabase) from the ViewModels.

## 5. Testing & Validation Checklist

*   [ ] User can sign up and log in with email/password.
*   [ ] User can log in with social providers.
*   [ ] User data is correctly created/read/updated/deleted in the database.
*   [ ] Files can be uploaded and downloaded from storage.
*   [ ] Real-time subscriptions are working as expected.
*   [ ] Existing users can log in and their data is migrated.

This plan provides a comprehensive overview of the migration process. Each step will be broken down into smaller tasks and tracked in the `Docs/Supabase_migration.md` file.
