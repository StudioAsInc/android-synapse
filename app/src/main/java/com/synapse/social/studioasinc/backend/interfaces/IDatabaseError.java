package com.synapse.social.studioasinc.backend.interfaces;

public interface IDatabaseError {
    int getCode();
    String getMessage();
    String getDetails();
}