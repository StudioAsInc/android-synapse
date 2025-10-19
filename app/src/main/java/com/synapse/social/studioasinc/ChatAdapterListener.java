package com.synapse.social.studioasinc;

import android.view.View;
import com.synapse.social.studioasinc.chat.model.ChatMessage;
import java.util.ArrayList;
import java.util.HashMap;

public interface ChatAdapterListener {
    void scrollToMessage(String messageId);
    void performHapticFeedback();
    void showMessageOverviewPopup(View anchor, int position, ArrayList<ChatMessage> data);
    void openUrl(String url);
    String getRecipientUid();
}
