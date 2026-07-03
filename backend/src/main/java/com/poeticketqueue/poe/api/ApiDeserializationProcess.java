package com.poeticketqueue.poe.api;

import java.util.List;

public interface ApiDeserializationProcess<T> {
    void deserializeIntoJavaObject(List<T> toAddTo, Entry e, Result r);
}
