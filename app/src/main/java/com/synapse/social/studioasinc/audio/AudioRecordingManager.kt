package com.synapse.social.studioasinc.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Handler
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import com.synapse.social.studioasinc.AsyncUploadService
import java.io.File
import java.io.IOException
import java.util.Calendar

class AudioRecordingManager(
    private val context: Context,
    private val listener: AudioRecordingListener
) {

    interface AudioRecordingListener {
        fun onUploadSuccess(url: String, duration: Long)
        fun onUploadFailure(error: String)
    }

    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private var isRecording = false
    private var recordStartTime: Long = 0
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun startRecording() {
        if (isRecording) return

        val cacheDir = context.externalCacheDir
        val audioCacheDir = File(cacheDir, "audio_records")
        if (!audioCacheDir.exists()) {
            audioCacheDir.mkdirs()
        }
        val audioFile = File(audioCacheDir, "${System.currentTimeMillis()}.mp3")
        audioFilePath = audioFile.absolutePath

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(320000)
            setOutputFile(audioFilePath)
            try {
                prepare()
                start()
                isRecording = true
                recordStartTime = System.currentTimeMillis()
                vibrator.vibrate(48)
                Toast.makeText(context, "Recording...", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Log.e("AudioRecordingManager", "prepare() failed", e)
                listener.onUploadFailure("Failed to start recording.")
            }
        }
    }

    fun stopRecordingAndUpload() {
        if (!isRecording) return

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: RuntimeException) {
            Log.e("AudioRecordingManager", "stop() failed", e)
        } finally {
            mediaRecorder = null
            isRecording = false
            vibrator.vibrate(48)
            val duration = System.currentTimeMillis() - recordStartTime
            uploadAudioFile(duration)
        }
    }

    private fun uploadAudioFile(duration: Long) {
        val filePath = audioFilePath ?: return
        val file = File(filePath)
        if (!file.exists()) {
            listener.onUploadFailure("Audio file not found.")
            return
        }

        AsyncUploadService.uploadWithNotification(
            context,
            filePath,
            file.name,
            object : AsyncUploadService.UploadProgressListener {
                override fun onProgress(filePath: String, percent: Int) {
                    // Optional: Handle progress update
                }

                override fun onSuccess(filePath: String, url: String, publicId: String) {
                    listener.onUploadSuccess(url, duration)
                }

                override fun onFailure(filePath: String, error: String) {
                    listener.onUploadFailure(error)
                }
            }
        )
    }

    fun cancelRecording() {
        if (!isRecording) return
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: RuntimeException) {
            Log.e("AudioRecordingManager", "stop() failed", e)
        } finally {
            mediaRecorder = null
            isRecording = false
            audioFilePath?.let { File(it).delete() }
            audioFilePath = null
        }
    }
}