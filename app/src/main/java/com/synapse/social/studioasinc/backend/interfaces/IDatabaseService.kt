package com.synapse.social.studioasinc.backend.interfaces

// --- Generic Database Abstractions ---

/** A generic representation of a database query. */
interface IQuery {
    fun orderByChild(path: String): IQuery
    fun equalTo(value: String?): IQuery
    fun limitToLast(limit: Int): IQuery
    fun limitToFirst(limit: Int): IQuery
    fun startAt(value: String): IQuery
    fun endAt(value: String): IQuery
}

/** A generic representation of a database reference, which is also a basic query. */
interface IDatabaseReference : IQuery

// --- Data Snapshot and Error Handling ---

interface IDataSnapshot {
    /**
     * Attempts to convert the data in this snapshot to a specific type.
     */
    fun <T> getValue(valueType: Class<T>): T?

    /**
     * Returns true if the snapshot contains any data.
     */
    fun exists(): Boolean

    /**
     * Gets the immediate children of this snapshot.
     */
    val children: Iterable<IDataSnapshot>

    /**
     * Gets the key (last part of the path) of this snapshot.
     */
    val key: String?
}

interface IDatabaseError {
    val message: String
    val code: Int
}

interface IDataListener {
    fun onDataChange(dataSnapshot: IDataSnapshot)
    fun onCancelled(databaseError: IDatabaseError)
}

/**
 * Defines the contract for database operations, fully abstracted from the underlying provider.
 */
interface IDatabaseService {
    /**
     * Gets a reference to a specific location in the database.
     */
    fun getReference(path: String): IDatabaseReference

    /**
     * Fetches data once from the database based on a query.
     */
    fun getData(query: IQuery, listener: IDataListener)

    /**
     * Writes data to a database reference.
     */
    fun setValue(ref: IDatabaseReference, value: Any?, listener: ICompletionListener<Unit>)

    /**
     * Updates specific children of a database reference without overwriting other data.
     */
    fun updateChildren(ref: IDatabaseReference, updates: Map<String, Any?>, listener: ICompletionListener<Unit>)
}