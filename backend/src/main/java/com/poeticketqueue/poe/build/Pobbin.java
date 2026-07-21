package com.poeticketqueue.poe.build;

import com.poeticketqueue.poe.item.Item;
import com.poeticketqueue.poe.util.Web;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.zip.InflaterInputStream;

public abstract class Pobbin extends Build {

    private static final Logger log = LoggerFactory.getLogger(Pobbin.class);

    private static final int MAX_DECOMPRESSED_BYTES = 16 * 1024 * 1024;

    private final String url;
    private final String name;
    private String html;
    private String pobXml;
    private String buildData;
    private List<String> itemDataList;

    protected Pobbin(String url, String name, boolean useTrueValues) {
        this.useTrueValues = useTrueValues;
        this.url = url;
        this.name = name;
        log.info("Loading build '{}' from {}", name, url);
        this.html = new Web(url).getResponse();
        parsePobbinPobXml();
        parsePobXmlIntoBuildData();
        parseBuildDataIntoItemData();
        this.items = itemDataList.stream()
                .filter(s -> s.startsWith("Rarity:"))
                .map(s -> Item.fromStringForBuild(s, this))
                .toList();
        log.info("Loaded build '{}' with {} items", name, this.items.size());
    }

    public static String parseNameFromHtml(String html) {
        return getSubstringBetween(html,
                "property=\"og:title\" content=\"",
                "\"/><meta data-xx=\"1.6\"");
    }

    private static String getSubstringBetween(String base, String prefix, String suffix) {
        if (StringUtils.isNoneBlank(base, prefix, suffix)) {
            int startIndex = base.indexOf(prefix) + prefix.length();
            int endIndex = base.indexOf(suffix, startIndex);
            return base.substring(startIndex, endIndex);
        }
        throw new NullPointerException("Cannot parse blank.");
    }

    private static String cleanItemData(String itemData) {
        itemData = itemData.replaceAll("<ModRange\\s+range=\"[^\"]*\"\\s+id=\"[^\"]*\"\\s*/>", "");
        itemData = itemData.replaceAll("<Item\\s+id=\"[^\"]*\"[^>]*>", "");
        itemData = itemData.replaceAll("\\s{2,}", "\n\n");
        int endIndex = itemData.indexOf("</Items");
        itemData = itemData.substring(0, endIndex + "</Items>".length());
        return itemData;
    }

    public Pobbin parsePobbinPobXml() {
        if (StringUtils.isNotBlank(html)) {
            this.pobXml = getSubstringBetween(html,
                    "aria-label=\"Path of Building buildcode\" readonly=\"\">",
                    "</textarea>");
            return this;
        }
        throw new NullPointerException("Cannot parse null.");
    }

    public Pobbin parsePobXmlIntoBuildData() {
        if (StringUtils.isNotBlank(pobXml)) {
            byte[] bytes = Base64.getUrlDecoder().decode(pobXml);
            try (InflaterInputStream gis = new InflaterInputStream(new ByteArrayInputStream(bytes))) {
                this.buildData = new String(readBounded(gis, MAX_DECOMPRESSED_BYTES), StandardCharsets.UTF_8);
                return this;
            } catch (BuildDataTooLargeException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to decompress PobXML build data", e);
            }
        } else {
            throw new NullPointerException("Cannot parse null.");
        }
        return null;
    }

    private static byte[] readBounded(InputStream in, int maxBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            total += n;
            if (total > maxBytes) {
                throw new BuildDataTooLargeException("PoB build data exceeds maximum allowed size");
            }
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static class BuildDataTooLargeException extends RuntimeException {
        BuildDataTooLargeException(String message) {
            super(message);
        }
    }

    @SuppressWarnings("deprecation")
    public Pobbin parseBuildDataIntoItemData() {
        String tmpItemData = StringUtils.replaceAll(buildData, "<Items\\s+[^>]*>", "<Items>");
        tmpItemData = StringEscapeUtils.unescapeXml(cleanItemData(tmpItemData));
        String itemDataString = getSubstringBetween(tmpItemData, "<Items>", "</Items>");
        this.itemDataList = Arrays.stream(itemDataString.split("</Item>"))
                .map(str -> {
                    str = str.trim();
                    str = str.replaceAll("\\{[^}]*\\}", "");
                    return str;
                })
                .toList();
        return this;
    }

    public String getPobXml() {
        if (pobXml == null) parsePobbinPobXml();
        return pobXml;
    }

    public String getUrl()                     { return url; }
    @Override public String getName()          { return name; }
    public String html()                       { return html; }
    public String getBuildData()               { return buildData; }
    public List<String> getItemDataList()      { return itemDataList; }
}
