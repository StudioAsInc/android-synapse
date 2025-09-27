package com.synapse.social.studioasinc;

import java.util.HashMap;

public interface ChatInteractionListener {
    void onReplySelected(String messageId);
    void onDeleteMessage(HashMap<String, Object> messageData);
}