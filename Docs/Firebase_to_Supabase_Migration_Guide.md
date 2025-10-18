# Firebase to Supabase Migration Guide

This guide provides a comprehensive, step-by-step plan for migrating an Android application from Firebase to Supabase.

## 1. Initial Setup and Configuration

The first step is to integrate the Supabase SDK into your Android project while ensuring a robust setup for different development environments.

### 1.1. Environment Setup

To manage different environments (e.g., development, staging, production), it's recommended to have separate Supabase projects for each. This ensures that your production data remains isolated and secure.

-   **Development:** Use for local development and testing.
-   **Staging:** A pre-production environment for final testing.
-   **Production:** The live environment for your users.

Each Supabase project will have its own unique API URL and keys. You can manage these using different `gradle.properties` files or by setting up environment variables in your CI/CD system.

### 1.2. Add Supabase Dependencies

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

### 1.3. Configure API Keys

It's crucial to handle your Supabase API keys securely.

1.  **Create `gradle.properties`:**
    In your project's root directory, create a file named `gradle.properties` (if it doesn't exist) and add your Supabase URL and public API key. For a multi-environment setup, you can create separate files like `debug.properties` and `release.properties` and load them in `build.gradle`.

    ```properties
    # gradle.properties
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

### 1.4. Initialize Supabase Client

Create a singleton object to initialize and provide a global instance of the Supabase client. This approach ensures that you have a single, shared instance of the client throughout your application, which is configured once and can be easily accessed by different parts of your code.

#### 1.4.1. Basic Initialization

Here's a basic initialization of the Supabase client with the essential plugins (`GoTrue` for authentication, `Postgrest` for database operations, and `Storage` for file storage):

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

#### 1.4.2. Advanced Configuration

For more advanced use cases, you can customize the configuration of each plugin. For example, you can enable auto-refreshing of the session token, specify a custom schema for your database, or set a custom encoder for your Postgrest client:

```kotlin
val client: SupabaseClient = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    install(GoTrue) {
        // Enable auto-refreshing of the session token
        autoRefreshToken = true
    }
    install(Postgrest) {
        // Specify a custom schema for your database
        defaultSchema = "my_schema"
        // Set a custom encoder for your Postgrest client
        encoder = CustomEncoder()
    }
    install(Storage) {
        // Set a custom transfer speed for file uploads
        transferSpeed = 1024 * 1024 // 1 MB/s
    }
}
```

### 1.5. Client Lifecycle Management
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

### 2.1. Handling Authentication States

It's essential to handle different authentication states in your application to provide a seamless user experience. The Supabase `GoTrue` client provides an easy way to listen for changes in the authentication state.

```kotlin
// Listen for authentication state changes
val authStateJob = lifecycleScope.launch {
    SupabaseClient.client.auth.sessionStatus.collect {
        when (it) {
            is SessionStatus.Authenticated -> {
                // User is signed in, navigate to the main screen
            }
            is SessionStatus.NotAuthenticated -> {
                // User is signed out, navigate to the login screen
            }
        }
    }
}

// Don't forget to cancel the job when the component is destroyed
override fun onDestroy() {
    super.onDestroy()
    authStateJob.cancel()
}
```

### 2.2. Email/Password Authentication

**Sign-Up:**

**Kotlin:**
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

**Java:**
```java
// Supabase Sign-Up
import io.github.jan.supabase.gotrue.GoTrue;
import io.github.jan.supabase.gotrue.providers.builtin.Email;

// In a method
GoTrue auth = SupabaseClient.INSTANCE.getClient().getAuth();
auth.signUpWith(new Email.Provider("user@example.com", "password"), (result) -> {
    if (result.isSuccess()) {
        // Handle successful sign-up
    } else {
        // Handle error
    }
    return null;
});
```

**Sign-In:**

**Kotlin:**
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

**Java:**
```java
// Supabase Sign-In
import io.github.jan.supabase.gotrue.GoTrue;
import io.github.jan.supabase.gotrue.providers.builtin.Email;

// In a method
GoTrue auth = SupabaseClient.INSTANCE.getClient().getAuth();
auth.signInWith(new Email.Provider("user@example.com", "password"), (result) -> {
    if (result.isSuccess()) {
        // Handle successful sign-in
    } else {
        // Handle error
    }
    return null;
});
```

### 2.3. Migrating Existing Users

Migrating your existing user base from Firebase to Supabase requires careful handling of user data and passwords. Supabase provides a "User Import" feature that allows you to create users with a pre-existing `user_id` and a hashed password.

#### 2.3.1. Exporting Users from Firebase

First, export your user data from Firebase using the Firebase CLI:

```bash
firebase auth:export users.json --format=json
```

This will generate a JSON file containing all your Firebase users.

#### 2.3.2. Transforming User Data

Next, you'll need to transform the exported JSON data into a format that Supabase can import. This typically involves writing a script (e.g., in Python or Node.js) to parse the JSON file and extract the required user information.

Here's an example of a Python script that transforms the Firebase user data into a CSV file that can be imported into Supabase:

```python
import json

def transform_firebase_users(input_file, output_file):
    with open(input_file, 'r') as f:
        data = json.load(f)

    with open(output_file, 'w') as f:
        f.write('email,password_hash\n')
        for user in data['users']:
            email = user['email']
            password_hash = user['passwordHash']
            f.write(f'{email},{password_hash}\n')

# Usage
transform_firebase_users('users.json', 'supabase_users.csv')
```

#### 2.3.3. Importing Users into Supabase

Once you have the transformed CSV file, you can import it into Supabase using the Supabase dashboard's "User Import" feature. This will create the users in your Supabase project with their existing email and a hashed password, allowing them to sign in with their old credentials.

### 2.4. Social Logins (Google)

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

### 3.1. Data Modeling: From NoSQL to Relational

One of the most significant changes when migrating from Firebase to Supabase is the shift from a NoSQL (JSON-like) data model to a relational (PostgreSQL) one. This requires careful planning to ensure that your data is structured correctly and that you can maintain the relationships between your data.

#### 3.1.1. Mapping Firebase Data to Supabase Tables

In Firebase, you might have a denormalized data structure where related data is nested within a single document. In Supabase, you'll want to normalize this data into separate tables and use foreign keys to define the relationships between them.

**Example:**

Consider a simple blogging application where you have users and posts. In Firebase, you might have a data structure like this:

```json
{
  "users": {
    "user1": {
      "name": "Alice",
      "email": "alice@example.com"
    }
  },
  "posts": {
    "post1": {
      "title": "My First Post",
      "content": "Hello, world!",
      "author": "user1"
    }
  }
}
```

In Supabase, you would create two tables: `users` and `posts`.

**`users` table:**

| id   | name  | email               |
| ---- | ----- | ------------------- |
| 1    | Alice | alice@example.com   |

**`posts` table:**

| id   | title           | content         | author_id |
| ---- | --------------- | --------------- | --------- |
| 1    | My First Post   | Hello, world!   | 1         |

The `posts` table has a foreign key column `author_id` that references the `id` column in the `users` table, establishing the relationship between a post and its author.

### 3.2. Data Export and Import Strategy

Migrating your data from Firebase to Supabase involves a three-step process: exporting from Firebase, transforming the data to a relational format, and importing it into Supabase.

#### 3.2.1. Exporting from Firebase

You can export your Firebase Realtime Database or Firestore data as a JSON file from the Google Cloud Console.

#### 3.2.2. Transforming Data

Once you have the exported JSON file, you'll need to write a script to transform it into a set of CSV files that correspond to the tables in your Supabase schema.

Here's an example of a Python script that transforms a Firebase JSON export into two CSV files (`users.csv` and `posts.csv`):

```python
import json

def transform_firebase_data(input_file):
    with open(input_file, 'r') as f:
        data = json.load(f)

    # Transform users
    with open('users.csv', 'w') as f:
        f.write('id,name,email\n')
        for user_id, user_data in data['users'].items():
            f.write(f'{user_id},{user_data["name"]},{user_data["email"]}\n')

    # Transform posts
    with open('posts.csv', 'w') as f:
        f.write('title,content,author_id\n')
        for post_id, post_data in data['posts'].items():
            f.write(f'{post_data["title"]},{post_data["content"]},{post_data["author"]}\n')

# Usage
transform_firebase_data('firebase_export.json')
```

#### 3.2.3. Importing into Supabase

After transforming your data, you can import the resulting CSV files into your Supabase project using the Supabase dashboard. You'll need to import the `users.csv` file first, as the `posts` table has a foreign key that depends on the `users` table.

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

### 3.4. Handling Complex Queries

Supabase's Postgrest client provides a powerful and flexible API for querying your data. You can perform complex queries, including joins, filters, and ordering, with just a few lines of code.

**Example:**

To fetch a post and its author's name, you can use a `select` query with a join:

```kotlin
// Fetch a post and its author's name
val post = SupabaseClient.client.postgrest["posts"]
    .select("*, author:users(name)") {
        eq("id", postId)
    }
    .single()
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

## 6. Testing and Verification

Thorough testing is crucial to ensure a successful migration. This section outlines a comprehensive testing strategy to verify that your application works as expected after migrating to Supabase.

### 6.1. Testing Strategy

Your testing strategy should include a combination of unit tests, integration tests, and end-to-end (E2E) tests to cover all aspects of your application.

-   **Unit Tests:** Test individual components, such as ViewModels and data models, in isolation.
-   **Integration Tests:** Test the interaction between different components, such as the Supabase client and your application's services.
-   **E2E Tests:** Test the entire application flow, from the user interface to the backend, to ensure that all features work correctly.

### 6.2. Sample Test Cases

Here are some sample test cases for common migration scenarios:

#### 6.2.1. Authentication

-   **Test Case:** Verify that a new user can sign up successfully.
-   **Test Case:** Verify that an existing user can sign in successfully.
-   **Test Case:** Verify that a user can sign out successfully.
-   **Test Case:** Verify that social logins (e.g., Google) work correctly.

#### 6.2.2. Data Operations

-   **Test Case:** Verify that a new post can be created successfully.
-   **Test Case:** Verify that an existing post can be updated successfully.
-   **Test Case:** Verify that a post can be deleted successfully.
-   **Test Case:** Verify that all posts for a given user can be fetched successfully.

#### 6.2.3. File Storage

-   **Test Case:** Verify that a file can be uploaded successfully.
-   **Test Case:** Verify that a file can be downloaded successfully.
-   **Test Case:** Verify that a file can be deleted successfully.

### 6.3. Error Handling

-   **Test Case:** Verify that the application handles network errors gracefully.
-   **Test Case:** Verify that the application handles Supabase API errors correctly.
-   **Test Case:** Verify that the application displays appropriate error messages to the user.

## 7. CI/CD Pipeline

Automating your build, testing, and deployment processes is essential for a reliable and efficient workflow. This section provides an overview of how to set up a CI/CD pipeline for your Android application with Supabase.

### 7.1. Setting Up a CI/CD Pipeline

You can use a variety of CI/CD tools, such as GitHub Actions, GitLab CI/CD, or Jenkins, to automate your workflow. The general steps for setting up a CI/CD pipeline are as follows:

1.  **Configure your CI/CD environment:** Install the required tools, such as the Android SDK and Java, on your CI/CD server.
2.  **Set up your build script:** Create a build script (e.g., a `build.gradle` file) that defines the steps for building your application.
3.  **Configure your CI/CD pipeline:** Create a configuration file (e.g., a `.github/workflows/main.yml` file for GitHub Actions) that defines the stages of your pipeline, such as building, testing, and deploying.
4.  **Set up environment variables:** Securely store your Supabase API keys and other sensitive information as environment variables in your CI/CD system.

### 7.2. Example: GitHub Actions

Here's an example of a simple GitHub Actions workflow that builds and tests an Android application:

```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v2
    - name: set up JDK 11
      uses: actions/setup-java@v2
      with:
        java-version: '11'
        distribution: 'temurin'
        cache: gradle

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    - name: Build with Gradle
      run: ./gradlew build
    - name: Run tests
      run: ./gradlew test
```

## 8. Rollback Strategy

A rollback strategy is a critical component of any migration plan. It provides a safety net in case you encounter critical issues after deploying the migrated application.

### 8.1. When to Rollback

You should consider rolling back to Firebase if you encounter any of the following issues:

-   **Critical bugs:** If you find critical bugs in the migrated application that affect a significant number of users.
-   **Data loss:** If you discover that data is being lost or corrupted during the migration process.
-   **Performance issues:** If the migrated application is significantly slower or less reliable than the Firebase version.

### 8.2. How to Rollback

The rollback process will depend on the specific issue you're facing. However, the general steps are as follows:

1.  **Disable the Supabase integration:** Turn off the Supabase integration in your application's code.
2.  **Re-enable the Firebase integration:** Re-enable the Firebase integration and redeploy your application.
3.  **Restore data from a backup:** If you've lost data, you'll need to restore it from a backup.
4.  **Investigate the root cause:** Once you've rolled back, you'll need to investigate the root cause of the issue to prevent it from happening again.

## 9. Performance and Security

This section provides a checklist of performance and security best practices to follow when migrating to Supabase.

### 9.1. Performance

-   **Query optimization:** Analyze your queries and ensure that you're using appropriate indexes to optimize their performance.
-   **Connection pooling:** Use a connection pool to manage your database connections and avoid the overhead of creating new connections for each request.
-   **Caching:** Cache frequently accessed data to reduce the number of queries to your database.
-   **Load testing:** Perform load testing to identify and address any performance bottlenecks before deploying to production.

### 9.2. Security

-   **Row-Level Security (RLS):** Use Supabase's RLS feature to control access to your data at the row level.
-   **Two-Factor Authentication (2FA):** Enable 2FA for your Supabase project to add an extra layer of security to your user accounts.
-   **Secure API keys:** Store your Supabase API keys securely and avoid exposing them in your client-side code.
-   **Regular security audits:** Conduct regular security audits to identify and address any potential vulnerabilities.
