package com.synapse.social.studioasinc.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.chat.presentation.MessageActionsViewModel
import com.synapse.social.studioasinc.databinding.DialogDeleteMessageBinding
import com.synapse.social.studioasinc.util.MessageAnimations
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Dialog for confirming message deletion
 * Provides options for "Delete for me" and "Delete for everyone"
 */
class DeleteConfirmationDialog : DialogFragment() {

    companion object {
        private const val TAG = "DeleteConfirmationDialog"
        private const val ARG_MESSAGE_ID = "message_id"
        private const val ARG_IS_OWN_MESSAGE = "is_own_message"

        /**
         * Create a new instance of DeleteConfirmationDialog
         * 
         * @param messageId The ID of the message to delete
         * @param isOwnMessage Whether the message was sent by the current user
         * @return New instance of DeleteConfirmationDialog
         */
        fun newInstance(messageId: String, isOwnMessage: Boolean): DeleteConfirmationDialog {
            val dialog = DeleteConfirmationDialog()
            val args = Bundle().apply {
                putString(ARG_MESSAGE_ID, messageId)
                putBoolean(ARG_IS_OWN_MESSAGE, isOwnMessage)
            }
            dialog.arguments = args
            return dialog
        }
    }

    private var _binding: DialogDeleteMessageBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MessageActionsViewModel

    private var messageId: String = ""
    private var isOwnMessage: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog)

        // Get arguments
        arguments?.let {
            messageId = it.getString(ARG_MESSAGE_ID, "")
            isOwnMessage = it.getBoolean(ARG_IS_OWN_MESSAGE, false)
        }

        // Initialize ViewModel
        viewModel = MessageActionsViewModel(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDeleteMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRadioButtons()
        setupButtons()
    }

    override fun onStart() {
        super.onStart()
        // Make dialog full width
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /**
     * Set up radio buttons with default selection and disable options based on message ownership
     */
    private fun setupRadioButtons() {
        // Set default selection to "Delete for me"
        binding.radioDeleteForMe.isChecked = true

        // Disable "Delete for everyone" option if message is from another user
        if (!isOwnMessage) {
            binding.radioDeleteForEveryone.isEnabled = false
            binding.deleteForEveryoneExplanation.isEnabled = false
            
            // Make the disabled option visually distinct
            binding.radioDeleteForEveryone.alpha = 0.5f
            binding.deleteForEveryoneExplanation.alpha = 0.5f
        }
    }

    /**
     * Set up button click listeners
     */
    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            deleteMessage()
        }
    }

    /**
     * Delete the message based on selected option
     */
    private fun deleteMessage() {
        // Get selected option from RadioGroup
        val deleteForEveryone = when (binding.deleteOptionsGroup.checkedRadioButtonId) {
            R.id.radio_delete_for_everyone -> true
            R.id.radio_delete_for_me -> false
            else -> false // Default to "Delete for me"
        }

        // Validate that user can delete for everyone
        if (deleteForEveryone && !isOwnMessage) {
            Toast.makeText(
                requireContext(),
                "You can only delete your own messages for everyone",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Disable buttons during deletion
        binding.btnDelete.isEnabled = false
        binding.btnCancel.isEnabled = false

        // Show loading indicator
        binding.btnDelete.text = getString(R.string.text_loading)
        Log.d(TAG, "Deleting message: $messageId (deleteForEveryone: $deleteForEveryone)")

        lifecycleScope.launch {
            viewModel.deleteMessage(messageId, deleteForEveryone)
                .collectLatest { state ->
                    when (state) {
                        is MessageActionsViewModel.MessageActionState.Loading -> {
                            // Loading state - show pulse animation on delete button
                            Log.d(TAG, "Deleting message...")
                            MessageAnimations.applyPendingActionAnimation(binding.btnDelete)
                        }
                        is MessageActionsViewModel.MessageActionState.Success -> {
                            // Clear animations
                            MessageAnimations.clearAnimations(binding.btnDelete)
                            
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
                            MessageAnimations.clearAnimations(binding.btnDelete)
                            
                            // Error - show error and re-enable buttons
                            Toast.makeText(
                                requireContext(),
                                state.error,
                                Toast.LENGTH_SHORT
                            ).show()
                            binding.btnDelete.isEnabled = true
                            binding.btnCancel.isEnabled = true
                            binding.btnDelete.text = getString(R.string.delete)
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
