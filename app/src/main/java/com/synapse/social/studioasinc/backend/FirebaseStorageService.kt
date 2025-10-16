package com.synapse.social.studioasinc.backend

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IStorageService

class FirebaseStorageService : IStorageService {

    private val storage = FirebaseStorage.getInstance()

    override fun uploadFile(uri: Uri, path: String, listener: ICompletionListener<String>) {
        val storageRef = storage.getReference(path)
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener {
                    listener.onComplete(it.toString(), null)
                }.addOnFailureListener {
                    listener.onComplete(null, it)
                }
            }
            .addOnFailureListener {
                listener.onComplete(null, it)
            }
    }

    override fun downloadUrl(path: String, listener: ICompletionListener<Uri>) {
        val storageRef = storage.getReference(path)
        storageRef.downloadUrl
            .addOnSuccessListener {
                listener.onComplete(it, null)
            }
            .addOnFailureListener {
                listener.onComplete(null, it)
            }
    }

    override fun deleteFile(path: String, listener: ICompletionListener<Unit>) {
        val storageRef = storage.getReference(path)
        storageRef.delete()
            .addOnSuccessListener {
                listener.onComplete(Unit, null)
            }
            .addOnFailureListener {
                listener.onComplete(null, it)
            }
    }
}
