package com.synapse.social.studioasinc.chat

import android.view.View
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.synapse.social.studioasinc.ChatActivity
import com.synapse.social.studioasinc.ChatAdapter
import com.synapse.social.studioasinc.R

/**
 * Manages multi-select mode for messages in chat
 * Handles selection state tracking and action toolbar lifecycle
 */
class MultiSelectManager(
    private val activity: ChatActivity,
    private val adapter: ChatAdapter
) {
    // Selection state
    private val selectedMessageIds = mutableSetOf<String>()
    var isMultiSelectMode = false
        private set
    
    // Toolbar references
    private var actionToolbar: MaterialToolbar? = null
    private var standardToolbar: MaterialToolbar? = null
    private var selectionCountText: TextView? = null
    
    /**
     * Enter multi-select mode with an initial message selected
     * @param initialMessageId The ID of the first message to select
     */
    fun enterMultiSelectMode(initialMessageId: String) {
        if (isMultiSelectMode) return
        
        isMultiSelectMode = true
        selectedMessageIds.clear()
        selectedMessageIds.add(initialMessageId)
        
        // Show action toolbar
        showActionToolbar()
        
        // Update adapter to show selection indicators
        adapter.notifyDataSetChanged()
    }
    
    /**
     * Exit multi-select mode and clear all selections
     */
    fun exitMultiSelectMode() {
        if (!isMultiSelectMode) return
        
        isMultiSelectMode = false
        selectedMessageIds.clear()
        
        // Hide action toolbar
        hideActionToolbar()
        
        // Update adapter to hide selection indicators
        adapter.notifyDataSetChanged()
    }
    
    /**
     * Toggle selection state of a message
     * @param messageId The ID of the message to toggle
     */
    fun toggleMessageSelection(messageId: String) {
        if (!isMultiSelectMode) return
        
        if (selectedMessageIds.contains(messageId)) {
            selectedMessageIds.remove(messageId)
            
            // Exit multi-select mode if no messages are selected
            if (selectedMessageIds.isEmpty()) {
                exitMultiSelectMode()
                return
            }
        } else {
            selectedMessageIds.add(messageId)
        }
        
        // Update action toolbar title with new count
        updateActionToolbarTitle()
        
        // Notify adapter to update the specific message
        adapter.notifyDataSetChanged()
    }
    
    /**
     * Check if a message is currently selected
     * @param messageId The ID of the message to check
     * @return true if the message is selected, false otherwise
     */
    fun isMessageSelected(messageId: String): Boolean {
        return selectedMessageIds.contains(messageId)
    }
    
    /**
     * Get list of all selected message IDs
     * @return List of selected message IDs
     */
    fun getSelectedMessages(): List<String> {
        return selectedMessageIds.toList()
    }
    
    /**
     * Get the count of selected messages
     * @return Number of selected messages
     */
    fun getSelectionCount(): Int {
        return selectedMessageIds.size
    }
    
    /**
     * Show the action toolbar and hide the standard toolbar
     */
    private fun showActionToolbar() {
        // Find toolbars if not already cached
        if (standardToolbar == null) {
            standardToolbar = activity.findViewById(R.id.toolbar)
        }
        if (actionToolbar == null) {
            actionToolbar = activity.findViewById(R.id.action_toolbar)
            
            // Setup navigation icon click listener (close button)
            actionToolbar?.setNavigationOnClickListener {
                exitMultiSelectMode()
            }
            
            // Setup menu item click listeners
            actionToolbar?.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_delete -> {
                        onDeleteActionClicked()
                        true
                    }
                    else -> false
                }
            }
        }
        if (selectionCountText == null) {
            selectionCountText = activity.findViewById(R.id.selection_count_text)
        }
        
        // Hide standard toolbar and show action toolbar
        standardToolbar?.visibility = View.GONE
        actionToolbar?.visibility = View.VISIBLE
        
        // Update title with initial count
        updateActionToolbarTitle()
    }
    
    /**
     * Callback for when delete action is clicked
     * This will be overridden by the activity to handle deletion
     */
    var onDeleteActionClicked: () -> Unit = {}
    
    /**
     * Hide the action toolbar and show the standard toolbar
     */
    private fun hideActionToolbar() {
        // Show standard toolbar and hide action toolbar
        standardToolbar?.visibility = View.VISIBLE
        actionToolbar?.visibility = View.GONE
    }
    
    /**
     * Update the action toolbar title to show selection count
     */
    private fun updateActionToolbarTitle() {
        val count = selectedMessageIds.size
        selectionCountText?.text = count.toString()
    }
}
