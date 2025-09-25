package com.synapse.social.studioasinc.backend

import com.google.android.gms.tasks.Task
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

class DatabaseService {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val valueEventListeners = mutableMapOf<DataListener, ValueEventListener>()
    private val childEventListeners = mutableMapOf<ChildListener, ChildEventListener>()

    interface DataListener {
        fun onDataChange(dataSnapshot: DataSnapshot)
        fun onCancelled(databaseError: DatabaseError)
    }

    interface ChildListener {
        fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?)
        fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?)
        fun onChildRemoved(dataSnapshot: DataSnapshot)
        fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?)
        fun onCancelled(databaseError: DatabaseError)
    }

    fun getReference(path: String): DatabaseReference {
        return database.getReference(path)
    }

    fun setValue(path: String, value: Any): Task<Void> {
        return getReference(path).setValue(value)
    }

    fun updateChildren(path: String, updates: Map<String, Any?>): Task<Void> {
        return getReference(path).updateChildren(updates)
    }

    fun getData(path: String, listener: DataListener) {
        getReference(path).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listener.onDataChange(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                listener.onCancelled(error)
            }
        })
    }

    fun addValueEventListener(path: String, listener: DataListener) {
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listener.onDataChange(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                listener.onCancelled(error)
            }
        }
        valueEventListeners[listener] = valueEventListener
        getReference(path).addValueEventListener(valueEventListener)
    }

    fun removeValueEventListener(path: String, listener: DataListener) {
        valueEventListeners[listener]?.let {
            getReference(path).removeEventListener(it)
            valueEventListeners.remove(listener)
        }
    }

    fun addChildEventListener(path: String, listener: ChildListener) {
        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                listener.onChildAdded(snapshot, previousChildName)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                listener.onChildChanged(snapshot, previousChildName)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                listener.onChildRemoved(snapshot)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                listener.onChildMoved(snapshot, previousChildName)
            }

            override fun onCancelled(error: DatabaseError) {
                listener.onCancelled(error)
            }
        }
        childEventListeners[listener] = childEventListener
        getReference(path).addChildEventListener(childEventListener)
    }

    fun removeChildEventListener(path: String, listener: ChildListener) {
        childEventListeners[listener]?.let {
            getReference(path).removeEventListener(it)
            childEventListeners.remove(listener)
        }
    }

    fun getData(query: Query, listener: DataListener) {
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listener.onDataChange(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                listener.onCancelled(error)
            }
        })
    }

    fun addValueEventListener(query: Query, listener: DataListener) {
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listener.onDataChange(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {
                listener.onCancelled(error)
            }
        }
        valueEventListeners[listener] = valueEventListener
        query.addValueEventListener(valueEventListener)
    }

    fun removeValueEventListener(query: Query, listener: DataListener) {
        valueEventListeners[listener]?.let {
            query.removeEventListener(it)
            valueEventListeners.remove(listener)
        }
    }

    fun addChildEventListener(query: Query, listener: ChildListener) {
        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                listener.onChildAdded(snapshot, previousChildName)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                listener.onChildChanged(snapshot, previousChildName)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                listener.onChildRemoved(snapshot)
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                listener.onChildMoved(snapshot, previousChildName)
            }
            override fun onCancelled(error: DatabaseError) {
                listener.onCancelled(error)
            }
        }
        childEventListeners[listener] = childEventListener
        query.addChildEventListener(childEventListener)
    }

    fun removeChildEventListener(query: Query, listener: ChildListener) {
        childEventListeners[listener]?.let {
            query.removeEventListener(it)
            childEventListeners.remove(listener)
        }
    }

    fun getServerTimestamp(): Any {
        return ServerValue.TIMESTAMP
    }
}