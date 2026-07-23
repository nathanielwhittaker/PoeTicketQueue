package com.poeticketqueue.announcement;

import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Maintains one JDA gateway connection per bot token, lazily created on first use and reused
 * across every subsequent announcement for that token. Building/connecting is blocking network
 * I/O, so it always happens outside the cache's lock and is bounded by a readiness timeout --
 * a token whose gateway never becomes ready cannot wedge the caller (the sole dispatcher
 * thread), and nothing not-ready is ever cached. Connections are shut down when the application
 * stops.
 */
@Component
public class CachingJdaProvider implements JdaProvider {

    private static final Logger log = LoggerFactory.getLogger(CachingJdaProvider.class);
    private static final long READY_TIMEOUT_SECONDS = 10;

    private final ConcurrentHashMap<String, JDA> cache = new ConcurrentHashMap<>();
    private final Object buildLock = new Object();
    private final Function<String, JDA> builder;

    public CachingJdaProvider() {
        this(CachingJdaProvider::buildRealJda);
    }

    public CachingJdaProvider(Function<String, JDA> builder) {
        this.builder = builder;
    }

    @Override
    public JDA get(String botToken) {
        JDA cached = cache.get(botToken);
        if (cached != null) {
            return cached;
        }
        synchronized (buildLock) {
            cached = cache.get(botToken);
            if (cached != null) {
                return cached;
            }
            JDA built = builder.apply(botToken);
            cache.put(botToken, built);
            return built;
        }
    }

    private static JDA buildRealJda(String token) {
        JDA jda = JDABuilder.createLight(token).build();
        try {
            CompletableFuture.supplyAsync(() -> {
                try {
                    return jda.awaitReady();
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                }
            }).get(READY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return jda;
        } catch (TimeoutException e) {
            shutdownQuietly(jda);
            throw new RuntimeException("Timed out waiting for Discord gateway to become ready", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            shutdownQuietly(jda);
            throw new RuntimeException("Interrupted while waiting for Discord gateway to become ready", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            shutdownQuietly(jda);
            throw new RuntimeException("Failed while waiting for Discord gateway to become ready", e.getCause());
        }
    }

    private static void shutdownQuietly(JDA jda) {
        try {
            jda.shutdownNow();
        } catch (Exception e) {
            log.warn("Failed to shut down a JDA instance that failed to become ready", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        cache.values().forEach(jda -> {
            try {
                jda.shutdown();
            } catch (Exception e) {
                log.warn("Failed to shut down a cached JDA instance", e);
            }
        });
    }
}
