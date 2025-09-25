package com.synapse.social.studioasinc.backend.interfaces

interface IDataSnapshot {
    fun <T> getValue(valueType: Class<T>): T?
}

interface IDatabaseError {
    fun getMessage(): String
}

interface IDataListener {
    fun onDataChange(dataSnapshot: IDataSnapshot)
    fun onCancelled(databaseError: IDatabaseError)
}

interface IDatabaseService {
    fun getData(path: String, listener: IDataListener)
}