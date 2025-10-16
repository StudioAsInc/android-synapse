package com.synapse.social.studioasinc.backend.interfaces

import android.net.Uri

interface IStorageService {
    fun uploadFile(uri: Uri, path: String, listener: ICompletionListener<String>)
    fun downloadUrl(path: String, listener: ICompletionListener<Uri>)
    fun deleteFile(path: String, listener: ICompletionListener<Unit>)
}
