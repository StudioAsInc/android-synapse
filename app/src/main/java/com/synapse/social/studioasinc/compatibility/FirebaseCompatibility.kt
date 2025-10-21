package com.synapse.social.studioasinc.compatibility

/**
 * Compatibility layer for Firebase types during migration.
 * These are stub implementations to prevent compilation errors.
 * TODO: Remove after complete migration to Supabase.
 */

// Stub Firebase classes to prevent compilation errors
class FirebaseAuth {
    companion object {
        fun getInstance(): FirebaseAuth = FirebaseAuth()
    }
    
    val currentUser: FirebaseUser? = null
    
    fun signOut() {}
}

class FirebaseUser {
    val uid: String = ""
    val email: String? = null
}

class FirebaseDatabase {
    companion object {
        fun getInstance(): FirebaseDatabase = FirebaseDatabase()
    }
    
    fun getReference(path: String): DatabaseReference = DatabaseReference()
}

class DatabaseReference {
    fun child(path: String): DatabaseReference = DatabaseReference()
    fun orderByChild(key: String): Query = Query()
    fun limitToLast(limit: Int): Query = Query()
    fun addListenerForSingleValueEvent(listener: ValueEventListener) {}
    fun addChildEventListener(listener: ChildEventListener): ChildEventListener = listener
    fun removeEventListener(listener: ValueEventListener) {}
    fun removeEventListener(listener: ChildEventListener) {}
    fun setValue(value: Any?) {}
    fun removeValue() {}
    fun updateChildren(updates: Map<String, Any?>) {}
    fun push(): DatabaseReference = DatabaseReference()
    val key: String? = null
}

class Query {
    fun addListenerForSingleValueEvent(listener: ValueEventListener) {}
    fun addValueEventListener(listener: ValueEventListener) {}
    fun limitToLast(limit: Int): Query = Query()
    fun orderByKey(): Query = Query()
    fun orderByChild(key: String): Query = Query()
    fun endBefore(value: String): Query = Query()
}

interface ValueEventListener {
    fun onDataChange(snapshot: DataSnapshot)
    fun onCancelled(error: DatabaseError)
}

interface ChildEventListener {
    fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?)
    fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?)
    fun onChildRemoved(snapshot: DataSnapshot)
    fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?)
    fun onCancelled(error: DatabaseError)
}

class DataSnapshot {
    fun exists(): Boolean = false
    val children: Iterable<DataSnapshot> = emptyList()
    val childrenCount: Long = 0
    val key: String? = null
    fun child(path: String): DataSnapshot = DataSnapshot()
    fun <T> getValue(type: Class<T>): T? = null
    fun <T> getValue(indicator: GenericTypeIndicator<T>): T? = null
}

class DatabaseError {
    val message: String = "Database error"
    fun toException(): Exception = Exception(message)
}

class GenericTypeIndicator<T>

class FirebaseApp {
    companion object {
        fun initializeApp(context: android.content.Context) {}
    }
}

// Additional Firebase compatibility classes
open class Task<T> {
    fun addOnCompleteListener(listener: (Task<T>) -> Unit): Task<T> = this
    fun addOnSuccessListener(listener: (T) -> Unit): Task<T> = this
    fun addOnFailureListener(listener: (Exception) -> Unit): Task<T> = this
    val isSuccessful: Boolean = true
    val result: T? = null
    val exception: Exception? = null
}

class AuthResult {
    val user: FirebaseUser? = null
}

// Stub implementations for missing Firebase classes
object FirebaseStorage {
    fun getInstance(): FirebaseStorage = this
    fun getReference(): StorageReference = StorageReference()
}

class StorageReference {
    fun child(path: String): StorageReference = StorageReference()
    fun putFile(uri: android.net.Uri): UploadTask = UploadTask()
    fun downloadUrl(): Task<android.net.Uri> = Task()
}

class UploadTask : Task<UploadTask.TaskSnapshot>() {
    class TaskSnapshot
}