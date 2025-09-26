package com.synapse.social.studioasinc.chat

import android.content.Context
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.Calendar

class AudioRecordingManager(
    private val context: Context,
    private val listener: AudioRecordingListener
) {
    private var audioRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private var isRecording = false
    private var recordMs: Long = 0
    private val recordHandler = Handler(Looper.getMainLooper())
    private var recordRunnable: Runnable? = null
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun startRecording() {
        if (isRecording) return

        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val audioDir = File(cacheDir, "audio_records")
        audioDir.mkdirs()
        val recordFile = File(audioDir, "${Calendar.getInstance().timeInMillis}.mp3")
        audioFilePath = recordFile.absolutePath

        audioRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(320000)
            setOutputFile(audioFilePath)
            try {
                prepare()
                start()
                isRecording = true
                vibrator.vibrate(48)
                startTimer()
            } catch (e: IOException) {
                Log.e("AudioRecordingManager", "prepare() failed", e)
            }
        }
    }

    fun stopRecording() {
        if (!isRecording) return

        try {
            audioRecorder?.apply {
                stop()
                release()
            }
        } catch (e: RuntimeException) {
            Log.e("AudioRecordingManager", "Error stopping media recorder: " + e.message)
        } finally {
            audioRecorder = null
            isRecording = false
            vibrator.vibrate(48)
            stopTimer()
            audioFilePath?.let { listener.onRecordingFinished(it, recordMs) }
            recordMs = 0
        }
    }

    private fun startTimer() {
        recordMs = 0
        recordRunnable = Runnable {
            recordMs += 500
            recordHandler.postDelayed(recordRunnable!!, 500)
        }
        recordHandler.post(recordRunnable!!)
    }

    private fun stopTimer() {
        recordRunnable?.let { recordHandler.removeCallbacks(it) }
    }

    fun release() {
        if (isRecording) {
            stopRecording()
        }
    }

    interface AudioRecordingListener {
        fun onRecordingFinished(filePath: String, duration: Long)
    }
}