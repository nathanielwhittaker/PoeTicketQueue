package com.poeticketqueue.announcement;

import com.poeticketqueue.model.Group;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Facade the app calls to announce a group event. Resolves the group's configured provider to
 * the matching {@link AnnouncementService} (defaulting to {@link NoOpAnnouncementService} when
 * unconfigured), and runs the announcement off the request path, isolating any failure so it
 * never affects the caller.
 */
@Service
public class AnnouncementDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementDispatcher.class);

    private final Map<AnnouncementProvider, AnnouncementService> servicesByProvider;
    private final Executor executor;
    private final ExecutorService ownedExecutor;

    @Autowired
    public AnnouncementDispatcher(List<AnnouncementService> services) {
        this(services, newDaemonExecutor());
    }

    public AnnouncementDispatcher(List<AnnouncementService> services, Executor executor) {
        this.servicesByProvider = services.stream()
                .collect(Collectors.toMap(AnnouncementService::provider, Function.identity()));
        this.executor = executor;
        this.ownedExecutor = (executor instanceof ExecutorService service) ? service : null;
    }

    public void announce(Group group, AnnouncementEvent event) {
        AnnouncementConfig config = group.getAnnouncementConfig();
        AnnouncementProvider provider = config == null ? AnnouncementProvider.NONE : config.getProvider();
        AnnouncementService service = servicesByProvider.getOrDefault(provider, new NoOpAnnouncementService());
        try {
            executor.execute(() -> {
                try {
                    service.announce(config, event);
                } catch (Exception e) {
                    log.warn("Announcement dispatch failed", e);
                }
            });
        } catch (Exception e) {
            log.warn("Announcement submission failed", e);
        }
    }

    public static AnnouncementDispatcher noOp() {
        return new AnnouncementDispatcher(List.of(new NoOpAnnouncementService()), Runnable::run);
    }

    private static ExecutorService newDaemonExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "announcement-dispatcher");
            thread.setDaemon(true);
            return thread;
        });
    }

    @PreDestroy
    public void shutdown() {
        if (ownedExecutor != null) {
            ownedExecutor.shutdown();
        }
    }
}
