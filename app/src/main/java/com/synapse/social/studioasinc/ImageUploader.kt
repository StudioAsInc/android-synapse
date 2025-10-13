package com.synapse.social.studioasinc

import com.google.gson.Gson
import com.synapse.social.studioasinc.model.ImgbbResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * A utility class for uploading images to the ImgBB API.
 */
class ImageUploader {

    /**
     * Uploads an image to the ImgBB API.
     *
     * @param filePath The path of the image file to upload.
     * @return A [Result] object containing the [ImgbbResponse] on success, or an exception on failure.
     */
    suspend fun uploadImage(filePath: String): Result<ImgbbResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                val boundary = "*****"
                val lineEnd = "\r\n"
                val twoHyphens = "--"

                val url = URL("https://api.imgbb.com/1/upload?expiration=0&key=${BuildConfig.IMG_BB_API_KEY}")

                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.doOutput = true
                connection.useCaches = false
                connection.requestMethod = "POST"
                connection.setRequestProperty("Connection", "Keep-Alive")
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")

                val dos = DataOutputStream(connection.outputStream)

                dos.writeBytes(twoHyphens + boundary + lineEnd)
                dos.writeBytes("Content-Disposition: form-data; name=\"image\";filename=\"${file.name}\"$lineEnd")
                dos.writeBytes(lineEnd)

                val fileInputStream = FileInputStream(file)
                val bufferSize = 1024
                val buffer = ByteArray(bufferSize)

                var bytesRead: Int
                while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                    dos.write(buffer, 0, bytesRead)
                }

                dos.writeBytes(lineEnd)
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)

                fileInputStream.close()
                dos.flush()
                dos.close()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val `in` = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (`in`.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    `in`.close()
                    val imgbbResponse = Gson().fromJson(response.toString(), ImgbbResponse::class.java)
                    Result.success(imgbbResponse)
                } else {
                    Result.failure(Exception("Error: $responseCode"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
