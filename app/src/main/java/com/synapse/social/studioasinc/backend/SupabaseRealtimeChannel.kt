package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.backend.interfaces.IRealtimeChannel
import io.github.jan.supabase.realtime.RealtimeChannel

class SupabaseRealtimeChannel(val channel: RealtimeChannel) : IRealtimeChannel {

    override fun unsubscribe() {
        channel.unsubscribe()
    }
}
