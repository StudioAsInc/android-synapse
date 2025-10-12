package com.synapse.social.studioasinc.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.synapse.social.studioasinc.model.User

/**
 * Repository for fetching and caching user data.
 */
object UserRepository {

    private val userCache = mutableMapOf<String, User>()

    /**
     * Gets a user from the cache or fetches it from Firebase.
     *
     * @param uid The ID of the user to get.
     * @param onResult A callback function that will be invoked with the `User` object.
     */
    fun getUser(uid: String, onResult: (User?) -> Unit) {
        if (userCache.containsKey(uid)) {
            onResult(userCache[uid])
        } else {
            val userRef = FirebaseDatabase.getInstance().getReference("skyline/users").child(uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)
                        user?.let {
                            userCache[uid] = it
                            onResult(it)
                        }
                    } else {
                        onResult(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(null)
                }
            })
        }
    }
}
