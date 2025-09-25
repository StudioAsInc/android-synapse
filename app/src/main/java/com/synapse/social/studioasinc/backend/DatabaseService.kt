package com.synapse.social.studioasinc.backend

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.synapse.social.studioasinc.backend.interfaces.IDataListener
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService

class DatabaseService : IDatabaseService {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    override fun getData(path: String, listener: IDataListener) {
        database.getReference(path).addListenerForSingleValueEvent(object : ValueEventListener {
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
}