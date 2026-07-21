package com.poeticketqueue.controller;

import com.poeticketqueue.model.Group;
import com.poeticketqueue.model.QueuedBuild;
import com.poeticketqueue.poe.item.Item;
import com.poeticketqueue.poe.item.Stat;
import com.poeticketqueue.poe.item.StatGroup;
import com.poeticketqueue.poe.item.StatGroupType;
import com.poeticketqueue.service.BuildImportService;
import com.poeticketqueue.service.GroupMemberService;
import com.poeticketqueue.service.GroupService;
import com.poeticketqueue.service.TradeSearchService;
import com.poeticketqueue.util.SessionAttributes;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@RestController
@RequestMapping("/api/groups")
public class GroupItemApiController {

    private static final Logger log = LoggerFactory.getLogger(GroupItemApiController.class);

    private final GroupService groupService;
    private final GroupMemberService groupMemberService;
    private final TradeSearchService tradeSearchService;
    private final BuildImportService buildImportService;

    public GroupItemApiController(GroupService groupService, GroupMemberService groupMemberService, TradeSearchService tradeSearchService, BuildImportService buildImportService) {
        this.groupService = groupService;
        this.groupMemberService = groupMemberService;
        this.tradeSearchService = tradeSearchService;
        this.buildImportService = buildImportService;
    }

    @GetMapping("/{groupCode}")
    public ResponseEntity<Group> getGroup(@PathVariable String groupCode) {
        return groupService.findByCode(groupCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{groupCode}/items")
    public ResponseEntity<Group> addItem(@PathVariable String groupCode, @RequestBody AddItemRequest request, HttpSession session) {
        Item item = new Item(request.name(), request.rarity(), request.baseType());
        item.queuedBy = (String) session.getAttribute(SessionAttributes.SCREEN_NAME);
        if (request.filterGroups() != null) {
            List<StatGroup> groups = request.filterGroups().stream()
                    .filter(g -> g.filters() != null)
                    .map(g -> {
                        List<Stat> stats = g.filters().stream()
                                .filter(f -> f.text() != null && !f.text().isBlank())
                                .map(f -> {
                                    Stat stat = new Stat(f.text(), f.statId(), f.min() != null ? f.min() : -1);
                                    if (f.max() != null) { stat.setMaxRoll(f.max()); }
                                    return stat;
                                })
                                .toList();
                        return new StatGroup(g.type(), g.countMin(), g.countMax(), stats);
                    })
                    .filter(g -> !g.getFilters().isEmpty())
                    .toList();
            item.setStatGroups(groups);
            item.setStats(groups.stream().flatMap(g -> g.getFilters().stream()).toList());
        }
        if (request.armourFilters() != null) {
            ArmourFiltersRequest af = request.armourFilters();
            if (af.ar()             != null) { item.armour           = zeroIfNull(af.ar().min()); item.maxArmour          = zeroIfNull(af.ar().max()); }
            if (af.ev()             != null) { item.evasion          = zeroIfNull(af.ev().min()); item.maxEvasion         = zeroIfNull(af.ev().max()); }
            if (af.es()             != null) { item.es               = zeroIfNull(af.es().min()); item.maxEs              = zeroIfNull(af.es().max()); }
            if (af.ward()           != null) { item.ward             = zeroIfNull(af.ward().min()); item.maxWard          = zeroIfNull(af.ward().max()); }
            if (af.block()          != null) { item.block            = zeroIfNull(af.block().min()); item.maxBlock         = zeroIfNull(af.block().max()); }
            if (af.basePercentile() != null) { item.basePercentile   = zeroIfNull(af.basePercentile().min()); item.maxBasePercentile = zeroIfNull(af.basePercentile().max()); }
        }
        return groupService.addItem(groupCode, item)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{groupCode}/items/{index}")
    public ResponseEntity<Group> removeItem(@PathVariable String groupCode, @PathVariable int index, HttpSession session) {
        return withTradePermission(groupCode, session, group -> {
            groupService.removeItem(groupCode, index);
            return ResponseEntity.ok(group);
        });
    }

    @PostMapping("/{groupCode}/items/{index}/buy")
    public ResponseEntity<BuyResponse> buyItem(@PathVariable String groupCode, @PathVariable int index, HttpSession session) {
        return withTradePermission(groupCode, session, group -> {
            List<Item> queue = group.getItemQueue();
            if (index < 0 || index >= queue.size()) {
                return ResponseEntity.badRequest().build();
            }

            String poeSessionId = (String) session.getAttribute(SessionAttributes.POE_SESSION_ID);
            if (StringUtils.isBlank(poeSessionId)) {
                return ResponseEntity.status(400).build();
            }
            try {
                String tradeUrl = tradeSearchService.search(queue.get(index), group.getPoeVersion(), group.getLeague(), poeSessionId);
                return ResponseEntity.ok(new BuyResponse(tradeUrl));
            } catch (Exception e) {
                log.warn("buyItem() -- trade search failed for group {}", groupCode, e);
                return ResponseEntity.status(502).build();
            }
        });
    }

    @PostMapping("/{groupCode}/builds")
    public ResponseEntity<?> importBuild(@PathVariable String groupCode, @RequestBody ImportBuildRequest request) {
        Optional<Group> maybeGroup = groupService.findByCode(groupCode);
        if (maybeGroup.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Group group = maybeGroup.get();
        BuildImportService.BuildImportOutcome outcome = buildImportService.importBuild(request.url(), group.getPoeVersion(), request.useTrueValues());
        return switch (outcome.status()) {
            case SUCCESS -> groupService.addBuild(groupCode, outcome.build())
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
            case VERSION_MISMATCH -> ResponseEntity.status(409).body(new ImportErrorResponse(outcome.message()));
            case INVALID_URL -> ResponseEntity.badRequest().body(new ImportErrorResponse(outcome.message()));
        };
    }

    @PostMapping("/{groupCode}/builds/{buildIndex}/items/{itemIndex}/buy")
    public ResponseEntity<BuyResponse> buyBuildItem(@PathVariable String groupCode, @PathVariable int buildIndex, @PathVariable int itemIndex, HttpSession session) {
        return withTradePermission(groupCode, session, group -> {
            List<QueuedBuild> builds = group.getBuildQueue();
            if (buildIndex < 0 || buildIndex >= builds.size()) {
                return ResponseEntity.badRequest().build();
            }
            List<Item> items = builds.get(buildIndex).getItems();
            if (itemIndex < 0 || itemIndex >= items.size()) {
                return ResponseEntity.badRequest().build();
            }

            String poeSessionId = (String) session.getAttribute(SessionAttributes.POE_SESSION_ID);
            if (StringUtils.isBlank(poeSessionId)) {
                return ResponseEntity.status(400).build();
            }
            try {
                String tradeUrl = tradeSearchService.search(items.get(itemIndex), group.getPoeVersion(), group.getLeague(), poeSessionId);
                return ResponseEntity.ok(new BuyResponse(tradeUrl));
            } catch (Exception e) {
                log.warn("buyBuildItem() -- trade search failed for group {}", groupCode, e);
                return ResponseEntity.status(502).build();
            }
        });
    }

    @DeleteMapping("/{groupCode}/builds/{buildIndex}/items/{itemIndex}")
    public ResponseEntity<Group> removeBuildItem(@PathVariable String groupCode, @PathVariable int buildIndex, @PathVariable int itemIndex, HttpSession session) {
        return withTradePermission(groupCode, session, group -> {
            groupService.removeBuildItem(groupCode, buildIndex, itemIndex);
            return ResponseEntity.ok(group);
        });
    }

    private <T> ResponseEntity<T> withTradePermission(String groupCode, HttpSession session, Function<Group, ResponseEntity<T>> action) {
        String screenName = (String) session.getAttribute(SessionAttributes.SCREEN_NAME);
        if (screenName == null) {
            return ResponseEntity.status(401).<T>build();
        }

        return groupService.findByCode(groupCode).map(group -> {
            if (!groupMemberService.canTrade(group, screenName)) {
                return ResponseEntity.status(403).<T>build();
            }
            return action.apply(group);
        }).orElse(ResponseEntity.notFound().<T>build());
    }

    private static int zeroIfNull(Integer val) {
        return val != null ? val : 0;
    }

    record AddItemRequest(String name, String rarity, String baseType, List<FilterGroupRequest> filterGroups, ArmourFiltersRequest armourFilters) {}
    record ArmourFiltersRequest(MinMax ar, MinMax ev, MinMax es, MinMax ward, MinMax block, MinMax basePercentile) {}
    record MinMax(Integer min, Integer max) {}
    record FilterGroupRequest(StatGroupType type, Integer countMin, Integer countMax, List<StatFilterRequest> filters) {}
    record StatFilterRequest(String statId, String text, Double min, Double max) {}
    record BuyResponse(String tradeUrl) {}
    record ImportBuildRequest(String url, boolean useTrueValues) {}
    record ImportErrorResponse(String message) {}
}
