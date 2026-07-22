package com.poeticketqueue.announcement;

import java.util.ArrayList;
import java.util.List;

/**
 * Test double that records every event it is asked to announce. Optionally throws to
 * simulate a failing third-party announcer (used to prove announcement failures are
 * isolated and never break the user action).
 */
public class CapturingAnnouncementService implements AnnouncementService {

    private final AnnouncementProvider provider;
    private final boolean throwOnAnnounce;
    public final List<AnnouncementEvent> events = new ArrayList<>();
    public final List<AnnouncementConfig> configs = new ArrayList<>();

    public CapturingAnnouncementService(AnnouncementProvider provider) {
        this(provider, false);
    }

    public CapturingAnnouncementService(AnnouncementProvider provider, boolean throwOnAnnounce) {
        this.provider = provider;
        this.throwOnAnnounce = throwOnAnnounce;
    }

    @Override
    public AnnouncementProvider provider() {
        return provider;
    }

    @Override
    public void announce(AnnouncementConfig config, AnnouncementEvent event) {
        configs.add(config);
        events.add(event);
        if (throwOnAnnounce) {
            throw new RuntimeException("simulated announcer failure");
        }
    }
}
