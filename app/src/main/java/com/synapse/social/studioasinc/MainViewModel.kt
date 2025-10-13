package com.synapse.social.studioasinc

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class MainViewModel : ViewModel() {

    private val _updateState = MutableLiveData<UpdateState>()
    val updateState: LiveData<UpdateState> = _updateState

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    fun checkForUpdates(context: Context) {
        if (!isNetworkAvailable(context)) {
            _updateState.value = UpdateState.NoUpdate
            return
        }

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    val url = URL("https://pastebin.com/raw/sQuaciVv")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connect()

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        connection.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        throw IOException("HTTP error code: ${connection.responseCode}")
                    }
                }

                val updateMap: HashMap<String, Any> = Gson().fromJson(
                    response,
                    object : TypeToken<HashMap<String, Any>>() {}.type
                )

                val currentVersionCode = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                } catch (e: PackageManager.NameNotFoundException) {
                    _updateState.value = UpdateState.Error("Version check failed: ${e.message}")
                    return@launch
                }

                val latestVersionCode = (updateMap["versionCode"] as? Double)?.toInt() ?: 0

                if (latestVersionCode > currentVersionCode) {
                    val title = updateMap["title"] as? String ?: ""
                    val versionName = updateMap["versionName"] as? String ?: ""
                    val changelog = updateMap["whatsNew"] as? String ?: ""
                    val updateLink = updateMap["updateLink"] as? String ?: ""
                    val isCancelable = updateMap["isCancelable"] as? Boolean ?: false

                    _updateState.value = UpdateState.UpdateAvailable(title, versionName, changelog, updateLink, isCancelable)
                } else {
                    _updateState.value = UpdateState.NoUpdate
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error("Update parsing error: ${e.message}")
            }
        }
    }

    fun checkUserAuthentication() {
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val userRef = FirebaseDatabase.getInstance().getReference("skyline/users").child(user.uid)
                userRef.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val banned = snapshot.child("banned").getValue(String::class.java)
                        if ("false" == banned) {
                            _authState.value = AuthState.Authenticated
                        } else {
                            _authState.value = AuthState.Banned
                        }
                    } else {
                        _authState.value = AuthState.NeedsProfileCompletion
                    }
                }.addOnFailureListener {
                    _authState.value = AuthState.Error("Database error")
                }
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }
}

sealed class UpdateState {
    data class UpdateAvailable(val title: String, val versionName: String, val changelog: String, val updateLink: String, val isCancelable: Boolean) : UpdateState()
    object NoUpdate : UpdateState()
    data class Error(val message: String) : UpdateState()
}

sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Banned : AuthState()
    object NeedsProfileCompletion : AuthState()
    data class Error(val message: String) : AuthState()
}
