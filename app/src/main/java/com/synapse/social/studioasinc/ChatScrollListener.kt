// To-do: Migrate Firebase to Supabase
// This class is responsible for triggering the loading of older messages when the user scrolls up.
// 1. **No direct Firebase dependencies in this file.**
// 2. **Refactor Pagination Logic**:
//    - The listener calls `activity._getOldChatMessagesRef()`, which is a method in `ChatActivity` that fetches older messages from Firebase.
//    - This call will need to be replaced with a call to a new method that handles pagination with Supabase.
//    - The new method should query the Supabase `messages` table using `range()` and `order()` to fetch the next page of messages.
// 3. **Decouple from Activity**:
//    - For better architecture, consider using an interface or a ViewModel to decouple this listener from the `ChatActivity` directly.
//    - The listener could simply call a method like `viewModel.loadMoreMessages()`.

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