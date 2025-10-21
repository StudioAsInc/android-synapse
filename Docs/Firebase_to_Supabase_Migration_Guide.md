# Firebase to Supabase Migration Guide

This guide provides a comprehensive, step-by-step plan for migrating the Synapse Android application from Firebase to Supabase.

## Database Schema Design

Based on the current Firebase Realtime Database structure, here's the proposed Supabase PostgreSQL schema:

### Core Tables

#### 1. Users Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uid TEXT UNIQUE NOT NULL, -- Firebase UID for migration compatibility
    email TEXT UNIQUE NOT NULL,
    username TEXT UNIQUE NOT NULL,
    nickname TEXT,
    biography TEXT,
    avatar TEXT,
    avatar_history_type TEXT DEFAULT 'local',
    profile_cover_image TEXT,
    account_premium BOOLEAN DEFAULT false,
    user_level_xp INTEGER DEFAULT 500,
    verify BOOLEAN DEFAULT false,
    account_type TEXT DEFAULT 'user' CHECK (account_type IN ('user', 'admin')),
    gender TEXT DEFAULT 'hidden',
    banned BOOLEAN DEFAULT false,
    status TEXT DEFAULT 'offline' CHECK (status IN ('online', 'offline', 'away')),
    join_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    one_signal_player_id TEXT,
    last_seen TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    chatting_with TEXT, -- For presence management
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_users_uid ON users(uid);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
```

#### 2. Chats Table
```sql
CREATE TABLE chats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id TEXT UNIQUE NOT NULL, -- Generated chat ID for compatibility
    participant_1 UUID NOT NULL REFERENCES users(id),
    participant_2 UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(participant_1, participant_2)
);

CREATE INDEX idx_chats_chat_id ON chats(chat_id);
CREATE INDEX idx_chats_participants ON chats(participant_1, participant_2);
```

#### 3. Messages Table
```sql
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_key TEXT UNIQUE NOT NULL, -- Firebase message key for compatibility
    chat_id UUID NOT NULL REFERENCES chats(id),
    sender_id UUID NOT NULL REFERENCES users(id),
    message_text TEXT,
    message_type TEXT DEFAULT 'text' CHECK (message_type IN ('text', 'image', 'voice', 'file', 'ai')),
    attachment_url TEXT,
    attachment_name TEXT,
    voice_duration INTEGER, -- in milliseconds
    reply_to_message_id UUID REFERENCES messages(id),
    push_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    edited_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_messages_chat_id ON messages(chat_id);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_messages_push_date ON messages(push_date);
CREATE INDEX idx_messages_message_key ON messages(message_key);
```

#### 4. Groups Table
```sql
CREATE TABLE groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id TEXT UNIQUE NOT NULL, -- Firebase group ID for compatibility
    name TEXT NOT NULL,
    description TEXT,
    avatar TEXT,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_groups_group_id ON groups(group_id);
```

#### 5. Group Members Table
```sql
CREATE TABLE group_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES groups(id),
    user_id UUID NOT NULL REFERENCES users(id),
    role TEXT DEFAULT 'member' CHECK (role IN ('admin', 'member')),
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(group_id, user_id)
);

CREATE INDEX idx_group_members_group_id ON group_members(group_id);
CREATE INDEX idx_group_members_user_id ON group_members(user_id);
```

#### 6. Group Messages Table
```sql
CREATE TABLE group_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_key TEXT UNIQUE NOT NULL,
    group_id UUID NOT NULL REFERENCES groups(id),
    sender_id UUID NOT NULL REFERENCES users(id),
    message_text TEXT,
    message_type TEXT DEFAULT 'text' CHECK (message_type IN ('text', 'image', 'voice', 'file', 'ai')),
    attachment_url TEXT,
    attachment_name TEXT,
    voice_duration INTEGER,
    reply_to_message_id UUID REFERENCES group_messages(id),
    push_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    edited_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_group_messages_group_id ON group_messages(group_id);
CREATE INDEX idx_group_messages_sender_id ON group_messages(sender_id);
CREATE INDEX idx_group_messages_push_date ON group_messages(push_date);
```

#### 7. Inbox Table (for chat list)
```sql
CREATE TABLE inbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    chat_partner_id UUID REFERENCES users(id), -- NULL for group chats
    group_id UUID REFERENCES groups(id), -- NULL for direct chats
    last_message_id UUID REFERENCES messages(id),
    last_group_message_id UUID REFERENCES group_messages(id),
    unread_count INTEGER DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, chat_partner_id),
    UNIQUE(user_id, group_id),
    CHECK ((chat_partner_id IS NOT NULL AND group_id IS NULL) OR (chat_partner_id IS NULL AND group_id IS NOT NULL))
);

CREATE INDEX idx_inbox_user_id ON inbox(user_id);
CREATE INDEX idx_inbox_updated_at ON inbox(updated_at);
```

#### 8. Block List Table
```sql
CREATE TABLE block_list (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id UUID NOT NULL REFERENCES users(id),
    blocked_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(blocker_id, blocked_id)
);

CREATE INDEX idx_block_list_blocker_id ON block_list(blocker_id);
CREATE INDEX idx_block_list_blocked_id ON block_list(blocked_id);
```

#### 9. Posts Table (for social features)
```sql
CREATE TABLE posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id TEXT UNIQUE NOT NULL, -- Firebase post ID for compatibility
    author_id UUID NOT NULL REFERENCES users(id),
    content TEXT,
    image_url TEXT,
    video_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_posts_author_id ON posts(author_id);
CREATE INDEX idx_posts_created_at ON posts(created_at);
```

#### 10. Username Registry Table
```sql
CREATE TABLE username_registry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    uid TEXT NOT NULL, -- Firebase UID
    email TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_username_registry_username ON username_registry(username);
```

### Row Level Security (RLS) Policies

```sql
-- Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE chats ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE group_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE group_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE inbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE block_list ENABLE ROW LEVEL SECURITY;
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;

-- Users can read their own profile and public profiles
CREATE POLICY "Users can view profiles" ON users FOR SELECT USING (true);
CREATE POLICY "Users can update own profile" ON users FOR UPDATE USING (auth.uid()::text = uid);

-- Chat access policies
CREATE POLICY "Users can view own chats" ON chats FOR SELECT USING (
    participant_1 = (SELECT id FROM users WHERE uid = auth.uid()::text) OR 
    participant_2 = (SELECT id FROM users WHERE uid = auth.uid()::text)
);

-- Message access policies
CREATE POLICY "Users can view messages in their chats" ON messages FOR SELECT USING (
    chat_id IN (
        SELECT id FROM chats WHERE 
        participant_1 = (SELECT id FROM users WHERE uid = auth.uid()::text) OR 
        participant_2 = (SELECT id FROM users WHERE uid = auth.uid()::text)
    )
);

CREATE POLICY "Users can insert messages in their chats" ON messages FOR INSERT WITH CHECK (
    sender_id = (SELECT id FROM users WHERE uid = auth.uid()::text) AND
    chat_id IN (
        SELECT id FROM chats WHERE 
        participant_1 = (SELECT id FROM users WHERE uid = auth.uid()::text) OR 
        participant_2 = (SELECT id FROM users WHERE uid = auth.uid()::text)
    )
);
```

## 1. Initial Setup and Configuration

### 1.1. Environment Setup

Create separate Supabase projects for different environments:
- **Development:** For local development and testing
- **Staging:** Pre-production environment for final testing  
- **Production:** Live environment for users

### 1.2. Remove Firebase and Add Supabase Dependencies

**Step 1: Update plugins in `app/build.gradle`:**

```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'
    id 'kotlinx-serialization' // Add for Supabase serialization
    // Remove Firebase plugins:
    // id 'com.google.firebase.crashlytics'
    // id 'com.google.firebase.firebase-perf'
}
```

**Step 2: Replace dependencies:**

```gradle
dependencies {
    // Supabase BOM and dependencies
    implementation(platform("io.github.jan-tennert.supabase:bom:2.6.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:gotrue-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.ktor:ktor-client-android:2.3.12")
    
    // Serialization for Supabase
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    
    // Remove all Firebase dependencies:
    // implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    // implementation 'com.google.firebase:firebase-auth-ktx'
    // implementation 'com.google.firebase:firebase-database-ktx'
    // implementation 'com.google.firebase:firebase-perf-ktx'
    // implementation 'com.google.firebase:firebase-crashlytics-ktx'
    // implementation 'com.google.firebase:firebase-analytics-ktx'
}

// Remove Google Services plugin:
// apply plugin: 'com.google.gms.google-services'
```

### 1.3. Configure API Keys

Add Supabase configuration to `gradle.properties`:

```properties
# Supabase Configuration
SUPABASE_URL=your_supabase_project_url
SUPABASE_ANON_KEY=your_supabase_anon_key
```

Expose keys in `app/build.gradle`:
```gradle
android {
    defaultConfig {
        buildConfigField "String", "SUPABASE_URL", "\"${SUPABASE_URL}\""
        buildConfigField "String", "SUPABASE_ANON_KEY", "\"${SUPABASE_ANON_KEY}\""
    }
}
```

### 1.4. Initialize Supabase Client

Create a singleton Supabase client in Kotlin:

```kotlin
// SupabaseClient.kt
package com.synapse.social.studioasinc

import io.github.jan.tennert.supabase.createSupabaseClient
import io.github.jan.tennert.supabase.gotrue.GoTrue
import io.github.jan.tennert.supabase.postgrest.Postgrest
import io.github.jan.tennert.supabase.realtime.Realtime
import io.github.jan.tennert.supabase.storage.Storage

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(GoTrue) {
            // Authentication configuration
        }
        install(Postgrest) {
            // Database configuration
        }
        install(Realtime) {
            // Realtime configuration
        }
        install(Storage) {
            // Storage configuration
        }
    }
}
```

### 1.5. Create Backend Service Interfaces

Update the existing backend service interfaces to support Supabase:

```kotlin
// ISupabaseAuthenticationService.kt
package com.synapse.social.studioasinc.backend.interfaces

import io.github.jan.tennert.supabase.gotrue.user.UserInfo

interface ISupabaseAuthenticationService {
    suspend fun getCurrentUser(): UserInfo?
    suspend fun signIn(email: String, password: String): UserInfo
    suspend fun signUp(email: String, password: String): UserInfo
    suspend fun signOut()
    suspend fun deleteUser()
}
```

```kotlin
// ISupabaseDatabaseService.kt
package com.synapse.social.studioasinc.backend.interfaces

interface ISupabaseDatabaseService {
    suspend fun <T> select(table: String, columns: String = "*"): List<T>
    suspend fun <T> selectSingle(table: String, columns: String = "*"): T?
    suspend fun insert(table: String, data: Map<String, Any?>): Map<String, Any?>
    suspend fun update(table: String, data: Map<String, Any?>): Map<String, Any?>
    suspend fun delete(table: String): Boolean
    suspend fun upsert(table: String, data: Map<String, Any?>): Map<String, Any?>
}
```

## 2. Authentication Migration

### 2.1. Create Supabase Authentication Service

```kotlin
// SupabaseAuthenticationService.kt
package com.synapse.social.studioasinc.backend

import io.github.jan.tennert.supabase.gotrue.auth
import io.github.jan.tennert.supabase.gotrue.providers.builtin.Email
import io.github.jan.tennert.supabase.gotrue.user.UserInfo
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.backend.interfaces.ISupabaseAuthenticationService

class SupabaseAuthenticationService : ISupabaseAuthenticationService {
    
    private val auth = SupabaseClient.client.auth
    
    override suspend fun getCurrentUser(): UserInfo? {
        return auth.currentUserOrNull()
    }
    
    override suspend fun signIn(email: String, password: String): UserInfo {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        return auth.currentUserOrNull() ?: throw Exception("Sign in failed")
    }
    
    override suspend fun signUp(email: String, password: String): UserInfo {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        return auth.currentUserOrNull() ?: throw Exception("Sign up failed")
    }
    
    override suspend fun signOut() {
        auth.signOut()
    }
    
    override suspend fun deleteUser() {
        auth.deleteUser()
    }
}
```

### 2.2. Create Supabase Database Service

```kotlin
// SupabaseDatabaseService.kt
package com.synapse.social.studioasinc.backend

import io.github.jan.tennert.supabase.postgrest.from
import kotlinx.serialization.json.JsonObject
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.backend.interfaces.ISupabaseDatabaseService

class SupabaseDatabaseService : ISupabaseDatabaseService {
    
    private val client = SupabaseClient.client
    
    override suspend fun <T> select(table: String, columns: String): List<T> {
        return client.from(table).select(columns = columns).decodeList()
    }
    
    override suspend fun <T> selectSingle(table: String, columns: String): T? {
        return try {
            client.from(table).select(columns = columns).decodeSingle<T>()
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun insert(table: String, data: Map<String, Any?>): Map<String, Any?> {
        return client.from(table).insert(data).decodeSingle()
    }
    
    override suspend fun update(table: String, data: Map<String, Any?>): Map<String, Any?> {
        return client.from(table).update(data).decodeSingle()
    }
    
    override suspend fun delete(table: String): Boolean {
        return try {
            client.from(table).delete()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun upsert(table: String, data: Map<String, Any?>): Map<String, Any?> {
        return client.from(table).upsert(data).decodeSingle()
    }
}
```

### 2.3. Migrating Existing Users

Migrating existing users from Firebase to Supabase requires careful handling of user data and authentication.

#### 2.3.1. Export Users from Firebase

```bash
# Export Firebase users
firebase auth:export users.json --project your-firebase-project-id
```

#### 2.3.2. Transform User Data

Create a migration script to transform Firebase user data:

```python
# migrate_users.py
import json
import csv
from datetime import datetime

def transform_firebase_users(firebase_export_file, output_csv):
    with open(firebase_export_file, 'r') as f:
        firebase_data = json.load(f)
    
    users = firebase_data.get('users', [])
    
    with open(output_csv, 'w', newline='') as csvfile:
        fieldnames = ['uid', 'email', 'email_verified', 'created_at', 'password_hash']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        
        for user in users:
            writer.writerow({
                'uid': user.get('localId'),
                'email': user.get('email'),
                'email_verified': user.get('emailVerified', False),
                'created_at': datetime.fromtimestamp(int(user.get('createdAt', 0)) / 1000).isoformat(),
                'password_hash': user.get('passwordHash', '')
            })

# Run the transformation
transform_firebase_users('users.json', 'supabase_users.csv')
```

#### 2.3.3. Import Users into Supabase

Use Supabase CLI or dashboard to import users:

```bash
# Using Supabase CLI
supabase db seed --db-url "your-supabase-db-url" --file supabase_users.csv
```

### 2.4. Realtime Authentication State

```kotlin
// Monitor authentication state changes
class AuthStateManager {
    private val auth = SupabaseClient.client.auth
    
    fun observeAuthState(callback: (UserInfo?) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> callback(status.session.user)
                    is SessionStatus.NotAuthenticated -> callback(null)
                    else -> { /* Handle loading states */ }
                }
            }
        }
    }
}
```

## 3. Data Migration

### 3.1. Create Supabase Realtime Service

```kotlin
// SupabaseRealtimeService.kt
package com.synapse.social.studioasinc.backend

import io.github.jan.tennert.supabase.realtime.PostgresAction
import io.github.jan.tennert.supabase.realtime.channel
import io.github.jan.tennert.supabase.realtime.postgresChangeFlow
import io.github.jan.tennert.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import com.synapse.social.studioasinc.SupabaseClient

class SupabaseRealtimeService {
    
    private val realtime = SupabaseClient.client.realtime
    
    fun subscribeToMessages(chatId: String): Flow<Map<String, Any?>> {
        val channel = realtime.channel("messages:$chatId")
        
        return channel.postgresChangeFlow<Map<String, Any?>>(
            schema = "public"
        ) {
            table = "messages"
            filter = "chat_id=eq.$chatId"
        }
    }
    
    fun subscribeToUserStatus(userId: String): Flow<Map<String, Any?>> {
        val channel = realtime.channel("user_status:$userId")
        
        return channel.postgresChangeFlow<Map<String, Any?>>(
            schema = "public"
        ) {
            table = "users"
            filter = "id=eq.$userId"
        }
    }
    
    suspend fun joinChannel(channelName: String) {
        realtime.channel(channelName).join()
    }
    
    suspend fun leaveChannel(channelName: String) {
        realtime.channel(channelName).leave()
    }
}
```

### 3.2. Data Migration Strategy

#### 3.2.1. Export Firebase Data

```javascript
// firebase_export.js
const admin = require('firebase-admin');
const fs = require('fs');

// Initialize Firebase Admin
const serviceAccount = require('./path/to/serviceAccountKey.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://your-project.firebaseio.com'
});

const db = admin.database();

async function exportData() {
  try {
    // Export users
    const usersSnapshot = await db.ref('skyline/users').once('value');
    const users = usersSnapshot.val();
    fs.writeFileSync('firebase_users.json', JSON.stringify(users, null, 2));
    
    // Export chats
    const chatsSnapshot = await db.ref('chats').once('value');
    const chats = chatsSnapshot.val();
    fs.writeFileSync('firebase_chats.json', JSON.stringify(chats, null, 2));
    
    // Export other data...
    console.log('Data export completed');
  } catch (error) {
    console.error('Export failed:', error);
  }
}

exportData();
```

#### 3.2.2. Transform and Import Data

```python
# transform_data.py
import json
import psycopg2
from datetime import datetime
import uuid

def connect_to_supabase():
    return psycopg2.connect(
        host="your-supabase-host",
        database="postgres",
        user="postgres",
        password="your-password"
    )

def migrate_users(firebase_users_file):
    with open(firebase_users_file, 'r') as f:
        firebase_users = json.load(f)
    
    conn = connect_to_supabase()
    cursor = conn.cursor()
    
    for uid, user_data in firebase_users.items():
        try:
            cursor.execute("""
                INSERT INTO users (
                    uid, email, username, nickname, biography, avatar,
                    account_premium, verify, account_type, status, join_date
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                ON CONFLICT (uid) DO NOTHING
            """, (
                uid,
                user_data.get('email'),
                user_data.get('username'),
                user_data.get('nickname'),
                user_data.get('biography'),
                user_data.get('avatar'),
                user_data.get('account_premium', False),
                user_data.get('verify', False),
                user_data.get('account_type', 'user'),
                user_data.get('status', 'offline'),
                datetime.fromtimestamp(int(user_data.get('join_date', 0)) / 1000)
            ))
        except Exception as e:
            print(f"Error migrating user {uid}: {e}")
    
    conn.commit()
    cursor.close()
    conn.close()

def migrate_chats(firebase_chats_file):
    with open(firebase_chats_file, 'r') as f:
        firebase_chats = json.load(f)
    
    conn = connect_to_supabase()
    cursor = conn.cursor()
    
    for chat_id, messages in firebase_chats.items():
        # Extract participant UIDs from chat_id
        participants = chat_id.split('_')
        if len(participants) == 2:
            try:
                # Get user IDs from UIDs
                cursor.execute("SELECT id FROM users WHERE uid IN (%s, %s)", participants)
                user_ids = cursor.fetchall()
                
                if len(user_ids) == 2:
                    # Create chat
                    cursor.execute("""
                        INSERT INTO chats (chat_id, participant_1, participant_2)
                        VALUES (%s, %s, %s)
                        ON CONFLICT (chat_id) DO NOTHING
                        RETURNING id
                    """, (chat_id, user_ids[0][0], user_ids[1][0]))
                    
                    chat_result = cursor.fetchone()
                    if chat_result:
                        chat_db_id = chat_result[0]
                        
                        # Migrate messages
                        for msg_key, msg_data in messages.items():
                            if isinstance(msg_data, dict) and 'message_text' in msg_data:
                                cursor.execute("SELECT id FROM users WHERE uid = %s", (msg_data.get('uid'),))
                                sender_result = cursor.fetchone()
                                
                                if sender_result:
                                    cursor.execute("""
                                        INSERT INTO messages (
                                            message_key, chat_id, sender_id, message_text,
                                            message_type, push_date
                                        ) VALUES (%s, %s, %s, %s, %s, %s)
                                        ON CONFLICT (message_key) DO NOTHING
                                    """, (
                                        msg_key,
                                        chat_db_id,
                                        sender_result[0],
                                        msg_data.get('message_text'),
                                        msg_data.get('message_type', 'text'),
                                        datetime.fromtimestamp(int(msg_data.get('push_date', 0)) / 1000)
                                    ))
            except Exception as e:
                print(f"Error migrating chat {chat_id}: {e}")
    
    conn.commit()
    cursor.close()
    conn.close()

# Run migrations
migrate_users('firebase_users.json')
migrate_chats('firebase_chats.json')
```

### 3.3. CRUD Operations with Supabase

```kotlin
// Example CRUD operations with Supabase
class MessageService {
    private val dbService = SupabaseDatabaseService()
    
    // Create a new message
    suspend fun createMessage(messageData: Map<String, Any?>): Map<String, Any?> {
        return dbService.insert("messages", messageData)
    }
    
    // Read messages with filters
    suspend fun getMessages(chatId: String, limit: Int = 50): List<Map<String, Any?>> {
        return dbService.selectWithFilter<Map<String, Any?>>(
            table = "messages",
            columns = "*"
        ) { query ->
            // Apply filters - this would need proper implementation
            query
        }
    }
    
    // Update a message
    suspend fun updateMessage(messageId: String, updates: Map<String, Any?>): Map<String, Any?> {
        return dbService.update("messages", updates)
    }
    
    // Delete a message (soft delete)
    suspend fun deleteMessage(messageId: String): Boolean {
        val deleteData = mapOf("deleted_at" to Date())
        return try {
            dbService.update("messages", deleteData)
            true
        } catch (e: Exception) {
            false
        }
    }
}
```

### 3.4. Complex Queries and Joins

```kotlin
// Advanced query examples
class AdvancedQueryService {
    private val dbService = SupabaseDatabaseService()
    
    // Get messages with user information (JOIN)
    suspend fun getMessagesWithUsers(chatId: String): List<Map<String, Any?>> {
        return dbService.selectWithFilter<Map<String, Any?>>(
            table = "messages",
            columns = """
                *,
                users!sender_id(username, nickname, avatar)
            """.trimIndent()
        ) { query ->
            // Filter by chat_id and order by push_date
            query
        }
    }
    
    // Get user's recent chats with last message
    suspend fun getUserRecentChats(userId: String): List<Map<String, Any?>> {
        return dbService.selectWithFilter<Map<String, Any?>>(
            table = "inbox",
            columns = """
                *,
                users!chat_partner_id(username, nickname, avatar, status),
                messages!last_message_id(message_text, push_date, message_type)
            """.trimIndent()
        ) { query ->
            // Filter by user_id and order by updated_at DESC
            query
        }
    }
}

## 4. File Storage Migration

### 4.1. Create Supabase Storage Service

```kotlin
// SupabaseStorageService.kt
package com.synapse.social.studioasinc.backend

import io.github.jan.tennert.supabase.storage.storage
import com.synapse.social.studioasinc.SupabaseClient
import java.io.File

class SupabaseStorageService {
    
    private val storage = SupabaseClient.client.storage
    
    /**
     * Upload a file to Supabase Storage.
     */
    suspend fun uploadFile(
        bucketName: String,
        fileName: String,
        file: File
    ): String {
        val bucket = storage.from(bucketName)
        bucket.upload(fileName, file)
        return bucket.publicUrl(fileName)
    }
    
    /**
     * Upload image with automatic resizing.
     */
    suspend fun uploadImage(
        bucketName: String,
        fileName: String,
        imageFile: File,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024
    ): String {
        // You might want to resize the image before uploading
        val bucket = storage.from(bucketName)
        bucket.upload(fileName, imageFile)
        return bucket.publicUrl(fileName)
    }
    
    /**
     * Delete a file from storage.
     */
    suspend fun deleteFile(bucketName: String, fileName: String): Boolean {
        return try {
            val bucket = storage.from(bucketName)
            bucket.delete(fileName)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get public URL for a file.
     */
    fun getPublicUrl(bucketName: String, fileName: String): String {
        return storage.from(bucketName).publicUrl(fileName)
    }
}
```

### 4.2. Migrate Existing Files

```python
# migrate_storage.py
import requests
import os
from supabase import create_client, Client

def migrate_firebase_storage_to_supabase():
    # Initialize Supabase client
    supabase: Client = create_client(
        "your-supabase-url",
        "your-supabase-key"
    )
    
    # List of Firebase Storage URLs to migrate
    firebase_urls = [
        "https://firebasestorage.googleapis.com/v0/b/your-project/o/images%2Ffile1.jpg",
        # Add more URLs...
    ]
    
    for firebase_url in firebase_urls:
        try:
            # Download file from Firebase
            response = requests.get(firebase_url)
            if response.status_code == 200:
                # Extract filename
                filename = firebase_url.split('/')[-1].split('?')[0]
                filename = filename.replace('%2F', '/')
                
                # Upload to Supabase
                supabase.storage.from_("avatars").upload(
                    filename,
                    response.content
                )
                print(f"Migrated: {filename}")
        except Exception as e:
            print(f"Error migrating {firebase_url}: {e}")

migrate_firebase_storage_to_supabase()
```

## 5. Realtime Data Synchronization

### 5.1. Replace Firebase Listeners with Supabase Realtime

```kotlin
// ChatActivitySupabase.kt - Realtime message listening
class ChatActivitySupabase : AppCompatActivity() {
    
    private lateinit var realtimeService: SupabaseRealtimeService
    private lateinit var chatService: SupabaseChatService
    
    private fun setupRealtimeListeners() {
        val chatId = intent.getStringExtra("chatId") ?: return
        
        // Subscribe to new messages
        lifecycleScope.launch {
            realtimeService.subscribeToMessages(chatId).collect { newMessage ->
                // Handle new message
                runOnUiThread {
                    addMessageToUI(newMessage)
                }
            }
        }
        
        // Subscribe to user status changes
        val otherUserId = intent.getStringExtra("otherUserId") ?: return
        lifecycleScope.launch {
            realtimeService.subscribeToUserStatus(otherUserId).collect { userStatus ->
                // Update user status in UI
                runOnUiThread {
                    updateUserStatus(userStatus)
                }
            }
        }
        
        // Subscribe to typing indicators
        lifecycleScope.launch {
            realtimeService.subscribeToTypingStatus(chatId).collect { typingStatus ->
                // Show/hide typing indicator
                runOnUiThread {
                    updateTypingIndicator(typingStatus)
                }
            }
        }
    }
    
    private fun addMessageToUI(message: Map<String, Any?>) {
        // Add message to RecyclerView
        chatAdapter.addMessage(message)
        chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
    }
    
    private fun updateUserStatus(userStatus: Map<String, Any?>) {
        val status = userStatus["status"] as? String
        val lastSeen = userStatus["last_seen"] as? String
        
        when (status) {
            "online" -> statusTextView.text = "Online"
            "offline" -> statusTextView.text = "Last seen $lastSeen"
            else -> statusTextView.text = "Unknown"
        }
    }
}
```

### 5.2. Presence Management

```kotlin
// SupabasePresenceManager.kt
class SupabasePresenceManager {
    
    private val realtimeService = SupabaseRealtimeService()
    private val dbService = SupabaseDatabaseService()
    
    suspend fun setUserOnline(userId: String) {
        // Update user status in database
        dbService.update("users", mapOf(
            "status" to "online",
            "last_seen" to Date()
        ))
        
        // Update presence in realtime
        realtimeService.updatePresence("user:$userId", mapOf(
            "status" to "online",
            "timestamp" to System.currentTimeMillis()
        ))
    }
    
    suspend fun setUserOffline(userId: String) {
        dbService.update("users", mapOf(
            "status" to "offline",
            "last_seen" to Date()
        ))
        
        realtimeService.updatePresence("user:$userId", mapOf(
            "status" to "offline",
            "timestamp" to System.currentTimeMillis()
        ))
    }
    
    suspend fun setChattingWith(userId: String, chattingWithId: String?) {
        dbService.update("users", mapOf(
            "chatting_with" to chattingWithId
        ))
    }
}

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

## 6. Complete Migration Summary

### 6.1. Files Created/Modified

**New Supabase Files Created:**
- `SupabaseClient.kt` - Singleton Supabase client
- `SupabaseAuthenticationService.kt` - Authentication service
- `SupabaseDatabaseService.kt` - Database service with typed methods
- `SupabaseRealtimeService.kt` - Realtime subscriptions
- `SupabaseChatService.kt` - Chat operations
- `SupabaseUserDataPusher.kt` - User profile management
- `SupabaseOneSignalManager.kt` - OneSignal integration
- `SupabaseModels.kt` - Serializable data models
- `AuthActivitySupabase.kt` - Migrated authentication activity

**Modified Files:**
- `app/build.gradle` - Removed Firebase, added Supabase dependencies
- `gradle.properties` - Added Supabase configuration
- `AndroidManifest.xml` - Removed Firebase providers/services
- `AuthenticationService.kt` - Updated to use Supabase backend
- `DatabaseService.kt` - Updated to use Supabase backend
- `UserDataPusher.kt` - Updated to use Supabase backend
- `OneSignalManager.kt` - Updated to use Supabase backend
- `AuthActivity.java` - Removed Firebase imports

### 6.2. Database Migration Steps

1. **Create Supabase Project:**
   ```bash
   # Install Supabase CLI
   npm install -g supabase
   
   # Initialize project
   supabase init
   
   # Start local development
   supabase start
   ```

2. **Run Database Schema:**
   ```sql
   -- Execute the complete schema from section "Database Schema Design"
   -- This creates all tables, indexes, and RLS policies
   ```

3. **Migrate Data:**
   ```bash
   # Export Firebase data
   firebase auth:export users.json --project your-firebase-project
   
   # Run migration scripts
   python migrate_users.py
   python migrate_chats.py
   ```

### 6.3. Configuration Steps

1. **Update `gradle.properties`:**
   ```properties
   SUPABASE_URL=https://your-project-ref.supabase.co
   SUPABASE_ANON_KEY=your-anon-key-here
   ```

2. **Remove Firebase Files:**
   - Delete `google-services.json`
   - Remove Firebase configuration files

3. **Update App Initialization:**
   ```kotlin
   // In Application class or MainActivity
   class SynapseApp : Application() {
       override fun onCreate() {
           super.onCreate()
           // Supabase client is initialized automatically
           // No need for FirebaseApp.initializeApp()
       }
   }
   ```

### 6.4. Testing Migration

1. **Authentication Testing:**
   ```kotlin
   // Test sign up
   val authService = SupabaseAuthenticationService()
   val user = authService.signUp("test@example.com", "password123")
   
   // Test sign in
   val signedInUser = authService.signIn("test@example.com", "password123")
   ```

2. **Database Testing:**
   ```kotlin
   // Test user operations
   val dbService = SupabaseDatabaseService()
   val user = dbService.getUserByUid("user-uid")
   
   // Test chat operations
   val chatService = SupabaseChatService()
   val messages = chatService.getChatMessages("chat-id")
   ```

3. **Realtime Testing:**
   ```kotlin
   // Test realtime subscriptions
   val realtimeService = SupabaseRealtimeService()
   realtimeService.subscribeToMessages("chat-id").collect { message ->
       // Handle new message
   }
   ```

## 7. Class-by-Class Migration Details

### 7.1. AuthActivity Migration

**Before (Firebase):**
```java
FirebaseApp.initializeApp(this);
authService = new AuthenticationService(); // Firebase-based
```

**After (Supabase):**
```java
// No initialization needed - Supabase client is singleton
authService = new AuthenticationService(); // Supabase-based
```

**Key Changes:**
- Removed `FirebaseApp.initializeApp()`
- Authentication now uses coroutines (async/await pattern)
- Error handling updated for Supabase exceptions
- User data structure changed to match PostgreSQL schema

**10.1.1. Step 1: Remove Firebase Dependencies**

The first step is to remove all Firebase-related imports from the `AuthActivity.java` file. This includes:

-   `com.google.firebase.FirebaseApp`
-   Classes from `com.google.firebase.auth`
-   Classes from `com.google.firebase.database`

**10.1.2. Step 2: Integrate Supabase Client**

Next, you'll need to initialize and use the Supabase client. Since `AuthActivity.java` is a Java class, you'll need to access the Supabase client through the `SupabaseClient.INSTANCE` singleton.

**10.1.3. Step 3: Replace Authentication Calls**

The core of the migration involves replacing the Firebase authentication calls with their Supabase equivalents.

**Sign-Up:**

-   **Firebase (Existing):** The existing code uses an `IAuthenticationService` interface, which abstracts the Firebase `createUserWithEmailAndPassword` method.
-   **Supabase (New):** The new implementation will directly use the Supabase `signUpWith` method. Since this is a suspend function in Kotlin, you'll need a wrapper to call it from Java.

**Sign-In:**

-   **Firebase (Existing):** The existing code uses an `IAuthenticationService` interface for `signInWithEmailAndPassword`.
-   **Supabase (New):** The new implementation will use the Supabase `signInWith` method, again with a Kotlin wrapper.

**10.1.4. Step 4: Replace Database Calls**

The `AuthActivity` also fetches the user's username from the database after a successful sign-in. This needs to be migrated to use Supabase PostgREST.

-   **Firebase (Existing):** The existing code uses `dbService.getData` to fetch the username from the Firebase Realtime Database.
-   **Supabase (New):** The new implementation will use the Supabase `select` method to query the `users` table.

**10.1.5. Step 5: Update OneSignal Integration**

The `updateOneSignalPlayerId` method needs to be updated to save the Player ID to Supabase instead of Firebase.

-   **Firebase (Existing):** The existing code uses `OneSignalManager.savePlayerIdToRealtimeDatabase`.
-   **Supabase (New):** The new implementation will use a Supabase `upsert` operation to save the Player ID to the `users` table.

This completes the migration of `AuthActivity.java` from Firebase to Supabase. By following these steps, you can successfully replace all Firebase dependencies and implement a robust, Supabase-powered authentication and data management system.

### 10.2. CompleteProfileActivity.java

This activity allows users to complete their profile by providing a username, nickname, biography, and profile picture.

**10.2.1. Step 1: Remove Firebase Dependencies**

-   Remove all Firebase-related imports, including `FirebaseAuth`, `FirebaseDatabase`, and `FirebaseStorage`.

**10.2.2. Step 2: Replace Username Availability Check**

The activity checks if a username is already taken. This logic needs to be migrated to use Supabase.

-   **Firebase (Existing):** The existing code uses a Firebase Realtime Database query to check for the existence of a username.
-   **Supabase (New):** The new implementation will use a Supabase `select` query with a `count` aggregation.

**10.2.3. Step 3: Replace Profile Data Submission**

The "complete" button pushes the user's profile data to the database. This needs to be migrated to use Supabase.

-   **Firebase (Existing):** The existing code uses a `UserDataPusher` class to push data to the Firebase Realtime Database.
-   **Supabase (New):** The new implementation will use a Supabase `upsert` operation to save the profile data.

**10.2.4. Step 4: Replace Image Upload**

The activity uses an `ImageUploader` class to upload the user's profile picture. This needs to be migrated to use Supabase Storage.

-   **Firebase (Existing):** The existing code uses a custom `ImageUploader` that likely uploads to Firebase Storage or another service.
-   **Supabase (New):** The new implementation will use the Supabase `upload` method.

**10.2.5. Step 5: Replace User Cancellation**

The activity allows the user to cancel the profile completion process, which should sign them out.

-   **Firebase (Existing):** The existing code signs the user out using `FirebaseAuth.getInstance().signOut()`.
-   **Supabase (New):** The new implementation will use the Supabase `signOut` method.

### 10.3. ChatActivity.java

This activity is the core of the real-time chat functionality. Migrating this class involves replacing Firebase Realtime Database listeners with Supabase Realtime channels.

**10.3.1. Step 1: Remove Firebase Dependencies**

-   Remove all Firebase-related imports, including `FirebaseAuth` and `FirebaseDatabase`.

**10.3.2. Step 2: Replace Message Sending**

-   **Firebase (Existing):** Messages are sent by pushing data to a Firebase Realtime Database reference.
-   **Supabase (New):** Messages will be sent by inserting data into a Supabase table.

**10.3.3. Step 3: Replace Real-time Message Listening**

-   **Firebase (Existing):** A `ChildEventListener` is used to listen for new messages.
-   **Supabase (New):** A Supabase Realtime channel will be used to listen for inserts on the `messages` table.

**10.3.4. Step 4: Replace User Status Listening**

-   **Firebase (Existing):** A `ValueEventListener` is used to listen for changes in the user's status.
-   **Supabase (New):** A Supabase Realtime channel can be used to listen for updates on the `users` table.

**10.3.5. Step 5: Replace Message History Fetching**

-   **Firebase (Existing):** `limitToLast` and `endBefore` are used to paginate through message history.
-   **Supabase (New):** `limit` and `order` will be used to fetch message history.
