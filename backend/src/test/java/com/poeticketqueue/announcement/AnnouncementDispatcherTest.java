package com.poeticketqueue.announcement;

import com.poeticketqueue.model.Group;
import com.poeticketqueue.poe.build.PoeVersion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The dispatcher is the facade the app calls. It resolves a group's per-group config to
 * the right provider impl, defaults to NoOp when unconfigured, runs the announcement off
 * the request path, and isolates every failure so the user action never breaks.
 */
class AnnouncementDispatcherTest {

    /** Runs submitted work inline so the test can assert synchronously. */
    private static final Executor DIRECT = Runnable::run;

    private Group groupWith(AnnouncementConfig config) {
        Group group = new Group("G", "CODE", PoeVersion.POE1, "Standard");
        group.setAnnouncementConfig(config);
        return group;
    }

    @Test
    void unconfiguredGroup_routesToNoOp_andNothingThrows() {
        CapturingAnnouncementService discord = new CapturingAnnouncementService(AnnouncementProvider.DISCORD);
        AnnouncementDispatcher dispatcher = new AnnouncementDispatcher(
                List.of(new NoOpAnnouncementService(), discord), DIRECT);

        // No announcement config at all -> NoOp; the Discord impl is never touched.
        Group group = groupWith(null);
        assertThatCode(() -> dispatcher.announce(group, AnnouncementEvent.queued("Ring", "Alice")))
                .doesNotThrowAnyException();
        assertThat(discord.events).isEmpty();
    }

    @Test
    void providerNone_routesToNoOp() {
        CapturingAnnouncementService discord = new CapturingAnnouncementService(AnnouncementProvider.DISCORD);
        AnnouncementDispatcher dispatcher = new AnnouncementDispatcher(
                List.of(new NoOpAnnouncementService(), discord), DIRECT);

        AnnouncementConfig none = new AnnouncementConfig();
        none.setProvider(AnnouncementProvider.NONE);

        dispatcher.announce(groupWith(none), AnnouncementEvent.purchased("Ring"));
        assertThat(discord.events).isEmpty();
    }

    @Test
    void discordConfig_routesToDiscordImpl_withThatGroupsConfig() {
        CapturingAnnouncementService discord = new CapturingAnnouncementService(AnnouncementProvider.DISCORD);
        AnnouncementDispatcher dispatcher = new AnnouncementDispatcher(
                List.of(new NoOpAnnouncementService(), discord), DIRECT);

        AnnouncementConfig config = new AnnouncementConfig();
        config.setProvider(AnnouncementProvider.DISCORD);
        config.setDiscordBotToken("bot-token");
        config.setDiscordChannelId("123");

        dispatcher.announce(groupWith(config), AnnouncementEvent.rejected("Ring"));

        assertThat(discord.events).hasSize(1);
        assertThat(discord.events.get(0).type()).isEqualTo(AnnouncementType.REJECTED);
        assertThat(discord.configs.get(0).getDiscordChannelId()).isEqualTo("123");
    }

    @Test
    void failingAnnouncer_isIsolated_dispatchDoesNotThrow() {
        CapturingAnnouncementService boom = new CapturingAnnouncementService(AnnouncementProvider.DISCORD, true);
        AnnouncementDispatcher dispatcher = new AnnouncementDispatcher(
                List.of(new NoOpAnnouncementService(), boom), DIRECT);

        AnnouncementConfig config = new AnnouncementConfig();
        config.setProvider(AnnouncementProvider.DISCORD);

        // The provider throws, but the dispatcher must swallow+log so the caller is unaffected.
        assertThatCode(() -> dispatcher.announce(groupWith(config), AnnouncementEvent.queued("Ring", "Alice")))
                .doesNotThrowAnyException();
        assertThat(boom.events).hasSize(1);
    }
}
