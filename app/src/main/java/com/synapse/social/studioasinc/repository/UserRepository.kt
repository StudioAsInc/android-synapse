package com.synapse.social.studioasinc.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.synapse.social.studioasinc.model.User
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Repository for fetching and caching user data.
 */
object UserRepository {

    private val userCache = mutableMapOf<String, User>()

    /**
     * Gets a user from the cache or fetches it from Firebase.
     *
     * @param uid The ID of the user to get.
     * @return A [User] object or null if the user is not found.
     */
    suspend fun getUser(uid: String): User? {
        if (userCache.containsKey(uid)) {
            return userCache[uid]
        }

        return suspendCoroutine { continuation ->
            val userRef = FirebaseDatabase.getInstance().getReference("skyline/users").child(uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)
                        user?.let {
                            userCache[uid] = it
                            continuation.resume(it)
                        } ?: continuation.resume(null)
                    } else {
                        continuation.resume(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(null)
                }
            })
        }
    }
}
