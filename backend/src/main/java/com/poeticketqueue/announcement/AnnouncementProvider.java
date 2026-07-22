package com.poeticketqueue.announcement;

/**
 * The third-party service a group's announcements are sent to. NONE disables announcements
 * entirely, routing every event to the {@link NoOpAnnouncementService}.
 */
public enum AnnouncementProvider {
    NONE,
    DISCORD
}
