package com.poeticketqueue.model;

public class Member {

    private String screenName;
    private GroupRole role;

    public Member(String screenName) {
        this(screenName, GroupRole.MEMBER);
    }

    public Member(String screenName, GroupRole role) {
        this.screenName = screenName;
        this.role = role;
    }

    public String getScreenName() { return screenName; }
    public void setScreenName(String screenName) { this.screenName = screenName; }
    public GroupRole getRole() { return role; }
    public void setRole(GroupRole role) { this.role = role; }
}
