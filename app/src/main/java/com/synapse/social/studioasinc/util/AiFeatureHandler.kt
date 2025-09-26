package com.synapse.social.studioasinc.util

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.service.studioasinc.AI.Gemini
import com.synapse.social.studioasinc.BaseMessageViewHolder
import com.synapse.social.studioasinc.ContentDisplayBottomSheetDialogFragment
import com.synapse.social.studioasinc.R
import java.util.HashMap

class AiFeatureHandler(private val activity: AppCompatActivity) {

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

    fun callGeminiForSummary(prompt: String, viewHolder: BaseMessageViewHolder) {
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

    fun callGeminiForExplanation(prompt: String, viewHolder: BaseMessageViewHolder) {
        val params = AiFeatureParams(
            prompt,
            activity.getString(R.string.gemini_system_instruction_explanation),
            "gemini-2.5-flash",
            activity.getString(R.string.gemini_explanation_title),
            "GeminiExplanation",
            activity.getString(R.string.gemini_error_explanation),
            viewHolder,
            1000
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