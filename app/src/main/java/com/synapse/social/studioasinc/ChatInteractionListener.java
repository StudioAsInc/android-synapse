// To-do: Migrate Firebase to Supabase
// 1. **No direct Firebase dependencies in this file.** This is an interface.
// 2. **Review Implementing Classes**: The classes that implement this interface (e.g., `ChatActivity`) will be the ones that need refactoring.
// 3. **Data Model**: The `HashMap<String, Object> messageData` parameter in `onDeleteMessage` is based on the Firebase data structure.
//    - This should be replaced with a type-safe data class (e.g., `Message.kt`) that represents the data from a Supabase table.
//    - This change will propagate to all implementing classes, ensuring a consistent and more robust data model throughout the application.

package com.synapse.social.studioasinc;

import java.util.HashMap;

public interface ChatInteractionListener {
    void onReplySelected(String messageId);
    void onDeleteMessage(HashMap<String, Object> messageData);
}