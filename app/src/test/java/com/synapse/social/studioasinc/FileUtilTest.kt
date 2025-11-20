package com.synapse.social.studioasinc

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class FileUtilTest {

    @Test
    fun testResizeBitmapFileRetainRatio_HandlesInvalidImageGracefully() {
        val tempFile = File.createTempFile("test_invalid_image", ".txt")
        tempFile.writeText("This is not a valid image file content.")
        tempFile.deleteOnExit()

        val destFile = File.createTempFile("test_dest", ".png")
        destFile.deleteOnExit()

        try {
            Mockito.mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
                mockedBitmapFactory.`when`<Bitmap> { BitmapFactory.decodeFile(tempFile.absolutePath) }.thenReturn(null)

                // This should not crash. If it returns cleanly, the test passes.
                FileUtil.resizeBitmapFileRetainRatio(tempFile.absolutePath, destFile.absolutePath, 100)
            }
        } catch (e: Exception) {
             fail("FileUtil.resizeBitmapFileRetainRatio threw unexpected exception: ${e.javaClass.simpleName} - ${e.message}")
        }
    }
}
