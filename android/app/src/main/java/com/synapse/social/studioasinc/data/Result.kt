package com.synapse.social.studioasinc.data

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception, val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (Exception) -> Unit): Result<T> {
    if (this is Result.Error) action(exception)
    return this
}

inline fun <T, R> Result<T>.fold(
    onSuccess: (T) -> R,
    onError: (Exception) -> R
): R = when (this) {
    is Result.Success -> onSuccess(data)
    is Result.Error -> onError(exception)
    is Result.Loading -> throw IllegalStateException("Cannot fold Loading state")
}

val <T> Result<T>.isSuccess: Boolean get() = this is Result.Success
val <T> Result<T>.isError: Boolean get() = this is Result.Error
val <T> Result<T>.isLoading: Boolean get() = this is Result.Loading

fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    else -> null
}

fun <T> Result<T>.getOrDefault(default: T): T = when (this) {
    is Result.Success -> data
    else -> default
}

fun <T> Result<T>.exceptionOrNull(): Exception? = when (this) {
    is Result.Error -> exception
    else -> null
}
