package com.synapse.social.studioasinc.backend.interfaces;

public interface IDataListener {
    void onDataReceived(IDataSnapshot dataSnapshot);
    void onCancelled(IDatabaseError databaseError);
}