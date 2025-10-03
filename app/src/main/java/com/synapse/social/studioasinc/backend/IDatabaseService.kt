package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IDataListener
import java.io.File

interface IDatabaseService {
    fun getReference(path: String): IDatabaseReference
    fun getUserById(uid: String, listener: IDataListener)
    fun setValue(path: String, value: Any, listener: ICompletionListener<*>)
    fun updateChildren(path: String, children: MutableMap<String, Any>, listener: ICompletionListener<*>)
    fun uploadFile(file: File, path: String, listener: ICompletionListener<*>)
    fun searchUsers(query: String, listener: IDataListener)
    fun getFollowers(uid: String, listener: IDataListener)
    fun getFollowing(uid: String, listener: IDataListener)
    fun getData(path: String, listener: IDataListener)
}