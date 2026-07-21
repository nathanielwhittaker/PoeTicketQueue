package com.poeticketqueue.poe.api;

public enum QueryStat {

    ENERGY_SHIELD   (new QueryStatType<PoETradeQuery.MinMax>(null, "es",               QueryStatType.FilterSection.ARMOUR_FILTERS) {}),
    EVASION         (new QueryStatType<PoETradeQuery.MinMax>(null, "ev",               QueryStatType.FilterSection.ARMOUR_FILTERS) {}),
    ARMOUR          (new QueryStatType<PoETradeQuery.MinMax>(null, "ar",               QueryStatType.FilterSection.ARMOUR_FILTERS) {}),
    WARD            (new QueryStatType<PoETradeQuery.MinMax>(null, "ward",             QueryStatType.FilterSection.ARMOUR_FILTERS) {}),
    BLOCK           (new QueryStatType<PoETradeQuery.MinMax>(null, "block",            QueryStatType.FilterSection.ARMOUR_FILTERS) {}),
    BASE_PERCENTILE (new QueryStatType<PoETradeQuery.MinMax>(null, "base_percentile",  QueryStatType.FilterSection.ARMOUR_FILTERS) {}),

    HIT_DAMAGE        (new QueryStatType<PoETradeQuery.MinMax>(null, "damage",  QueryStatType.FilterSection.WEAPON_FILTERS) {}),
    LOCAL_CRIT        (new QueryStatType<PoETradeQuery.MinMax>(null, "crit",    QueryStatType.FilterSection.WEAPON_FILTERS) {}),
    PHYSICAL_DPS      (new QueryStatType<PoETradeQuery.MinMax>(null, "pdps",    QueryStatType.FilterSection.WEAPON_FILTERS) {}),
    ELEMENTAL_DPS     (new QueryStatType<PoETradeQuery.MinMax>(null, "edps",    QueryStatType.FilterSection.WEAPON_FILTERS) {}),
    ATTACKS_PER_SECOND(new QueryStatType<PoETradeQuery.MinMax>(null, "aps",     QueryStatType.FilterSection.WEAPON_FILTERS) {}),

    LINKS        (new QueryStatType<Integer>(null, "links",     QueryStatType.FilterSection.SOCKET_FILTERS, 0) {}),

    ILVL         (new QueryStatType<Integer>(null, "ilvl",      QueryStatType.FilterSection.MISC_FILTERS, 0) {}),
    GEM_LEVEL    (new QueryStatType<Integer>(null, "gem_level", QueryStatType.FilterSection.MISC_FILTERS, 1) {}),
    CORRUPTED    (new QueryStatType<Boolean>(null, "corrupted", QueryStatType.FilterSection.MISC_FILTERS) {}),

    RARITY       (new QueryStatType<String> (null, "rarity",   QueryStatType.FilterSection.TYPE_FILTERS) {});

    private final QueryStatType<?> type;

    QueryStat(QueryStatType<?> type) { this.type = type; }

    public QueryStatType<?> getType() { return type; }
}
