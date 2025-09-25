package com.synapse.social.studioasinc.backend

import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.synapse.social.studioasinc.backend.interfaces.IDataListener
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService

class DatabaseService : IDatabaseService {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    override fun getReference(path: String): DatabaseReference {
        return database.getReference(path)
    }

    override fun getData(path: String, listener: IDataListener) {
        getReference(path).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dataSnapshot = object : IDataSnapshot {
                    override fun <T> getValue(valueType: Class<T>): T? = snapshot.getValue(valueType)
                }
                listener.onDataChange(dataSnapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                val databaseError = object : IDatabaseError {
                    override fun getMessage(): String = error.message
                }
                listener.onCancelled(databaseError)
            }
        })
    }

    override fun getData(query: Query, listener: IDataListener) {
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dataSnapshot = object : IDataSnapshot {
                    override fun <T> getValue(valueType: Class<T>): T? = snapshot.getValue(valueType)
                }
                listener.onDataChange(dataSnapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                val databaseError = object : IDatabaseError {
                    override fun getMessage(): String = error.message
                }
                listener.onCancelled(databaseError)
            }
        })
    }

    override fun setValue(ref: DatabaseReference, value: Any?): Task<Void> {
        return ref.setValue(value)
    }

    override fun updateChildren(ref: DatabaseReference, updates: Map<String, Any?>): Task<Void> {
        return ref.updateChildren(updates)
    }
}