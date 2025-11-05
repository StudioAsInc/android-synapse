package com.synapse.social.studioasinc.chat

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.repository.MessageActionRepository
import com.synapse.social.studioasinc.model.MessageEdit
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Dialog fragment that displays the edit history of a message
 * Shows all previous versions of the message with timestamps
 */
class EditHistoryDialog : DialogFragment() {

    companion object {
        private const val TAG = "EditHistoryDialog"
        private const val ARG_MESSAGE_ID = "message_id"

        /**
         * Create a new instance of EditHistoryDialog
         * @param messageId The ID of the message to show edit history for
         */
        fun newInstance(messageId: String): EditHistoryDialog {
            return EditHistoryDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_MESSAGE_ID, messageId)
                }
            }
        }
    }

    private lateinit var repository: MessageActionRepository
    private lateinit var rvEditHistory: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var btnClose: Button
    private lateinit var adapter: EditHistoryAdapter

    private val messageId: String
        get() = arguments?.getString(ARG_MESSAGE_ID) ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = MessageActionRepository(requireContext())
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        rvEditHistory = view.findViewById(R.id.rv_edit_history)
        tvEmptyState = view.findViewById(R.id.tv_empty_state)
        btnClose = view.findViewById(R.id.btn_close)

        // Set up RecyclerView
        adapter = EditHistoryAdapter()
        rvEditHistory.layoutManager = LinearLayoutManager(requireContext())
        rvEditHistory.adapter = adapter

        // Set up close button
        btnClose.setOnClickListener {
            dismiss()
        }

        // Load edit history
        loadEditHistory()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return dialog
    }

    /**
     * Load edit history from repository
     */
    private fun loadEditHistory() {
        lifecycleScope.launch {
            try {
                val result = repository.getEditHistory(messageId)

                if (result.isSuccess) {
                    val editHistory = result.getOrNull() ?: emptyList()

                    if (editHistory.isEmpty()) {
                        showEmptyState()
                    } else {
                        // Sort by newest first
                        val sortedHistory = editHistory.sortedByDescending { it.editedAt }
                        showEditHistory(sortedHistory)
                    }
                } else {
                    Log.e(TAG, "Failed to load edit history", result.exceptionOrNull())
                    showEmptyState()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading edit history", e)
                showEmptyState()
            }
        }
    }

    /**
     * Show edit history in RecyclerView
     */
    private fun showEditHistory(editHistory: List<MessageEdit>) {
        rvEditHistory.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        adapter.submitList(editHistory)
    }

    /**
     * Show empty state message
     */
    private fun showEmptyState() {
        rvEditHistory.visibility = View.GONE
        tvEmptyState.visibility = View.VISIBLE
    }

    /**
     * RecyclerView adapter for edit history items
     */
    private inner class EditHistoryAdapter : RecyclerView.Adapter<EditHistoryViewHolder>() {

        private val items = mutableListOf<MessageEdit>()

        fun submitList(newItems: List<MessageEdit>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditHistoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_edit_history, parent, false)
            return EditHistoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: EditHistoryViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    /**
     * ViewHolder for edit history items
     */
    private inner class EditHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
        private val tvPreviousContent: TextView = itemView.findViewById(R.id.tv_previous_content)
        private val divider: View = itemView.findViewById(R.id.divider)

        fun bind(edit: MessageEdit) {
            // Format timestamp as relative time
            tvTimestamp.text = getRelativeTimeString(edit.editedAt)

            // Set previous content
            tvPreviousContent.text = edit.previousContent

            // Hide divider for last item
            divider.visibility = if (bindingAdapterPosition == adapter.itemCount - 1) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        /**
         * Convert timestamp to relative time string (e.g., "2 hours ago")
         */
        private fun getRelativeTimeString(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "$hours ${if (hours == 1L) "hour" else "hours"} ago"
                }
                diff < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "$days ${if (days == 1L) "day" else "days"} ago"
                }
                else -> {
                    // Format as date for older edits
                    val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                    dateFormat.format(Date(timestamp))
                }
            }
        }
    }
}
