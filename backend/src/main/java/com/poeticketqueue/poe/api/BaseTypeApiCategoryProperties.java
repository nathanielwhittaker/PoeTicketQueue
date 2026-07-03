package com.poeticketqueue.poe.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class BaseTypeApiCategoryProperties {

    private static final Logger log = LoggerFactory.getLogger(BaseTypeApiCategoryProperties.class);

    private final Map<String, String> categories = new LinkedHashMap<>();

    public BaseTypeApiCategoryProperties(String resourceName) {
        Properties props = new Properties();
        String path = "/poe/" + resourceName;
        try (InputStream is = BaseTypeApiCategoryProperties.class.getResourceAsStream(path)) {
            if (is == null) {
                log.warn("Base type category resource not found: {}", path);
                return;
            }
            props.load(is);
        } catch (IOException e) {
            log.error("Failed to load base type categories from {}", path, e);
            throw new RuntimeException(e);
        }
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key, "").trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                categories.put(key, value);
            }
        }
    }

    public String getCategory(String baseType) {
        return categories.get(baseType);
    }
}
