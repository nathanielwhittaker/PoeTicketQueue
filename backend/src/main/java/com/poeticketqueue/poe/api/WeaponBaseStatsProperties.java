package com.poeticketqueue.poe.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class WeaponBaseStatsProperties {

    private static final Logger log = LoggerFactory.getLogger(WeaponBaseStatsProperties.class);

    private final Map<String, WeaponBaseStats> stats = new LinkedHashMap<>();

    public WeaponBaseStatsProperties(String resourceName) {
        Properties props = new Properties();
        String path = "/poe/" + resourceName;
        try (InputStream is = WeaponBaseStatsProperties.class.getResourceAsStream(path)) {
            if (is == null) {
                log.warn("Weapon base stats resource not found: {}", path);
                return;
            }
            props.load(is);
        } catch (IOException e) {
            log.error("Failed to load weapon base stats from {}", path, e);
            throw new RuntimeException(e);
        }
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key, "").trim();
            if (value.isEmpty() || value.startsWith("#")) continue;
            String[] parts = value.split(",");
            if (parts.length < 3) continue;
            try {
                int physMin    = Integer.parseInt(parts[0].trim());
                int physMax    = Integer.parseInt(parts[1].trim());
                double aps     = Double.parseDouble(parts[2].trim());
                stats.put(key.trim(), new WeaponBaseStats(physMin, physMax, aps));
            } catch (NumberFormatException ignored) {}
        }
    }

    public WeaponBaseStats getStats(String baseType) {
        if (baseType == null) return null;
        return stats.get(baseType);
    }
}
