package com.synapse.social.studioasinc.backend.interfaces

import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query

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

/**
 * This interface defines the contract for database operations.
 * NOTE: This interface currently leaks Firebase-specific types like `DatabaseReference`, `Query`, and `Task`.
 * This is a temporary measure to fix build errors and will be refactored in a future task
 * to create a pure abstraction layer.
 */
interface IDatabaseService {
    fun getData(path: String, listener: IDataListener)
    fun getReference(path: String): DatabaseReference
    fun getData(query: Query, listener: IDataListener)
    fun setValue(ref: DatabaseReference, value: Any?): Task<Void>
    fun updateChildren(ref: DatabaseReference, updates: Map<String, Any?>): Task<Void>
}