package com.synapse.social.studioasinc.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.model.ImgbbResponse
import com.synapse.social.studioasinc.repository.ImageUploadRepository
import kotlinx.coroutines.launch

/**
 * A ViewModel for uploading images.
 */
class ImageUploadViewModel : ViewModel() {

    private val repository = ImageUploadRepository()

    private val _uploadStatus = MutableLiveData<Result<ImgbbResponse>>()
    /**
     * The status of the image upload.
     */
    val uploadStatus: LiveData<Result<ImgbbResponse>> = _uploadStatus

    /**
     * Uploads an image.
     *
     * @param filePath The path of the image file to upload.
     */
    fun uploadImage(filePath: String) {
        viewModelScope.launch {
            val result = repository.uploadImage(filePath)
            _uploadStatus.postValue(result)
        }
    }
}
