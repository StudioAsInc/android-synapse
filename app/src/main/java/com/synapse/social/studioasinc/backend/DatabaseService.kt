package com.synapse.social.studioasinc.backend

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.synapse.social.studioasinc.backend.interfaces.*

// --- Wrapper Implementations for Firebase ---

/**
 * Wraps a Firebase `Query` to conform to the generic `IQuery` interface.
 */
private class FirebaseQueryWrapper(val query: Query) : IQuery {
    override fun orderByChild(path: String): IQuery = FirebaseQueryWrapper(query.orderByChild(path))
    override fun equalTo(value: String?): IQuery = FirebaseQueryWrapper(query.equalTo(value))
    override fun limitToLast(limit: Int): IQuery = FirebaseQueryWrapper(query.limitToLast(limit))
    override fun limitToFirst(limit: Int): IQuery = FirebaseQueryWrapper(query.limitToFirst(limit))
    override fun startAt(value: String): IQuery = FirebaseQueryWrapper(query.startAt(value))
    override fun endAt(value: String): IQuery = FirebaseQueryWrapper(query.endAt(value))
}

/**
 * Wraps a Firebase `DatabaseReference` to conform to the generic `IDatabaseReference` interface.
 */
private class FirebaseDbReferenceWrapper(val dbRef: DatabaseReference) : IDatabaseReference {
    private val queryWrapper = FirebaseQueryWrapper(dbRef)
    override fun orderByChild(path: String): IQuery = queryWrapper.orderByChild(path)
    override fun equalTo(value: String?): IQuery = queryWrapper.equalTo(value)
    override fun limitToLast(limit: Int): IQuery = queryWrapper.limitToLast(limit)
    override fun limitToFirst(limit: Int): IQuery = queryWrapper.limitToFirst(limit)
    override fun startAt(value: String): IQuery = queryWrapper.startAt(value)
    override fun endAt(value: String): IQuery = queryWrapper.endAt(value)
}

/**
 * Wraps a Firebase `DataSnapshot` to conform to the generic `IDataSnapshot` interface.
 */
private class FirebaseDataSnapshot(private val snapshot: DataSnapshot) : IDataSnapshot {
    override fun <T> getValue(valueType: Class<T>): T? = snapshot.getValue(valueType)
    override fun exists(): Boolean = snapshot.exists()
    override val children: Iterable<IDataSnapshot>
        get() = snapshot.children.map { FirebaseDataSnapshot(it) }
    override val key: String?
        get() = snapshot.key
}

/**
 * Wraps a Firebase `DatabaseError` to conform to the generic `IDatabaseError` interface.
 */
private class FirebaseDbError(private val error: DatabaseError) : IDatabaseError {
    override val message: String
        get() = error.message
    override val code: Int
        get() = error.code
}

// --- Service Implementation ---

class DatabaseService : IDatabaseService {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    override fun getReference(path: String): IDatabaseReference {
        return FirebaseDbReferenceWrapper(database.getReference(path))
    }

    override fun getData(query: IQuery, listener: IDataListener) {
        // Unwrap the generic IQuery to get the underlying Firebase Query
        val firebaseQuery = (query as? FirebaseQueryWrapper)?.query
            ?: (query as? FirebaseDbReferenceWrapper)?.dbRef
            ?: throw IllegalArgumentException("Unsupported IQuery type provided")

        firebaseQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listener.onDataChange(FirebaseDataSnapshot(snapshot))
            }

            override fun onCancelled(error: DatabaseError) {
                listener.onCancelled(FirebaseDbError(error))
            }
        })
    }

    override fun setValue(ref: IDatabaseReference, value: Any?, listener: ICompletionListener<Unit>) {
        val firebaseRef = (ref as? FirebaseDbReferenceWrapper)?.dbRef
            ?: throw IllegalArgumentException("Unsupported IDatabaseReference type provided")

        firebaseRef.setValue(value).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                listener.onComplete(Unit, null)
            } else {
                listener.onComplete(null, task.exception)
            }
        }
    }

    override fun updateChildren(ref: IDatabaseReference, updates: Map<String, Any?>, listener: ICompletionListener<Unit>) {
        val firebaseRef = (ref as? FirebaseDbReferenceWrapper)?.dbRef
            ?: throw IllegalArgumentException("Unsupported IDatabaseReference type provided")

        firebaseRef.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                listener.onComplete(Unit, null)
            } else {
                listener.onComplete(null, task.exception)
            }
        }
    }
}