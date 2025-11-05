# Error Handling Guide for Message Actions

## Overview

This document describes the comprehensive error handling system implemented for message actions (Reply, Forward, Edit, Delete, AI Summary) in the Synapse chat application.

## Components

### 1. ErrorHandler Utility (`util/ErrorHandler.kt`)

Centralized error handling that maps technical exceptions to user-friendly messages.

**Key Features:**
- Maps technical errors to localized user-friendly messages
- Provides consistent Snackbar display methods
- Detects network errors, rate limit errors, and retryable errors
- Includes full context logging for debugging

**Usage Example:**
```kotlin
val errorMessage = ErrorHandler.getErrorMessage(
    context = context,
    errorType = ErrorHandler.ErrorType.FORWARD,
    exception = exception,
    messageId = messageId,
    userId = userId
)

ErrorHandler.showErrorSnackbar(
    view = binding.root,
    message = errorMessage,
    retryAction = { /* retry logic */ }
)
```

### 2. RetryHandler Utility (`util/RetryHandler.kt`)

Automatic retry logic with exponential backoff for network failures.

**Configuration:**
- Maximum 3 retry attempts (configurable)
- Exponential backoff: 1s, 2s, 4s
- Detects retryable exceptions (IOException, SocketTimeoutException, etc.)
- Supports custom retry configurations

**Usage Example:**
```kotlin
val result = RetryHandler.executeWithRetryResult { attemptNumber ->
    // Your network operation here
    performNetworkRequest()
}

result.fold(
    onSuccess = { /* handle success */ },
    onFailure = { /* handle failure */ }
)
```

### 3. String Resources (`res/values/strings.xml`)

Comprehensive error messages for all scenarios:

**Reply Errors:**
- `error_reply_generic`: "Unable to reply to this message"
- `error_reply_deleted`: "This message has been deleted"

**Forward Errors:**
- `error_forward_generic`: "Failed to forward message. Please try again."
- `error_forward_no_permission`: "You don't have permission to send messages in %1$s"
- `error_forward_partial`: "Forwarded to %1$d of %2$d conversations"
- `error_forward_no_network`: "No network connection. Message will be forwarded when online."

**Edit Errors:**
- `error_edit_generic`: "Unable to edit message. Please try again."
- `error_edit_too_old`: "This message is too old to edit (>48 hours)"
- `error_edit_empty`: "Message content cannot be empty"
- `error_edit_not_found`: "Message not found"

**Delete Errors:**
- `error_delete_generic`: "Failed to delete message. Please try again."
- `error_delete_already_deleted`: "Unable to delete for everyone. Message may have already been deleted."

**AI Summary Errors:**
- `error_ai_summary_generic`: "Unable to generate summary. Please try again."
- `error_ai_summary_rate_limit`: "Rate limit reached. Try again in %1$d minutes"
- `error_ai_summary_too_short`: "Message is too short to summarize"
- `error_ai_summary_network`: "Network error. Please check your connection and try again."

### 4. Enhanced ViewModel (`MessageActionsViewModel.kt`)

All ViewModel methods now include:
- Comprehensive error logging with context (messageId, userId, parameters)
- User-friendly error messages via ErrorHandler
- Automatic retry for network failures via RetryHandler
- Success message localization

**Example:**
```kotlin
fun editMessage(messageId: String, newContent: String): Flow<MessageActionState> = flow {
    try {
        // Validation
        if (newContent.isBlank()) {
            val errorMessage = context.getString(R.string.error_edit_empty)
            Log.w(TAG, "Edit validation failed: empty content - MessageId: $messageId")
            emit(MessageActionState.Error(errorMessage))
            return@flow
        }

        // Network check and queueing
        if (!NetworkUtil.isNetworkAvailable(context)) {
            // Queue for later
            actionQueue.add(createEditAction(messageId, newContent))
            emit(MessageActionState.Success("Edit queued. Will be applied when online."))
            return@flow
        }

        // Execute with retry
        emit(MessageActionState.Loading)
        val result = repository.editMessage(messageId, newContent)

        result.fold(
            onSuccess = {
                val successMessage = context.getString(R.string.success_edit)
                Log.d(TAG, "Message edited successfully - MessageId: $messageId")
                emit(MessageActionState.Success(successMessage))
            },
            onFailure = { error ->
                val errorMessage = ErrorHandler.getErrorMessage(
                    context, ErrorHandler.ErrorType.EDIT, error, messageId
                )
                Log.e(TAG, "Edit failed - MessageId: $messageId", error)
                emit(MessageActionState.Error(errorMessage))
            }
        )
    } catch (e: Exception) {
        val errorMessage = ErrorHandler.getErrorMessage(
            context, ErrorHandler.ErrorType.EDIT, e, messageId
        )
        Log.e(TAG, "Unexpected error - MessageId: $messageId", e)
        emit(MessageActionState.Error(errorMessage))
    }
}
```

### 5. Enhanced Repository (`MessageActionRepository.kt`)

All repository methods now use RetryHandler for automatic retry:

```kotlin
suspend fun editMessage(messageId: String, newContent: String): Result<Unit> = 
    withContext(Dispatchers.IO) {
        RetryHandler.executeWithRetryResult { attemptNumber ->
            Log.d(TAG, "Editing message: $messageId - Attempt: $attemptNumber")
            
            // Validation and database operations
            // ...
            
            Log.d(TAG, "Message edited successfully - MessageId: $messageId")
            Unit
        }
    }
```

### 6. UI Extension Functions (`util/MessageActionExtensions.kt`)

Convenient extension functions for collecting Flow states in Fragments:

```kotlin
// In your Fragment
collectMessageActionState(
    flow = viewModel.editMessage(messageId, newContent),
    view = binding.root,
    onSuccess = { message ->
        // Handle success
        dismiss()
    },
    onError = { error ->
        // Handle error
    },
    retryAction = {
        // Retry the operation
        viewModel.editMessage(messageId, newContent)
    }
)
```

## Error Logging Format

All errors are logged with full context for debugging:

```
Log.e(TAG, "Action failed - MessageId: $messageId, UserId: $userId, Parameters: {...}", exception)
```

**Logged Information:**
- Action type (EDIT, DELETE, FORWARD, etc.)
- Message ID
- User ID (when available)
- Action parameters (content length, target chats, etc.)
- Attempt number (for retries)
- Full exception stack trace

## Network Error Handling

### Detection
Network errors are automatically detected:
- `IOException`
- `SocketTimeoutException`
- `UnknownHostException`
- Messages containing "network", "connection", "timeout"

### Retry Strategy
1. **Automatic Retry**: Up to 3 attempts with exponential backoff (1s, 2s, 4s)
2. **Offline Queueing**: Actions queued when offline, processed when connection restored
3. **User Retry**: Snackbar with retry button for manual retry

### Example Flow
```
Attempt 1: Failed (timeout) → Wait 1s
Attempt 2: Failed (timeout) → Wait 2s
Attempt 3: Failed (timeout) → Show error with retry button
```

## Rate Limiting (AI Summary)

### Detection
- 429 HTTP status code
- "rate limit" in error message

### Handling
1. Store reset time in SharedPreferences
2. Display countdown timer to user
3. Disable AI Summary action until reset
4. No automatic retry for rate limit errors

### User Message
```
"Rate limit reached. Try again in 15 minutes"
```

## Best Practices

### 1. Always Use ErrorHandler
```kotlin
// ✅ Good
val errorMessage = ErrorHandler.getErrorMessage(context, errorType, exception, messageId)
emit(MessageActionState.Error(errorMessage))

// ❌ Bad
emit(MessageActionState.Error(exception.message ?: "Error"))
```

### 2. Log with Full Context
```kotlin
// ✅ Good
Log.e(TAG, "Edit failed - MessageId: $messageId, ContentLength: ${content.length}", exception)

// ❌ Bad
Log.e(TAG, "Edit failed", exception)
```

### 3. Use RetryHandler for Network Operations
```kotlin
// ✅ Good
RetryHandler.executeWithRetryResult { attemptNumber ->
    performNetworkOperation()
}

// ❌ Bad
try {
    performNetworkOperation()
} catch (e: Exception) {
    // Manual retry logic
}
```

### 4. Provide Retry Actions
```kotlin
// ✅ Good
ErrorHandler.showErrorSnackbar(
    view = view,
    message = errorMessage,
    retryAction = { viewModel.retryOperation() }
)

// ❌ Bad
ErrorHandler.showErrorSnackbar(view, errorMessage)
```

## Testing Error Scenarios

### Network Errors
1. Disable network connection
2. Perform action
3. Verify queuing message displayed
4. Re-enable network
5. Verify action processed automatically

### Rate Limiting
1. Trigger rate limit (make many AI summary requests)
2. Verify countdown timer displayed
3. Verify AI Summary action disabled
4. Wait for reset time
5. Verify action re-enabled

### Validation Errors
1. Try to edit with empty content → "Message content cannot be empty"
2. Try to edit message >48 hours old → "This message is too old to edit"
3. Try to forward to invalid chat → "You don't have permission..."

## Monitoring and Debugging

### Log Filtering
Use these tags to filter logs:
- `MessageActionsViewModel`: ViewModel operations
- `MessageActionRepository`: Repository operations
- `GeminiAIService`: AI summary operations
- `ErrorHandler`: Error handling
- `RetryHandler`: Retry operations

### Example Logcat Filter
```
tag:MessageActionsViewModel OR tag:ErrorHandler OR tag:RetryHandler
```

### Key Metrics to Monitor
- Retry success rate
- Average retry attempts before success
- Most common error types
- Rate limit frequency
- Queued action processing time

## Future Enhancements

1. **Analytics Integration**: Track error rates and types
2. **User Feedback**: Allow users to report persistent errors
3. **Adaptive Retry**: Adjust retry strategy based on error patterns
4. **Circuit Breaker**: Temporarily disable features with high failure rates
5. **Error Recovery**: Automatic recovery strategies for common errors
