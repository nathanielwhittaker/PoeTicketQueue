package com.poeticketqueue.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poeticketqueue.poe.api.PathOfExileTradeApi;
import com.poeticketqueue.poe.api.PoETradeQuery;
import com.poeticketqueue.poe.api.QueryStat;
import com.poeticketqueue.poe.build.PoeVersion;
import com.poeticketqueue.poe.item.Item;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TradeSearchService {

    private static final Logger log = LoggerFactory.getLogger(TradeSearchService.class);

    private static final Map<String, String> CATEGORY_MAP = Map.ofEntries(
            Map.entry("Body Armour",    "armour.chest"),
            Map.entry("Helmet",         "armour.helmet"),
            Map.entry("Gloves",         "armour.gloves"),
            Map.entry("Boots",          "armour.boots"),
            Map.entry("Shield",         "armour.shield"),
            Map.entry("Focus",          "armour.focus"),
            Map.entry("Quiver",         "armour.quiver"),
            Map.entry("Wand",           "weapon.wand"),
            Map.entry("Sceptre",        "weapon.sceptre"),
            Map.entry("Claw",           "weapon.claw"),
            Map.entry("Dagger",         "weapon.dagger"),
            Map.entry("Rune Dagger",    "weapon.runedagger"),
            Map.entry("One Hand Sword", "weapon.onesword"),
            Map.entry("One Hand Axe",   "weapon.oneaxe"),
            Map.entry("One Hand Mace",  "weapon.onemace"),
            Map.entry("Flail",          "weapon.flail"),
            Map.entry("Spear",          "weapon.spear"),
            Map.entry("Staff",          "weapon.staff"),
            Map.entry("Warstaff",       "weapon.warstaff"),
            Map.entry("Bow",            "weapon.bow"),
            Map.entry("Two Hand Sword", "weapon.twosword"),
            Map.entry("Two Hand Axe",   "weapon.twoaxe"),
            Map.entry("Two Hand Mace",  "weapon.twomace"),
            Map.entry("Crossbow",       "weapon.crossbow"),
            Map.entry("Quarterstaff",   "weapon.quarterstaff"),
            Map.entry("Amulet",         "accessory.amulet"),
            Map.entry("Ring",           "accessory.ring"),
            Map.entry("Belt",           "accessory.belt"),
            Map.entry("Jewel",          "jewel.base"),
            Map.entry("Abyss Jewel",    "jewel.abyss"),
            Map.entry("Cluster Jewel",  "jewel.cluster"),
            Map.entry("Flask",          "flask"),
            Map.entry("Skill Gem",      "gem.activegem"),
            Map.entry("Support Gem",    "gem.supportgem")
    );

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public TradeSearchService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String search(Item item, PoeVersion poeVersion, String league, String poeSessionId) throws Exception {
        String encodedLeague = league.replace(" ", "%20");
        String body = buildQuery(item, league);

        log.debug("Trade search POST body: {}", body);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(poeVersion.getTradeApi().getTradeSearchUrl() + encodedLeague))
                .header("Content-Type", "application/json")
                .header("User-Agent", "PoeTicketQueue/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (StringUtils.isNotBlank(poeSessionId)) {
            requestBuilder.header("Cookie", "POESESSID=" + poeSessionId);
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        log.debug("Trade search response: {}", response.body());

        JsonNode json = objectMapper.readTree(response.body());
        JsonNode idNode = json.get("id");
        if (idNode == null) {
            log.error("Trade API returned no id. Response: {}", response.body());
            throw new RuntimeException("Trade API error: " + response.body());
        }
        return poeVersion.getTradeApi().getTradeWebUrl() + encodedLeague + "/" + idNode.asText();
    }

    private String buildQuery(Item item, String league) {
        PoETradeQuery query = new PoETradeQuery(league)
                .status(PathOfExileTradeApi.sellerStatusType)
                .sortPriceAsc();

        if (StringUtils.isNotBlank(item.name) && "UNIQUE".equalsIgnoreCase(item.rarity)) {
            query.name(item.name);
        }

        String category = CATEGORY_MAP.get(item.baseType);
        if (StringUtils.isNotBlank(category)) {
            query.category(category);
        }

        if (item.getStatGroups() != null && !item.getStatGroups().isEmpty()) {
            query.filterGroups(item.getStatGroups());
        } else if (item.getStats() != null) {
            query.stats(item.getStats());
        }

        Map<QueryStat, Object> filters = new LinkedHashMap<>();
        filters.put(QueryStat.ENERGY_SHIELD,    minMax(item.getEs(),             item.getMaxEs()));
        filters.put(QueryStat.EVASION,          minMax(item.getEvasion(),        item.getMaxEvasion()));
        filters.put(QueryStat.ARMOUR,           minMax(item.getArmour(),         item.getMaxArmour()));
        filters.put(QueryStat.WARD,             minMax(item.getWard(),           item.getMaxWard()));
        filters.put(QueryStat.BLOCK,            minMax(item.getBlock(),          item.getMaxBlock()));
        filters.put(QueryStat.BASE_PERCENTILE,  minMax(item.getBasePercentile(), item.getMaxBasePercentile()));
        if (StringUtils.isNotBlank(item.rarity)) {
            filters.put(QueryStat.RARITY, item.rarity.toLowerCase());
        }
        query.filterAll(filters);

        return query.toJson();
    }

    private static PoETradeQuery.MinMax minMax(int min, int max) {
        return new PoETradeQuery.MinMax(min > 0 ? min : null, max > 0 ? max : null);
    }
}
