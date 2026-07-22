package com.poeticketqueue.announcement;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Per-group announcement configuration. The bot token is a secret and must never be
 * serialized back to a client -- {@link #getDiscordBotToken()} is write-only via
 * {@link JsonIgnore}.
 */
public class AnnouncementConfig {

    private AnnouncementProvider provider = AnnouncementProvider.NONE;
    private String discordBotToken;
    private String discordChannelId;

    public AnnouncementProvider getProvider() {
        return provider;
    }

    public void setProvider(AnnouncementProvider provider) {
        this.provider = provider;
    }

    @JsonIgnore
    public String getDiscordBotToken() {
        return discordBotToken;
    }

    public void setDiscordBotToken(String discordBotToken) {
        this.discordBotToken = discordBotToken;
    }

    public String getDiscordChannelId() {
        return discordChannelId;
    }

    public void setDiscordChannelId(String discordChannelId) {
        this.discordChannelId = discordChannelId;
    }
}
