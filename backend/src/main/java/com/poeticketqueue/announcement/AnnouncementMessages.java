package com.poeticketqueue.announcement;

import org.apache.commons.lang3.StringUtils;

/**
 * Renders the provider-neutral text sent for an {@link AnnouncementEvent}. Every impl sends
 * exactly this text so the wording is consistent across providers.
 */
public final class AnnouncementMessages {

    private AnnouncementMessages() {
    }

    public static String render(AnnouncementEvent event) {
        return switch (event.type()) {
            case QUEUED -> StringUtils.isNotBlank(event.actorScreenName())
                    ? event.actorScreenName() + " queued " + event.itemLabel()
                    : event.itemLabel() + " was queued";
            case REJECTED -> event.itemLabel() + " was rejected";
            case PURCHASED -> event.itemLabel() + " was purchased";
        };
    }
}
