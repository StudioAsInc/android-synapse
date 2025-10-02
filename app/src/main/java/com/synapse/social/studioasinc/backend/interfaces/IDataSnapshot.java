package com.synapse.social.studioasinc.backend.interfaces;

import java.util.Map;

public interface IDataSnapshot {
    boolean exists();
    boolean hasChildren();
    Object getValue();
    <T> T getValue(Class<T> valueType);
    IDataSnapshot getChild(String path);
    Iterable<IDataSnapshot> getChildren();
    String getKey();
}