package com.synapse.social.studioasinc.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _avatarUrl = MutableLiveData<String?>()
    val avatarUrl: LiveData<String?> = _avatarUrl

    fun fetchUserAvatar(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = SupabaseClient.client.postgrest["skyline/users"].select(
                    columns = Columns.list("avatar")
                ) {
                    eq("id", userId)
                }.data

                if (response.isNotBlank()) {
                    val user = response.trim()
                    // Further parsing might be needed depending on the exact response format
                    _avatarUrl.postValue(user)
                } else {
                    _avatarUrl.postValue(null)
                }
            } catch (e: Exception) {
                _avatarUrl.postValue(null)
            }
        }
    }
}
