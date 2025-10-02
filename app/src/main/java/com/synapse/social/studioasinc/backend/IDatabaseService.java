package com.synapse.social.studioasinc.backend;

import com.synapse.social.studioasinc.backend.interfaces.IDataListener;
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener;
import java.io.File;
import java.util.Map;

public interface IDatabaseService {
    IDatabaseReference getReference(String path);
    void getUserById(String uid, IDataListener listener);
    void setValue(String path, Object value, ICompletionListener listener);
    void updateChildren(String path, Map<String, Object> children, ICompletionListener listener);
    void uploadFile(File file, String path, ICompletionListener listener);
    void searchUsers(String query, IDataListener listener);
    void getFollowers(String uid, IDataListener listener);
    void getFollowing(String uid, IDataListener listener);
    void getData(String path, IDataListener listener);
}