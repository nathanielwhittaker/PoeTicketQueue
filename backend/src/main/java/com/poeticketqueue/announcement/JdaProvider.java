package com.poeticketqueue.announcement;

import net.dv8tion.jda.api.JDA;

/**
 * Resolves the shared {@link JDA} gateway connection for a given bot token.
 */
public interface JdaProvider {

    JDA get(String botToken);
}
