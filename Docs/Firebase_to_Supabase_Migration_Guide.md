# Firebase to Supabase Migration Guide

This guide provides a comprehensive, step-by-step plan for migrating an Android application from Firebase to Supabase.

## 1. Initial Setup and Configuration

The first step is to integrate the Supabase SDK into your Android project.

### 1.1. Add Supabase Dependencies

Add the Supabase Bill of Materials (BOM) and the required Supabase libraries to your `app/build.gradle` file.

```groovy
// app/build.gradle

dependencies {
    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:1.0.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:gotrue-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")

    // Ktor client for Supabase
    implementation("io.ktor:ktor-client-android:2.3.4")
}
```

### 1.2. Configure API Keys

It's crucial to handle your Supabase API keys securely.

1.  **Create `gradle.properties`:**
    In your project's root directory, create a file named `gradle.properties` (if it doesn't exist) and add your Supabase URL and public API key:

    ```properties
    SUPABASE_URL="YOUR_SUPABASE_URL"
    SUPABASE_ANON_KEY="YOUR_SUPABASE_ANON_KEY"
    ```

2.  **Add to `.gitignore`:**
    Ensure that `gradle.properties` is included in your `.gitignore` file to prevent your keys from being committed to version control.

    ```
    gradle.properties
    ```

3.  **Expose Keys in `build.gradle`:**
    In your `app/build.gradle` file, expose the keys as `buildConfigField` values:

    ```groovy
    // app/build.gradle

    android {
        defaultConfig {
            ...
        }
        buildTypes {
            release {
                ...
                buildConfigField "String", "SUPABASE_URL", "\"${project.properties['SUPABASE_URL']}\""
                buildConfigField "String", "SUPABASE_ANON_KEY", "\"${project.properties['SUPABASE_ANON_KEY']}\""
            }
            debug {
                ...
                buildConfigField "String", "SUPABASE_URL", "\"${project.properties['SUPABASE_URL']}\""
                buildConfigField "String", "SUPABASE_ANON_KEY", "\"${project.properties['SUPABASE_ANON_KEY']}\""
            }
        }
    }
    ```

### 1.3. Initialize Supabase Client

Create a singleton object to initialize and provide a global instance of the Supabase client.

```kotlin
// In a suitable location, e.g., your.package.name.backend/SupabaseClient.kt

package your.package.name.backend

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import your.package.name.BuildConfig

object SupabaseClient {

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(GoTrue)
        install(Postgrest)
        install(Storage)
    }
}
```

### 1.4. Client Lifecycle Management
In Android, it's important to manage asynchronous operations within the lifecycle of UI components. Use `viewModelScope` from a ViewModel or `lifecycleScope` from an Activity/Fragment to launch coroutines. This ensures that operations are automatically canceled when the UI component is destroyed, preventing memory leaks.

```kotlin
// Inside a ViewModel
viewModelScope.launch {
    // Your Supabase calls here
}

// Inside an Activity/Fragment
lifecycleScope.launch {
    // Your Supabase calls here
}
```

## 2. Authentication Migration

Migrate user authentication from Firebase to Supabase.

### 2.1. Email/Password Authentication

**Sign-Up:**

```kotlin
// Supabase Sign-Up
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email

viewModelScope.launch {
    try {
        val user = SupabaseClient.client.auth.signUpWith(Email) {
            email = "user@example.com"
            password = "password"
        }
        // Handle successful sign-up
    } catch (e: Exception) {
        // Handle error
    }
}
```

**Sign-In:**

```kotlin
// Supabase Sign-In
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email

viewModelScope.launch {
    try {
        val result = SupabaseClient.client.auth.signInWith(Email) {
            email = "user@example.com"
            password = "password"
        }
        // Handle successful sign-in
    } catch (e: Exception) {
        // Handle error
    }
}
```

### 2.2. Migrating Existing Users
Migrating users from Firebase Auth is a multi-step process:
1.  **Export Firebase Users:** Use the Firebase CLI to export your user data:
    ```bash
    firebase auth:export users.json --format=json
    ```
2.  **Transform Data:** Write a script to transform the exported JSON into a CSV format that Supabase can import. You'll need to map Firebase fields to Supabase's `auth.users` table columns.
3.  **Import to Supabase:** Use the Supabase dashboard's "User" import feature or write a script using the Supabase Admin SDK to create users from the transformed data.

### 2.3. Social Logins (Google)

```kotlin
// AuthActivity.kt (migrated)
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google

// Note: The idToken must be obtained from the Google Sign-In for Android library first.
// This typically involves configuring Google Sign-In and handling the result.
suspend fun signInWithGoogle(idToken: String) {
    SupabaseClient.client.auth.signInWith(Google) {
        this.idToken = idToken
    }
}
```

## 3. Data Migration

Migrate your data from Firebase Realtime Database/Firestore to Supabase PostgreSQL.

### 3.1. Data Modeling
-   **Firebase:** NoSQL, JSON-like data structure.
-   **Supabase:** Relational (PostgreSQL), with tables, columns, and relationships.

You'll need to design a relational schema in Supabase that mirrors your existing Firebase data structure. Use the Supabase dashboard's table editor to create your tables and define relationships.

### 3.2. Data Export and Import Strategy
1.  **Export from Firebase:** Export your Firestore or Realtime Database data as a JSON file from the Google Cloud Console.
2.  **Transform Data:** Write a script (e.g., in Python or Node.js) to parse the exported JSON and convert it into a CSV file for each table in your Supabase schema.
3.  **Import into Supabase:** Use the Supabase dashboard to import the CSV files into their corresponding tables.

### 3.3. CRUD Operations

**Firebase (Existing Code):**

```java
// Example: Creating a post in Firebase
DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("posts");
String postId = postsRef.push().getKey();
Post post = new Post("User123", "Hello, World!");
postsRef.child(postId).setValue(post);
```

**Supabase (New Code):**

```kotlin
// Example: Creating a post in Supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

@Serializable
data class Post(val userId: String, val content: String)

suspend fun createPost(post: Post) {
    SupabaseClient.client.postgrest["posts"].insert(post)
}
```

## 4. File Storage Migration

Migrate files from Firebase Storage to Supabase Storage.

**Firebase (Existing Code):**

```java
// Example: Uploading a file to Firebase Storage
StorageReference storageRef = FirebaseStorage.getInstance().getReference();
StorageReference imageRef = storageRef.child("images/" + file.getName());
UploadTask uploadTask = imageRef.putFile(fileUri);
```

**Supabase (New Code):**

```kotlin
// Example: Uploading a file to Supabase Storage
import io.github.jan.supabase.storage.storage

suspend fun uploadFile(bucketName: String, path: String, fileBytes: ByteArray) {
    SupabaseClient.client.storage[bucketName].upload(path, fileBytes)
}
```

## 5. Realtime Data Synchronization

Replace Firebase's realtime listeners with Supabase's realtime capabilities.

**Firebase (Existing Code):**

```java
// Example: Listening for changes in Firebase
DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("posts").child(postId);
postRef.addValueEventListener(new ValueEventListener() {
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        Post post = dataSnapshot.getValue(Post.class);
        // Update UI
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        // Handle error
    }
});
```

**Supabase (New Code):**

```kotlin
// Example: Listening for changes in Supabase
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow

suspend fun listenToPostChanges(postId: String) {
    val channel = SupabaseClient.client.channel("posts_channel")
    val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
        table = "posts"
    }

    changeFlow.collect {
        when (it) {
            is PostgresAction.Insert -> {
                // Handle new post
            }
            is PostgresAction.Update -> {
                // Handle updated post
            }
            is PostgresAction.Delete -> {
                // Handle deleted post
            }
            else -> {}
        }
    }

    channel.join()
}
```

## 6. Error Handling and Testing

-   **Error Handling:** Wrap Supabase calls in `try-catch` blocks to handle exceptions gracefully.
-   **Testing:** Thoroughly test all authentication flows, data operations, and file storage functionalities after the migration to ensure everything works as expected.