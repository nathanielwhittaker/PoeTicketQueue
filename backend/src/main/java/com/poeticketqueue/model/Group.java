package com.poeticketqueue.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.poeticketqueue.announcement.AnnouncementConfig;
import com.poeticketqueue.poe.build.PoeVersion;
import com.poeticketqueue.poe.item.Item;

import java.util.ArrayList;
import java.util.List;

public class Group {

    private String name;
    private String code;
    private PoeVersion poeVersion;
    private String league;
    private final List<Item> itemQueue = new ArrayList<>();
    private final List<QueuedBuild> buildQueue = new ArrayList<>();
    private final List<Member> members = new ArrayList<>();
    private final List<PurchaseNotification> purchaseNotifications = new ArrayList<>();
    private AnnouncementConfig announcementConfig;

    public Group(String name, String code, PoeVersion poeVersion, String league) {
        this.name = name;
        this.code = code;
        this.poeVersion = poeVersion;
        this.league = league;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public PoeVersion getPoeVersion() {
        return poeVersion;
    }

    public String getLeague() {
        return league;
    }

    public List<Item> getItemQueue() {
        return itemQueue;
    }

    public List<QueuedBuild> getBuildQueue() {
        return buildQueue;
    }

    public List<Member> getMembers() {
        return members;
    }

    @JsonIgnore
    public List<PurchaseNotification> getPurchaseNotifications() {
        return purchaseNotifications;
    }

    @JsonIgnore
    public AnnouncementConfig getAnnouncementConfig() {
        return announcementConfig;
    }

    public void setAnnouncementConfig(AnnouncementConfig announcementConfig) {
        this.announcementConfig = announcementConfig;
    }
}
