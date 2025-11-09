package com.synapse.social.studioasinc.AI

import android.content.Context
import android.util.Log
import android.widget.TextView
import com.synapse.social.studioasinc.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.random.Random

class Gemini private constructor(
    private val context: Context,
    private val apiKeys: List<String>,
    private var model: String,
    private var responseType: String,
    private var tone: String,
    private var size: String,
    private var maxTokens: Int,
    private var temperature: Double,
    private var showThinking: Boolean,
    private var thinkingText: String,
    private var systemInstruction: String,
    private var responseTextView: TextView?
) {

    interface GeminiCallback {
        fun onSuccess(response: String)
        fun onError(error: String)
        fun onThinking()
    }

    class Builder(private val context: Context) {
        private var apiKeys: List<String> = loadApiKeysFromRaw(context)
        private var model: String = "gemini-1.5-flash"
        private var responseType: String = "text"
        private var tone: String = "normal"
        private var size: String = "normal"
        private var maxTokens: Int = 2500
        private var temperature: Double = 1.0
        private var showThinking: Boolean = false
        private var thinkingText: String = "Thinking..."
        private var systemInstruction: String = 
            "Your name is Synapse AI, you are an AI made for Synapse (social media) assistance"
        private var responseTextView: TextView? = null

        fun model(model: String) = apply { this.model = model }
        fun responseType(responseType: String) = apply { this.responseType = responseType }
        fun tone(tone: String) = apply { this.tone = tone }
        fun size(size: String) = apply { this.size = size }
        fun maxTokens(maxTokens: Int) = apply { this.maxTokens = maxTokens }
        fun temperature(temperature: Double) = apply { this.temperature = temperature }
        fun showThinking(showThinking: Boolean) = apply { this.showThinking = showThinking }
        fun thinkingText(thinkingText: String) = apply { this.thinkingText = thinkingText }
        fun systemInstruction(systemInstruction: String) = apply { this.systemInstruction = systemInstruction }
        fun responseTextView(textView: TextView) = apply { this.responseTextView = textView }

        fun build(): Gemini {
            return Gemini(
                context = context,
                apiKeys = apiKeys,
                model = model,
                responseType = responseType,
                tone = tone,
                size = size,
                maxTokens = maxTokens,
                temperature = temperature,
                showThinking = showThinking,
                thinkingText = thinkingText,
                systemInstruction = systemInstruction,
                responseTextView = responseTextView
            )
        }

        private fun loadApiKeysFromRaw(context: Context): List<String> {
            val keys = mutableListOf<String>()
            try {
                context.resources.openRawResource(R.raw.gemini_api).use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val jsonString = reader.readText()
                    val jsonArray = JSONArray(jsonString)
                    
                    for (i in 0 until jsonArray.length()) {
                        keys.add(jsonArray.getString(i))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading API keys: ${e.message}")
            }
            return keys
        }
    }

    fun sendPrompt(prompt: String, callback: GeminiCallback? = null) {
        if (prompt.isBlank()) {
            handleError("Prompt is empty!", callback)
            return
        }

        if (apiKeys.isEmpty()) {
            handleError("No API keys available!", callback)
            return
        }

        if (showThinking) {
            callback?.onThinking() ?: responseTextView?.post {
                responseTextView?.text = thinkingText
            }
        }

        Thread {
            try {
                val selectedApiKey = apiKeys.random()
                val response = sendGeminiRequest(prompt, selectedApiKey)

                responseTextView?.post {
                    responseTextView?.text = response
                }

                callback?.onSuccess(response)
            } catch (e: Exception) {
                val error = "Error: ${e.message}"
                Log.e(TAG, error, e)
                handleError(error, callback)
            }
        }.start()
    }

    private fun sendGeminiRequest(prompt: String, apiKey: String): String {
        val urlString = "$BASE_URL$model:generateContent?key=$apiKey"
        var conn: HttpURLConnection? = null

        try {
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            val payload = buildPayload(prompt)
            val body = payload.toString()

            conn.outputStream.use { os ->
                val input = body.toByteArray(StandardCharsets.UTF_8)
                os.write(input)
                os.flush()
            }

            val code = conn.responseCode
            val inputStream = if (code >= 400) conn.errorStream else conn.inputStream
            val rawResponse = inputStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }?.trim() ?: ""

            Log.d(TAG, "HTTP code=$code rawResponse=$rawResponse")

            return if (code in 200..299) {
                try {
                    extractTextFromGeminiResponse(rawResponse)
                } catch (e: JSONException) {
                    throw Exception("Failed to parse AI response.", e)
                }
            } else {
                throw Exception("HTTP $code error: $rawResponse")
            }
        } finally {
            conn?.disconnect()
        }
    }

    private fun buildPayload(prompt: String): JSONObject {
        return JSONObject().apply {
            try {
                val fullSystemInstruction = buildFullSystemInstruction()
                if (fullSystemInstruction.isNotEmpty()) {
                    put("system_instruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", fullSystemInstruction))
                        })
                    })
                }

                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })

                put("generationConfig", JSONObject().apply {
                    put("temperature", temperature)
                    put("maxOutputTokens", maxTokens)
                })
            } catch (e: JSONException) {
                Log.e(TAG, "buildPayload JSONException: ${e.message}")
            }
        }
    }

    private fun buildFullSystemInstruction(): String {
        return buildString {
            append(systemInstruction)

            if (tone != "normal") {
                append(" Respond in a $tone tone.")
            }

            if (size != "normal") {
                append(" Make the response $size in length.")
            }

            if (responseType != "text") {
                append(" Format the response as $responseType.")
            }
        }
    }

    private fun extractTextFromGeminiResponse(raw: String): String {
        if (raw.isEmpty()) {
            throw JSONException("Empty response from API")
        }

        try {
            val root = JSONObject(raw)

            if (root.has("candidates")) {
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    if (candidate.has("content")) {
                        val content = candidate.getJSONObject("content")
                        if (content.has("parts")) {
                            val parts = content.getJSONArray("parts")
                            if (parts.length() > 0) {
                                return parts.getJSONObject(0).optString("text", "No text found in response part.")
                            }
                        }
                    }
                }
            }

            throw JSONException("Could not extract text from the response.")
        } catch (e: JSONException) {
            Log.w(TAG, "extractTextFromGeminiResponse JSON parse error: ${e.message}")
            throw e
        }
    }

    private fun handleError(error: String, callback: GeminiCallback?) {
        callback?.onError(error) ?: responseTextView?.post {
            responseTextView?.text = error
        }
    }

    // Getters and setters
    fun setModel(model: String) { this.model = model }
    fun setResponseType(responseType: String) { this.responseType = responseType }
    fun setTone(tone: String) { this.tone = tone }
    fun setSize(size: String) { this.size = size }
    fun setMaxTokens(maxTokens: Int) { this.maxTokens = maxTokens }
    fun setTemperature(temperature: Double) { this.temperature = temperature }
    fun setShowThinking(showThinking: Boolean) { this.showThinking = showThinking }
    fun setThinkingText(thinkingText: String) { this.thinkingText = thinkingText }
    fun setSystemInstruction(systemInstruction: String) { this.systemInstruction = systemInstruction }
    fun setResponseTextView(responseTextView: TextView?) { this.responseTextView = responseTextView }

    fun getModel(): String = model
    fun getResponseType(): String = responseType
    fun getTone(): String = tone
    fun getSize(): String = size
    fun getMaxTokens(): Int = maxTokens
    fun getTemperature(): Double = temperature
    fun isShowThinking(): Boolean = showThinking
    fun getThinkingText(): String = thinkingText
    fun getSystemInstruction(): String = systemInstruction

    companion object {
        private const val TAG = "GeminiAPI"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    }
}
