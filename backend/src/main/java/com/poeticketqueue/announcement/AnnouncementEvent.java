package com.poeticketqueue.announcement;

/**
 * A single provider-neutral event to announce. {@code actorScreenName} is only meaningful for
 * {@link AnnouncementType#QUEUED} (who queued the item); it may be blank/null when unknown.
 */
public record AnnouncementEvent(AnnouncementType type, String itemLabel, String actorScreenName) {

    public static AnnouncementEvent queued(String itemLabel, String actorScreenName) {
        return new AnnouncementEvent(AnnouncementType.QUEUED, itemLabel, actorScreenName);
    }

    public static AnnouncementEvent rejected(String itemLabel) {
        return new AnnouncementEvent(AnnouncementType.REJECTED, itemLabel, null);
    }

    public static AnnouncementEvent purchased(String itemLabel) {
        return new AnnouncementEvent(AnnouncementType.PURCHASED, itemLabel, null);
    }
}
