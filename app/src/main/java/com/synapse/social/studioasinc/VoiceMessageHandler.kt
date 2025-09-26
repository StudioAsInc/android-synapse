package com.synapse.social.studioasinc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.util.Calendar

class VoiceMessageHandler(
    private val activity: ChatActivity,
    private val listener: VoiceMessageListener
) {
    private var AudioMessageRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private var isRecording = false
    private var recordMs: Long = 0
    private val vbr = activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    interface VoiceMessageListener {
        fun onVoiceMessageRecorded(url: String, duration: Long)
    }

    fun setupVoiceButton(btn_voice_message: View) {
        btn_voice_message.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (ContextCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startAudioRecorder()
                        Toast.makeText(activity, "Recording...", Toast.LENGTH_SHORT).show()
                    } else {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(Manifest.permission.RECORD_AUDIO),
                            1000
                        )
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    stopAudioRecorder()
                    uploadAudioFile()
                    true
                }
                else -> false
            }
        }
    }

    private fun startAudioRecorder() {
        val cc = Calendar.getInstance()
        recordMs = 0
        AudioMessageRecorder = MediaRecorder()

        val getCacheDir = activity.externalCacheDir ?: activity.cacheDir
        val getCacheDirName = "audio_records"
        val getCacheFolder = File(getCacheDir, getCacheDirName)
        getCacheFolder.mkdirs()
        val getRecordFile = File(getCacheFolder, cc.timeInMillis.toString() + ".mp3")
        audioFilePath = getRecordFile.absolutePath

        AudioMessageRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(320000)
            setOutputFile(audioFilePath)
            try {
                prepare()
                start()
                isRecording = true
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        vbr.vibrate(48)
    }

    private fun stopAudioRecorder() {
        if (isRecording) {
            AudioMessageRecorder?.apply {
                try {
                    stop()
                    release()
                } catch (e: RuntimeException) {
                    // Handle exception
                }
            }
            AudioMessageRecorder = null
            isRecording = false
            vbr.vibrate(48)
        }
    }

    private fun uploadAudioFile() {
        if (audioFilePath != null && audioFilePath.isNotEmpty()) {
            val file = File(audioFilePath!!)
            if (file.exists()) {
                AsyncUploadService.uploadWithNotification(
                    activity,
                    audioFilePath!!,
                    file.name,
                    object : AsyncUploadService.UploadProgressListener {
                        override fun onProgress(filePath: String, percent: Int) {
                            // Handle progress
                        }

                        override fun onSuccess(filePath: String, url: String, publicId: String) {
                            listener.onVoiceMessageRecorded(url, recordMs)
                        }

                        override fun onFailure(filePath: String, error: String) {
                            Toast.makeText(
                                activity,
                                "Failed to upload audio.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
            }
        }
    }
}