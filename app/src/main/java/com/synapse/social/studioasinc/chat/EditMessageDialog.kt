package com.synapse.social.studioasinc.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.chat.presentation.MessageActionsViewModel
import com.synapse.social.studioasinc.databinding.DialogEditMessageBinding
import com.synapse.social.studioasinc.util.MessageAnimations
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Dialog for editing message content
 * Supports character counting, validation, and edit submission
 */
class EditMessageDialog : DialogFragment() {

    companion object {
        private const val TAG = "EditMessageDialog"
        private const val ARG_MESSAGE_ID = "message_id"
        private const val ARG_CURRENT_TEXT = "current_text"
        private const val ARG_MESSAGE_TIMESTAMP = "message_timestamp"
        private const val MAX_CHARACTERS = 5000
        private const val WARNING_THRESHOLD = 4500 // Show warning at 90%
        private const val EDIT_TIME_LIMIT_MS = 48 * 60 * 60 * 1000L // 48 hours in milliseconds

        /**
         * Create a new instance of EditMessageDialog
         * 
         * @param messageId The ID of the message to edit
         * @param currentText The current text content of the message
         * @param messageTimestamp The timestamp when the message was created (in milliseconds)
         * @return New instance of EditMessageDialog
         */
        fun newInstance(messageId: String, currentText: String, messageTimestamp: Long): EditMessageDialog {
            val dialog = EditMessageDialog()
            val args = Bundle().apply {
                putString(ARG_MESSAGE_ID, messageId)
                putString(ARG_CURRENT_TEXT, currentText)
                putLong(ARG_MESSAGE_TIMESTAMP, messageTimestamp)
            }
            dialog.arguments = args
            return dialog
        }
    }

    private var _binding: DialogEditMessageBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MessageActionsViewModel

    private var messageId: String = ""
    private var currentText: String = ""
    private var messageTimestamp: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog)

        // Get arguments
        arguments?.let {
            messageId = it.getString(ARG_MESSAGE_ID, "")
            currentText = it.getString(ARG_CURRENT_TEXT, "")
            messageTimestamp = it.getLong(ARG_MESSAGE_TIMESTAMP, 0L)
        }

        // Initialize ViewModel
        viewModel = MessageActionsViewModel(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Validate message age before showing dialog
        if (!isMessageEditableByAge()) {
            Toast.makeText(
                requireContext(),
                "This message is too old to edit (>48 hours)",
                Toast.LENGTH_SHORT
            ).show()
            dismiss()
            return
        }

        setupEditText()
        setupCharacterCounter()
        setupButtons()
    }

    override fun onStart() {
        super.onStart()
        // Make dialog full width
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Show keyboard automatically
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    /**
     * Set up the EditText with current message text
     * Pre-populate with current text and set cursor to end
     */
    private fun setupEditText() {
        binding.etMessageContent.apply {
            // Pre-populate with current text
            setText(currentText)
            
            // Set cursor to end of text
            setSelection(currentText.length)
            
            // Request focus
            requestFocus()
        }
    }

    /**
     * Set up character counter with TextWatcher
     */
    private fun setupCharacterCounter() {
        // Initialize character count display
        updateCharacterCount(currentText.length)

        // Add TextWatcher to update count on text change
        binding.etMessageContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                updateCharacterCount(length)
                updateSaveButtonState(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * Update character count display
     * Changes color to warning when approaching limit
     */
    private fun updateCharacterCount(count: Int) {
        binding.tvCharacterCount.text = "$count / $MAX_CHARACTERS"

        // Change color to warning when approaching limit
        val color = if (count >= WARNING_THRESHOLD) {
            ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
        } else {
            ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        }
        binding.tvCharacterCount.setTextColor(color)
    }

    /**
     * Update save button enabled state based on text content
     */
    private fun updateSaveButtonState(text: String) {
        // Disable save button if text is empty or unchanged
        val isTextValid = text.isNotBlank() && text != currentText
        binding.btnSave.isEnabled = isTextValid
    }

    /**
     * Set up button click listeners
     */
    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveEditedMessage()
        }
    }

    /**
     * Check if message is within the editable time window (48 hours)
     */
    private fun isMessageEditableByAge(): Boolean {
        val currentTime = System.currentTimeMillis()
        val messageAge = currentTime - messageTimestamp
        return messageAge <= EDIT_TIME_LIMIT_MS
    }

    /**
     * Save the edited message
     */
    private fun saveEditedMessage() {
        val newContent = binding.etMessageContent.text?.toString() ?: ""

        // Validate non-empty content
        if (newContent.isBlank()) {
            Toast.makeText(requireContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate message age again before saving
        if (!isMessageEditableByAge()) {
            Toast.makeText(
                requireContext(),
                "This message is too old to edit (>48 hours)",
                Toast.LENGTH_SHORT
            ).show()
            dismiss()
            return
        }

        // Disable buttons during save
        binding.btnSave.isEnabled = false
        binding.btnCancel.isEnabled = false

        // Show loading state (you could add a ProgressBar to the layout)
        Log.d(TAG, "Saving edited message: $messageId")

        lifecycleScope.launch {
            viewModel.editMessage(messageId, newContent)
                .collectLatest { state ->
                    when (state) {
                        is MessageActionsViewModel.MessageActionState.Loading -> {
                            // Loading state - show pulse animation on save button
                            Log.d(TAG, "Saving message...")
                            MessageAnimations.applyPendingActionAnimation(binding.btnSave)
                        }
                        is MessageActionsViewModel.MessageActionState.Success -> {
                            // Clear animations
                            MessageAnimations.clearAnimations(binding.btnSave)
                            
                            // Success - show message and dismiss
                            Toast.makeText(
                                requireContext(),
                                state.message,
                                Toast.LENGTH_SHORT
                            ).show()
                            dismiss()
                        }
                        is MessageActionsViewModel.MessageActionState.Error -> {
                            // Clear animations
                            MessageAnimations.clearAnimations(binding.btnSave)
                            
                            // Error - show error and re-enable buttons
                            Toast.makeText(
                                requireContext(),
                                state.error,
                                Toast.LENGTH_SHORT
                            ).show()
                            binding.btnSave.isEnabled = true
                            binding.btnCancel.isEnabled = true
                        }
                        is MessageActionsViewModel.MessageActionState.Idle -> {
                            // Idle state - do nothing
                        }
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
