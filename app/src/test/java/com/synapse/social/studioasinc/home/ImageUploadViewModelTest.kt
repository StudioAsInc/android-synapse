package com.synapse.social.studioasinc.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.synapse.social.studioasinc.model.Data
import com.synapse.social.studioasinc.model.Image
import com.synapse.social.studioasinc.model.ImgbbResponse
import com.synapse.social.studioasinc.model.Thumb
import com.synapse.social.studioasinc.repository.ImageUploadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ImageUploadViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()

    @Mock
    private lateinit var repository: ImageUploadRepository

    @Mock
    private lateinit var observer: Observer<Result<ImgbbResponse>>

    private lateinit var viewModel: ImageUploadViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = ImageUploadViewModel()
        viewModel.uploadStatus.observeForever(observer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
        viewModel.uploadStatus.removeObserver(observer)
    }

    @Test
    fun `uploadImage should post success value to live data when repository returns success`() = runBlockingTest {
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

        `when`(repository.uploadImage(filePath)).thenReturn(successResult)

        viewModel.uploadImage(filePath)

        verify(observer).onChanged(successResult)
    }

    @Test
    fun `uploadImage should post failure value to live data when repository returns failure`() = runBlockingTest {
        val filePath = "test.jpg"
        val exception = Exception("Upload failed")
        val failureResult = Result.failure<ImgbbResponse>(exception)

        `when`(repository.uploadImage(filePath)).thenReturn(failureResult)

        viewModel.uploadImage(filePath)

        verify(observer).onChanged(failureResult)
    }
}
