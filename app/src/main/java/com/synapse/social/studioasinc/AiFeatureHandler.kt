package com.synapse.social.studioasinc

import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.service.studioasinc.AI.Gemini
import com.synapse.social.studioasinc.chat.model.ChatMessage
import kotlin.math.max
import kotlin.math.min

class AiFeatureHandler(
    private val activity: AppCompatActivity,
    private val gemini: Gemini,
    private val message_et: EditText,
    private val chatMessagesList: ArrayList<ChatMessage>,
    private val auth: FirebaseAuth,
    private var secondUserName: String,
    private val mMessageReplyLayoutBodyRightUsername: TextView,
    private val mMessageReplyLayoutBodyRightMessage: TextView
) {

    fun setSecondUserName(name: String) {
        this.secondUserName = name
    }

    private data class AiFeatureParams(
        val prompt: String,
        val systemInstruction: String,
        val model: String,
        val bottomSheetTitle: String,
        val logTag: String,
        val errorMessage: String,
        val viewHolder: BaseMessageViewHolder,
        val maxTokens: Int?
    )

    fun handleSendButtonLongClick(replyMessageID: String): Boolean {
        if (message_et.text.toString().isNotEmpty()) {
            val prompt = "Fix grammar, punctuation, and clarity without changing meaning. " +
                    "Preserve original formatting (line breaks, lists, markdown). " +
                    "Censor profanity by replacing letters with asterisks. " +
                    "Return ONLY the corrected RAW text.\n```" +
                    message_et.text.toString() + "```"
            callGeminiForSend(prompt, true)
        } else {
            if (replyMessageID.isNotEmpty() && replyMessageID != "null") {
                val repliedMessageIndex = chatMessagesList.indexOfFirst { it.key == replyMessageID }

                if (repliedMessageIndex != -1) {
                    val contextBuilder = StringBuilder()
                    contextBuilder.append("You are helping 'Me' to write a reply in a conversation with '$secondUserName'.\n")
                    contextBuilder.append("Here is the recent chat history:\n---\n")

                    val startIndex = max(0, repliedMessageIndex - 10)
                    val endIndex = min(chatMessagesList.size - 1, repliedMessageIndex + 10)

                    for (i in startIndex..endIndex) {
                        val message = chatMessagesList[i]
                        val sender = if (message.uid == auth.currentUser?.uid) "Me" else secondUserName
                        contextBuilder.append("$sender: ${message.messageText}\n")
                    }

                    contextBuilder.append("---\n")

                    val repliedMessageSender = mMessageReplyLayoutBodyRightUsername.text.toString()
                    val repliedMessageText = mMessageReplyLayoutBodyRightMessage.text.toString()

                    contextBuilder.append("I need to reply to this message from '$repliedMessageSender': \"$repliedMessageText\"\n")
                    contextBuilder.append("Based on the conversation history, please suggest a short, relevant reply from 'Me'.")

                    val prompt = contextBuilder.toString()
                    callGeminiForSend(prompt, false)
                }
            } else {
                val prompt = "Suggest a generic, friendly greeting."
                callGeminiForSend(prompt, false)
            }
        }
        return true
    }

    private fun callGeminiForSend(prompt: String, showThinking: Boolean) {
        gemini.setModel("gemini-2.5-flash-lite")
        gemini.setShowThinking(showThinking)
        gemini.setSystemInstruction(
            "You are a concise text assistant. Always return ONLY the transformed text (no explanation, no labels). " +
                    "Preserve original formatting. Censor profanity by replacing letters with asterisks (e.g., s***t). " +
                    "Keep the language and tone of the input unless asked to change it."
        )
        gemini.sendPrompt(prompt, object : Gemini.GeminiCallback {
            override fun onSuccess(response: String) {
                activity.runOnUiThread { message_et.setText(response) }
            }

            override fun onError(error: String) {
                activity.runOnUiThread { message_et.setText("Error: $error") }
            }

            override fun onThinking() {
                if (showThinking) {
                    activity.runOnUiThread { message_et.setText(gemini.thinkingText) }
                }
            }
        })
    }

    internal fun callGeminiForSummary(prompt: String, viewHolder: BaseMessageViewHolder) {
        val params = AiFeatureParams(
            prompt,
            activity.getString(R.string.gemini_system_instruction_summary),
            "gemini-2.5-flash-lite",
            activity.getString(R.string.gemini_summary_title),
            "GeminiSummary",
            activity.getString(R.string.gemini_error_summary),
            viewHolder,
            null
        )
        callGeminiForAiFeature(params)
    }

    internal fun callGeminiForExplanation(prompt: String, viewHolder: BaseMessageViewHolder) {
        val params = AiFeatureParams(
            prompt,
            activity.getString(R.string.gemini_system_instruction_explanation),
            "gemini-2.5-flash",
            activity.getString(R.string.gemini_explanation_title),
            "GeminiExplanation",
            activity.getString(R.string.gemini_error_explanation),
            viewHolder,
            null
        )
        callGeminiForAiFeature(params)
    }

    private fun callGeminiForAiFeature(params: AiFeatureParams) {
        val builder = Gemini.Builder(activity)
            .model(params.model)
            .showThinking(true)
            .systemInstruction(params.systemInstruction)

        params.maxTokens?.let { builder.maxTokens(it) }

        val gemini = builder.build()

        gemini.sendPrompt(params.prompt, object : Gemini.GeminiCallback {
            override fun onSuccess(response: String) {
                activity.runOnUiThread {
                    params.viewHolder.stopShimmer()
                    val bottomSheet = ContentDisplayBottomSheetDialogFragment.newInstance(response, params.bottomSheetTitle)
                    bottomSheet.show(activity.supportFragmentManager, bottomSheet.tag)
                }
            }

            override fun onError(error: String) {
                activity.runOnUiThread {
                    params.viewHolder.stopShimmer()
                    Toast.makeText(activity, "${params.errorMessage}$error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onThinking() {
                activity.runOnUiThread {
                    params.viewHolder.startShimmer()
                }
            }
        })
    }
}