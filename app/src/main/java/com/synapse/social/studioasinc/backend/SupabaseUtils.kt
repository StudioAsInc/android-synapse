package com.synapse.social.studioasinc.backend

import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.gotrue.GoTrue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
    val result = postgrest.from("users").select {
        count(Count.EXACT)
        filter {
            eq("username", username)
        }
    }
    result.countOrNull()?.let { it > 0 } ?: false
}

fun createUserProfile(
    postgrest: Postgrest,
    userData: Map<String, Any>,
    executor: Executor
): CompletableFuture<Unit> = suspendToFuture(executor) {
    postgrest.from("users").insert(
        buildJsonObject {
            userData.forEach { (key, value) ->
                put(key, Json.parseToJsonElement(value.toString()))
            }
        }
    )
    Unit
}

fun createUsernameMapping(
    postgrest: Postgrest,
    usernameData: Map<String, Any>,
    executor: Executor
): CompletableFuture<Unit> = suspendToFuture(executor) {
    postgrest.from("usernames").insert(
        buildJsonObject {
            usernameData.forEach { (key, value) ->
                put(key, Json.parseToJsonElement(value.toString()))
            }
        }
    )
    Unit
}

fun signOut(
    auth: GoTrue,
    executor: Executor
): CompletableFuture<Unit> = suspendToFuture(executor) {
    auth.signOut()
    Unit
}
