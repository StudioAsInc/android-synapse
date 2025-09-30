package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.backend.IRealtimeChannel
import io.github.jan.supabase.realtime.RealtimeChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SupabaseRealtimeChannel(val channel: RealtimeChannel) : IRealtimeChannel {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun unsubscribe() {
        scope.launch {
            channel.unsubscribe()
        }
    }
}
