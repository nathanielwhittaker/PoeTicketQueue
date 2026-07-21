package com.poeticketqueue.poe.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poeticketqueue.poe.build.Build;
import com.poeticketqueue.poe.config.PropertiesManagerCore;
import com.poeticketqueue.poe.item.Item;
import com.poeticketqueue.poe.item.Stat;
import com.poeticketqueue.poe.util.Web;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class PathOfExileTradeApi {

    private static final Logger log = LoggerFactory.getLogger(PathOfExileTradeApi.class);

    public static final String sellerStatusType = "available";

    private List<Stat> stats = null;
    private List<Item> allBaseTypes = null;
    private List<Item> allBaseTypesWithLocalMods = null;

    public abstract String getLeague();
    public abstract String getStatsApiUrl();
    public abstract String getItemsApiUrl();
    public abstract String getTradeSearchUrl();
    public abstract String getTradeWebUrl();
    public abstract String getLeaguesApiUrl();

    protected Map<QueryStat, Object> versionSpecificFilters(Item item) {
        return Map.of();
    }

    protected Map<QueryStatType.FilterSection, QueryStatType.FilterSection> getFilterSectionRemaps() {
        return Map.of();
    }

    public static Map<Build, Map<Item, PathOfExileTradeApiResponse>> batchProcessBuilds(List<Build> builds) {
        return batchProcessBuilds(builds, () -> {});
    }

    public static Map<Build, Map<Item, PathOfExileTradeApiResponse>> batchProcessBuilds(List<Build> builds, Runnable onItemComplete) {
        Map<Build, Map<Item, PathOfExileTradeApiResponse>> results = new LinkedHashMap<>();
        for (Build build : builds) {
            results.put(build, build.getTradeApi().batchedPostTradeSearch(build, onItemComplete));
        }
        return results;
    }

    public Map<Item, PathOfExileTradeApiResponse> batchedPostTradeSearch(Build build) {
        return batchedPostTradeSearch(build, () -> {});
    }

    public Map<Item, PathOfExileTradeApiResponse> batchedPostTradeSearch(Build build, Runnable onItemComplete) {
        Map<Item, PathOfExileTradeApiResponse> results = new LinkedHashMap<>();
        List<Item> items = build.getItems();
        log.info("Starting trade search for {} items in build '{}'", items.size(), build.getName());
        for (Item item : items) {
            results.put(item, postTradeSearch(item, build));
            onItemComplete.run();
            try {
                Thread.sleep(PropertiesManagerCore.getTradeApiRequestDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.info("Completed trade search for build '{}'", build.getName());
        return results;
    }

    public PathOfExileTradeApiResponse postTradeSearch(Item item, Build build) {
        String json = buildJsonForPost(item, build);
        String url = getTradeSearchUrl() + getLeague();
        log.debug("POST {}", url);
        String responseStr = new Web(url)
                .addHeader("Cookie", "POESESSID=" + PropertiesManagerCore.getPoeSessId())
                .addHeader("Content-Type", "application/json")
                .addHeader("Origin", "https://www.pathofexile.com")
                .addHeader("Referer", "https://www.pathofexile.com/trade")
                .addBody(json)
                .getResponse();
        ObjectMapper objMapper = new ObjectMapper();
        objMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            return objMapper.readValue(responseStr, PathOfExileTradeApiResponse.class);
        } catch (Exception e) {
            log.error("Failed to deserialize trade API response for item '{}'", item.getName(), e);
            return null;
        }
    }

    public String buildJsonForPost(Item item, Build build) {
        String category = null;
        if (StringUtils.isNotBlank(item.getBaseType()) && !"UNIQUE".equalsIgnoreCase(item.getRarity())) {
            BaseTypeApiCategoryProperties categoryProps = build.getBaseTypeCategoryProperties();
            if (categoryProps != null) {
                category = categoryProps.getCategory(item.getBaseType());
            }
        }
        return buildJsonForPost(item, category);
    }

    public String buildJsonForPost(Item item, String category) {
        PoETradeQuery query = new PoETradeQuery(getLeague());
        if (StringUtils.isNotBlank(item.getName()) && "UNIQUE".equalsIgnoreCase(item.getRarity())) {
            query.name(item.getName());
        }
        if (StringUtils.isNotBlank(item.getBaseType())) {
            query.baseType(item.getBaseType());
            if (StringUtils.isNotBlank(category)) {
                query.category(category);
            }
        }

        Map<QueryStat, Object> filters = new LinkedHashMap<>();
        filters.put(QueryStat.GEM_LEVEL,        item.getGemLevel());
        filters.putAll(defenseFilters(item));
        filters.putAll(weaponFilters(item));
        filters.putAll(versionSpecificFilters(item));
        if (item.getRarity()    != null) { filters.put(QueryStat.RARITY,    item.getRarity()); }
        if (item.getCorrupted() != null) { filters.put(QueryStat.CORRUPTED, item.getCorrupted()); }

        query.stats(item.getStats())
                .status(sellerStatusType)
                .sortPriceAsc()
                .sectionRemap(getFilterSectionRemaps())
                .filterAll(filters);

        return query.toJson();
    }

    public static Map<QueryStat, Object> defenseFilters(Item item) {
        Map<QueryStat, Object> filters = new LinkedHashMap<>();
        filters.put(QueryStat.ENERGY_SHIELD,    new PoETradeQuery.MinMax(nullIfZero(item.getEs()),             nullIfZero(item.getMaxEs())));
        filters.put(QueryStat.EVASION,          new PoETradeQuery.MinMax(nullIfZero(item.getEvasion()),        nullIfZero(item.getMaxEvasion())));
        filters.put(QueryStat.ARMOUR,           new PoETradeQuery.MinMax(nullIfZero(item.getArmour()),         nullIfZero(item.getMaxArmour())));
        filters.put(QueryStat.WARD,             new PoETradeQuery.MinMax(nullIfZero(item.getWard()),           nullIfZero(item.getMaxWard())));
        filters.put(QueryStat.BLOCK,            new PoETradeQuery.MinMax(nullIfZero(item.getBlock()),          nullIfZero(item.getMaxBlock())));
        filters.put(QueryStat.BASE_PERCENTILE,  new PoETradeQuery.MinMax(nullIfZero(item.getBasePercentile()), nullIfZero(item.getMaxBasePercentile())));
        return filters;
    }

    public static Map<QueryStat, Object> weaponFilters(Item item) {
        Map<QueryStat, Object> filters = new LinkedHashMap<>();
        filters.put(QueryStat.HIT_DAMAGE,         new PoETradeQuery.MinMax(nullIfZero(item.getDamage()),     nullIfZero(item.getMaxDamage())));
        filters.put(QueryStat.LOCAL_CRIT,         new PoETradeQuery.MinMax(nullIfZero(item.getLocalCrit()),  nullIfZero(item.getMaxLocalCrit())));
        filters.put(QueryStat.PHYSICAL_DPS,       new PoETradeQuery.MinMax(nullIfZero(item.getPDPS()),       nullIfZero(item.getMaxPDPS())));
        filters.put(QueryStat.ELEMENTAL_DPS,      new PoETradeQuery.MinMax(nullIfZero(item.getEDPS()),       nullIfZero(item.getMaxEDPS())));
        filters.put(QueryStat.ATTACKS_PER_SECOND, new PoETradeQuery.MinMax(nullIfZero(item.getAPS()),        nullIfZero(item.getMaxAPS())));
        return filters;
    }

    private static Integer nullIfZero(int val) {
        return val > 0 ? val : null;
    }

    public List<Stat> getStats() {
        if (stats == null) stats = fetchStats(getStatsApiUrl());
        return stats;
    }

    public List<Item> getBaseTypesWithLocalMods() {
        if (allBaseTypesWithLocalMods == null) {
            ensureAllBaseTypesLoaded();
            allBaseTypesWithLocalMods = allBaseTypes.stream()
                    .filter(item -> item.getBaseType() != null && item.getName() == null
                            && item.getText() == null
                            && (item.type.equals("armour") || item.type.equals("weapon")))
                    .toList();
        }
        return allBaseTypesWithLocalMods;
    }

    public List<Item> getAllBaseTypes() {
        ensureAllBaseTypesLoaded();
        return allBaseTypes;
    }

    private void ensureAllBaseTypesLoaded() {
        if (allBaseTypes == null) allBaseTypes = fetchAllBaseTypes(getItemsApiUrl());
    }

    protected static List<Stat> fetchStats(String url) {
        String statJson = getResponseFromPoeDataApi(url);
        List<Stat> list = new ArrayList<>();
        parseApiResponseJson(statJson, (stats, e, r) -> stats.add(new Stat(e.text.toLowerCase(), e.id)), list);
        log.info("Loaded {} stats from {}", list.size(), url);
        return list;
    }

    protected static List<Item> fetchAllBaseTypes(String url) {
        String itemJson = getResponseFromPoeDataApi(url);
        List<Item> list = new ArrayList<>();
        parseApiResponseJson(itemJson, (items, e, r) -> {
            Item item = new Item();
            item.name = e.name;
            item.baseType = e.type;
            item.type = r.id;
            items.add(item);
        }, list);
        log.info("Loaded {} base types from {}", list.size(), url);
        return list;
    }

    private static String getResponseFromPoeDataApi(String apiUrl) {
        return new Web(apiUrl)
                .addHeader("Cookie", "POESESSID=" + PropertiesManagerCore.getPoeSessId())
                .addHeader("Origin", "https://www.pathofexile.com")
                .addHeader("Referer", "https://www.pathofexile.com/trade")
                .getResponse();
    }

    private static <T> void parseApiResponseJson(String json, ApiDeserializationProcess<T> process, List<T> toAddTo) {
        ObjectMapper objMapper = new ObjectMapper();
        objMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            ApiResponse response = objMapper.readValue(json, ApiResponse.class);
            for (Result r : response.result) {
                for (Entry e : r.entries) {
                    process.deserializeIntoJavaObject(toAddTo, e, r);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse API response JSON", e);
        }
    }
}
