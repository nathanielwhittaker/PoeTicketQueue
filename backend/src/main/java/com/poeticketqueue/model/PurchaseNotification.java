package com.poeticketqueue.model;

import java.util.UUID;

public class PurchaseNotification {

    private final String id;
    private final String targetScreenName;
    private final String itemLabel;
    private final long timestamp;

    public PurchaseNotification(String targetScreenName, String itemLabel) {
        this.id = UUID.randomUUID().toString();
        this.targetScreenName = targetScreenName;
        this.itemLabel = itemLabel;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getTargetScreenName() {
        return targetScreenName;
    }

    public String getItemLabel() {
        return itemLabel;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
