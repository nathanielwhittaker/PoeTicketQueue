package com.poeticketqueue.announcement;

/**
 * A pluggable per-group announcement backend. Each provider (Discord, etc.) implements this
 * and is resolved by {@link AnnouncementDispatcher} from a group's {@link AnnouncementConfig}.
 */
public interface AnnouncementService {

    AnnouncementProvider provider();

    void announce(AnnouncementConfig config, AnnouncementEvent event);
}
