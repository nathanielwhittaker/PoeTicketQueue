package com.poeticketqueue.poe.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class PropertiesManagerCore {

    private static final Logger log = LoggerFactory.getLogger(PropertiesManagerCore.class);

    private static final Properties apiProperties       = new Properties();
    private static final Properties itemParsingProperties = new Properties();
    private static final Properties runeStatsProperties  = new Properties();
    private static final Properties userConfigProperties = new Properties();
    private static final Properties sessionOverrides     = new Properties();
    private static Set<String> uniqueItemWhitelist = Set.of();

    static {
        loadResource(apiProperties,          "PathOfExileApiQuery.properties");
        loadResource(itemParsingProperties,  "ItemParsing.properties");
        loadResource(runeStatsProperties,    "RuneStats.properties");
        loadResource(userConfigProperties,   "BaseUserConfig.properties");
        uniqueItemWhitelist = loadUniqueItemWhitelist();
    }

    private static void loadResource(Properties target, String filename) {
        String path = "/poe/" + filename;
        try (InputStream is = PropertiesManagerCore.class.getResourceAsStream(path)) {
            if (is == null) {
                log.warn("Resource not found: {}", path);
                return;
            }
            target.load(is);
        } catch (IOException e) {
            log.error("Failed to load resource: {}", path, e);
            throw new RuntimeException(e);
        }
    }

    private static Set<String> loadUniqueItemWhitelist() {
        try (InputStream is = PropertiesManagerCore.class.getResourceAsStream("/poe/UniqueItems.txt")) {
            if (is == null) return Set.of();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .collect(Collectors.toUnmodifiableSet());
            }
        } catch (IOException e) {
            log.error("Failed to read UniqueItems.txt", e);
            return Set.of();
        }
    }

    public static void setSessionOverride(String key, String value) {
        if (value == null || value.isBlank()) {
            sessionOverrides.remove(key);
        } else {
            sessionOverrides.setProperty(key, value);
        }
    }

    private static String getEffective(String key) {
        String session = sessionOverrides.getProperty(key);
        if (session != null && !session.isBlank()) return session;
        return userConfigProperties.getProperty(key);
    }

    public static String getPoeSessId()             { return getEffective("POESESSID"); }
    public static String getLeague()                { return getEffective("league"); }
    public static String getLeaguePoe2()            { return getEffective("leaguePoE2"); }

    public static String getStatsApiLink()          { return apiProperties.getProperty("statsApiLink"); }
    public static String getStatsApiLinkPoe2()      { return apiProperties.getProperty("statsApiLinkPoE2"); }
    public static String getItemsApiLink()          { return apiProperties.getProperty("itemsApiLink"); }
    public static String getItemsApiLinkPoe2()      { return apiProperties.getProperty("itemsApiLinkPoE2"); }
    public static String getTradeApiSearchUrl()     { return apiProperties.getProperty("tradeApiSearchUrl"); }
    public static String getTradeApiSearchUrlPoe2() { return apiProperties.getProperty("tradeApiSearchUrlPoE2"); }
    public static String getTradeWebSearchUrl()     { return apiProperties.getProperty("tradeWebSearchUrl"); }
    public static String getTradeWebSearchUrlPoe2() { return apiProperties.getProperty("tradeWebSearchUrlPoE2"); }
    public static long   getTradeApiRequestDelayMs(){ return Long.parseLong(apiProperties.getProperty("tradeApiRequestDelayMs", "4000")); }

    public static double getStatRollDelta() {
        return Double.parseDouble(itemParsingProperties.getProperty("statRollDelta", "1.0"));
    }

    public static List<String> getRollDeltaExclusions() {
        String raw = itemParsingProperties.getProperty("rollDeltaExclusions", "");
        if (raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static Set<String> getUniqueItemWhitelist() {
        return uniqueItemWhitelist;
    }

    public static Map<String, List<String>> getRuneStatsMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (String key : runeStatsProperties.stringPropertyNames()) {
            String raw = runeStatsProperties.getProperty(key, "");
            if (raw.isBlank()) continue;
            List<String> stats = Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            map.put(key.trim().toLowerCase(), stats);
        }
        return map;
    }
}
