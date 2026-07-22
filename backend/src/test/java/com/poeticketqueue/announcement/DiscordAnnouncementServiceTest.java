package com.poeticketqueue.announcement;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises the JDA-backed impl entirely behind mocks -- NO real gateway connection or
 * network is opened. Proves the rendered text is sent to the group's configured channel,
 * that a send failure is isolated, and that the JDA instance is cached/reused per token.
 */
class DiscordAnnouncementServiceTest {

    private AnnouncementConfig config(String token, String channelId) {
        AnnouncementConfig c = new AnnouncementConfig();
        c.setProvider(AnnouncementProvider.DISCORD);
        c.setDiscordBotToken(token);
        c.setDiscordChannelId(channelId);
        return c;
    }

    @Test
    void reportsDiscordProvider() {
        DiscordAnnouncementService svc = new DiscordAnnouncementService(token -> mock(JDA.class));
        assertThat(svc.provider()).isEqualTo(AnnouncementProvider.DISCORD);
    }

    @Test
    void announce_sendsRenderedTextToConfiguredChannel() {
        JDA jda = mock(JDA.class);
        TextChannel channel = mock(TextChannel.class);
        MessageCreateAction action = mock(MessageCreateAction.class);
        when(jda.getTextChannelById("123")).thenReturn(channel);
        when(channel.sendMessage(anyString())).thenReturn(action);
        when(action.setAllowedMentions(any())).thenReturn(action);

        DiscordAnnouncementService svc = new DiscordAnnouncementService(token -> jda);
        svc.announce(config("bot-token", "123"), AnnouncementEvent.purchased("Precious Ring"));

        ArgumentCaptor<CharSequence> sent = ArgumentCaptor.forClass(CharSequence.class);
        verify(channel).sendMessage(sent.capture());
        assertThat(sent.getValue().toString()).isEqualTo("Precious Ring was purchased");

        // Security: user-controlled item labels / screen names must NOT be able to @everyone/@here/@role.
        // The send must suppress all mentions.
        verify(action).setAllowedMentions(argThat(Collection::isEmpty));
    }

    @Test
    void announce_unknownChannel_isIsolated() {
        JDA jda = mock(JDA.class);
        when(jda.getTextChannelById(anyString())).thenReturn(null);

        DiscordAnnouncementService svc = new DiscordAnnouncementService(token -> jda);
        assertThatCode(() -> svc.announce(config("bot-token", "nope"), AnnouncementEvent.queued("Ring", "Alice")))
                .doesNotThrowAnyException();
    }

    @Test
    void announce_sendFailure_isIsolated() {
        JDA jda = mock(JDA.class);
        TextChannel channel = mock(TextChannel.class);
        when(jda.getTextChannelById(anyString())).thenReturn(channel);
        when(channel.sendMessage(anyString())).thenThrow(new RuntimeException("gateway down"));

        DiscordAnnouncementService svc = new DiscordAnnouncementService(token -> jda);
        assertThatCode(() -> svc.announce(config("bot-token", "123"), AnnouncementEvent.rejected("Ring")))
                .doesNotThrowAnyException();
    }

    @Test
    void cachingJdaProvider_buildsOncePerToken_andReuses() {
        AtomicInteger builds = new AtomicInteger();
        Function<String, JDA> builder = token -> {
            builds.incrementAndGet();
            return mock(JDA.class);
        };
        CachingJdaProvider provider = new CachingJdaProvider(builder);

        JDA first = provider.get("token-A");
        JDA second = provider.get("token-A");
        provider.get("token-B");

        assertThat(first).isSameAs(second);      // reused for the same token
        assertThat(builds.get()).isEqualTo(2);   // one build per distinct token
    }
}
