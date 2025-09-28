package com.synapse.social.studioasinc.backend.interfaces

interface IRealtimeListener {
    fun onInsert(snapshot: IDataSnapshot)
    fun onUpdate(snapshot: IDataSnapshot)
    fun onDelete(snapshot: IDataSnapshot)
}

interface IRealtimeChannel {
    fun unsubscribe()
}
