package com.synapse.social.studioasinc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
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
    private val vbr: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
            activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    interface VoiceMessageListener {
        fun onVoiceMessageRecorded(path: String, duration: Long)
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
                    true
                }
                else -> false
            }
        }
    }

    private fun startAudioRecorder() {
        val cc = Calendar.getInstance()
        recordMs = 0
        AudioMessageRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(activity)
        } else {
            MediaRecorder()
        }

        val getCacheDir = activity.externalCacheDir
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vbr.vibrate(VibrationEffect.createOneShot(48, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vbr.vibrate(48)
        }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vbr.vibrate(VibrationEffect.createOneShot(48, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vbr.vibrate(48)
            }
            audioFilePath?.let { listener.onVoiceMessageRecorded(it, recordMs) }
        }
    }
}