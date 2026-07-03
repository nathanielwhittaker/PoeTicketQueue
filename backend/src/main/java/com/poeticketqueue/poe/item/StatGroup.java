package com.poeticketqueue.poe.item;

import java.io.Serializable;
import java.util.List;

public class StatGroup implements Serializable {

    private StatGroupType type;
    private Integer countMin;
    private Integer countMax;
    private List<Stat> filters;

    public StatGroup() {}

    public StatGroup(StatGroupType type, Integer countMin, Integer countMax, List<Stat> filters) {
        this.type = type;
        this.countMin = countMin;
        this.countMax = countMax;
        this.filters = filters;
    }

    public StatGroupType getType()      { return type; }
    public Integer getCountMin()        { return countMin; }
    public Integer getCountMax()        { return countMax; }
    public List<Stat> getFilters()      { return filters; }
}
