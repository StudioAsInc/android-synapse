package com.synapse.social.studioasinc;

import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;

public interface ChatAdapterListener {
    void scrollToMessage(String messageId);
    void performHapticFeedback();
    void showMessageOverviewPopup(View anchor, ChatMessage message);
    void openUrl(String url);
    String getRecipientUid();
}
