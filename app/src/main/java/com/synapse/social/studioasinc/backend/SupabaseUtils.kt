package com.synapse.social.studioasinc.backend

import io.github.jan.supabase.postgrest.Postgrest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

fun <T> suspendToFuture(
    executor: Executor,
    block: suspend () -> T
): CompletableFuture<T> {
    val future = CompletableFuture<T>()
    val dispatcher = executor.asCoroutineDispatcher()
    CoroutineScope(dispatcher + SupervisorJob()).launch {
        try {
            future.complete(block())
        } catch (e: Exception) {
            future.completeExceptionally(e)
        }
    }
    return future
}

fun isUsernameTaken(
    postgrest: Postgrest,
    username: String,
    executor: Executor
): CompletableFuture<Boolean> = suspendToFuture(executor) {
    val result = postgrest.from("users").select().eq("username", username).execute()
    !result.data.isEmpty()
}

fun createUserProfile(
    postgrest: Postgrest,
    userData: Map<String, Any>,
    executor: Executor
): CompletableFuture<Unit> = suspendToFuture(executor) {
    postgrest.from("users").insert(userData, upsert = true).execute()
    Unit
}

import io.github.jan.supabase.gotrue.GoTrue

fun createUsernameMapping(
    postgrest: Postgrest,
    usernameData: Map<String, Any>,
    executor: Executor
): CompletableFuture<Unit> = suspendToFuture(executor) {
    postgrest.from("usernames").insert(usernameData, upsert = true).execute()
    Unit
}

fun signOut(
    auth: GoTrue,
    executor: Executor
): CompletableFuture<Unit> = suspendToFuture(executor) {
    auth.signOut()
    Unit
}