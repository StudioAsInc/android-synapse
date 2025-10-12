package com.synapse.social.studioasinc.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeViewModel : ViewModel() {

    private val _posts = MutableLiveData<State<List<Post>>>()
    val posts: LiveData<State<List<Post>>> = _posts

    private val _stories = MutableLiveData<State<List<Story>>>()
    val stories: LiveData<State<List<Story>>> = _stories

    private val database = FirebaseDatabase.getInstance()
    private val postsRef = database.getReference("skyline/posts")
    private val storiesDbRef = database.getReference("skyline/stories")
    private val currentUser = FirebaseAuth.getInstance().currentUser

    fun fetchPosts() {
        _posts.value = State.Loading
        postsRef.orderByChild("publish_date").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val postList = snapshot.children.mapNotNull { it.getValue(Post::class.java) }
                    _posts.value = State.Success(postList.sortedByDescending { it.publish_date })
                } else {
                    _posts.value = State.Success(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _posts.value = State.Error(error.message)
            }
        })
    }

    fun fetchStories() {
        _stories.value = State.Loading
        storiesDbRef.orderByChild("publish_date").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val storiesList = mutableListOf<Story>()

                currentUser?.uid?.let {
                    storiesList.add(Story(uid = it)) // "My Story" placeholder
                }

                if (snapshot.exists()) {
                    val fetchedStories = snapshot.children.mapNotNull { it.getValue(Story::class.java) }
                    storiesList.addAll(fetchedStories.filter { it.uid != currentUser?.uid })
                }
                _stories.value = State.Success(storiesList)
            }

            override fun onCancelled(error: DatabaseError) {
                _stories.value = State.Error(error.message)
            }
        })
    }

    sealed class State<out T> {
        object Loading : State<Nothing>()
        data class Success<out T>(val data: T) : State<T>()
        data class Error(val message: String) : State<Nothing>()
    }
}
