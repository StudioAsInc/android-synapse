// To-do: Migrate Firebase to Supabase
// 1. **No direct Firebase dependencies in this file.** This is an interface.
// 2. **Review Implementing Classes**: The classes that implement this interface (e.g., `ChatActivity`) will need significant refactoring.
// 3. **Data Model**: The `ArrayList<HashMap<String, Object>> data` parameter in `showMessageOverviewPopup` is based on the Firebase data structure. This will need to be updated to use a proper data class (e.g., `Message.kt`) that represents data from Supabase.
// 4. **User ID**: The `getRecipientUid()` method is based on Firebase's user ID system. This will need to be consistent with the user ID system in Supabase.

package com.synapse.social.studioasinc;

import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;

public interface ChatAdapterListener {
    void scrollToMessage(String messageId);
    void performHapticFeedback();
    void showMessageOverviewPopup(View anchor, int position, ArrayList<HashMap<String, Object>> data);
    void openUrl(String url);
    String getRecipientUid();
}
