package com.synapse.social.studioasinc.util

import android.util.Log
import kotlinx.coroutines.delay

class RetryPolicy {
    suspend fun <T> executeWithRetry(
        times: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 10000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
                Log.e("RetryPolicy", "Attempt failed", e)
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block() // last attempt
    }
}
