package com.poeticketqueue.announcement;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Announces group events to a Discord text channel via JDA. Every failure is isolated here so
 * a Discord/network problem never breaks the user action that triggered the announcement. The
 * bot token is never logged.
 */
@Service
public class DiscordAnnouncementService implements AnnouncementService {

    private static final Logger log = LoggerFactory.getLogger(DiscordAnnouncementService.class);

    private final JdaProvider jdaProvider;

    public DiscordAnnouncementService(JdaProvider jdaProvider) {
        this.jdaProvider = jdaProvider;
    }

    @Override
    public AnnouncementProvider provider() {
        return AnnouncementProvider.DISCORD;
    }

    @Override
    public void announce(AnnouncementConfig config, AnnouncementEvent event) {
        try {
            JDA jda = jdaProvider.get(config.getDiscordBotToken());
            TextChannel channel = jda.getTextChannelById(config.getDiscordChannelId());
            if (channel == null) {
                log.warn("Discord announce failed -- unknown channel id for group");
                return;
            }
            channel.sendMessage(AnnouncementMessages.render(event))
                    .setAllowedMentions(java.util.Collections.emptyList())
                    .queue(null, err -> log.warn("Discord announce send failed", err));
        } catch (Exception e) {
            log.warn("Discord announce failed", e);
        }
    }
}
