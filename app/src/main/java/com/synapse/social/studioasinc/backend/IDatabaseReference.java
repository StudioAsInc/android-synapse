package com.synapse.social.studioasinc.backend;

public interface IDatabaseReference {
    IDatabaseReference child(String s);
    IDatabaseReference push();
    String getKey();
}