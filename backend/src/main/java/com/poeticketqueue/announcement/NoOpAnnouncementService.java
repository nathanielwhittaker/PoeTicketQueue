package com.poeticketqueue.announcement;

import org.springframework.stereotype.Service;

/**
 * Default announcement backend for groups with no provider configured. Does nothing.
 */
@Service
public class NoOpAnnouncementService implements AnnouncementService {

    @Override
    public AnnouncementProvider provider() {
        return AnnouncementProvider.NONE;
    }

    @Override
    public void announce(AnnouncementConfig config, AnnouncementEvent event) {
        // No-op by design.
    }
}
