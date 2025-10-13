package com.synapse.social.studioasinc.repository

import com.synapse.social.studioasinc.ImageUploader
import com.synapse.social.studioasinc.model.ImgbbResponse

/**
 * A repository for uploading images.
 */
class ImageUploadRepository {

    private val imageUploader = ImageUploader()

    /**
     * Uploads an image.
     *
     * @param filePath The path of the image file to upload.
     * @return A [Result] object containing the [ImgbbResponse] on success, or an exception on failure.
     */
    suspend fun uploadImage(filePath: String): Result<ImgbbResponse> {
        return imageUploader.uploadImage(filePath)
    }
}
