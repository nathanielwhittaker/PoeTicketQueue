package com.poeticketqueue.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poeticketqueue.poe.build.PoeVersion;
import com.poeticketqueue.service.PoeStatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/data")
public class PoeDataApiController {

    private static final Logger log = LoggerFactory.getLogger(PoeDataApiController.class);

    private final PoeStatService poeStatService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public PoeDataApiController(PoeStatService poeStatService, ObjectMapper objectMapper) {
        this.poeStatService = poeStatService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{poeVersion}/base-types")
    public ResponseEntity<List<BaseTypeEntry>> getBaseTypes(@PathVariable PoeVersion poeVersion) {
        List<BaseTypeEntry> entries = poeStatService.getAllBaseTypes(poeVersion).stream()
                .map(item -> {
                    boolean unique = item.name != null;
                    String text   = unique ? item.name    : item.baseType;
                    String detail = unique ? item.baseType : item.type;
                    return new BaseTypeEntry(text, detail, unique);
                })
                .filter(e -> e.text() != null)
                .distinct()
                .toList();
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/{poeVersion}/stats")
    public ResponseEntity<List<StatEntry>> getStats(@PathVariable PoeVersion poeVersion) {
        List<StatEntry> entries = poeStatService.getStats(poeVersion).stream()
                .map(stat -> new StatEntry(stat.getText(), stat.getId()))
                .toList();
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/{poeVersion}/leagues")
    public ResponseEntity<LeagueListResponse> getLeagues(@PathVariable PoeVersion poeVersion) {
        String url = poeVersion.getTradeApi().getLeaguesApiUrl();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "PoeTicketQueue/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode leaguesNode = objectMapper.readTree(response.body());
            List<LeagueEntry> leagues = new ArrayList<>();
            String defaultLeague = "Standard";
            for (JsonNode node : leaguesNode) {
                String id = node.get("id").asText();
                String name = node.get("name").asText();
                JsonNode category = node.get("category");
                JsonNode rules = node.get("rules");
                boolean isSsf = rules != null && StreamSupport.stream(rules.spliterator(), false)
                        .anyMatch(r -> "NoParties".equals(r.path("id").asText()));
                if (isSsf) {
                    continue;
                }
                leagues.add(new LeagueEntry(id, name));
                boolean isCurrent = category != null && category.path("current").asBoolean(false);
                boolean noRules = rules != null && rules.isEmpty();
                if (isCurrent && noRules && defaultLeague.equals("Standard")) {
                    defaultLeague = id;
                }
            }
            return ResponseEntity.ok(new LeagueListResponse(leagues, defaultLeague));
        } catch (Exception e) {
            return ResponseEntity.ok(new LeagueListResponse(List.of(new LeagueEntry("Standard", "Standard")), "Standard"));
        }
    }

    record BaseTypeEntry(String text, String detail, boolean unique) {}
    record StatEntry(String text, String id) {}
    record LeagueEntry(String id, String name) {}
    record LeagueListResponse(List<LeagueEntry> leagues, String defaultLeague) {}
}
