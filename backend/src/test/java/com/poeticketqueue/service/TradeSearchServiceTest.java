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
}
