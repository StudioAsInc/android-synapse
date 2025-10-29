# E2EE Messaging Design Document

## Overview

This document outlines the design for implementing End-to-End Encryption (E2EE) in the Synapse Android application. The design uses the Signal Protocol (Double Ratchet Algorithm) as the cryptographic foundation, leveraging Android's security features and integrating seamlessly with the existing Supabase backend architecture.

The E2EE system will be implemented as a modular layer that sits between the messaging UI and the Supabase backend, ensuring that all message content is encrypted on the sender's device and can only be decrypted by the intended recipient(s).

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Presentation Layer                      │
│  (ChatActivity, MessageAdapter, EncryptionStatusView)       │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                     ViewModel Layer                          │
│        (ChatViewModel, E2EEStatusViewModel)                  │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                    Repository Layer                          │
│  (MessageRepository, E2EEKeyRepository, MediaRepository)    │
└─────┬──────────────────────────────────────────┬────────────┘
      │                                          │
┌─────▼──────────────────┐          ┌───────────▼────────────┐
│   E2EE Crypto Layer    │          │   Supabase Backend     │
│  - SignalProtocol      │          │   - Postgrest          │
│  - KeyManager          │          │   - Storage            │
│  - SessionManager      │          │   - Realtime           │
│  - EncryptionService   │          └────────────────────────┘
└─────┬──────────────────┘
      │
┌─────▼──────────────────┐
│  Android Keystore      │
│  (Secure Key Storage)  │
└────────────────────────┘
```

### Module Structure

The E2EE implementation will be organized into the following packages:

```
com.synapse.social.studioasinc/
├── e2ee/
│   ├── crypto/
│   │   ├── SignalProtocolManager.kt
│   │   ├── KeyPairGenerator.kt
│   │   ├── SessionCipher.kt
│   │   └── IdentityKeyStore.kt
│   ├── storage/
│   │   ├── SecureKeyStore.kt
│   │   ├── SessionStore.kt
│   │   ├── PreKeyStore.kt
│   │   └── SignedPreKeyStore.kt
│   ├── models/
│   │   ├── E2EEMessage.kt
│   │   ├── KeyBundle.kt
│   │   ├── EncryptedMedia.kt
│   │   └── VerificationCode.kt
│   ├── repository/
│   │   ├── E2EEKeyRepository.kt
│   │   └── E2EEMessageRepository.kt
│   ├── service/
│   │   ├── EncryptionService.kt
│   │   ├── DecryptionService.kt
│   │   ├── KeyExchangeService.kt
│   │   └── KeyBackupService.kt
│   └── ui/
│       ├── EncryptionStatusView.kt
│       ├── VerificationDialog.kt
│       └── KeyBackupActivity.kt
```

## Components and Interfaces

### 1. Cryptographic Core Components

#### SignalProtocolManager
Manages the Signal Protocol implementation and coordinates encryption/decryption operations.

```kotlin
interface SignalProtocolManager {
    suspend fun initialize(userId: String)
    suspend fun encryptMessage(recipientId: String, plaintext: ByteArray): EncryptedMessage
    suspend fun decryptMessage(senderId: String, encryptedMessage: EncryptedMessage): ByteArray
    suspend fun createSession(recipientId: String, keyBundle: KeyBundle)
    suspend fun hasSession(userId: String): Boolean
}
```

**Implementation Details:**
- Uses libsignal-protocol-java library (or Kotlin port)
- Implements Double Ratchet Algorithm for forward secrecy
- Manages X3DH (Extended Triple Diffie-Hellman) key agreement
- Handles session state and ratchet progression

#### KeyPairGenerator
Generates and manages cryptographic key pairs.

```kotlin
interface KeyPairGenerator {
    fun generateIdentityKeyPair(): IdentityKeyPair
    fun generatePreKeys(start: Int, count: Int): List<PreKeyRecord>
    fun generateSignedPreKey(identityKeyPair: IdentityKeyPair, signedPreKeyId: Int): SignedPreKeyRecord
}
```

**Implementation Details:**
- Uses Curve25519 for key generation
- Generates identity keys (long-term)
- Generates one-time prekeys (ephemeral)
- Generates signed prekeys with rotation support

#### EncryptionService
High-level service for encrypting messages and media.

```kotlin
interface EncryptionService {
    suspend fun encryptTextMessage(recipientId: String, text: String): E2EEMessage
    suspend fun encryptMediaFile(recipientId: String, fileUri: Uri): EncryptedMedia
    suspend fun encryptGroupMessage(groupId: String, text: String): E2EEMessage
}
```

**Implementation Details:**
- Handles message serialization before encryption
- Manages session establishment if needed
- Implements AES-256-GCM for media encryption
- Generates unique IVs for each encryption operation

#### DecryptionService
High-level service for decrypting messages and media.

```kotlin
interface DecryptionService {
    suspend fun decryptTextMessage(senderId: String, encryptedMessage: E2EEMessage): String
    suspend fun decryptMediaFile(senderId: String, encryptedMedia: EncryptedMedia): Uri
    suspend fun decryptGroupMessage(groupId: String, encryptedMessage: E2EEMessage): String
}
```

### 2. Storage Components

#### SecureKeyStore
Manages secure storage of private keys using Android Keystore.

```kotlin
interface SecureKeyStore {
    suspend fun storeIdentityKeyPair(userId: String, keyPair: IdentityKeyPair)
    suspend fun getIdentityKeyPair(userId: String): IdentityKeyPair?
    suspend fun storeSessionKey(sessionId: String, key: ByteArray)
    suspend fun getSessionKey(sessionId: String): ByteArray?
    suspend fun deleteAllKeys(userId: String)
}
```

**Implementation Details:**
- Uses Android Keystore System for hardware-backed security
- Implements AES-256 encryption for key wrapping
- Requires user authentication (biometric/PIN) for key access on API 28+
- Stores keys in EncryptedSharedPreferences for API 23-27

#### SessionStore
Stores Signal Protocol session state.

```kotlin
interface SessionStore {
    suspend fun loadSession(address: SignalProtocolAddress): SessionRecord?
    suspend fun storeSession(address: SignalProtocolAddress, record: SessionRecord)
    suspend fun containsSession(address: SignalProtocolAddress): Boolean
    suspend fun deleteSession(address: SignalProtocolAddress)
    suspend fun deleteAllSessions(userId: String)
}
```

#### PreKeyStore & SignedPreKeyStore
Manage one-time and signed prekeys.

```kotlin
interface PreKeyStore {
    suspend fun loadPreKey(preKeyId: Int): PreKeyRecord
    suspend fun storePreKey(preKeyId: Int, record: PreKeyRecord)
    suspend fun containsPreKey(preKeyId: Int): Boolean
    suspend fun removePreKey(preKeyId: Int)
}

interface SignedPreKeyStore {
    suspend fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord
    suspend fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord)
    suspend fun containsSignedPreKey(signedPreKeyId: Int): Boolean
    suspend fun removeSignedPreKey(signedPreKeyId: Int)
}
```

### 3. Repository Layer

#### E2EEKeyRepository
Manages key distribution and retrieval from Supabase.

```kotlin
interface E2EEKeyRepository {
    suspend fun publishKeyBundle(userId: String, keyBundle: KeyBundle): Result<Unit>
    suspend fun fetchKeyBundle(userId: String): Result<KeyBundle>
    suspend fun uploadEncryptedBackup(userId: String, encryptedBackup: ByteArray): Result<Unit>
    suspend fun downloadEncryptedBackup(userId: String): Result<ByteArray>
    suspend fun markPreKeyAsUsed(userId: String, preKeyId: Int): Result<Unit>
}
```

**Supabase Schema:**
```sql
-- Key bundles table
CREATE TABLE e2ee_key_bundles (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id),
    identity_key TEXT NOT NULL,
    signed_pre_key JSONB NOT NULL,
    one_time_pre_keys JSONB[] NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Key backups table
CREATE TABLE e2ee_key_backups (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id),
    encrypted_backup BYTEA NOT NULL,
    backup_version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Device registrations (for multi-device support)
CREATE TABLE e2ee_devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id),
    device_id TEXT NOT NULL,
    identity_key TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, device_id)
);
```

#### E2EEMessageRepository
Handles encrypted message storage and retrieval.

```kotlin
interface E2EEMessageRepository {
    suspend fun sendEncryptedMessage(message: E2EEMessage): Result<String>
    suspend fun fetchEncryptedMessages(conversationId: String, limit: Int): Result<List<E2EEMessage>>
    suspend fun markMessageAsDecrypted(messageId: String): Result<Unit>
}
```

**Supabase Schema:**
```sql
-- Encrypted messages table
CREATE TABLE encrypted_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL,
    sender_id UUID REFERENCES auth.users(id),
    recipient_id UUID REFERENCES auth.users(id),
    encrypted_content BYTEA NOT NULL,
    message_type TEXT NOT NULL, -- 'text', 'media', 'group'
    encryption_version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    decrypted_at TIMESTAMPTZ
);

-- Encrypted media table
CREATE TABLE encrypted_media (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    message_id UUID REFERENCES encrypted_messages(id),
    encrypted_file_path TEXT NOT NULL,
    encrypted_key BYTEA NOT NULL,
    media_type TEXT NOT NULL,
    file_size BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Group encryption keys table
CREATE TABLE group_encryption_keys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL,
    key_version INT NOT NULL,
    encrypted_keys JSONB NOT NULL, -- Map of user_id to encrypted group key
    created_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ
);
```

### 4. Service Layer

#### KeyExchangeService
Manages the X3DH key exchange protocol.

```kotlin
interface KeyExchangeService {
    suspend fun initiateKeyExchange(recipientId: String): Result<Unit>
    suspend fun completeKeyExchange(senderId: String, keyBundle: KeyBundle): Result<Unit>
    suspend fun rotateSignedPreKey(): Result<Unit>
    suspend fun generateAndUploadPreKeys(count: Int): Result<Unit>
}
```

**Implementation Details:**
- Implements X3DH protocol for initial key agreement
- Handles prekey rotation (weekly for signed prekeys)
- Manages one-time prekey replenishment
- Validates key bundle signatures

#### KeyBackupService
Manages encrypted key backups.

```kotlin
interface KeyBackupService {
    suspend fun createBackup(passphrase: String): Result<Unit>
    suspend fun restoreBackup(passphrase: String): Result<Unit>
    suspend fun verifyBackupPassphrase(passphrase: String): Result<Boolean>
    suspend fun deleteBackup(): Result<Unit>
}
```

**Implementation Details:**
- Uses PBKDF2 with 100,000 iterations for passphrase derivation
- Encrypts backup with AES-256-GCM
- Includes identity keys, session states, and prekeys
- Validates backup integrity with HMAC

### 5. UI Components

#### EncryptionStatusView
Custom view showing encryption status in conversations.

```kotlin
class EncryptionStatusView : FrameLayout {
    fun setEncryptionStatus(status: EncryptionStatus)
    fun setVerificationStatus(verified: Boolean)
    fun setOnVerifyClickListener(listener: () -> Unit)
}

sealed class EncryptionStatus {
    object NotEncrypted : EncryptionStatus()
    object Encrypted : EncryptionStatus()
    object EncryptedAndVerified : EncryptionStatus()
    data class EncryptionError(val error: String) : EncryptionStatus()
}
```

#### VerificationDialog
Dialog for comparing verification codes.

```kotlin
class VerificationDialog : DialogFragment {
    fun setVerificationCode(code: String)
    fun setOnVerifiedListener(listener: () -> Unit)
}
```

## Data Models

### E2EEMessage
```kotlin
@Serializable
data class E2EEMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val encryptedContent: ByteArray,
    val messageType: MessageType,
    val encryptionVersion: Int = 1,
    val timestamp: Long,
    val metadata: MessageMetadata? = null
)

enum class MessageType {
    TEXT, MEDIA, GROUP, KEY_EXCHANGE
}

@Serializable
data class MessageMetadata(
    val mediaId: String? = null,
    val groupId: String? = null,
    val preKeyId: Int? = null
)
```

### KeyBundle
```kotlin
@Serializable
data class KeyBundle(
    val userId: String,
    val identityKey: String, // Base64 encoded
    val signedPreKey: SignedPreKey,
    val oneTimePreKeys: List<PreKey>,
    val timestamp: Long
)

@Serializable
data class SignedPreKey(
    val keyId: Int,
    val publicKey: String, // Base64 encoded
    val signature: String // Base64 encoded
)

@Serializable
data class PreKey(
    val keyId: Int,
    val publicKey: String // Base64 encoded
)
```

### EncryptedMedia
```kotlin
@Serializable
data class EncryptedMedia(
    val id: String,
    val messageId: String,
    val encryptedFilePath: String,
    val encryptedKey: ByteArray,
    val iv: ByteArray,
    val mediaType: MediaType,
    val fileSize: Long,
    val thumbnail: ByteArray? = null
)

enum class MediaType {
    IMAGE, VIDEO, AUDIO, DOCUMENT
}
```

### VerificationCode
```kotlin
data class VerificationCode(
    val code: String,
    val localFingerprint: String,
    val remoteFingerprint: String
) {
    fun toDisplayString(): String {
        // Format as groups of 5 digits for easy comparison
        return code.chunked(5).joinToString(" ")
    }
}
```

## Error Handling

### Error Types

```kotlin
sealed class E2EEError : Exception() {
    data class KeyGenerationError(override val message: String) : E2EEError()
    data class EncryptionError(override val message: String) : E2EEError()
    data class DecryptionError(override val message: String) : E2EEError()
    data class KeyExchangeError(override val message: String) : E2EEError()
    data class SessionNotFoundError(val userId: String) : E2EEError()
    data class InvalidKeyBundleError(override val message: String) : E2EEError()
    data class StorageError(override val message: String) : E2EEError()
    data class NetworkError(override val cause: Throwable) : E2EEError()
    data class BackupError(override val message: String) : E2EEError()
}
```

### Error Handling Strategy

1. **Encryption Failures:**
   - Retry with exponential backoff (3 attempts)
   - Queue message for later if session establishment fails
   - Show user-friendly error with retry option
   - Log detailed error for debugging (without sensitive data)

2. **Decryption Failures:**
   - Display "Unable to decrypt message" placeholder
   - Attempt session re-establishment
   - Provide option to request sender to resend
   - Never show raw encrypted content

3. **Key Exchange Failures:**
   - Retry key bundle fetch (3 attempts)
   - Fall back to unencrypted messaging with user consent
   - Show clear indication that E2EE is unavailable
   - Cache failed attempts to avoid repeated failures

4. **Storage Failures:**
   - Attempt recovery from backup if available
   - Prompt user to re-initialize E2EE
   - Preserve encrypted messages for later decryption
   - Clear indication of storage issues in UI

### Error Recovery Flow

```
┌─────────────────┐
│ Encryption Fail │
└────────┬────────┘
         │
    ┌────▼────┐
    │ Retry 1 │
    └────┬────┘
         │ Fail
    ┌────▼────┐
    │ Retry 2 │
    └────┬────┘
         │ Fail
    ┌────▼────┐
    │ Retry 3 │
    └────┬────┘
         │ Fail
    ┌────▼──────────────┐
    │ Queue for Later   │
    │ Show User Error   │
    └───────────────────┘
```

## Testing Strategy

### Unit Tests

1. **Cryptographic Operations:**
   - Test key generation produces valid key pairs
   - Test encryption/decryption round-trip
   - Test session establishment and ratcheting
   - Test key derivation functions
   - Test signature verification

2. **Storage Operations:**
   - Test key storage and retrieval
   - Test session persistence
   - Test prekey management
   - Test backup creation and restoration

3. **Repository Layer:**
   - Test key bundle upload/download
   - Test message encryption/decryption flow
   - Test error handling and retries
   - Mock Supabase responses

### Integration Tests

1. **End-to-End Message Flow:**
   - Test complete message encryption and decryption
   - Test key exchange between two users
   - Test group message encryption
   - Test media encryption and decryption

2. **Key Management:**
   - Test prekey rotation
   - Test signed prekey rotation
   - Test backup and restore flow
   - Test multi-device key synchronization

3. **Error Scenarios:**
   - Test behavior with missing keys
   - Test network failure handling
   - Test corrupted message handling
   - Test session recovery

### UI Tests

1. **Encryption Status Display:**
   - Test encryption indicators appear correctly
   - Test verification flow
   - Test error message display

2. **User Flows:**
   - Test first-time E2EE setup
   - Test sending encrypted message
   - Test receiving encrypted message
   - Test key backup creation

### Security Tests

1. **Key Security:**
   - Verify private keys never leave device
   - Verify keys are stored in Android Keystore
   - Test key deletion on app uninstall
   - Verify no keys in logs or crash reports

2. **Protocol Security:**
   - Test forward secrecy (old messages can't be decrypted with new keys)
   - Test post-compromise security (new messages secure after key compromise)
   - Verify proper IV generation (no reuse)
   - Test signature verification

### Performance Tests

1. **Encryption Performance:**
   - Measure encryption time for various message sizes
   - Test media encryption performance
   - Measure key exchange latency
   - Test battery impact

2. **Storage Performance:**
   - Test key retrieval speed
   - Measure session cache effectiveness
   - Test database query performance

## Implementation Phases

### Phase 1: Core Cryptography (Foundation)
- Implement SignalProtocolManager
- Implement KeyPairGenerator
- Implement SecureKeyStore with Android Keystore
- Set up basic encryption/decryption services
- Create data models

### Phase 2: Key Management
- Implement key exchange service
- Create Supabase schema for key bundles
- Implement E2EEKeyRepository
- Add prekey generation and rotation
- Implement session management

### Phase 3: Message Encryption
- Implement EncryptionService
- Implement DecryptionService
- Create encrypted message schema
- Integrate with existing message repository
- Add encryption to message sending flow

### Phase 4: Media Encryption
- Implement media file encryption
- Create encrypted media schema
- Integrate with Supabase Storage
- Add progress indicators for large files
- Implement thumbnail encryption

### Phase 5: UI Integration
- Create EncryptionStatusView
- Add encryption indicators to chat UI
- Implement verification dialog
- Add E2EE settings screen
- Create onboarding flow

### Phase 6: Key Backup & Recovery
- Implement KeyBackupService
- Create backup UI
- Implement passphrase-based encryption
- Add backup restoration flow
- Test recovery scenarios

### Phase 7: Group Encryption
- Implement group key management
- Add member key distribution
- Handle member add/remove
- Test group message encryption

### Phase 8: Polish & Optimization
- Optimize performance
- Add comprehensive error handling
- Implement analytics (privacy-preserving)
- Security audit
- Documentation

## Security Considerations

1. **Key Storage:**
   - All private keys stored in Android Keystore (hardware-backed when available)
   - Keys require user authentication on API 28+
   - Keys deleted on app uninstall
   - No keys in SharedPreferences or files

2. **Forward Secrecy:**
   - Double Ratchet ensures forward secrecy
   - Session keys rotated with each message
   - Old keys deleted after use

3. **Post-Compromise Security:**
   - New DH ratchet step provides healing
   - Compromised keys don't affect future messages

4. **Metadata Protection:**
   - Message content encrypted
   - Sender/recipient IDs visible (required for routing)
   - Timestamps visible (required for ordering)
   - Consider future metadata protection enhancements

5. **Authentication:**
   - Identity keys signed by long-term keys
   - Verification codes for user authentication
   - Protection against MITM attacks

6. **Randomness:**
   - Use SecureRandom for all cryptographic operations
   - Proper IV generation for each encryption
   - No IV reuse

## Dependencies

### New Dependencies Required

```gradle
// Signal Protocol
implementation 'org.signal:libsignal-android:0.40.1'

// Additional crypto (if needed)
implementation 'org.bouncycastle:bcprov-jdk15on:1.70'

// Secure storage
implementation 'androidx.security:security-crypto:1.1.0-alpha06'
```

### Android Permissions

```xml
<!-- No additional permissions required -->
<!-- E2EE operates entirely on-device -->
```

## Migration Strategy

### Existing Users

1. **Opt-in Approach:**
   - E2EE is optional initially
   - Users can enable E2EE per conversation
   - Existing messages remain unencrypted
   - Clear UI distinction between encrypted/unencrypted

2. **Key Generation:**
   - Generate keys on first E2EE message send
   - Background key bundle upload
   - Prekey generation and rotation

3. **Backward Compatibility:**
   - Support both encrypted and unencrypted messages
   - Clear indicators for message type
   - Graceful fallback if recipient doesn't support E2EE

### Future Enhancements

1. **Multi-device Support:**
   - Sync keys across user's devices
   - Device-specific identity keys
   - Session management per device

2. **Disappearing Messages:**
   - Time-based message deletion
   - Screenshot detection
   - Forward protection

3. **Voice/Video Encryption:**
   - SRTP for real-time communication
   - Key exchange via Signal Protocol
   - WebRTC integration

4. **Advanced Verification:**
   - QR code scanning for verification
   - Safety numbers
   - Key change notifications
