package com.synapse.social.studioasinc.repository

import com.synapse.social.studioasinc.ImageUploader
import com.synapse.social.studioasinc.model.Data
import com.synapse.social.studioasinc.model.Image
import com.synapse.social.studioasinc.model.ImgbbResponse
import com.synapse.social.studioasinc.model.Thumb
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ImageUploadRepositoryTest {

    @Mock
    private lateinit var imageUploader: ImageUploader

    private lateinit var repository: ImageUploadRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = ImageUploadRepository()
    }

    @Test
    fun `uploadImage should return success result when imageUploader returns success`() = runBlockingTest {
        val filePath = "test.jpg"
        val imgbbResponse = ImgbbResponse(
            Data(
                "1",
                "test",
                "viewer",
                "url",
                "displayUrl",
                "800",
                "600",
                "1024",
                "time",
                "expiration",
                Image("filename", "name", "mime", "extension", "url"),
                Thumb("filename", "name", "mime", "extension", "url"),
                "deleteUrl"
            ), true, 200
        )
        val successResult = Result.success(imgbbResponse)

        `when`(imageUploader.uploadImage(filePath)).thenReturn(successResult)

        val result = repository.uploadImage(filePath)

        assertEquals(successResult, result)
    }

    @Test
    fun `uploadImage should return failure result when imageUploader returns failure`() = runBlockingTest {
        val filePath = "test.jpg"
        val exception = Exception("Upload failed")
        val failureResult = Result.failure<ImgbbResponse>(exception)

        `when`(imageUploader.uploadImage(filePath)).thenReturn(failureResult)

        val result = repository.uploadImage(filePath)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
