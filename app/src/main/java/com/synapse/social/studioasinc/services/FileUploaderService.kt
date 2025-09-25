package com.synapse.social.studioasinc.services

import com.synapse.social.studioasinc.UploadFiles

class FileUploaderService {

    interface UploadListener {
        fun onProgress(percent: Int)
        fun onSuccess(url: String, publicId: String)
        fun onFailure(error: String)
    }

    interface DeleteListener {
        fun onSuccess()
        fun onFailure(error: String)
    }

    fun uploadFile(filePath: String, fileName: String, listener: UploadListener) {
        UploadFiles.uploadFile(filePath, fileName, object : UploadFiles.UploadCallback {
            override fun onProgress(percent: Int) {
                listener.onProgress(percent)
            }

            override fun onSuccess(url: String, publicId: String) {
                listener.onSuccess(url, publicId)
            }

            override fun onFailure(error: String) {
                listener.onFailure(error)
            }
        })
    }

    fun deleteFile(publicId: String, listener: DeleteListener) {
        UploadFiles.deleteByPublicId(publicId, object : UploadFiles.DeleteCallback {
            override fun onSuccess() {
                listener.onSuccess()
            }

            override fun onFailure(error: String) {
                listener.onFailure(error)
            }
        })
    }
}