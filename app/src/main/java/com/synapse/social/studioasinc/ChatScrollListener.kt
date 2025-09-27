package com.synapse.social.studioasinc

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatScrollListener(
    private val activity: ChatActivity,
    private val layoutManager: LinearLayoutManager
) : RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        if (dy < 0) { //check for scroll up
            if (layoutManager.findFirstVisibleItemPosition() <= 2) {
                val oldestMessageKey = activity.getOldestMessageKey()
                if (!activity.isLoading() && oldestMessageKey != null && oldestMessageKey.isNotEmpty() && oldestMessageKey != "null") {
                    activity._getOldChatMessagesRef()
                }
            }
        }
    }
}