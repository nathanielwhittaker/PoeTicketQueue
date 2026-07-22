package com.poeticketqueue.model;

import com.poeticketqueue.poe.build.PoeVersion;
import com.poeticketqueue.poe.item.Item;

import java.util.ArrayList;
import java.util.List;

public class QueuedBuild {

    private final String name;
    private final String url;
    private final PoeVersion poeVersion;
    private final List<Item> items;
    private String queuedBy;

    public QueuedBuild(String name, String url, PoeVersion poeVersion, List<Item> items) {
        this.name = name;
        this.url = url;
        this.poeVersion = poeVersion;
        this.items = new ArrayList<>(items);
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public PoeVersion getPoeVersion() {
        return poeVersion;
    }

    public List<Item> getItems() {
        return items;
    }

    public String getQueuedBy() {
        return queuedBy;
    }

    public void setQueuedBy(String queuedBy) {
        this.queuedBy = queuedBy;
    }
}
