package com.synapse.social.studioasinc.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.Story
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _posts = MutableLiveData<State<List<Post>>>()
    val posts: LiveData<State<List<Post>>> = _posts

    private val _stories = MutableLiveData<State<List<Story>>>()
    val stories: LiveData<State<List<Story>>> = _stories

    private val authService = SupabaseAuthenticationService()
    private val dbService = SupabaseDatabaseService()

    fun fetchPosts() {
        _posts.value = State.Loading
        viewModelScope.launch {
            try {
                val postList = dbService.select<Post>("posts")
                _posts.value = State.Success(postList.sortedByDescending { it.created_at })
            } catch (e: Exception) {
                _posts.value = State.Error(e.message ?: "Failed to fetch posts")
            }
        }
    }

    fun fetchStories() {
        _stories.value = State.Loading
        viewModelScope.launch {
            try {
                val storiesList = mutableListOf<Story>()
                
                val currentUser = authService.getCurrentUser()
                currentUser?.id?.let {
                    storiesList.add(Story(uid = it)) // "My Story" placeholder
                }

                val fetchedStories = dbService.select<Story>("stories")
                storiesList.addAll(fetchedStories.filter { it.uid != currentUser?.id })
                _stories.value = State.Success(storiesList)
            } catch (e: Exception) {
                _stories.value = State.Error(e.message ?: "Failed to fetch stories")
            }
        }
    }

    sealed class State<out T> {
        object Loading : State<Nothing>()
        data class Success<out T>(val data: T) : State<T>()
        data class Error(val message: String) : State<Nothing>()
    }
}
