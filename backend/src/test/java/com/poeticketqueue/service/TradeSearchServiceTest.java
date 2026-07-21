package com.poeticketqueue.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poeticketqueue.poe.item.Item;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic, network-FREE contract tests for {@link TradeSearchService#buildQuery(Item, String)}.
 *
 * <p>The PoE trade API only accepts a {@code name} filter for UNIQUE items; sending a {@code name}
 * for a non-unique makes the whole search fail with "Unknown item name". These tests assert that
 * {@code buildQuery} omits {@code name} for non-uniques and includes it for uniques.
 *
 * <p>The service has no other collaborators besides an {@link ObjectMapper}, so it is exercised
 * directly with {@code new TradeSearchService(new ObjectMapper())} — no Spring context, no mocking.
 */
class TradeSearchServiceTest {

    private final TradeSearchService service = new TradeSearchService(new ObjectMapper());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rareItem_omitsNameFromQuery() throws Exception {
        Item item = new Item("Some Rare Name", "RARE", "Astral Plate");

        String json = service.buildQuery(item, "Standard");

        assertThat(json).doesNotContain("\"name\":");
        JsonNode query = objectMapper.readTree(json).path("query");
        assertThat(query.has("name")).isFalse();
        assertThat(query.path("type").asText()).isEqualTo("Astral Plate");
    }

    @Test
    void normalOrMagicItem_omitsNameFromQuery() throws Exception {
        Item item = new Item("Astral Plate", "NORMAL", "Astral Plate");

        String json = service.buildQuery(item, "Standard");

        JsonNode query = objectMapper.readTree(json).path("query");
        assertThat(query.has("name")).isFalse();
    }

    @Test
    void uniqueItem_includesNameAndBaseTypeInQuery() throws Exception {
        Item item = new Item("Kaom's Heart", "UNIQUE", "Glorious Plate");

        String json = service.buildQuery(item, "Standard");

        assertThat(json).contains("\"name\":\"Kaom's Heart\"");
        JsonNode query = objectMapper.readTree(json).path("query");
        assertThat(query.path("name").asText()).isEqualTo("Kaom's Heart");
        assertThat(query.path("type").asText()).isEqualTo("Glorious Plate");
    }

    @Test
    void weaponFilters_serializeAsMinMaxRangesUnderWeaponFilters() throws Exception {
        Item item = new Item("Some Bow", "RARE", "Thicket Bow");
        item.damage = 500;          // Hit Damage -> trade key "damage"
        item.maxDamage = 900;
        item.localBaseCrit = 7;     // Crit Chance -> "crit"
        item.pdps = 100;            // Physical DPS -> "pdps"
        item.maxPdps = 200;
        item.edps = 50;             // Elemental DPS -> "edps"
        item.aps = 1;               // Attacks Per Second -> "aps"

        String json = service.buildQuery(item, "Standard");

        JsonNode weapon = objectMapper.readTree(json)
                .path("query").path("filters").path("weapon_filters").path("filters");

        assertThat(weapon.path("damage").path("min").asInt()).isEqualTo(500);
        assertThat(weapon.path("damage").path("max").asInt()).isEqualTo(900);
        assertThat(weapon.path("crit").path("min").asInt()).isEqualTo(7);
        assertThat(weapon.path("pdps").path("min").asInt()).isEqualTo(100);
        assertThat(weapon.path("pdps").path("max").asInt()).isEqualTo(200);
        assertThat(weapon.path("edps").path("min").asInt()).isEqualTo(50);
        assertThat(weapon.path("aps").path("min").asInt()).isEqualTo(1);
    }

    @Test
    void weaponFilters_omittedWhenNoWeaponValuesSet() throws Exception {
        Item item = new Item("Astral Plate", "NORMAL", "Astral Plate");

        String json = service.buildQuery(item, "Standard");

        JsonNode filters = objectMapper.readTree(json).path("query").path("filters");
        assertThat(filters.has("weapon_filters")).isFalse();
    }
}
