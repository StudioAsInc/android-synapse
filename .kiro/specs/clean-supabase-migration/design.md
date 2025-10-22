# Clean Supabase Migration Design

## Overview

This design outlines a complete, clean migration from Firebase to Supabase, removing all Firebase dependencies and compatibility layers. The new architecture will use native Supabase patterns with modern Kotlin coroutines, proper repository pattern, and clean architecture principles.

## Architecture

### Current State Analysis

The current codebase has:
- Firebase compatibility layer masking Supabase implementation
- Mixed callback and coroutine patterns
- Direct Firebase API usage throughout the application
- Inconsistent error handling
- Tightly coupled data access logic

### Target Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  Activities, Fragments, ViewModels, Compose UI             │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                   Domain Layer                              │
│  Use Cases, Repository Interfaces, Domain Models           │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                    Data Layer                               │
│  Repository Implementations, Supabase Services, DTOs       │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                 Supabase Client                             │
│  Auth, PostgREST, Realtime, Storage                        │
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. Data Layer Redesign

#### Repository Pattern Implementation

```kotlin
// Domain Repository Interface
interface UserRepository {
    suspend fun getUserById(id: String): Result<User>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun getCurrentUser(): Result<User?>
    fun observeUserChanges(userId: String): Flow<User>
}

// Supabase Implementation
class SupabaseUserRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val userMapper: UserMapper
) : UserRepository {
    
    override suspend fun getUserById(id: String): Result<User> = runCatching {
        val userDto = supabaseClient.from("users")
            .select()
            .eq("id", id)
            .decodeSingle<UserDto>()
        userMapper.toDomain(userDto)
    }
    
    override fun observeUserChanges(userId: String): Flow<User> {
        return supabaseClient.realtime
            .channel("user_changes")
            .postgresChangeFlow<UserDto>(schema = "public") {
                table = "users"
                filter = "id=eq.$userId"
            }
            .map { userMapper.toDomain(it) }
    }
}
```

#### Chat Repository Implementation

```kotlin
interface ChatRepository {
    suspend fun sendMessage(message: Message): Result<Unit>
    suspend fun getMessages(chatId: String, limit: Int = 50): Result<List<Message>>
    fun observeMessages(chatId: String): Flow<List<Message>>
    suspend fun markAsRead(chatId: String, userId: String): Result<Unit>
}

class SupabaseChatRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val messageMapper: MessageMapper
) : ChatRepository {
    
    override suspend fun sendMessage(message: Message): Result<Unit> = runCatching {
        val messageDto = messageMapper.toDto(message)
        supabaseClient.from("messages").insert(messageDto)
    }
    
    override fun observeMessages(chatId: String): Flow<List<Message>> {
        return supabaseClient.realtime
            .channel("chat_$chatId")
            .postgresChangeFlow<MessageDto>(schema = "public") {
                table = "messages"
                filter = "chat_id=eq.$chatId"
            }
            .map { messages -> messages.map(messageMapper::toDomain) }
    }
}
```

### 2. Authentication Layer Redesign

#### Native Supabase Auth Service

```kotlin
interface AuthService {
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUser(): Result<User?>
    fun observeAuthState(): Flow<AuthState>
}

class SupabaseAuthService @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val userMapper: UserMapper
) : AuthService {
    
    override suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        
        val userInfo = supabaseClient.auth.currentUserOrNull()
            ?: throw AuthException("Sign in failed")
            
        userMapper.fromSupabaseUser(userInfo)
    }
    
    override fun observeAuthState(): Flow<AuthState> {
        return supabaseClient.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> AuthState.Authenticated(
                    userMapper.fromSupabaseUser(status.session.user)
                )
                is SessionStatus.NotAuthenticated -> AuthState.NotAuthenticated
                is SessionStatus.LoadingFromStorage -> AuthState.Loading
                is SessionStatus.NetworkError -> AuthState.Error(status.throwable)
            }
        }
    }
}
```

### 3. Realtime Layer Redesign

#### Native Supabase Realtime Manager

```kotlin
interface RealtimeManager {
    fun subscribeToChat(chatId: String): Flow<RealtimeEvent<Message>>
    fun subscribeToUserPresence(userId: String): Flow<PresenceState>
    suspend fun updatePresence(status: PresenceStatus): Result<Unit>
    suspend fun joinChannel(channelName: String): Result<Unit>
    suspend fun leaveChannel(channelName: String): Result<Unit>
}

class SupabaseRealtimeManager @Inject constructor(
    private val supabaseClient: SupabaseClient
) : RealtimeManager {
    
    private val activeChannels = mutableMapOf<String, RealtimeChannel>()
    
    override fun subscribeToChat(chatId: String): Flow<RealtimeEvent<Message>> {
        val channelName = "chat_$chatId"
        
        return flow {
            val channel = supabaseClient.realtime.channel(channelName)
            activeChannels[channelName] = channel
            
            channel.postgresChangeFlow<MessageDto>(schema = "public") {
                table = "messages"
                filter = "chat_id=eq.$chatId"
            }.collect { messageDto ->
                emit(RealtimeEvent.MessageReceived(messageMapper.toDomain(messageDto)))
            }
        }
    }
    
    override suspend fun updatePresence(status: PresenceStatus): Result<Unit> = runCatching {
        supabaseClient.realtime.channel("presence").track(
            mapOf(
                "status" to status.name,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
}
```

### 4. Use Cases Layer

#### Clean Use Cases Implementation

```kotlin
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val realtimeManager: RealtimeManager
) {
    suspend operator fun invoke(
        chatId: String,
        content: String,
        messageType: MessageType = MessageType.TEXT
    ): Result<Unit> = runCatching {
        
        val currentUser = userRepository.getCurrentUser().getOrThrow()
            ?: throw AuthException("User not authenticated")
        
        val message = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = currentUser.id,
            content = content,
            type = messageType,
            timestamp = System.currentTimeMillis()
        )
        
        chatRepository.sendMessage(message).getOrThrow()
    }
}

class ObserveChatMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(chatId: String): Flow<List<Message>> {
        return chatRepository.observeMessages(chatId)
    }
}
```

### 5. ViewModel Layer Redesign

#### Modern ViewModel with Coroutines

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val observeChatMessagesUseCase: ObserveChatMessagesUseCase,
    private val realtimeManager: RealtimeManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    fun loadChat(chatId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                observeChatMessagesUseCase(chatId).collect { messages ->
                    _uiState.update { 
                        it.copy(
                            messages = messages,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }
    
    fun sendMessage(content: String) {
        val currentChatId = _uiState.value.chatId ?: return
        
        viewModelScope.launch {
            sendMessageUseCase(currentChatId, content)
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }
}

data class ChatUiState(
    val chatId: String? = null,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

## Data Models

### Domain Models

```kotlin
@Serializable
data class User(
    val id: String,
    val email: String,
    val username: String,
    val nickname: String? = null,
    val avatar: String? = null,
    val isVerified: Boolean = false,
    val accountType: AccountType = AccountType.USER,
    val status: UserStatus = UserStatus.OFFLINE,
    val lastSeen: Long? = null
)

@Serializable
data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long,
    val editedAt: Long? = null,
    val replyToId: String? = null
)

@Serializable
data class Chat(
    val id: String,
    val participants: List<String>,
    val type: ChatType,
    val name: String? = null,
    val avatar: String? = null,
    val lastMessage: Message? = null,
    val createdAt: Long,
    val updatedAt: Long
)
```

### DTO Models for Supabase

```kotlin
@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val username: String,
    val nickname: String? = null,
    val avatar: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("account_type") val accountType: String = "user",
    val status: String = "offline",
    @SerialName("last_seen") val lastSeen: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
    @SerialName("message_type") val messageType: String = "text",
    @SerialName("created_at") val createdAt: String,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null
)
```

## Error Handling

### Domain Error Types

```kotlin
sealed class AppError : Exception() {
    data class NetworkError(override val message: String) : AppError()
    data class AuthError(override val message: String) : AppError()
    data class DatabaseError(override val message: String) : AppError()
    data class ValidationError(override val message: String) : AppError()
    data class UnknownError(override val message: String) : AppError()
}

// Extension to convert Supabase exceptions
fun Throwable.toAppError(): AppError = when (this) {
    is RestException -> AppError.NetworkError(message ?: "Network error")
    is GoTrueException -> AppError.AuthError(message ?: "Authentication error")
    is PostgrestException -> AppError.DatabaseError(message ?: "Database error")
    else -> AppError.UnknownError(message ?: "Unknown error")
}
```

## Testing Strategy

### Repository Testing

```kotlin
@Test
class SupabaseUserRepositoryTest {
    
    @Mock private lateinit var supabaseClient: SupabaseClient
    @Mock private lateinit var userMapper: UserMapper
    
    private lateinit var repository: SupabaseUserRepository
    
    @Test
    fun `getUserById returns user when found`() = runTest {
        // Given
        val userId = "user123"
        val userDto = UserDto(id = userId, email = "test@example.com", username = "test")
        val user = User(id = userId, email = "test@example.com", username = "test")
        
        whenever(supabaseClient.from("users").select().eq("id", userId).decodeSingle<UserDto>())
            .thenReturn(userDto)
        whenever(userMapper.toDomain(userDto)).thenReturn(user)
        
        // When
        val result = repository.getUserById(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
    }
}
```

### Use Case Testing

```kotlin
@Test
class SendMessageUseCaseTest {
    
    @Mock private lateinit var chatRepository: ChatRepository
    @Mock private lateinit var userRepository: UserRepository
    
    private lateinit var useCase: SendMessageUseCase
    
    @Test
    fun `invoke sends message successfully`() = runTest {
        // Given
        val user = User(id = "user123", email = "test@example.com", username = "test")
        whenever(userRepository.getCurrentUser()).thenReturn(Result.success(user))
        whenever(chatRepository.sendMessage(any())).thenReturn(Result.success(Unit))
        
        // When
        val result = useCase("chat123", "Hello world")
        
        // Then
        assertTrue(result.isSuccess)
        verify(chatRepository).sendMessage(any())
    }
}
```

## Migration Strategy

### Phase 1: Infrastructure Setup
1. Remove Firebase compatibility layer completely
2. Set up proper dependency injection with Hilt
3. Create repository interfaces and domain models
4. Implement Supabase client configuration

### Phase 2: Data Layer Migration
1. Implement repository pattern with Supabase
2. Create proper DTO models with serialization
3. Implement error handling and mapping
4. Add comprehensive testing

### Phase 3: Business Logic Migration
1. Create use cases for all business operations
2. Implement proper coroutine-based async handling
3. Add validation and business rules
4. Test use cases thoroughly

### Phase 4: Presentation Layer Migration
1. Update ViewModels to use use cases
2. Implement proper state management with StateFlow
3. Update UI components to observe state
4. Add proper error handling in UI

### Phase 5: Realtime Features Migration
1. Implement native Supabase realtime subscriptions
2. Replace all Firebase listeners with Supabase channels
3. Implement presence management
4. Add connection lifecycle management

## Performance Considerations

### Caching Strategy
- Implement proper caching in repository layer
- Use Room database for offline support
- Cache user sessions and frequently accessed data

### Query Optimization
- Use PostgREST select with specific columns
- Implement pagination for large datasets
- Use proper indexing in Supabase database

### Realtime Optimization
- Manage channel subscriptions lifecycle
- Implement proper reconnection logic
- Use channel filters to reduce unnecessary updates

## Security Considerations

### Row Level Security
- Implement proper RLS policies in Supabase
- Ensure users can only access their own data
- Validate permissions at database level

### Authentication Security
- Use secure session management
- Implement proper token refresh
- Add rate limiting for auth operations

### Data Validation
- Validate all inputs at multiple layers
- Sanitize user-generated content
- Implement proper error messages without exposing internals