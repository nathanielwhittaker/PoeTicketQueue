package com.poeticketqueue.poe.api;

import com.poeticketqueue.poe.item.Stat;
import com.poeticketqueue.poe.item.StatGroup;
import com.poeticketqueue.poe.item.StatGroupType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.json.*;
import java.util.*;
import java.util.stream.Collectors;

public class PoETradeQuery {

    private List<Stat> stats;
    private List<StatGroup> statGroups;
    private String league;
    private String name;
    private String baseType;
    private String category;
    private String status = "online";
    private String priceSort = "asc";
    private final List<QueryStatType<?>> activeFilters = new ArrayList<>();
    private Map<QueryStatType.FilterSection, QueryStatType.FilterSection> sectionRemap = Map.of();

    public PoETradeQuery(String league) {
        this.league = league;
        this.stats = new ArrayList<>();
    }

    public PoETradeQuery filterAll(Map<QueryStat, ?> filters) {
        filters.forEach(this::filter);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> PoETradeQuery filter(QueryStat stat, T val) {
        QueryStatType<T> template = (QueryStatType<T>) stat.getType();
        QueryStatType<T> instance = new QueryStatType<>(
                template.getStat(),
                template.getTradeApiStatLabel(),
                template.getFilterSection(),
                template.getMinThreshold()) {};
        instance.setVal(val);
        activeFilters.add(instance);
        return this;
    }

    public PoETradeQuery name(String name)         { this.name = name; return this; }
    public PoETradeQuery status(String status)     { this.status = status; return this; }
    public PoETradeQuery baseType(String baseType) { this.baseType = baseType; return this; }
    public PoETradeQuery category(String category) { this.category = category; return this; }
    public PoETradeQuery sortPriceAsc()            { this.priceSort = "asc"; return this; }
    public PoETradeQuery sortPriceDesc()           { this.priceSort = "desc"; return this; }

    public PoETradeQuery sectionRemap(Map<QueryStatType.FilterSection, QueryStatType.FilterSection> remap) {
        this.sectionRemap = remap;
        return this;
    }

    public PoETradeQuery stats(List<Stat> stats) {
        this.stats.addAll(stats);
        return this;
    }

    public PoETradeQuery filterGroups(List<StatGroup> groups) {
        this.statGroups = groups;
        return this;
    }

    private static JsonObjectBuilder statValueBuilder(Stat s) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (s.getRoll()    != -1) { b.add("min", s.getRoll()); }
        if (s.getMaxRoll() != -1) { b.add("max", s.getMaxRoll()); }
        return b;
    }

    public String toJson() {
        JsonObjectBuilder queryObj = Json.createObjectBuilder()
                .add("status", Json.createObjectBuilder().add("option", status));

        if (name != null) queryObj.add("name", name);
        if (StringUtils.isNotBlank(baseType)) queryObj.add("type", baseType);

        JsonArrayBuilder statFiltersWrapper = Json.createArrayBuilder();
        if (CollectionUtils.isNotEmpty(statGroups)) {
            for (StatGroup group : statGroups) {
                if (CollectionUtils.isEmpty(group.getFilters())) { continue; }
                JsonArrayBuilder groupFilters = Json.createArrayBuilder();
                for (Stat s : group.getFilters()) {
                    if (s.getId() == null) { continue; }
                    JsonObjectBuilder entry = Json.createObjectBuilder().add("id", s.getId());
                    JsonObject val = statValueBuilder(s).build();
                    if (!val.isEmpty()) { entry.add("value", val); }
                    groupFilters.add(entry);
                    if (s.getAlternateId() != null) {
                        JsonObjectBuilder altEntry = Json.createObjectBuilder().add("id", s.getAlternateId());
                        if (!val.isEmpty()) { altEntry.add("value", val); }
                        groupFilters.add(altEntry);
                    }
                }
                JsonObjectBuilder groupObj = Json.createObjectBuilder()
                        .add("type", group.getType().toApiValue())
                        .add("filters", groupFilters);
                if (group.getType() == StatGroupType.COUNT) {
                    JsonObjectBuilder countVal = Json.createObjectBuilder();
                    if (group.getCountMin() != null) { countVal.add("min", group.getCountMin()); }
                    if (group.getCountMax() != null) { countVal.add("max", group.getCountMax()); }
                    groupObj.add("value", countVal);
                }
                statFiltersWrapper.add(groupObj);
            }
        } else if (CollectionUtils.isNotEmpty(stats)) {
            for (Stat s : stats) {
                if (s.getId() == null) { continue; }
                if (s.getAlternateId() != null) {
                    JsonArrayBuilder filters = Json.createArrayBuilder()
                            .add(Json.createObjectBuilder().add("id", s.getId())
                                    .add("value", statValueBuilder(s)))
                            .add(Json.createObjectBuilder().add("id", s.getAlternateId())
                                    .add("value", statValueBuilder(s)));
                    statFiltersWrapper.add(Json.createObjectBuilder()
                            .add("type", "count")
                            .add("value", Json.createObjectBuilder().add("min", 1))
                            .add("filters", filters));
                } else {
                    JsonObjectBuilder statEntry = Json.createObjectBuilder()
                            .add("id", s.getId())
                            .add("value", statValueBuilder(s));
                    statFiltersWrapper.add(Json.createObjectBuilder()
                            .add("type", "and")
                            .add("filters", Json.createArrayBuilder().add(statEntry)));
                }
            }
        }
        queryObj.add("stats", statFiltersWrapper);

        Map<QueryStatType.FilterSection, List<QueryStatType<?>>> bySection = activeFilters.stream()
                .collect(Collectors.groupingBy(
                        st -> sectionRemap.getOrDefault(st.getFilterSection(), st.getFilterSection()),
                        () -> new EnumMap<>(QueryStatType.FilterSection.class),
                        Collectors.toList()));

        JsonObjectBuilder filterJson = Json.createObjectBuilder();
        for (QueryStatType.FilterSection section : QueryStatType.FilterSection.values()) {
            JsonObjectBuilder filtersInner = Json.createObjectBuilder();

            if (section == QueryStatType.FilterSection.TYPE_FILTERS && category != null) {
                filtersInner.add("category", Json.createObjectBuilder().add("option", category));
            }

            for (QueryStatType<?> st : bySection.getOrDefault(section, List.of())) {
                if (st.getVal() == null) continue;
                FilterSerializer s = ValSerializer.BY_TYPE.get(st.getVal().getClass());
                if (s != null) s.apply(filtersInner, st);
            }

            JsonObject filtersObj = filtersInner.build();
            if (!filtersObj.isEmpty()) {
                filterJson.add(section.getJsonKey(), Json.createObjectBuilder().add("filters", filtersObj));
            }
        }
        queryObj.add("filters", filterJson);

        return Json.createObjectBuilder()
                .add("query", queryObj)
                .add("sort", Json.createObjectBuilder().add("price", priceSort))
                .build().toString();
    }

    public record MinMax(Integer min, Integer max) {}

    @FunctionalInterface
    private interface FilterSerializer {
        void apply(JsonObjectBuilder builder, QueryStatType<?> statType);
    }

    private enum ValSerializer implements FilterSerializer {
        STRING {
            @Override public void apply(JsonObjectBuilder builder, QueryStatType<?> st) {
                if (st.getVal() instanceof String s && StringUtils.isNotBlank(s)) {
                    builder.add(st.getTradeApiStatLabel(), s);
                }
            }
        },
        BOOLEAN {
            @Override public void apply(JsonObjectBuilder builder, QueryStatType<?> st) {
                if (st.getVal() instanceof Boolean b) {
                    builder.add(st.getTradeApiStatLabel(), Json.createObjectBuilder().add("option", b));
                }
            }
        },
        INTEGER {
            @Override public void apply(JsonObjectBuilder builder, QueryStatType<?> st) {
                if (st.getVal() instanceof Integer i && i >= st.getMinThreshold()) {
                    builder.add(st.getTradeApiStatLabel(), Json.createObjectBuilder().add("min", i));
                }
            }
        },
        MINMAX {
            @Override public void apply(JsonObjectBuilder builder, QueryStatType<?> st) {
                if (!(st.getVal() instanceof MinMax mm)) { return; }
                JsonObjectBuilder range = Json.createObjectBuilder();
                if (mm.min() != null && mm.min() > 0) { range.add("min", mm.min()); }
                if (mm.max() != null && mm.max() > 0) { range.add("max", mm.max()); }
                JsonObject built = range.build();
                if (!built.isEmpty()) {
                    builder.add(st.getTradeApiStatLabel(), built);
                }
            }
        };

        static final Map<Class<?>, ValSerializer> BY_TYPE = Map.of(
                String.class,  STRING,
                Boolean.class, BOOLEAN,
                Integer.class, INTEGER,
                MinMax.class,  MINMAX
        );
    }
}
